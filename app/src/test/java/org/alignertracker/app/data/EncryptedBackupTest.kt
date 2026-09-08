package org.alignertracker.app.data

import org.junit.Assert.*
import org.junit.Test

class EncryptedBackupTest {
    private val password = "a long portable password".toCharArray()

    @Test
    fun roundTripUsesFreshSaltAndNonceAndSupportsUnicode() {
        val content = "private archive š č 笑".toByteArray()
        val a = EncryptedBackup.encrypt(content, password)
        val b = EncryptedBackup.encrypt(content, password)
        assertFalse(a.contentEquals(b))
        assertTrue(EncryptedBackup.isEncrypted(a))
        assertArrayEquals(content, EncryptedBackup.decrypt(a, password))
    }

    @Test
    fun wrongPasswordTamperAndTruncationNeverReturnPlaintext() {
        val file = EncryptedBackup.encrypt("sensitive history".toByteArray(), password)
        for (candidate in
            listOf(
                file.copyOf(file.size - 1),
                file.copyOf().also { it[it.lastIndex] = (it.last() + 1).toByte() },
                file.copyOf().also { it[10] = (it[10] + 1).toByte() },
            )) {
            assertThrows(IllegalArgumentException::class.java) {
                EncryptedBackup.decrypt(candidate, password)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackup.decrypt(file, "wrong".toCharArray())
        }
    }

    @Test
    fun unsupportedVersionAndWorkFactorAreRejectedBeforeKdf() {
        val file = EncryptedBackup.encrypt(byteArrayOf(1), password)
        for (offset in listOf(4, 5)) {
            val changed = file.copyOf().also { it[offset] = 127 }
            assertThrows(IllegalArgumentException::class.java) {
                EncryptedBackup.decrypt(changed, password)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            EncryptedBackup.encrypt(byteArrayOf(1), "short".toCharArray())
        }
    }
}
