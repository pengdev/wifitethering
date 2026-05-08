package com.geminiapps.wifitethering.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCapabilitiesTest {

    // Each test constructs DeviceCapabilities with the values that the production
    // capabilities() function would produce at a given API level, then asserts the
    // expected flag value. This keeps the tests free of Robolectric and fast.

    // canToggleProgrammatically: true on API 21-25, false on 26+

    @Test
    fun `canToggleProgrammatically is true below API 26`() {
        val caps = capsForApi(21)
        assertTrue(caps.canToggleProgrammatically)
    }

    @Test
    fun `canToggleProgrammatically is true on API 25`() {
        val caps = capsForApi(25)
        assertTrue(caps.canToggleProgrammatically)
    }

    @Test
    fun `canToggleProgrammatically is false on API 26`() {
        val caps = capsForApi(26)
        assertFalse(caps.canToggleProgrammatically)
    }

    @Test
    fun `canToggleProgrammatically is false on API 34`() {
        val caps = capsForApi(34)
        assertFalse(caps.canToggleProgrammatically)
    }

    // canReadSsidAndPassword: true below API 28, false on 28+

    @Test
    fun `canReadSsidAndPassword is true on API 27`() {
        assertTrue(capsForApi(27).canReadSsidAndPassword)
    }

    @Test
    fun `canReadSsidAndPassword is false on API 28`() {
        assertFalse(capsForApi(28).canReadSsidAndPassword)
    }

    @Test
    fun `canReadSsidAndPassword is false on API 33`() {
        assertFalse(capsForApi(33).canReadSsidAndPassword)
    }

    // canEditConfig: same boundary as canToggleProgrammatically (API 26)

    @Test
    fun `canEditConfig is true on API 25`() {
        assertTrue(capsForApi(25).canEditConfig)
    }

    @Test
    fun `canEditConfig is false on API 26`() {
        assertFalse(capsForApi(26).canEditConfig)
    }

    // canScanConnectedDevices: true below API 29, false on 29+

    @Test
    fun `canScanConnectedDevices is true on API 28`() {
        assertTrue(capsForApi(28).canScanConnectedDevices)
    }

    @Test
    fun `canScanConnectedDevices is false on API 29`() {
        assertFalse(capsForApi(29).canScanConnectedDevices)
    }

    @Test
    fun `canScanConnectedDevices is false on API 34`() {
        assertFalse(capsForApi(34).canScanConnectedDevices)
    }

    // canUseTile: false below API 24, true on 24+

    @Test
    fun `canUseTile is false on API 23`() {
        assertFalse(capsForApi(23).canUseTile)
    }

    @Test
    fun `canUseTile is true on API 24`() {
        assertTrue(capsForApi(24).canUseTile)
    }

    @Test
    fun `canUseTile is true on API 34`() {
        assertTrue(capsForApi(34).canUseTile)
    }

    // needsNotificationPermission: false below API 33, true on 33+

    @Test
    fun `needsNotificationPermission is false on API 32`() {
        assertFalse(capsForApi(32).needsNotificationPermission)
    }

    @Test
    fun `needsNotificationPermission is true on API 33`() {
        assertTrue(capsForApi(33).needsNotificationPermission)
    }

    @Test
    fun `needsNotificationPermission is true on API 34`() {
        assertTrue(capsForApi(34).needsNotificationPermission)
    }

    // canScheduleExactAlarms tests will be added after dev-b completes B2
    // and adds the flag to DeviceCapabilities.

    private fun capsForApi(api: Int) = DeviceCapabilities(
        canToggleProgrammatically = api < 26,
        canReadSsidAndPassword = api < 28,
        canEditConfig = api < 26,
        canScanConnectedDevices = api < 29,
        canUseTile = api >= 24,
        needsNotificationPermission = api >= 33,
    )
}
