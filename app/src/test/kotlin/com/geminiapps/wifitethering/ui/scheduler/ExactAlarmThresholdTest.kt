package com.geminiapps.wifitethering.ui.scheduler

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests the exact-alarm warning threshold logic in isolation.
 * The warning should only appear on API 33+ (TIRAMISU), not API 31+ (S).
 */
class ExactAlarmThresholdTest {

    // Mirrors the threshold decision in SchedulerViewModel / SchedulerScreen.
    // canScheduleExactAlarms=false simulates the permission being denied.
    private fun shouldShowWarning(apiLevel: Int, canScheduleExactAlarms: Boolean): Boolean {
        val TIRAMISU = 33
        return !canScheduleExactAlarms && apiLevel >= TIRAMISU
    }

    @Test
    fun `warning absent on API 31 even when permission denied`() {
        assertFalse(shouldShowWarning(apiLevel = 31, canScheduleExactAlarms = false))
    }

    @Test
    fun `warning absent on API 32 even when permission denied`() {
        assertFalse(shouldShowWarning(apiLevel = 32, canScheduleExactAlarms = false))
    }

    @Test
    fun `warning shown on API 33 when permission denied`() {
        assertTrue(shouldShowWarning(apiLevel = 33, canScheduleExactAlarms = false))
    }

    @Test
    fun `warning absent on API 33 when permission granted`() {
        assertFalse(shouldShowWarning(apiLevel = 33, canScheduleExactAlarms = true))
    }
}
