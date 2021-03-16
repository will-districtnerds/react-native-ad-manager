package com.matejdr.admanager;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.facebook.react.views.view.ReactViewGroup;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
//import com.google.android.gms.ads.doubleclick.AppEventListener;
import com.google.android.gms.ads.admanager.AppEventListener;
import com.google.android.gms.ads.doubleclick.PublisherAdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.doubleclick.PublisherAdView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.matejdr.admanager.customClasses.CustomTargeting;
import com.matejdr.admanager.utils.Targeting;

class BannerAdView extends ReactViewGroup implements AppEventListener, LifecycleEventListener {

//    protected PublisherAdView adView;
    protected AdManagerAdView adManagerView;

    String[] testDevices;
    AdSize[] validAdSizes;
    String adUnitID;
    AdSize adSize;

    // Targeting
    Boolean hasTargeting = false;
    CustomTargeting[] customTargeting;
    String[] categoryExclusions;
    String[] keywords;
    String contentURL;
    String publisherProvidedID;
    Location location;
    String correlator;

    public BannerAdView(final Context context, ReactApplicationContext applicationContext) {
        super(context);
        applicationContext.addLifecycleEventListener(this);
        this.createAdView();
    }

    private void createAdView() {
        if (this.adManagerView != null) this.adManagerView.destroy();

        final Context context = getContext();
//        this.adView = new PublisherAdView(context);
        this.adManagerView = new AdManagerAdView(context);
        this.adManagerView.setAppEventListener(this);
        this.adManagerView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                AdSize adSize = adManagerView.getAdSize();
                Log.d("Ads", "onAdLoaded adUnitID: "+adUnitID);
                Log.d("Ads", "onAdLoaded adSize: "+adSize);
                int width = adManagerView.getAdSize().getWidthInPixels(context);
                int height = adManagerView.getAdSize().getHeightInPixels(context);
                int left = adManagerView.getLeft();
                int top = adManagerView.getTop();
                Log.d("Ads", "onAdLoaded width: "+width);
                Log.d("Ads", "onAdLoaded height: "+height);
                Log.d("Ads", "onAdLoaded left: "+left);
                Log.d("Ads", "onAdLoaded top: "+top);
                adManagerView.measure(width, height);
                adManagerView.layout(left, top, left + width, top + height);
                sendOnSizeChangeEvent();
                WritableMap ad = Arguments.createMap();
                ad.putString("type", "banner");
                ad.putString("gadSize", adManagerView.getAdSize().toString());
                adManagerView.setVisibility(View.VISIBLE);
                Log.d("Ads", "onAdLoaded ad: "+ad.toString());
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_LOADED, ad);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String errorMessage = "Unknown error";
                switch (errorCode) {
                    case PublisherAdRequest.ERROR_CODE_INTERNAL_ERROR:
                        errorMessage = "Internal error, an invalid response was received from the ad server.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_INVALID_REQUEST:
                        errorMessage = "Invalid ad request, possibly an incorrect ad unit ID was given.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NETWORK_ERROR:
                        errorMessage = "The ad request was unsuccessful due to network connectivity.";
                        break;
                    case PublisherAdRequest.ERROR_CODE_NO_FILL:
                        errorMessage = "The ad request was successful, but no ad was returned due to lack of ad inventory.";
                        break;
                }
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                error.putString("message", errorMessage);
                event.putMap("error", error);
                Log.d("Ads", "onAdFailedToLoad event: "+event.toString());
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError var1) {
                String errorMessage = "Unknown error";

                // Gets the domain from which the error came.
                String errorDomain = var1.getDomain();
                // Gets the error code. See
                // https://developers.google.com/android/reference/com/google/android/gms/ads/AdRequest#constant-summary
                // for a list of possible codes.
                int errorCode = var1.getCode();
                // Gets an error message.
                errorMessage = var1.getMessage();
                // Gets additional response information about the request. See
                // https://developers.google.com/admob/android/response-info for more
                // information.
                ResponseInfo responseInfo = var1.getResponseInfo();
                // Gets the cause of the error, if available.
                AdError cause = var1.getCause();
                // All of this information is available via the error's toString() method.
                Log.d("Ads", "onAdFailedToLoad var1: "+var1.toString());
                WritableMap event = Arguments.createMap();
                WritableMap error = Arguments.createMap();
                error.putString("message", errorMessage);
                event.putMap("error", error);
                Log.d("Ads", "onAdFailedToLoad event: "+event.toString());
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_FAILED_TO_LOAD, event);
            }

            @Override
            public void onAdOpened() {
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_OPENED, null);
            }

            @Override
            public void onAdClosed() {
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_CLOSED, null);
            }

            @Override
            public void onAdLeftApplication() {
                sendEvent(RNAdManagerBannerViewManager.EVENT_AD_LEFT_APPLICATION, null);
            }
        });
