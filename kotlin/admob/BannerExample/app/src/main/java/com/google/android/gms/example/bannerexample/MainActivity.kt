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
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.example.bannerexample.databinding.ActivityMainBinding
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MainActivity"

/** Main Activity. Inflates main activity xml and child fragments. */
class MainActivity : AppCompatActivity() {

  private val isMobileAdsInitializeCalled = AtomicBoolean(false)
  private val initialLayoutComplete = AtomicBoolean(false)
  private lateinit var binding: ActivityMainBinding
  private lateinit var adView: AdView
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
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    adView = AdView(this)
    binding.adViewContainer.addView(adView)

    // Log the Mobile Ads SDK version.
    Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion())

    googleMobileAdsConsentManager = GoogleMobileAdsConsentManager.getInstance(applicationContext)
    googleMobileAdsConsentManager.gatherConsent(this) { error ->
      if (error != null) {
        // Consent not obtained in current session.
        Log.d(TAG, "${error.errorCode}: ${error.message}")
      }

      if (googleMobileAdsConsentManager.canRequestAds) {
        initializeMobileAdsSdk()
      }

      if (googleMobileAdsConsentManager.isPrivacyOptionsRequired) {
        // Regenerate the options menu to include a privacy setting.
        invalidateOptionsMenu()
      }
    }

    // This sample attempts to load ads using consent obtained in the previous session.
    if (googleMobileAdsConsentManager.canRequestAds) {
      initializeMobileAdsSdk()
    }

    // Since we're loading the banner based on the adContainerView size, we need to wait until this
    // view is laid out before we can get the width.
    binding.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
      if (!initialLayoutComplete.getAndSet(true) && googleMobileAdsConsentManager.canRequestAds) {
        loadBanner()
      }
    }

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
    adView.pause()
    super.onPause()
  }

  /** Called when returning to the activity. */
  public override fun onResume() {
    super.onResume()
    adView.resume()
  }

  /** Called before the activity is destroyed. */
  public override fun onDestroy() {
    adView.destroy()
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
            googleMobileAdsConsentManager.showPrivacyOptionsForm(this@MainActivity) { formError ->
              if (formError != null) {
                Toast.makeText(this@MainActivity, formError.message, Toast.LENGTH_SHORT).show()
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
    // This is an ad unit ID for a test ad. Replace with your own banner ad unit ID.
    adView.adUnitId = "ca-app-pub-3940256099942544/9214589741"
    adView.setAdSize(adSize)

    // Create an ad request.
    val adRequest = AdRequest.Builder().build()

    // Start loading the ad in the background.
    adView.loadAd(adRequest)
  }

  private fun initializeMobileAdsSdk() {
    if (isMobileAdsInitializeCalled.getAndSet(true)) {
      return
    }

    // Initialize the Mobile Ads SDK.
    MobileAds.initialize(this) {}

    // Load an ad.
    if (initialLayoutComplete.get()) {
      loadBanner()
    }
  }
}
