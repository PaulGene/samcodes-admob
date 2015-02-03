package com.samcodes.admob;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.android.gms.ads.*;

public class AdMobExtension extends Extension 
{
	private static String TAG = "AdMobExtension";
	private static String testDeviceId = "::ENV_AdmobTestDeviceId::";
	
	// Assumes 1:1 mapping from ad unit ids to ads
	private static HashMap<String, AdView> unitIdToBannerView = new HashMap<String, AdView>();
	private static HashMap<String, InterstitialAd> unitIdToInterstitial = new HashMap<String, InterstitialAd>();
	
	public static HaxeObject callback = null;
	public static void setListener(HaxeObject haxeCallback) {
		Log.i(TAG, "Setting Haxe AdMob listener object");
		callback = haxeCallback;
	}
	
	public static void callHaxe(final String name, final Object[] args) {
		if(callback != null) {
			callbackHandler.post(new Runnable() {
				public void run() {
					Log.d(TAG, "Calling " + name + " from Java");
					callback.call(name, args);
				}
			});
		} else {
			Log.w(TAG, "AdMob callback object is null, ignoring AdMob callback");
		}
	}
	
	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}
	
	@Override
	public void onResume() {
		for (AdView v : unitIdToBannerView.values()) {
			if(v != null) {
				v.resume();
			}
		}
		
		super.onResume();
	}
	
	@Override
	public void onPause() {
		for (AdView v : unitIdToBannerView.values()) {
			if(v != null) {
				v.pause();
			}
		}
		
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		for (AdView v : unitIdToBannerView.values()) {
			if(v != null) {
				v.destroy();
			}
		}
		
		super.onDestroy();
	}
	
	public static void showBanner(final String id) {		
		final AdView view = getBannerViewForUnitId(id);
		
		if(view != null) {
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					// TODO add banner view to view hierarchy?
					Log.d(TAG, "Showing banner with id " + id);
					view.setVisibility(View.VISIBLE);
				}
			});
		}
	}
	
	public static void hideBanner(final String id) {
		final AdView view = getBannerViewForUnitId(id);
		
		if(view != null) {
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					// TODO remove banner view from view hierarchy?
					Log.d(TAG, "Hiding banner with id " + id);
					view.setVisibility(View.INVISIBLE);
				}
			});
		}
	}
	
	public static boolean hasCachedInterstitial(String id) {
		final InterstitialAd ad = getInterstitialForUnitId(id);
		
		if(ad == null) {
			return false;
		}
		
		final Semaphore mutex = new Semaphore(0);
		final AtomicBoolean isLoaded = new AtomicBoolean();
		
		mainActivity.runOnUiThread(new Runnable() {
			public void run() {
				isLoaded.set(ad.isLoaded());
				mutex.release();
			}
		});
		
		try {
			mutex.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return isLoaded.get();
	}
	
	public static void cacheInterstitial(final String id) {
		final InterstitialAd ad = getInterstitialForUnitId(id);
		
		if(ad != null) {
			mainActivity.runOnUiThread(new Runnable() {
				public void run() {
					AdRequest request = null;
					
					if(testDeviceId != "null") {
						request = new AdRequest.Builder()
						.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
						.addTestDevice(testDeviceId) // TODO would be good to do more than one
						.build();
					} else {
						request = new AdRequest.Builder()
						.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
						.build();
					}
					
					if(ad.getAdListener() == null) {
						ad.setAdListener(new InterstitialListener(id));
					} else if(ad.getAdListener().getClass().equals(InterstitialListener.class) == false) {
						ad.setAdListener(new InterstitialListener(id));
					}
					
					Log.d(TAG, "Caching interstitial with id " + id);
					ad.loadAd(request);
				}
			});
		}
	}
	
	public static void showInterstitial(final String id) {
		final InterstitialAd ad = getInterstitialForUnitId(id);
		
		if(ad != null) {
			if(hasCachedInterstitial(id)) {
				mainActivity.runOnUiThread(new Runnable() {
					public void run() {
						Log.d(TAG, "Preparing to show interstitial");
						
						if(ad.getAdListener() == null) {
							ad.setAdListener(new InterstitialListener(id));
						} else if(ad.getAdListener().getClass().equals(InterstitialListener.class) == false) {
							ad.setAdListener(new InterstitialListener(id));
						}
						
						Log.d(TAG, "Showing interstitial with ad unit id " + ad.getAdUnitId());
						ad.show();
					}
				});
			} else {
				Log.w(TAG, "Attempted to show interstitial that had not been cached. Sending another cache request instead, will show interstitial once it has cached (timeout 1 minute)");
				Log.w(TAG, "Note this implies that if this interstitial is cached in the near future it will shown");
				cacheInterstitial(id);
				
				final Timer timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						if(hasCachedInterstitial(id)) {
							timer.cancel();
							
							mainActivity.runOnUiThread(new Runnable() {
								public void run() {
									Log.d(TAG, "Preparing to show interstitial");
									
									if(ad.getAdListener() == null) {
										ad.setAdListener(new InterstitialListener(id));
									} else if(ad.getAdListener().getClass().equals(InterstitialListener.class) == false) {
										ad.setAdListener(new InterstitialListener(id));
									}
									
									Log.d(TAG, "Showing interstitial with ad unit id " + ad.getAdUnitId());
									ad.show();
								}
							});
						}
					}
				}, 30, 2000);
			}
		}
	}
	
	private static InterstitialAd addInterstitialForUnitId(final String id) {
		if(unitIdToInterstitial.containsKey(id)) {
			Log.e(TAG, "This interstitial is already in the ad unit id->interstitial map...");
		}
		
		final InterstitialAd ad = new InterstitialAd(mainActivity);
		
		if(ad.getAdUnitId() == null) {
			ad.setAdUnitId(id);
		} else if(ad.getAdUnitId().equals(id) == false) {
			ad.setAdUnitId(id);
		}
		
		unitIdToInterstitial.put(id, ad);
		
		return ad;
	}
	
	private static AdView addBannerViewForUnitId(final String id) {
		if(unitIdToBannerView.containsKey(id)) {
			Log.e(TAG, "This banner is already in the ad unit id->bannerview map...");
		}
		
		final AdView ad = new AdView(mainActivity);
		
		if(ad.getAdUnitId() == null) {
			ad.setAdUnitId(id);
			Log.d(TAG, "Ad unit id was null, setting to " + id);
		} else if(ad.getAdUnitId().equals(id) == false) {
			ad.setAdUnitId(id);
			Log.d(TAG, "Ad unit id was equal to " + ad.getAdUnitId() + " setting it to " + id);
		}
		
		unitIdToBannerView.put(id, ad);
		
		return ad;
	}
	
	private static AdView getBannerViewForUnitId(String id) {
		AdView ad = unitIdToBannerView.get(id);
		
		if(ad == null) {
			Log.i(TAG, "Could not get banner view with id, adding a new one...");
			ad = addBannerViewForUnitId(id);
		}
		
		return ad;
	}
	
	private static InterstitialAd getInterstitialForUnitId(String id) {
		InterstitialAd ad = unitIdToInterstitial.get(id);
		
		if(ad == null) {
			Log.i(TAG, "Could not get interstitial with id " + id + " adding a new one...");
			ad = addInterstitialForUnitId(id);
		}
		
		return ad;
	}
	
	/** Gets a string error reason from an error code. */
	public static String getErrorReason(int errorCode) {
		String errorReason = "";
		switch(errorCode) {
			case AdRequest.ERROR_CODE_INTERNAL_ERROR:
				errorReason = "Internal error";
				break;
			case AdRequest.ERROR_CODE_INVALID_REQUEST:
				errorReason = "Invalid request";
				break;
			case AdRequest.ERROR_CODE_NETWORK_ERROR:
				errorReason = "Network Error";
				break;
			case AdRequest.ERROR_CODE_NO_FILL:
				errorReason = "No fill";
				break;
			}
			
		return errorReason;
	}
}