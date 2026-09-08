package org.alignertracker.app.data

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned portable envelope; cryptography is provided by platform JCA implementations. */
object EncryptedBackup {
    private val magic = byteArrayOf(0x41, 0x54, 0x42, 0x4b, 1)
    private const val iterations = 600_000
    private const val saltSize = 16
    private const val nonceSize = 12
    private const val headerSize = 5 + 4 + saltSize + nonceSize
    const val MAX_PLAINTEXT_BYTES = 32 * 1024 * 1024
    const val MAX_FILE_BYTES = MAX_PLAINTEXT_BYTES + headerSize + 16

    fun isEncrypted(bytes: ByteArray): Boolean = bytes.size >= 4 && bytes.take(4) == magic.take(4)

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        require(plaintext.size <= MAX_PLAINTEXT_BYTES) { "Backup is too large." }
        require(password.size in 12..1024) { "Use a password of 12 to 1024 characters." }
        val random = SecureRandom()
        val salt = ByteArray(saltSize).also(random::nextBytes)
        val nonce = ByteArray(nonceSize).also(random::nextBytes)
        val header =
            ByteBuffer.allocate(headerSize)
                .put(magic)
                .putInt(iterations)
                .put(salt)
                .put(nonce)
                .array()
        val key = derive(password, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, nonce),
            )
            cipher.updateAAD(header)
            return header + cipher.doFinal(plaintext)
        } finally {
            key.fill(0)
        }
    }

    fun decrypt(file: ByteArray, password: CharArray): ByteArray {
        require(file.size in (headerSize + 16)..MAX_FILE_BYTES) {
            "Invalid or oversized encrypted backup."
        }
        require(file.copyOfRange(0, magic.size).contentEquals(magic)) {
            "Unsupported encrypted backup version."
        }
        require(password.size in 1..1024) { "Enter the backup password (at most 1024 characters)." }
        val header = file.copyOfRange(0, headerSize)
        val input = ByteBuffer.wrap(header).apply { position(magic.size) }
        // The envelope version fixes the work factor: hostile files cannot select unbounded work.
        require(input.int == iterations) { "Unsupported backup key derivation settings." }
        val salt = ByteArray(saltSize).also(input::get)
        val nonce = ByteArray(nonceSize).also(input::get)
        val key = derive(password, salt)
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, nonce),
            )
            cipher.updateAAD(header)
            return cipher.doFinal(file, headerSize, file.size - headerSize)
        } catch (_: GeneralSecurityException) {
            // Authenticated encryption cannot distinguish a wrong password from damaged content.
            throw IllegalArgumentException(
                "The password is incorrect or the backup is damaged. Your existing data is unchanged."
            )
        } finally {
            key.fill(0)
        }
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
