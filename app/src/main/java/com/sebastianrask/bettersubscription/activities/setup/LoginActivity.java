package com.sebastianrask.bettersubscription.activities.setup;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rey.material.widget.ProgressView;
import com.rey.material.widget.SnackBar;
import com.sebastianrask.bettersubscription.PocketPlaysApplication;
import com.sebastianrask.bettersubscription.R;
import com.sebastianrask.bettersubscription.activities.UsageTrackingAppCompatActivity;
import com.sebastianrask.bettersubscription.service.Service;
import com.sebastianrask.bettersubscription.service.Settings;
import com.sebastianrask.bettersubscription.tasks.GetFollowsFromDB;
import com.sebastianrask.bettersubscription.tasks.HandlerUserLoginTask;

import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;

public class LoginActivity extends UsageTrackingAppCompatActivity {
	private static GetFollowsFromDB subscriptionsTask;
	private static boolean toTransition = false, isPartOfSetup = true;
	private String LOG_TAG = "LoginActivity";
	private final String LOGIN_URL = "https://api.twitch.tv/kraken/oauth2/authorize?response_type=token&client_id=" + Service.getApplicationClientID() + "&redirect_uri=http%3A%2F%2Fnrask.net%2Foauth_authorizing&scope=user_read+chat_login+user_follows_edit+user_subscriptions";

	private boolean isWebViewShown = false,
					isWebViewHiding = false,
					hasTransitioned = false,
					isSkipping = false;

	private SupportAnimator transitionAnimationWhite = null,
							transitionAnimationBlue = null;

	private final int LOGO_ROTATION_SPEED 				= 15 * 1000; // The time it takes for the icon to rotate 360 degrees. The Higher the slower.
	private final int SNACKBAR_DURATION 				=  8 * 1000;
	private final int SHOW_WEBVIEW_ANIMATION_DURATION 	= 900;
	private final int SHOW_LOGO_ANIMATION_DURATION 		= 600;
	private final int SHOW_TEXT_ANIMATION_DURATION 		= 600;
	private final int HIDE_VIEW_ANIMATION_DURATION 		= 550;
	private final int SHOW_SUCCESS_ICON_DURATION 		= 800;
	private final int SHOW_CONTINUE_ICON_DURATION 		= 650;
	private final int REVEAL_ANIMATION_DURATION 		= 650;
	private final int HIDE_INSTRUCTIONS_DURATION		= 500;
	private final int REVEAL_ANIMATION_DELAY 			= 200;
	private final int SHOW_TEXT_ANIMATION_BASE_DELAY 	= 105;
	private final int SHOW_TEXT_ANIMATION_DELAY 		= 105;
	private final int SHOW_SNACKBAR_DELAY 				= 200;
	private final int SHOW_CONTINUE_ICON_DELAY 			= 600;
	private final int TRANSITION_DELAY					= 2000;


	private ImageView 		mGearIcon,
							mSuccessIcon,
							mContinueIcon;
	private ProgressView 	mWebViewProgress;
	private WebView 		loginWebView;
	private TextView 		mLoginTextLineOne,
							mLoginTextLineTwo,
							mSuccessMessage,
							mSkipText;
	private View 			mContinueFAB,
							mContinueFABShadow,
							mSuccessCircle,
							mSuccessCircleShadow,
							mTransitionViewWhite,
							mTransitionViewBlue;
	private FrameLayout 	mContinueFABContainer,
							mWebViewContainer;
	private RelativeLayout 	mLoginTextContainer;
	private SnackBar 		mSnackbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		trackEvent(R.string.category_read, R.string.action_login);

		mLoginTextContainer 	= (RelativeLayout) findViewById(R.id.login_text_container);
		mWebViewProgress 		= (ProgressView) findViewById(R.id.SetupProgress);
		mWebViewContainer 		= (FrameLayout) findViewById(R.id.webview_container);
		mContinueFABContainer 	= (FrameLayout) findViewById(R.id.login_continue_circle_container);
		mGearIcon 				= (ImageView) findViewById(R.id.login_icon);
		mSuccessIcon 			= (ImageView) findViewById(R.id.login_icon_done);
		mContinueIcon			= (ImageView) findViewById(R.id.forward_arrow);
		mLoginTextLineOne 		= (TextView) findViewById(R.id.login_text_line_one);
		mLoginTextLineTwo 		= (TextView) findViewById(R.id.login_text_line_two);
		mSuccessMessage			= (TextView) findViewById(R.id.login_success_message);
		mSkipText				= (TextView) findViewById(R.id.skip_text);
		loginWebView 			= (WebView) findViewById(R.id.login_webview);
		mContinueFAB 			= findViewById(R.id.login_continue_circle);
		mContinueFABShadow 		= findViewById(R.id.login_continue_circle_shadow);
		mSuccessCircle 			= findViewById(R.id.login_success_circle);
		mSuccessCircleShadow	= findViewById(R.id.login_success_shadow);
		mTransitionViewWhite 	= findViewById(R.id.transition_view);
		mTransitionViewBlue 	= findViewById(R.id.transition_view_blue);

