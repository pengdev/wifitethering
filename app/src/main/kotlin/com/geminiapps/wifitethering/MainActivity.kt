package com.geminiapps.wifitethering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.geminiapps.wifitethering.data.PreferencesRepository
import com.geminiapps.wifitethering.domain.BillingManager
import com.geminiapps.wifitethering.ui.AppNavHost
import com.geminiapps.wifitethering.ui.theme.AppTheme
import com.geminiapps.wifitethering.ui.theme.WifiTetheringTheme
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository
    @Inject lateinit var billingManager: BillingManager

    private var interstitialAd: InterstitialAd? = null
    private var interstitialShownThisSession = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadInterstitialIfNeeded()

        setContent {
            val appTheme by preferencesRepository.appTheme.collectAsStateWithLifecycle(
                initialValue = AppTheme.SYSTEM
            )
            WifiTetheringTheme(appTheme = appTheme) {
                AppNavHost(onRequestUpgrade = { billingManager.launchBillingFlow(this) })
            }
        }
    }

    private fun loadInterstitialIfNeeded() {
        lifecycleScope.launch {
            val isPremium = preferencesRepository.isPremium.first()
            if (isPremium) return@launch

            InterstitialAd.load(
                this@MainActivity,
                getString(R.string.admob_interstitial_id),
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        showInterstitialOnce()
                    }
                }
            )
        }
    }

    private fun showInterstitialOnce() {
        if (interstitialShownThisSession) return
        val ad = interstitialAd ?: return
        interstitialShownThisSession = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
            }
        }
        ad.show(this)
    }
}
