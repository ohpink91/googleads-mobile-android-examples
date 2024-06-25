/*
 * Copyright 2024 Google LLC
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

package com.google.android.gms.example.jetpackcomposedemo.composables

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * A CompositionLocal that can provide a `NativeAdView` to ad attributes such as `NativeHeadline`.
 */
internal val LocalNativeAdView = staticCompositionLocalOf<NativeAdView?> { null }

/**
 * This is the Compose wrapper for a NativeAdView.
 *
 * @param nativeAdState The NativeAdState object containing ad configuration.
 * @param modifier The modifier to apply to the banner ad.
 * @param modifier modify the native ad view container.
 * @param nativeAdResult Unit which returns the loaded native ad.
 * @param content A composable function that defines the rest of the native ad view's elements.
 */
@Composable
fun NativeAdView(
  nativeAdState: NativeAdState,
  modifier: Modifier = Modifier,
  nativeAdResult: (NativeAd) -> Unit,
  content: @Composable () -> Unit,
) {
  val localContext = LocalContext.current
  val nativeAdView = remember { NativeAdView(localContext).apply { id = View.generateViewId() } }
  var lastNativeAd by remember { mutableStateOf<NativeAd?>(null) }

  AndroidView(
    factory = {
      val adLoader = AdLoader.Builder(localContext, nativeAdState.adUnitId)
      if (nativeAdState.nativeAdOptions != null) {
        adLoader.withNativeAdOptions(nativeAdState.nativeAdOptions)
      }
      adLoader.withAdListener(
        object : AdListener() {
          override fun onAdFailedToLoad(error: LoadAdError) {
            nativeAdState.onAdFailedToLoad?.invoke(error)
          }

          override fun onAdLoaded() {
            nativeAdState.onAdLoaded?.invoke()
          }

          override fun onAdClicked() {
            nativeAdState.onAdClicked?.invoke()
          }

          override fun onAdClosed() {
            nativeAdState.onAdClosed?.invoke()
          }

          override fun onAdImpression() {
            nativeAdState.onAdImpression?.invoke()
          }

          override fun onAdOpened() {
            nativeAdState.onAdOpened?.invoke()
          }

          override fun onAdSwipeGestureClicked() {
            nativeAdState.onAdSwipeGestureClicked?.invoke()
          }
        }
      )

      adLoader.forNativeAd { nativeAd ->
        // Destroy old native ad assets to prevent memory leaks.
        lastNativeAd?.destroy()
        lastNativeAd = nativeAd

        // Set the native ad on the native ad view.
        nativeAdView.setNativeAd(nativeAd)
        nativeAdResult(nativeAd)
      }
      adLoader.build().loadAd(nativeAdState.adRequest)

      nativeAdView.apply {
        layoutParams =
          ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
          )
        addView(
          ComposeView(context).apply {
            layoutParams =
              ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
              )
            setContent {
              // Set `nativeAdView` as the current LocalNativeAdView so that
              // `content` can access the `NativeAdView` via `LocalNativeAdView.current`.
              // This would allow ad attributes (such as `NativeHeadline`) to attribute
              // its contained View subclass via setter functions (e.g. nativeAdView.headlineView =
              // view)
              CompositionLocalProvider(LocalNativeAdView provides nativeAdView) { content.invoke() }
            }
          }
        )
      }
    },
    modifier = modifier,
  )

  DisposableEffect(Unit) {
    onDispose {
      // Destroy old native ad assets to prevent memory leaks.
      lastNativeAd?.destroy()
      lastNativeAd = null
    }
  }
}

/**
 * The ComposeWrapper container for a advertiserView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdAdvertiserView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  AndroidView(
    factory = { context ->
      ComposeView(context).apply {
        id = View.generateViewId()
        setContent(content)
        nativeAdView.advertiserView = this
      }
    },
    modifier = modifier,
    update = { view -> view.setContent(content) },
  )
}

/**
 * The ComposeWrapper container for a bodyView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdBodyView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.bodyView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper container for a callToActionView inside a NativeAdView. This composable must
 * be invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdCallToActionView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.callToActionView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper for a adChoicesView inside a NativeAdView. This composable must be invoked
 * from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 */
@Composable
fun NativeAdChoicesView(modifier: Modifier = Modifier) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  AndroidView(
    factory = {
      AdChoicesView(localContext).apply {
        minimumWidth = 15
        minimumHeight = 15
      }
    },
    update = { view -> nativeAdView.adChoicesView = view },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper container for a headlineView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdHeadlineView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.headlineView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper container for a iconView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdIconView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.iconView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper for a mediaView inside a NativeAdView. This composable must be invoked from
 * within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 */
@Composable
fun NativeAdMediaView(modifier: Modifier = Modifier) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  AndroidView(
    factory = { MediaView(localContext) },
    update = { view -> nativeAdView.mediaView = view },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper container for a priceView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdPriceView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.priceView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper container for a starRatingView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdStarRatingView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.starRatingView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}

/**
 * The ComposeWrapper container for a storeView inside a NativeAdView. This composable must be
 * invoked from within a `NativeAdView`.
 *
 * @param modifier modify the native ad view element.
 * @param content A composable function that defines the content of this native asset.
 */
@Composable
fun NativeAdStoreView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
  val localContext = LocalContext.current
  val localComposeView = remember { ComposeView(localContext).apply { id = View.generateViewId() } }
  AndroidView(
    factory = {
      nativeAdView.storeView = localComposeView
      localComposeView.apply { setContent(content) }
    },
    modifier = modifier,
  )
}