//        this.addView(this.adView);
        this.addView(this.adManagerView);
    }

    private void sendOnSizeChangeEvent() {
        int width;
        int height;
        ReactContext reactContext = (ReactContext) getContext();
        WritableMap event = Arguments.createMap();
//        AdSize adSize = this.adView.getAdSize();
        AdSize adSize = this.adManagerView.getAdSize();
        if (adSize == AdSize.SMART_BANNER) {
            width = (int) PixelUtil.toDIPFromPixel(adSize.getWidthInPixels(reactContext));
            height = (int) PixelUtil.toDIPFromPixel(adSize.getHeightInPixels(reactContext));
        } else {
            width = adSize.getWidth();
            height = adSize.getHeight();
        }
        event.putString("type", "banner");
        event.putDouble("width", width);
        event.putDouble("height", height);
        sendEvent(RNAdManagerBannerViewManager.EVENT_SIZE_CHANGE, event);
    }

    private void sendEvent(String name, @Nullable WritableMap event) {
        ReactContext reactContext = (ReactContext) getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                name,
                event);
    }

    public void loadBanner() {
        ArrayList<AdSize> adSizes = new ArrayList<AdSize>();
        if (this.adSize != null) {
            adSizes.add(this.adSize);
        }
        if (this.validAdSizes != null) {
            for (int i = 0; i < this.validAdSizes.length; i++) {
                adSizes.add(this.validAdSizes[i]);
            }
        }

        if (adSizes.size() == 0) {
            adSizes.add(AdSize.BANNER);
        }

        AdSize[] adSizesArray = adSizes.toArray(new AdSize[adSizes.size()]);
//        this.adView.setAdSizes(adSizesArray);
        this.adManagerView.setAdSizes(adSizesArray);

        AdManagerAdRequest.Builder adManagerRequestBuilder = new AdManagerAdRequest.Builder();
        List<String> testDeviceIds = Arrays.asList("B3EEABB8EE11C2BE770B684D95219ECB");
        RequestConfiguration configuration =
                new RequestConfiguration.Builder().setTestDeviceIds(testDeviceIds).build();
        MobileAds.setRequestConfiguration(configuration);
/*
        PublisherAdRequest.Builder adRequestBuilder = new PublisherAdRequest.Builder();
        if (testDevices != null) {
            for (int i = 0; i < testDevices.length; i++) {
                String testDevice = testDevices[i];
                Log.d("Ads", "loadBanner testDevice: "+testDevice);
                if (testDevice == "SIMULATOR") {
                    testDevice = PublisherAdRequest.DEVICE_ID_EMULATOR;
                }
                adRequestBuilder.addTestDevice(testDevice);
            }
        }
*/
        if (correlator == null) {
            correlator = (String) Targeting.getCorelator(adUnitID);
        }
        Bundle bundle = new Bundle();
        bundle.putString("correlator", correlator);

//        adRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, bundle);
        adManagerRequestBuilder.addNetworkExtrasBundle(AdMobAdapter.class, bundle);

        // Targeting
        if (hasTargeting) {
            if (customTargeting != null && customTargeting.length > 0) {
                for (int i = 0; i < customTargeting.length; i++) {
                    String key = customTargeting[i].key;
                    Log.d("Ads", "loadBanner customTargeting key: "+key);
                    if (!key.isEmpty()) {
                        if (customTargeting[i].value != null && !customTargeting[i].value.isEmpty()) {
//                            adRequestBuilder.addCustomTargeting(key, customTargeting[i].value);
                            adManagerRequestBuilder.addCustomTargeting(key, customTargeting[i].value);
                        } else if (customTargeting[i].values != null && !customTargeting[i].values.isEmpty()) {
//                            adRequestBuilder.addCustomTargeting(key, customTargeting[i].values);
                            adManagerRequestBuilder.addCustomTargeting(key, customTargeting[i].values);
                        }
                    }
                }
            }
            if (categoryExclusions != null && categoryExclusions.length > 0) {
                for (int i = 0; i < categoryExclusions.length; i++) {
                    String categoryExclusion = categoryExclusions[i];
                    if (!categoryExclusion.isEmpty()) {
//                        adRequestBuilder.addCategoryExclusion(categoryExclusion);
                        adManagerRequestBuilder.addCategoryExclusion(categoryExclusion);
                    }
                }
            }
            if (keywords != null && keywords.length > 0) {
                for (int i = 0; i < keywords.length; i++) {
                    String keyword = keywords[i];
                    if (!keyword.isEmpty()) {
//                        adRequestBuilder.addKeyword(keyword);
                        adManagerRequestBuilder.addKeyword(keyword);
                    }
                }
            }
            if (contentURL != null) {
//                adRequestBuilder.setContentUrl(contentURL);
                adManagerRequestBuilder.setContentUrl(contentURL);
            }
            if (publisherProvidedID != null) {
//                adRequestBuilder.setPublisherProvidedId(publisherProvidedID);
                adManagerRequestBuilder.setPublisherProvidedId(publisherProvidedID);
            }
            if (location != null) {
//                adRequestBuilder.setLocation(location);
                adManagerRequestBuilder.setLocation(location);
            }
        }

//        PublisherAdRequest adRequest = adRequestBuilder.build();
        AdManagerAdRequest adManagerRequest = adManagerRequestBuilder.build();
//        this.adView.loadAd(adRequest);
        this.adManagerView.loadAd(adManagerRequest);
    }

    public void setAdUnitID(String adUnitID) {
        if (this.adUnitID != null) {
            // We can only set adUnitID once, so when it was previously set we have
            // to recreate the view
            this.createAdView();
        }
        this.adUnitID = adUnitID;
//        this.adView.setAdUnitId(adUnitID);
        this.adManagerView.setAdUnitId(adUnitID);
    }

    public void setTestDevices(String[] testDevices) {
        this.testDevices = testDevices;
    }

    // Targeting
    public void setCustomTargeting(CustomTargeting[] customTargeting) {
        this.customTargeting = customTargeting;
    }

    public void setCategoryExclusions(String[] categoryExclusions) {
        this.categoryExclusions = categoryExclusions;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }

    public void setContentURL(String contentURL) {
        this.contentURL = contentURL;
    }

    public void setPublisherProvidedID(String publisherProvidedID) {
        this.publisherProvidedID = publisherProvidedID;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setAdSize(AdSize adSize) {
        this.adSize = adSize;
    }

    public void setValidAdSizes(AdSize[] adSizes) {
        this.validAdSizes = adSizes;
    }

    public void setCorrelator(String correlator) {
        this.correlator = correlator;
    }

    @Override
    public void onAppEvent(String name, String info) {
        WritableMap event = Arguments.createMap();
        event.putString("name", name);
        event.putString("info", info);
        sendEvent(RNAdManagerBannerViewManager.EVENT_APP_EVENT, event);
    }

    @Override
    public void onHostResume() {
/*
        if (this.adView != null) {
            this.adView.resume();
        }
 */
        if (this.adManagerView != null) {
            this.adManagerView.resume();
        }
    }

    @Override
    public void onHostPause() {
        /*
        if (this.adView != null) {
            this.adView.pause();
        }
         */
        if (this.adManagerView != null) {
            this.adManagerView.pause();
        }
    }

    @Override
    public void onHostDestroy() {
        /*
        if (this.adView != null) {
            this.adView.destroy();
        }
         */
        if (this.adManagerView != null) {
            this.adManagerView.destroy();
        }
    }
}
