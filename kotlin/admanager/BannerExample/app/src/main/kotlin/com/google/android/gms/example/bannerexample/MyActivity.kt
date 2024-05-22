/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.example.bannerexample

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowMetrics
import android.view.ViewTreeObserver
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.example.bannerexample.databinding.ActivityMyBinding
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MainActivity"

/** Main Activity. Inflates main activity xml and child fragments. */
class MyActivity : AppCompatActivity() {

  companion object {
    init {
      Timber.plant(Timber.DebugTree())
    }
  }

  private val isMobileAdsInitializeCalled = AtomicBoolean(false)
  private val initialLayoutComplete = AtomicBoolean(false)
  private lateinit var binding: ActivityMyBinding

  private var adView: AdManagerAdView? = null
  private lateinit var googleMobileAdsConsentManager: GoogleMobileAdsConsentManager

  // Get the ad size with screen width.
  private val adSize: AdSize
    get() {
      val displayMetrics = resources.displayMetrics
      val adWidthPixels =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          val windowMetrics: WindowMetrics = this.windowManager.currentWindowMetrics
          windowMetrics.bounds.width()
        } else {
          displayMetrics.widthPixels
        }
      val density = displayMetrics.density
      val adWidth = (adWidthPixels / density).toInt()
      return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMyBinding.inflate(layoutInflater)
    setContentView(binding.root)


    // Log the Mobile Ads SDK version.
    Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

    googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
    googleMobileAdsConsentManager.gatherConsent(this) { error ->
      Timber.d(
        "gatherConsent: canRequestAds=%s isPrivacyOptionsRequired=%s error=%s",
        googleMobileAdsConsentManager.canRequestAds,
        googleMobileAdsConsentManager.isPrivacyOptionsRequired,
        error
      )
      if (error != null) {
        // Consent not obtained in current session.
        Log.d(TAG, "${error.errorCode}: ${error.message}")
      }
      initializeMobileAdsSdk()

      if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
        // Regenerate the options menu to include a privacy setting.
        invalidateOptionsMenu()
      }
    }

    // This sample attempts to load ads using consent obtained in the previous session.
    initializeMobileAdsSdk()

    // Since we're loading the banner based on the adContainerView size, we need to wait until this
    // view is laid out before we can get the width.
    binding.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener(object :
      ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        if (initialLayoutComplete.compareAndSet(false, true)) {
          binding.adViewContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
          considerLoadBanner()
        }
      }
    })

    // Set your test devices. Check your logcat output for the hashed device ID to
    // get test ads on a physical device. e.g.
    // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
    // to get test ads on this device."
    MobileAds.setRequestConfiguration(
      RequestConfiguration.Builder().setTestDeviceIds(listOf("ABCDEF012345")).build()
    )
  }

  /** Called when leaving the activity. */
  public override fun onPause() {
    adView?.pause()
    super.onPause()
  }

  /** Called when returning to the activity. */
  public override fun onResume() {
    super.onResume()
    adView?.resume()
  }

  /** Called before the activity is destroyed. */
  public override fun onDestroy() {
    destroy()
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.action_menu, menu)
    val moreMenu = menu?.findItem(R.id.action_more)
    moreMenu?.isVisible = googleMobileAdsConsentManager.isPrivacyOptionsRequired
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val menuItemView = findViewById<View>(item.itemId)
    PopupMenu(this, menuItemView).apply {
      menuInflater.inflate(R.menu.popup_menu, menu)
      show()
      setOnMenuItemClickListener { popupMenuItem ->
        when (popupMenuItem.itemId) {
          R.id.privacy_settings -> {
            // Handle changes to user consent.
            googleMobileAdsConsentManager.showPrivacyOptionsForm(this@MyActivity) { formError ->
              if (formError != null) {
                Toast.makeText(this@MyActivity, formError.message, Toast.LENGTH_SHORT).show()
              }
            }
            true
          }
          else -> false
        }
      }
      return super.onOptionsItemSelected(item)
    }
  }

  private fun loadBanner() {
    Timber.d("loadBanner: adSize=%s", adSize)
    destroy()

    val adView = AdManagerAdView(this)
    this.adView = adView

    adView.adListener = object : AdListener() {
      override fun onAdFailedToLoad(p0: LoadAdError) {
        Timber.d("onAdFailedToLoad: error=%s", p0)
      }

      override fun onAdLoaded() {
        Timber.d("onAdLoaded")
      }

      override fun onAdImpression() {
        Timber.d("onAdImpression")
      }

      override fun onAdClicked() {
        Timber.d("onAdClicked")
      }

      override fun onAdSwipeGestureClicked() {
        Timber.d("onAdSwipeGestureClicked")
      }

      override fun onAdOpened() {
        Timber.d("onAdOpened")
      }

      override fun onAdClosed() {
        Timber.d("onAdClosed")
      }
    }

    binding.adViewContainer.addView(adView)

    // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
    adView.adUnitId = "/6499/example/banner"
    adView.setAdSize(adSize)

    // Create an ad request.
    val adRequest = AdManagerAdRequest.Builder().build()

    // Start loading the ad in the background.
    adView.loadAd(adRequest)

  }

  private fun destroy() {
    Timber.d("destroy: adView=%s", adView)
    binding.adViewContainer.removeAllViews()

    adView?.destroy()
    adView = null
  }

  private fun initializeMobileAdsSdk() {
    val canRequestAds = googleMobileAdsConsentManager.canRequestAds
    Timber.d(
      "initializeMobileAdsSdk: canRequestAds=%s called=%s",
      canRequestAds, isMobileAdsInitializeCalled
    )
    if (!canRequestAds) {
      return
    }
    if (isMobileAdsInitializeCalled.compareAndSet(false, true)) {
      // Initialize the Mobile Ads SDK.
      MobileAds.initialize(this) {}
    }

    // Load an ad.
    considerLoadBanner()
  }

  override fun onStart() {
    super.onStart()
    considerLoadBanner()
  }

  override fun onStop() {
    super.onStop()
    destroy()
  }

  private fun considerLoadBanner() {
    Timber.d(
      "considerLoadBanner: initialLayoutComplete=%s canRequestAds=%s isMobileAdsInitializeCalled=%s adView=%s",
      initialLayoutComplete,
      googleMobileAdsConsentManager.canRequestAds,
      isMobileAdsInitializeCalled,
      adView
    )
    if (initialLayoutComplete.get()
      && googleMobileAdsConsentManager.canRequestAds
      && isMobileAdsInitializeCalled.get()
      && adView == null
    ) {
      loadBanner()
    }
  }


}