		mSuccessMessage		.setVisibility(View.INVISIBLE);
		mContinueIcon		.setVisibility(View.INVISIBLE);
		mSuccessCircleShadow.setVisibility(View.INVISIBLE);
		mSuccessIcon		.setVisibility(View.INVISIBLE);
		mSuccessCircle		.setVisibility(View.INVISIBLE);
		mLoginTextLineOne	.setVisibility(View.INVISIBLE);
		mLoginTextLineTwo	.setVisibility(View.INVISIBLE);
		loginWebView		.setVisibility(View.INVISIBLE);
		mGearIcon			.setVisibility(View.INVISIBLE);
		mTransitionViewBlue	.setVisibility(View.INVISIBLE);
		mTransitionViewWhite.setVisibility(View.INVISIBLE);
		mSkipText			.setVisibility(View.INVISIBLE);

		checkSetupType();

		float textPosition = (int) (2.5 * (getScreenHeight() / 5));
		mLoginTextContainer	.setY(textPosition);
		mSuccessMessage		.setY(textPosition);
		mContinueFABContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showLoginView();

				if (PocketPlaysApplication.isCrawlerUpdate)
					showSkippingAnimation();
			}
		});

		Service.bringToBack(mTransitionViewWhite);
		Service.bringToBack(mTransitionViewBlue);
		mLoginTextLineOne		.bringToFront();
		mLoginTextLineTwo		.bringToFront();

		initSkipView();
		initSnackbar();
		initLoginView();
		showLogoAnimations();
		showTextLineAnimations(mLoginTextLineOne, 1);
		showTextLineAnimations(mLoginTextLineTwo, 2);
		showTextLineAnimations(mSkipText, 2);

		AnimationSet animationSet = getContinueIconAnimations(270);
		animationSet.setStartOffset(SHOW_CONTINUE_ICON_DELAY);
		animationSet.start();
	}

	/**
	 * initiates the skip view.
	 */
	private void initSkipView() {
		mSkipText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				trackEvent(R.string.category_click, R.string.action_skip);
				isSkipping = true;
				showSkippingAnimation();
			}
		});
	}

	/**
	 * Checks if the login is part of setup, a relogin or token revalidation
	 * Sets states correctly depending on result
	 */
	private void checkSetupType() {
		if (getIntent().hasExtra(getString(R.string.login_intent_part_of_setup))) {
			isPartOfSetup = getIntent().getBooleanExtra(getString(R.string.login_intent_part_of_setup), true);

			if (getIntent().hasExtra(getString(R.string.login_intent_token_not_valid)) && getIntent().getBooleanExtra(getString(R.string.login_intent_token_not_valid), false)) {
				mLoginTextLineOne.setText(getString(R.string.login_unvalid_token_text_line_one));
				mLoginTextLineTwo.setText(getString(R.string.login_unvalid_token_text_line_two));
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if(transitionAnimationWhite != null && hasTransitioned) {
			showReverseTransitionAnimation();
		}
	}

	@Override
	public void onBackPressed() {
		if (isWebViewShown && !isWebViewHiding) {
			hideLoginView();
		} else {
			toTransition = false;
			hideAllViews().setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					LoginActivity.super.onBackPressed();
					// We don't want a transition when going back. The activities handle the animation themselves.
					overridePendingTransition(0, 0);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
		}
	}
	public static boolean hasLoadedFollows() {
		if(subscriptionsTask != null && subscriptionsTask.isFinished()) {
			return true;
		} else {
			return false;
		}
	}
	public void handleLoginSuccess() {
		new Settings(this).setLogin(true);

		toTransition = true;
		showSuccessAnimation();
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				if(toTransition) {
					showTransitionAnimation();
				}
			}
		}, TRANSITION_DELAY);

		mContinueFABContainer.setClickable(false);
		// Gets the follows that already exist in the database. Then Checks if any of them are online.
		// THEN it retrieves the user's follows from Twitch.
		// It is better to it this way, as this will give us the opportunity to use this Activity again,
		// if the user later removes Pocket Play Authentication from Twitch and we need the user to sign in again
		// OR if the user signs in to a new account.

		// Seb you wonderful man. - Seb from the future
		subscriptionsTask = new GetFollowsFromDB();
		subscriptionsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext());
	}

	public void handleLoginFailure() {

	}

	public void handleNoInternet() {
		if(mSnackbar.isShown()) {
			mSnackbar.dismiss();
			mSnackbar.clearAnimation();
		}

		if(isWebViewShown) {
			hideLoginView();
		}

		mContinueFABContainer.setClickable(false);

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mSnackbar
						.duration(0)
						.text(getResources().getString(R.string.login_user_no_internet))
						.actionText(getResources().getString(R.string.login_user_no_internet_action))
						.actionClickListener(new SnackBar.OnActionClickListener() {
							@Override
							public void onActionClick(SnackBar sb, int actionId) {
								mContinueFABContainer.setClickable(true);
								loginWebView.loadUrl(LOGIN_URL);
								sb.dismiss();
							}
						})
						.show(LoginActivity.this);
			}
		}, SHOW_SNACKBAR_DELAY);
	}

	private void handleUserCancel() {
		mSnackbar
				.duration(SNACKBAR_DURATION)
				.text(getResources().getString(R.string.login_user_cancel))
				.actionText(getResources().getString(R.string.login_user_cancel_action))
				.actionClickListener(new SnackBar.OnActionClickListener() {
					@Override
					public void onActionClick(SnackBar sb, int actionId) {
						sb.dismiss();
					}
				});


		hideLoginView().getAnimations().get(0).setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				loginWebView.loadUrl(LOGIN_URL);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						mSnackbar.show(LoginActivity.this);
					}
				}, SHOW_SNACKBAR_DELAY);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
	}

	private void showLoginView() {
		showWebViewAnimation();
		hideFABAnimation();
		hideContinueIconAnimations();
	}

	private AnimationSet hideLoginView() {
		showFABAnimation();
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				getContinueIconAnimations(270);
			}
		}, SHOW_WEBVIEW_ANIMATION_DURATION - 400);
		return hideWebViewAnimation();
	}

	private int getScreenHeight() {
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		final DisplayMetrics displayMetrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(displayMetrics);
		return displayMetrics.heightPixels;
	}

	private String getAccessTokenFromURL(String url) {
		String startIdentifier = "access_token";
		String endIdentifier = "&scope";

		int startIndex = url.indexOf(startIdentifier) + startIdentifier.length() + 1;
		int lastIndex = url.indexOf(endIdentifier);

		return url.substring(startIndex, lastIndex);
	}

	private void initSnackbar() {
		mSnackbar = new SnackBar(this)
							.applyStyle(R.style.snack_bar_style_mobile);
	}

	private void initLoginView() {
		CookieManager cm = CookieManager.getInstance();
		cm.removeAllCookie();
		WebViewDatabase db = WebViewDatabase.getInstance(this);
		db.clearFormData();

		WebSettings ws = loginWebView.getSettings();
		ws.setSaveFormData(false);
		ws.setSavePassword(false);

		loginWebView.clearCache(true);
		loginWebView.getSettings().setSaveFormData(false);
		loginWebView.getSettings().setJavaScriptEnabled(true);
		loginWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
		loginWebView.getSettings().setSupportZoom(true);

		loginWebView.setWebViewClient(
											 new WebViewClient() {
												 @Override
												 public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
													 if (failingUrl.equals(LOGIN_URL)) {
														 // The user has no internet connection
														 Log.e(LOG_TAG, "The WebView failed to load URL - " + failingUrl);
														 handleNoInternet();
													 }
												 }

												 @Override
												 public boolean shouldOverrideUrlLoading(WebView view, String url) {
													 Log.d(LOG_TAG, "shouldOverrideUrlLoading - " + url);

													 // The user has successfully logged in
													 // Example:
													 // http://nrask.net/oauth_authorizing#access_token=r6z9er061caeq4sjq75ncv7goh27p8&scope=user_read+channel_read+chat_login+user_follows_edit
													 // Access Token from response:
													 // r6z9er061caeq4sjq75ncv7goh27p8
													 if (url.contains("access_token") && url.contains("oauth_authorizing")) {
														 String mAccessToken = getAccessTokenFromURL(url);
														 Log.d(LOG_TAG, "Access token received - " + mAccessToken);
														 if (isWebViewShown) {
															 hideLoginView();
														 }
														 HandlerUserLoginTask handleTask = new HandlerUserLoginTask();
														 handleTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getBaseContext(), mAccessToken, LoginActivity.this);

														 CookieManager cm = CookieManager.getInstance();
														 cm.removeAllCookie();

														 view.clearCache(true);
														 view.clearHistory();
														 view.clearFormData();
													 } else if (url.contains("The+user+denied+you+access")) {
														 Log.d(LOG_TAG, "The user cancelled the login");
														 // The user pressed "Cancel in the webview"
														 handleUserCancel();
														 return true;
													 } else if (!isPartOfSetup && url.contains("passport")) {
														 /*
														 Log.d(LOG_TAG, "CONTAINING PASSWORD");
														 view.loadUrl(url);
														 */
													 }

													 return false;
												 }

												 @Override
												 public void onPageFinished(WebView view, String url) {
													 super.onPageFinished(view, url);
													 mWebViewProgress.stop();
												 }
											 }
		);

		loginWebView.loadUrl(LOGIN_URL);
	}

	private AnimationSet hideAllViews() {
		if(mContinueIcon.getVisibility() == View.VISIBLE) {
			hideContinueIconAnimations();
		}
		hideViewAnimation(mGearIcon, HIDE_VIEW_ANIMATION_DURATION);
		hideViewAnimation(mLoginTextLineOne, HIDE_VIEW_ANIMATION_DURATION);
		if(mSuccessMessage.getVisibility() != View.INVISIBLE) {
			hideViewAnimation(mSuccessMessage, HIDE_VIEW_ANIMATION_DURATION);
		}

		if(mSuccessIcon.getVisibility() != View.INVISIBLE) {
			hideViewAnimation(mSuccessIcon, HIDE_VIEW_ANIMATION_DURATION);
			hideViewAnimation(mSuccessCircle, HIDE_VIEW_ANIMATION_DURATION);
			hideViewAnimation(mSuccessCircleShadow, HIDE_VIEW_ANIMATION_DURATION);
		}

		return hideViewAnimation(mLoginTextLineTwo, HIDE_VIEW_ANIMATION_DURATION);
	}

	private void navigateToNotificationActivity() {
		// Go to the login activity, with no transition.
		hasTransitioned = true;
		if (!isPartOfSetup) {
			if(!LoginActivity.hasLoadedFollows()) {
				this.startActivity(new Intent(getBaseContext(), ConfirmSetupActivity.class));
				this.overridePendingTransition(0, 0);
			} else {
				this.startActivity(Service.getLoggedInIntent(getBaseContext()));
				this.overridePendingTransition(0, 0);
			}

		} else {
			Intent loginActivityIntent = new Intent(getBaseContext(), NotificationActivity.class);
			loginActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			ActivityCompat.startActivity(this, loginActivityIntent, null);
		}
	}

	private void showSkippingAnimation() {
		// Get the center for the FAB
		int cx = (int) mContinueFABContainer.getX() + mContinueFABContainer.getMeasuredHeight() / 2;
		int cy = (int) mContinueFABContainer.getY() + mContinueFABContainer.getMeasuredWidth() / 2;

		// get the final radius for the clipping circle
		int dx = Math.max(cx, mTransitionViewWhite.getWidth() - cx);
		int dy = Math.max(cy, mTransitionViewWhite.getHeight() - cy);
		float finalRadius = (float) Math.hypot(dx, dy);

		mTransitionViewBlue.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		final SupportAnimator whiteTransitionAnimation =
				ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0, finalRadius);
		whiteTransitionAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		whiteTransitionAnimation.setDuration(REVEAL_ANIMATION_DURATION);
		whiteTransitionAnimation.addListener(new SupportAnimator.AnimatorListener() {
			@Override
			public void onAnimationStart() {
				mTransitionViewWhite.bringToFront();
				mTransitionViewWhite.setVisibility(View.VISIBLE);
				mContinueFABContainer.setClickable(false);
				if(mContinueIcon.getVisibility() == View.VISIBLE) {
					hideContinueIconAnimations();
				}
			}

			@Override
			public void onAnimationEnd() {
				hasTransitioned = true;
				transitionAnimationWhite = whiteTransitionAnimation;
				new Settings(getBaseContext()).setSetup(true);
				new Settings(getBaseContext()).setLogin(false);
				Intent intent = Service.getNotLoggedInIntent(getBaseContext());
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				startActivity(intent);
			}

			@Override
			public void onAnimationCancel() {

			}

			@Override
			public void onAnimationRepeat() {

			}
		});

		whiteTransitionAnimation.start();
	}

	/**
	 * Animations only here from and down
	 */

	private void showTransitionAnimation() {
		// Get the center for the FAB
		int cx = (int) mContinueFABContainer.getX() + mContinueFABContainer.getMeasuredHeight() / 2;
		int cy = (int) mContinueFABContainer.getY() + mContinueFABContainer.getMeasuredWidth() / 2;

		// get the final radius for the clipping circle
		int dx = Math.max(cx, mTransitionViewWhite.getWidth() - cx);
		int dy = Math.max(cy, mTransitionViewWhite.getHeight() - cy);
		float finalRadius = (float) Math.hypot(dx, dy);

		//mTransitionViewBlue.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		//mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		final SupportAnimator whiteTransitionAnimation =
				ViewAnimationUtils.createCircularReveal(mTransitionViewWhite, cx, cy, 0, finalRadius);
		whiteTransitionAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		whiteTransitionAnimation.setDuration(REVEAL_ANIMATION_DURATION);
		whiteTransitionAnimation.addListener(new SupportAnimator.AnimatorListener() {
			@Override
			public void onAnimationStart() {
				mTransitionViewWhite.bringToFront();
				mTransitionViewWhite.setVisibility(View.VISIBLE);
				mContinueFABContainer.setClickable(false);
				if(mContinueIcon.getVisibility() == View.VISIBLE) {
					hideContinueIconAnimations();
				}
			}

			@Override
			public void onAnimationEnd() {
				transitionAnimationWhite = whiteTransitionAnimation;
			}

			@Override
			public void onAnimationCancel() {

			}

			@Override
			public void onAnimationRepeat() {

			}
		});


		final SupportAnimator blueTransitionAnimation =
				ViewAnimationUtils.createCircularReveal(mTransitionViewBlue, cx, cy, 0, finalRadius);
		blueTransitionAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		blueTransitionAnimation.setDuration(REVEAL_ANIMATION_DURATION);
		blueTransitionAnimation.addListener(new SupportAnimator.AnimatorListener() {
			@Override
			public void onAnimationStart() {
				mTransitionViewBlue.setVisibility(View.VISIBLE);
				mTransitionViewBlue.bringToFront();
				mContinueFABShadow.bringToFront();
				mContinueFAB.bringToFront();
			}

			@Override
			public void onAnimationEnd() {
				transitionAnimationBlue = blueTransitionAnimation;
				mTransitionViewBlue.setLayerType(View.LAYER_TYPE_NONE, null);
				mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationCancel() {
				onAnimationEnd();
			}

			@Override
			public void onAnimationRepeat() {

			}
		});

		whiteTransitionAnimation.start();
		blueTransitionAnimation.setStartDelay(REVEAL_ANIMATION_DELAY);
		blueTransitionAnimation.start();

		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Log.d(LOG_TAG, "Navigating to NotificationActivity");
				navigateToNotificationActivity();
			}
		}, REVEAL_ANIMATION_DELAY + REVEAL_ANIMATION_DURATION);
	}

	private void showReverseTransitionAnimation() {
		mTransitionViewWhite.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		if (transitionAnimationBlue != null && mTransitionViewBlue != null) {
			mTransitionViewBlue.setLayerType(View.LAYER_TYPE_HARDWARE, null);
			SupportAnimator blueReversed = transitionAnimationBlue.reverse();
			blueReversed.setInterpolator(new AccelerateDecelerateInterpolator());
			blueReversed.addListener(new SupportAnimator.AnimatorListener() {
				@Override
				public void onAnimationStart() {
					mTransitionViewBlue.setVisibility(View.VISIBLE);
					mTransitionViewBlue.bringToFront();
				}

				@Override
				public void onAnimationEnd() {
					Service.bringToBack(mTransitionViewBlue);
					mTransitionViewBlue.setVisibility(View.INVISIBLE);
				}

				@Override
				public void onAnimationCancel() {

				}

				@Override
				public void onAnimationRepeat() {

				}
			});
			blueReversed.setDuration(REVEAL_ANIMATION_DURATION);
			blueReversed.start();
		}

		final SupportAnimator whiteReversed = transitionAnimationWhite.reverse();
		whiteReversed.setInterpolator(new AccelerateDecelerateInterpolator());
		whiteReversed.addListener(new SupportAnimator.AnimatorListener() {
			@Override
			public void onAnimationStart() {
				mTransitionViewWhite.setVisibility(View.VISIBLE);
				mTransitionViewWhite.bringToFront();
			}

			@Override
			public void onAnimationEnd() {
				Service.bringToBack(mTransitionViewWhite);
				mTransitionViewBlue.setLayerType(View.LAYER_TYPE_NONE, null);
				mTransitionViewWhite.setLayerType(View.LAYER_TYPE_NONE, null);
				mTransitionViewWhite.setVisibility(View.INVISIBLE);
				mContinueFABContainer.setClickable(true);
				mContinueFABContainer.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						showTransitionAnimation();
					}
				});

				mContinueIcon.bringToFront();
				mContinueIcon.setVisibility(View.VISIBLE);
				getContinueIconAnimations(360).start();
			}

			@Override
			public void onAnimationCancel() {

			}

			@Override
			public void onAnimationRepeat() {

			}
		});
		whiteReversed.setDuration(REVEAL_ANIMATION_DURATION);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				whiteReversed.start();
			}
		}, REVEAL_ANIMATION_DELAY);
		hasTransitioned = false;
	}

	private AnimationSet showSuccessAnimation() {
		Log.d(LOG_TAG, "Showing Success Animation");
		mSuccessMessage.setText(
			getResources().getString(R.string.login_on_success_message, new Settings(getBaseContext()).getGeneralTwitchDisplayName())
		);
		final AnimationSet mCircleShadowAnimations = getSuccessShadowAnimation();
		final AnimationSet mCircleAnimations = getSuccessCircleAnimation();
		final AnimationSet mIconAnimations = getSuccessIconAnimation();


		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mSuccessCircleShadow.startAnimation(mCircleShadowAnimations);
				mSuccessIcon.startAnimation(mIconAnimations);
				showTextLineAnimations(mSuccessMessage, 1);
			}
		}, 150);
		mSuccessCircle.startAnimation(mCircleAnimations);
		hideContinueIconAnimations();
		hideViewAnimation(mLoginTextContainer, HIDE_INSTRUCTIONS_DURATION);

		return mCircleAnimations;
	}

	private AnimationSet hideViewAnimation(final View view, final int DURATION) {
		view.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mScaleAnimation.setDuration(DURATION);
		mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

		Animation mAlphaAnimation = new AlphaAnimation(1f, 0f);
		mAlphaAnimation.setDuration(DURATION/2);
		mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
		mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});


		final AnimationSet mViewAnimations = new AnimationSet(false);
		mViewAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
		mViewAnimations.setFillBefore(true);
		mViewAnimations.setFillAfter(true);
		mViewAnimations.addAnimation(mScaleAnimation);
		mViewAnimations.addAnimation(mAlphaAnimation);

		mViewAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				view.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});


		view.startAnimation(mViewAnimations);
		return mViewAnimations;
	}

	private AnimationSet getContinueIconAnimations(int toDegree) {
		mContinueIcon.setVisibility(View.VISIBLE);
		Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		Animation mRotateAnimation = new RotateAnimation(
																toDegree - 360, toDegree,
																Animation.RELATIVE_TO_SELF, 0.5f,
																Animation.RELATIVE_TO_SELF, 0.5f
		);
		mRotateAnimation.setRepeatCount(0);
		mScaleAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mContinueIcon.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		AnimationSet mAnimations = new AnimationSet(true);
		mAnimations.setDuration(SHOW_CONTINUE_ICON_DURATION);
		mAnimations.setFillAfter(true);
		mAnimations.setInterpolator(new OvershootInterpolator(1.5f));
		mAnimations.addAnimation(mScaleAnimation);
		mAnimations.addAnimation(mRotateAnimation);

		mContinueIcon.startAnimation(mAnimations);


		return mAnimations;
	}

	private AnimationSet hideContinueIconAnimations() {
		Animation mScaleAnimation = new ScaleAnimation(1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		Animation mRotateAnimation = new RotateAnimation(
																mContinueIcon.getRotation(),  360 - mContinueIcon.getRotation(),
																Animation.RELATIVE_TO_SELF, 0.5f,
																Animation.RELATIVE_TO_SELF, 0.5f
		);
		mRotateAnimation.setRepeatCount(0);
		mRotateAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mContinueIcon.setVisibility(View.INVISIBLE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		AnimationSet mAnimations = new AnimationSet(true);
		mAnimations.setDuration(SHOW_CONTINUE_ICON_DURATION);
		mAnimations.setFillAfter(true);
		mAnimations.setInterpolator(new OvershootInterpolator(1.5f));
		mAnimations.addAnimation(mScaleAnimation);
		mAnimations.addAnimation(mRotateAnimation);

		mContinueIcon.startAnimation(mAnimations);

		return mAnimations;
	}

	private AnimationSet showTextLineAnimations(final TextView mTextLine, int lineNumber) {
		mTextLine.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		int travelDistance = (lineNumber < 3)
									 ? (int) TypedValue.applyDimension(
																			  TypedValue.COMPLEX_UNIT_SP,
																			  getResources().getDimension(R.dimen.welcome_text_line_three_size),
																			  getResources().getDisplayMetrics())
									 : 0;

		float overshoot = (lineNumber == 1) ? 2f : 1f;
		final Animation mTranslationAnimation = new TranslateAnimation(0, 0, travelDistance, 0);
		mTranslationAnimation.setInterpolator(new OvershootInterpolator(overshoot));

		final Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
		mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
		mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mTextLine.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mTextLine.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		final AnimationSet mWelcomeTextAnimations = new AnimationSet(false);
		mWelcomeTextAnimations.setDuration(SHOW_TEXT_ANIMATION_DURATION);
		mWelcomeTextAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
		mWelcomeTextAnimations.setFillBefore(true);
		mWelcomeTextAnimations.setFillAfter(true);
		mWelcomeTextAnimations.addAnimation(mAlphaAnimation);
		mWelcomeTextAnimations.addAnimation(mTranslationAnimation);

		int delay = (lineNumber < 3)
							? SHOW_TEXT_ANIMATION_BASE_DELAY * lineNumber
							: SHOW_TEXT_ANIMATION_BASE_DELAY * (lineNumber * 2);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				mTextLine.startAnimation(mWelcomeTextAnimations);

			}
		}, delay + SHOW_TEXT_ANIMATION_DELAY);

		return mWelcomeTextAnimations;
	}

	private AnimationSet getSuccessShadowAnimation() {
		mSuccessCircleShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		float mEndScale = 1.1f;
		Animation mScaleAnimation = new ScaleAnimation(0, mEndScale, 0, mEndScale, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mScaleAnimation.setDuration(SHOW_SUCCESS_ICON_DURATION);
		mScaleAnimation.setInterpolator(new OvershootInterpolator(3f));

		Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
		mAlphaAnimation.setDuration(SHOW_SUCCESS_ICON_DURATION);
		mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
		mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mSuccessCircleShadow.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		final AnimationSet mLogoAnimations = new AnimationSet(false);
		mLogoAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
		mLogoAnimations.setFillBefore(true);
		mLogoAnimations.setFillAfter(true);
		mLogoAnimations.addAnimation(mScaleAnimation);
		mLogoAnimations.addAnimation(mAlphaAnimation);

		mLogoAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mSuccessCircleShadow.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});


		return mLogoAnimations;
	}

	private AnimationSet getSuccessCircleAnimation() {
		mSuccessCircle.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		float mEndScale = 1.1f;
		Animation mScaleAnimation = new ScaleAnimation(0, mEndScale, 0, mEndScale, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mScaleAnimation.setDuration(SHOW_SUCCESS_ICON_DURATION);
		mScaleAnimation.setInterpolator(new OvershootInterpolator(3f));


		Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
		mAlphaAnimation.setDuration(SHOW_SUCCESS_ICON_DURATION);
		mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
		mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mSuccessCircle.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		final AnimationSet mLogoAnimations = new AnimationSet(false);
		mLogoAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
		mLogoAnimations.setFillBefore(true);
		mLogoAnimations.setFillAfter(true);
		mLogoAnimations.addAnimation(mScaleAnimation);
		mLogoAnimations.addAnimation(mAlphaAnimation);

		mLogoAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mSuccessCircle.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		return mLogoAnimations;
	}

	private AnimationSet getSuccessIconAnimation() {
		mSuccessIcon.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		float mEndScale = 1.1f;
		Animation mScaleAnimation = new ScaleAnimation(0, mEndScale, 0, mEndScale, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mScaleAnimation.setDuration(SHOW_SUCCESS_ICON_DURATION);
		mScaleAnimation.setInterpolator(new OvershootInterpolator(3f));

		Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
		mAlphaAnimation.setDuration(SHOW_SUCCESS_ICON_DURATION);
		mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
		mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mSuccessIcon.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		final AnimationSet mLogoAnimations = new AnimationSet(false);
		mLogoAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
		mLogoAnimations.setFillBefore(true);
		mLogoAnimations.setFillAfter(true);
		mLogoAnimations.addAnimation(mScaleAnimation);
		mLogoAnimations.addAnimation(mAlphaAnimation);

		mLogoAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mSuccessIcon.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});


		return mLogoAnimations;
	}

	private AnimationSet showLogoAnimations() {
		mGearIcon.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		Animation mScaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		mScaleAnimation.setDuration(SHOW_LOGO_ANIMATION_DURATION);
		mScaleAnimation.setInterpolator(new OvershootInterpolator(0.7f));

		Animation mAlphaAnimation = new AlphaAnimation(0f, 1f);
		mAlphaAnimation.setDuration(SHOW_LOGO_ANIMATION_DURATION);
		mAlphaAnimation.setInterpolator(new DecelerateInterpolator());
		mAlphaAnimation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mGearIcon.setVisibility(View.VISIBLE);
			}

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		RotateAnimation mRotateAnimation = new RotateAnimation(
																	  0, 360,
																	  Animation.RELATIVE_TO_SELF, 0.5f,
																	  Animation.RELATIVE_TO_SELF, 0.5f
		);
		mRotateAnimation.setInterpolator(new LinearInterpolator());
		mRotateAnimation.setDuration(LOGO_ROTATION_SPEED);
		mRotateAnimation.setRepeatCount(Animation.INFINITE);

		final AnimationSet mLogoAnimations = new AnimationSet(false);
		mLogoAnimations.setInterpolator(new AccelerateDecelerateInterpolator());
		mLogoAnimations.setFillBefore(true);
		mLogoAnimations.setFillAfter(true);
		mLogoAnimations.addAnimation(mScaleAnimation);
		mLogoAnimations.addAnimation(mRotateAnimation);
		mLogoAnimations.addAnimation(mAlphaAnimation);

		mLogoAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mGearIcon.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});


		mGearIcon.startAnimation(mLogoAnimations);
		return mLogoAnimations;
	}

	private AnimationSet showWebViewAnimation() {
		loginWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		isWebViewShown = true;
		loginWebView.setVisibility(View.VISIBLE);
		loginWebView.bringToFront();
		mWebViewProgress.bringToFront();

		Animation mTranslationAnimation = new TranslateAnimation(
																		Animation.RELATIVE_TO_PARENT, 0,
																		Animation.RELATIVE_TO_PARENT, 0,
																		Animation.RELATIVE_TO_PARENT, 1f,
																		Animation.RELATIVE_TO_PARENT, 0
		);

		mTranslationAnimation.setFillAfter(true);
		mTranslationAnimation.setInterpolator(new OvershootInterpolator(1f));

		AnimationSet mAnimations = new AnimationSet(false);
		mAnimations.addAnimation(mTranslationAnimation);
		mAnimations.setDuration(SHOW_WEBVIEW_ANIMATION_DURATION);
		mAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				loginWebView.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		mWebViewContainer.startAnimation(mAnimations);
		return mAnimations;
	}

	private AnimationSet hideWebViewAnimation() {
		loginWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

		Animation mTranslationAnimation = new TranslateAnimation(
																		Animation.RELATIVE_TO_PARENT, 0,
																		Animation.RELATIVE_TO_PARENT, 0,
																		Animation.RELATIVE_TO_PARENT, 0,
																		Animation.RELATIVE_TO_PARENT, 1f
		);

		mTranslationAnimation.setFillAfter(true);
		mTranslationAnimation.setInterpolator(new OvershootInterpolator(1f));

		AnimationSet mAnimations = new AnimationSet(false);
		mAnimations.addAnimation(mTranslationAnimation);
		mAnimations.setDuration(SHOW_WEBVIEW_ANIMATION_DURATION);
		mAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				isWebViewHiding = true;
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				loginWebView.setLayerType(View.LAYER_TYPE_NONE, null);
				isWebViewHiding = false;
				isWebViewShown = false;
				loginWebView.setVisibility(View.INVISIBLE);
				loginWebView.setTranslationY(1);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		mWebViewContainer.startAnimation(mAnimations);
		return mAnimations;
	}

	private AnimationSet hideFABAnimation() {
		mContinueFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mContinueFABShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null);


		Animation mTranslationAnimation = new TranslateAnimation(0, 0, 0, getScreenHeight() * -1);
		mTranslationAnimation.setFillAfter(true);
		mTranslationAnimation.setInterpolator(new OvershootInterpolator(1f));

		AnimationSet mAnimations = new AnimationSet(false);
		mAnimations.setFillAfter(true);
		mAnimations.addAnimation(mTranslationAnimation);
		mAnimations.setDuration(SHOW_WEBVIEW_ANIMATION_DURATION);
		mAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				mContinueFABContainer.setClickable(false);
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mContinueFAB.setLayerType(View.LAYER_TYPE_NONE, null);
				mContinueFABShadow.setLayerType(View.LAYER_TYPE_NONE, null);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		mContinueFABContainer.startAnimation(mAnimations);
		return mAnimations;
	}

	private AnimationSet showFABAnimation() {
		mContinueFAB.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		mContinueFABShadow.setLayerType(View.LAYER_TYPE_HARDWARE, null);


		Animation mTranslationAnimation = new TranslateAnimation(0, 0, -1 * getScreenHeight(), 0);
		mTranslationAnimation.setFillAfter(true);
		mTranslationAnimation.setInterpolator(new OvershootInterpolator(0.7f));

		AnimationSet mAnimations = new AnimationSet(false);
		mAnimations.setFillAfter(true);
		mAnimations.addAnimation(mTranslationAnimation);
		mAnimations.setDuration(SHOW_WEBVIEW_ANIMATION_DURATION);
		mAnimations.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mContinueFAB.setLayerType(View.LAYER_TYPE_NONE, null);
				mContinueFABShadow.setLayerType(View.LAYER_TYPE_NONE, null);
				mContinueFABContainer.setClickable(true);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});

		mContinueFABContainer.startAnimation(mAnimations);
		return mAnimations;
	}

}
