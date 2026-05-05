package com.geminiapps.wifitethering.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Ignore
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Verifies production credentials are wired before releasing to the Play Store.
 *
 * These tests parse strings.xml directly (no Android context required) so they
 * run as plain JVM unit tests on any CI machine.
 */
class ProductionCredentialsTest {

    private val stringsXml = loadStringsXml()

    // --- billing -----------------------------------------------------------

    @Test
    fun `premium_product_id must be premium_monthly`() {
        val id = stringsXml["premium_product_id"]
        assertEquals(
            "premium_product_id must match the Play Console in-app product ID",
            "premium_monthly",
            id,
        )
    }

    // --- admob -------------------------------------------------------------
    //
    // MANUAL ACTION REQUIRED: These tests are @Ignore until real AdMob IDs are
    // obtained from https://admob.google.com for the app com.geminiapps.wifitethering.
    //
    // Steps:
    //   1. Create/locate AdMob app entry for com.geminiapps.wifitethering
    //   2. Create a Banner ad unit and an Interstitial ad unit
    //   3. Replace the three strings below in res/values/strings.xml:
    //        admob_app_id         — from the app overview page (format ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX)
    //        admob_banner_id      — ad unit ID for the Banner unit
    //        admob_interstitial_id — ad unit ID for the Interstitial unit
    //   4. Remove the @Ignore annotations from these three tests and re-run.

    private val googleTestPublisher = "ca-app-pub-3940256099942544"

    @Ignore("MANUAL: replace test AdMob app ID with real ID before release — see comment above")
    @Test
    fun `admob_app_id must not be the Google test publisher ID`() {
        val appId = stringsXml["admob_app_id"] ?: error("admob_app_id missing from strings.xml")
        assertFalse(
            "admob_app_id is still the Google test ID ($appId). Replace with your real AdMob app ID.",
            appId.startsWith(googleTestPublisher),
        )
    }

    @Ignore("MANUAL: replace test AdMob banner ID with real ID before release — see comment above")
    @Test
    fun `admob_banner_id must not be the Google test publisher ID`() {
        val bannerId = stringsXml["admob_banner_id"] ?: error("admob_banner_id missing from strings.xml")
        assertFalse(
            "admob_banner_id is still a Google test ad unit ($bannerId). Replace with your real banner ad unit ID.",
            bannerId.startsWith(googleTestPublisher),
        )
    }

    @Ignore("MANUAL: replace test AdMob interstitial ID with real ID before release — see comment above")
    @Test
    fun `admob_interstitial_id must not be the Google test publisher ID`() {
        val interstitialId = stringsXml["admob_interstitial_id"] ?: error("admob_interstitial_id missing from strings.xml")
        assertFalse(
            "admob_interstitial_id is still a Google test ad unit ($interstitialId). Replace with your real interstitial ad unit ID.",
            interstitialId.startsWith(googleTestPublisher),
        )
    }

    // --- helpers -----------------------------------------------------------

    private fun loadStringsXml(): Map<String, String> {
        val file = checkNotNull(
            ProductionCredentialsTest::class.java.classLoader
                ?.getResourceAsStream("values/strings.xml")
        ) { "strings.xml not found on test classpath — check sourceSets configuration" }

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            map[el.getAttribute("name")] = el.textContent.trim()
        }
        return map
    }
}
