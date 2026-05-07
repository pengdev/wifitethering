package com.geminiapps.wifitethering.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeTest {

    @Test
    fun `AppTheme enum includes MATERIAL_YOU`() {
        val values = AppTheme.entries.map { it.name }
        assertTrue("MATERIAL_YOU missing from AppTheme", "MATERIAL_YOU" in values)
    }

    @Test
    fun `AppTheme has exactly four values`() {
        assert(AppTheme.entries.size == 4)
    }
}
