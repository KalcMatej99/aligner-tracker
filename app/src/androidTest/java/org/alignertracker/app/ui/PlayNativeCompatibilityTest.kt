package org.alignertracker.app.ui

import android.system.Os
import android.system.OsConstants
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Run explicitly with -e require16kb true on the dedicated 16 KB emulator. */
class PlayNativeCompatibilityTest {
    @Test
    fun nativeLibrariesLoadOn16KbRuntime() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("require16kb") == "true")
        assertEquals(16384L, Os.sysconf(OsConstants._SC_PAGESIZE))
        System.loadLibrary("androidx.graphics.path")
        System.loadLibrary("datastore_shared_counter")
    }
}
