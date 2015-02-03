#import <UIKit/UIKit.h>
#import <CoreFoundation/CoreFoundation.h>
#include <ctype.h>
#include <objc/runtime.h>

#include "SamcodesAdMob.h"

#import "GADInterstitial.h"
#import "GADBannerView.h"

@interface AdMobImplementation : NSObject <GADInterstitialDelegate, GADBannerViewDelegate> {
	NSMutableDictionary* bannerDictionary;
	NSMutableDictionary* interstitialDictionary;
}

-(GADInterstitial*)getInterstitialForAdUnit:(NSString*)location;
-(GADBannerView*)getBannerForAdUnit:(NSString*)location;

@end

@implementation AdMobImplementation

+ (AdMobImplementation*)sharedInstance
{
   static AdImplementation* sharedInstance = nil;
   static dispatch_once_t onceToken;
   dispatch_once(&onceToken, ^{
      sharedInstance = [[self alloc] init];	  
	  bannerDictionary = [[NSMutableDictionary alloc] init];
	  interstitialDictionary = [[NSMutableDictionary alloc] init];
   });

   return sharedInstance;
}

-(GADInterstitial*)getInterstitialForAdUnit:(NSString*)location {
	id interstitial = [interstitialDictionary objectForKey:location];
	
	if(interstitial == nil) {
	}
	
	return interstitial;
}

-(GADBannerView*)getBannerForAdUnit:(NSString*)location {
	id banner = [bannerDictionary objectForKey:location];
	
	if(banner == nil) {
	}
	
	return banner;
}

- (void)interstitialDidReceiveAd:(GADInterstitial *)ad {
	sendAdMobEvent("onInterstitialLoaded", [ad.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)interstitial:(GADInterstitial *)ad didFailToReceiveAdWithError:(GADRequestError *)error {
	sendAdMobEvent("onInterstitialFailedToLoad", [ad.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)interstitialWillPresentScreen:(GADInterstitial *)ad {
	sendAdMobEvent("onInterstitialOpened", [ad.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)interstitialWillDismissScreen:(GADInterstitial *)ad {
	sendAdMobEvent("onInterstitialClosed", [ad.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)interstitialDidDismissScreen:(GADInterstitial *)ad {
}

- (void)interstitialWillLeaveApplication:(GADInterstitial *)ad {
	sendAdMobEvent("onInterstitialLeftApplication", [ad.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)adViewDidReceiveAd:(GADBannerView *)adView {
	sendAdMobEvent("onBannerLoaded", [adView.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)adView:(GADBannerView *)adView didFailToReceiveAdWithError:(GADRequestError *)error {
	sendAdMobEvent("onBannerFailedToLoad", [adView.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)adViewWillPresentScreen:(GADBannerView *)adView {
	sendAdMobEvent("onBannerOpened", [adView.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)adViewWillDismissScreen:(GADBannerView *)adView {
	sendAdMobEvent("onBannerClosed", [adView.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

- (void)adViewDidDismissScreen:(GADBannerView *)adView {
}

- (void)adViewWillLeaveApplication:(GADBannerView *)adView {
	sendAdMobEvent("onBannerLeftApplication", [adView.adUnitID cStringUsingEncoding:[NSString defaultCStringEncoding]]);
}

@end

extern "C" void sendAdMobEvent(const char* type, const char* location);

namespace samcodesadmob
{
    void initAdMob(const char* testDeviceHash)
    {
		NSString *nsTestDeviceHash = [[NSString alloc] initWithUTF8String:testDeviceHash];
		AdMobImplementation *instance = [AdMobImplementation sharedInstance];
		
		
    }
	
	void showInterstitial(const char* location)
	{
        NSString *nsLocation = [[NSString alloc] initWithUTF8String:location];
		AdMobImplementation *instance = [AdMobImplementation sharedInstance];
		GADInterstitial* interstitial = [instance getInterstitialForAdUnit:nsLocation];
		
		if([interstitial isReady]) {
			[ad presentFromRootViewController:[[[UIApplication sharedApplication] keyWindow] rootViewController]];
		} else {
			// TODO serve cache request and then check if ready with a timer
		}
    }
	
    void cacheInterstitial(const char* location)
    {
        NSString *nsLocation = [[NSString alloc] initWithUTF8String:location];
		AdMobImplementation *instance = [AdMobImplementation sharedInstance];
		GADInterstitial* interstitial = [instance getInterstitialForAdUnit:nsLocation];
		
		GADRequest *request = [GADRequest request];
		request.testDevices = @[ GAD_SIMULATOR_ID ]; // TODO add testDeviceHash
		[interstitial loadRequest:request];
    }
	
    bool hasCachedInterstitial(const char* location)
    {
        NSString *nsLocation = [[NSString alloc] initWithUTF8String:location];
		AdMobImplementation *instance = [AdMobImplementation sharedInstance];
		GADInterstitial* interstitial = [instance getInterstitialForAdUnit:nsLocation];
		
		return [interstitial isReady];
    }
	
    void showBanner(const char* location)
    {
        NSString *nsLocation = [[NSString alloc] initWithUTF8String:location];
		AdMobImplementation *instance = [AdMobImplementation sharedInstance];
		GADBannerView* banner = [instance getBannerForAdUnit:nsLocation];
		
		banner.hidden = false;
    }
	
    void hideBanner(const char* location)
    {
        NSString *nsLocation = [[NSString alloc] initWithUTF8String:location];
		AdMobImplementation *instance = [AdMobImplementation sharedInstance];
		GADBannerView* banner = [instance getBannerForAdUnit:nsLocation];
		
		banner.hidden = true;
    }
}