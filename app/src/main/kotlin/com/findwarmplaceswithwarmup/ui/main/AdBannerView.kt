package com.findwarmplaceswithwarmup.ui.main

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.findwarmplaceswithwarmup.data.AppLanguage
import com.findwarmplaceswithwarmup.data.MonetizationConfig
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

fun buildGdprAdRequest(personalizedAds: Boolean): AdRequest {
  val builder = AdRequest.Builder()
  if (!personalizedAds) {
    val extras = Bundle().apply {
      putString("npa", "1")
    }
    builder.addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
  }
  return builder.build()
}

@Composable
fun AdBannerView(
  isAdFree: Boolean,
  personalizedAds: Boolean,
  language: AppLanguage,
  onAdFreeClick: () -> Unit,
  onPrivacyClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  if (isAdFree) return

  val isEn = language == AppLanguage.EN
  var isAdLoaded by remember { mutableStateOf(false) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding(),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
    tonalElevation = 4.dp,
    shadowElevation = 3.dp,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onPrivacyClick() },
        ) {
          Text(
            text = if (isEn) "AD • PRIVACY" else "WERBUNG • DATENSCHUTZ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
          )
        }
        Text(
          text = if (isEn) "☕ Hot Drink / Ad-Free" else "☕ Heißgetränk / Werbefrei",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onAdFreeClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        contentAlignment = Alignment.Center,
      ) {
        // Fallback placeholder strip while AdMob connects or loads
        if (!isAdLoaded) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
              .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Text(
              text = if (isEn) "☕ WarmUp – Discover cozy winter places & cafés" else "☕ WarmUp – Finde gemütliche Winterorte & Cafés",
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
          }
        }

        // Live Google AdMob AdView
        AndroidView(
          modifier = Modifier.fillMaxWidth(),
          factory = { context ->
            AdView(context).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
              )
              adUnitId = MonetizationConfig.ADMOB_BANNER_AD_UNIT_ID

              val displayMetrics = context.resources.displayMetrics
              val adWidthPixels = displayMetrics.widthPixels.toFloat()
              val density = displayMetrics.density
              val adWidth = (adWidthPixels / density).toInt().coerceAtLeast(320)
              try {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth))
              } catch (_: Exception) {
                setAdSize(AdSize.BANNER)
              }

              adListener = object : AdListener() {
                override fun onAdLoaded() {
                  isAdLoaded = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                  // If adaptive failed, try standard 320x50 banner once
                  try {
                    setAdSize(AdSize.BANNER)
                    loadAd(buildGdprAdRequest(personalizedAds))
                  } catch (_: Exception) {}
                }
              }

              loadAd(buildGdprAdRequest(personalizedAds))
            }
          },
        )
      }
    }
  }
}

class InterstitialAdManager(private val context: Context) {
  private var interstitialAd: InterstitialAd? = null
  private var isLoading = false
  private var lastShownTime = 0L
  private var actionCounter = 0

  fun loadAd(personalizedAds: Boolean) {
    if (isLoading || interstitialAd != null) return
    isLoading = true

    InterstitialAd.load(
      context,
      MonetizationConfig.ADMOB_INTERSTITIAL_AD_UNIT_ID,
      buildGdprAdRequest(personalizedAds),
      object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) {
          interstitialAd = ad
          isLoading = false
        }

        override fun onAdFailedToLoad(error: LoadAdError) {
          interstitialAd = null
          isLoading = false
        }
      },
    )
  }

  fun notifyActionAndShowIfReady(
    activity: Activity,
    personalizedAds: Boolean,
    isAdFree: Boolean,
    onDismissed: () -> Unit = {},
  ) {
    if (isAdFree) {
      onDismissed()
      return
    }

    actionCounter++
    val now = System.currentTimeMillis()
    val timeElapsed = now - lastShownTime >= MonetizationConfig.INTERSTITIAL_MIN_INTERVAL_MS
    val thresholdReached = actionCounter >= MonetizationConfig.INTERSTITIAL_ACTION_THRESHOLD

    val ad = interstitialAd
    if (ad != null && timeElapsed && thresholdReached) {
      ad.fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
          interstitialAd = null
          lastShownTime = System.currentTimeMillis()
          actionCounter = 0
          loadAd(personalizedAds)
          onDismissed()
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
          interstitialAd = null
          loadAd(personalizedAds)
          onDismissed()
        }
      }
      ad.show(activity)
    } else {
      if (ad == null) {
        loadAd(personalizedAds)
      }
      onDismissed()
    }
  }
}
