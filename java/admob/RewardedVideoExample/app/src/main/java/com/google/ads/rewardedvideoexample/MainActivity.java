package com.google.ads.rewardedvideoexample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Main Activity. Inflates main activity xml. */
@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

  // Check your logcat output for the test device hashed ID e.g.
  // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
  // to get test ads on this device" or
  // "Use new ConsentDebugSettings.Builder().addTestDeviceHashedId("ABCDEF012345") to set this as
  // a debug device".
  public static final String TEST_DEVICE_HASHED_ID = "ABCDEF012345";

  private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
  private static final long COUNTER_TIME = 10;
  private static final int GAME_OVER_REWARD = 1;
  private static final String TAG = "MainActivity";
  private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);

  private int coinCount;
  private TextView coinCountText;
  private CountDownTimer countDownTimer;
  private boolean gameOver;
  private boolean gamePaused;

  private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;
  private RewardedAd rewardedAd;
  private Button retryButton;
  private Button showVideoButton;
  private long timeRemaining;
  boolean isLoading;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Log the Mobile Ads SDK version.
    Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion());

    googleMobileAdsConsentManager =
        GoogleMobileAdsConsentManager.getInstance(getApplicationContext());
    googleMobileAdsConsentManager.gatherConsent(
        this,
        consentError -> {
          if (consentError != null) {
            // Consent not obtained in current session.
            Log.w(
                TAG,
                String.format("%s: %s", consentError.getErrorCode(), consentError.getMessage()));
          }

          startGame();

          if (googleMobileAdsConsentManager.canRequestAds()) {
            initializeMobileAdsSdk();
          }

          if (googleMobileAdsConsentManager.isPrivacyOptionsRequired()) {
            // Regenerate the options menu to include a privacy setting.
            invalidateOptionsMenu();
          }
        });

    // This sample attempts to load ads using consent obtained in the previous session.
    if (googleMobileAdsConsentManager.canRequestAds()) {
      initializeMobileAdsSdk();
    }

    // Create the "retry" button, which tries to show a rewarded ad between game plays.
    retryButton = findViewById(R.id.retry_button);
    retryButton.setVisibility(View.INVISIBLE);
    retryButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            startGame();
            if (rewardedAd != null && !isLoading && googleMobileAdsConsentManager.canRequestAds()) {
              loadRewardedAd();
            }
          }
        });

    // Create the "show" button, which shows a rewarded video if one is loaded.
    showVideoButton = findViewById(R.id.show_video_button);
    showVideoButton.setVisibility(View.INVISIBLE);
    showVideoButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            showRewardedVideo();
          }
        });

    // Display current coin count to user.
    coinCountText = findViewById(R.id.coin_count_text);
    coinCount = 0;
    coinCountText.setText("Coins: " + coinCount);
  }

  @Override
  public void onPause() {
    super.onPause();
    pauseGame();
  }

  @Override
  public void onResume() {
    super.onResume();
    resumeGame();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.action_menu, menu);
    MenuItem moreMenu = menu.findItem(R.id.action_more);
    moreMenu.setVisible(googleMobileAdsConsentManager.isPrivacyOptionsRequired());
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    View menuItemView = findViewById(item.getItemId());
    PopupMenu popup = new PopupMenu(this, menuItemView);
    popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());
    popup
        .getMenu()
        .findItem(R.id.privacy_settings)
        .setVisible(googleMobileAdsConsentManager.isPrivacyOptionsRequired());
    popup.show();
    popup.setOnMenuItemClickListener(
        popupMenuItem -> {
          if (popupMenuItem.getItemId() == R.id.privacy_settings) {
            // Handle changes to user consent.
            googleMobileAdsConsentManager.showPrivacyOptionsForm(
                this,
                formError -> {
                  if (formError != null) {
                    Toast.makeText(this, formError.getMessage(), Toast.LENGTH_SHORT).show();
                  }
                });
            return true;
          } else if (popupMenuItem.getItemId() == R.id.ad_inspector) {
            MobileAds.openAdInspector(
                this,
                error -> {
                  // Error will be non-null if ad inspector closed due to an error.
                  if (error != null) {
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                  }
                });
            return true;
          }
          return false;
        });
    return super.onOptionsItemSelected(item);
  }

  private void pauseGame() {
    if (gameOver || gamePaused) {
      return;
    }
    countDownTimer.cancel();
    gamePaused = true;
  }

  private void resumeGame() {
    if (gameOver || !gamePaused) {
      return;
    }
    createTimer(timeRemaining);
    gamePaused = false;
  }

  private void loadRewardedAd() {
    if (rewardedAd == null) {
      isLoading = true;
      AdRequest adRequest = new AdRequest.Builder().build();
      RewardedAd.load(
          this,
          AD_UNIT_ID,
          adRequest,
          new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
              // Handle the error.
              Log.d(TAG, loadAdError.getMessage());
              rewardedAd = null;
              MainActivity.this.isLoading = false;
              Toast.makeText(MainActivity.this, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
              MainActivity.this.rewardedAd = rewardedAd;
              Log.d(TAG, "onAdLoaded");
              MainActivity.this.isLoading = false;
              Toast.makeText(MainActivity.this, "onAdLoaded", Toast.LENGTH_SHORT).show();
            }
          });
    }
  }

  private void addCoins(int coins) {
    coinCount += coins;
    coinCountText.setText("Coins: " + coinCount);
  }

  private void startGame() {
    // Hide the retry button, load the ad, and start the timer.
    retryButton.setVisibility(View.INVISIBLE);
    showVideoButton.setVisibility(View.INVISIBLE);
    createTimer(COUNTER_TIME);
    gamePaused = false;
    gameOver = false;
  }

  // Create the game timer, which counts down to the end of the level
  // and shows the "retry" button.
  private void createTimer(long time) {
    final TextView textView = findViewById(R.id.timer);
    if (countDownTimer != null) {
      countDownTimer.cancel();
    }
    countDownTimer =
        new CountDownTimer(time * 1000, 50) {
          @Override
          public void onTick(long millisUnitFinished) {
            timeRemaining = ((millisUnitFinished / 1000) + 1);
            textView.setText("seconds remaining: " + timeRemaining);
          }

          @Override
          public void onFinish() {
            if (rewardedAd != null) {
              showVideoButton.setVisibility(View.VISIBLE);
            }
            textView.setText("You Lose!");
            addCoins(GAME_OVER_REWARD);
            retryButton.setVisibility(View.VISIBLE);
            gameOver = true;
          }
        };
    countDownTimer.start();
  }

  private void showRewardedVideo() {
    if (rewardedAd == null) {
      Log.d("TAG", "The rewarded ad wasn't ready yet.");
      return;
    }
    showVideoButton.setVisibility(View.INVISIBLE);

    rewardedAd.setFullScreenContentCallback(
        new FullScreenContentCallback() {
          @Override
          public void onAdShowedFullScreenContent() {
            // Called when ad is shown.
            Log.d(TAG, "onAdShowedFullScreenContent");
            Toast.makeText(MainActivity.this, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT)
                .show();
          }

          @Override
          public void onAdFailedToShowFullScreenContent(AdError adError) {
            // Called when ad fails to show.
            Log.d(TAG, "onAdFailedToShowFullScreenContent");
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            rewardedAd = null;
            Toast.makeText(
                    MainActivity.this, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT)
                .show();
          }

          @Override
          public void onAdDismissedFullScreenContent() {
            // Called when ad is dismissed.
            // Don't forget to set the ad reference to null so you
            // don't show the ad a second time.
            rewardedAd = null;
            Log.d(TAG, "onAdDismissedFullScreenContent");
            Toast.makeText(MainActivity.this, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT)
                .show();
            if (googleMobileAdsConsentManager.canRequestAds()) {
              // Preload the next rewarded ad.
              MainActivity.this.loadRewardedAd();
            }
          }
        });
    Activity activityContext = MainActivity.this;
    rewardedAd.show(
        activityContext,
        new OnUserEarnedRewardListener() {
          @Override
          public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
            // Handle the reward.
            Log.d("TAG", "The user earned the reward.");
            addCoins(coinCount);
          }
        });
  }

  private void initializeMobileAdsSdk() {
    if (isMobileAdsInitializeCalled.getAndSet(true)) {
      return;
    }

    // Set your test devices.
    MobileAds.setRequestConfiguration(
        new RequestConfiguration.Builder()
            .setTestDeviceIds(Arrays.asList(TEST_DEVICE_HASHED_ID))
            .build());

    new Thread(
            () -> {
              // Initialize the Google Mobile Ads SDK on a background thread.
              MobileAds.initialize(this, initializationStatus -> {});

              // Load an ad on the main thread.
              runOnUiThread(() -> loadRewardedAd());
            })
        .start();
  }
}
