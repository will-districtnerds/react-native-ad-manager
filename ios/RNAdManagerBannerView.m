#import "RNAdManagerBannerView.h"

#import <GoogleMobileAds/GoogleMobileAds.h>
#import <React/RCTUtils.h>

#import <React/RCTLog.h>

#include "RCTConvert+GADAdSize.h"
#import "RNAdManagerUtils.h"

@interface RNAdManagerBannerView () <GADBannerViewDelegate, GADAdSizeDelegate, GADAppEventDelegate>

@property (nonatomic, strong) DFPBannerView *bannerView;

@end

@implementation RNAdManagerBannerView

- (void)dealloc
{
    _bannerView.delegate = nil;
    _bannerView.adSizeDelegate = nil;
    _bannerView.appEventDelegate = nil;
    _bannerView.rootViewController = nil;
}

- (void)setAdUnitID:(NSString *)adUnitID
{
  _adUnitID = adUnitID;
    NSLog(@"RNAdManagerBannerView setAdUnitID adUnitID %@", adUnitID);
//  [self createViewIfCan];
}

- (void)setAdSize:(NSString *)adSize
{
  _adSize = adSize;
//  [self createViewIfCan];
}

- (void)setValidAdSizes:(NSArray *)adSizes
{
    __block NSMutableArray *validAdSizes = [[NSMutableArray alloc] initWithCapacity:adSizes.count];
    [adSizes enumerateObjectsUsingBlock:^(id jsonValue, NSUInteger idx, __unused BOOL *stop) {
        GADAdSize adSize = [RCTConvert GADAdSize:jsonValue];
        NSLog(@"RNAdManagerBannerView setValidAdSizes GADAdSize %@", jsonValue);
        if (GADAdSizeEqualToSize(adSize, kGADAdSizeInvalid)) {
            RCTLogWarn(@"Invalid adSize %@", jsonValue);
        } else {
            [validAdSizes addObject:NSValueFromGADAdSize(adSize)];
        }
    }];

    _validAdSizes = validAdSizes;
//    [self createViewIfCan];
}

- (void)setTargeting:(NSDictionary *)targeting
{
  _targeting = targeting;
//  [self createViewIfCan];
}

- (void)setCorrelator:(NSString *)correlator
{
  _correlator = correlator;
}

// Initialise BannerAdView as soon as all the props are set
- (void)createViewIfCan
{
    if (!_adUnitID || !_adSize/* || !_validAdSizes || !_targeting*/) {
        return;
    }

    if (_bannerView) {
        [_bannerView removeFromSuperview];
    }

    GADAdSize adSize = [RCTConvert GADAdSize:_adSize];
    NSLog(@"RNAdManagerBannerView createViewIfCan _adSize %@", _adSize);
    DFPBannerView *bannerView;
    if (!GADAdSizeEqualToSize(adSize, kGADAdSizeInvalid)) {
//        self.bannerView.adSize = adSize;
        bannerView = [[DFPBannerView alloc] initWithAdSize:adSize];
    } else {
        bannerView = [[DFPBannerView alloc] initWithAdSize:kGADAdSizeBanner];
    }

    bannerView.delegate = self;
    bannerView.adSizeDelegate = self;
    bannerView.appEventDelegate = self;
    bannerView.rootViewController = RCTPresentedViewController();
    bannerView.translatesAutoresizingMaskIntoConstraints = YES;

    GADMobileAds.sharedInstance.requestConfiguration.testDeviceIdentifiers = _testDevices;
    DFPRequest *request = [DFPRequest request];

    GADExtras *extras = [[GADExtras alloc] init];
    if (_correlator == nil) {
        _correlator = getCorrelator(_adUnitID);
    }
    extras.additionalParameters = [[NSDictionary alloc] initWithObjectsAndKeys:
                                   _correlator, @"correlator",
                                   nil];
    [request registerAdNetworkExtras:extras];
    
    if (_targeting != nil) {
        NSDictionary *customTargeting = [_targeting objectForKey:@"customTargeting"];
        if (customTargeting != nil) {
            request.customTargeting = customTargeting;
        }
        NSArray *categoryExclusions = [_targeting objectForKey:@"categoryExclusions"];
        if (categoryExclusions != nil) {
            request.categoryExclusions = categoryExclusions;
        }
        NSArray *keywords = [_targeting objectForKey:@"keywords"];
        if (keywords != nil) {
            request.keywords = keywords;
        }
        NSString *contentURL = [_targeting objectForKey:@"contentURL"];
        if (contentURL != nil) {
            request.contentURL = contentURL;
        }
        NSString *publisherProvidedID = [_targeting objectForKey:@"publisherProvidedID"];
        if (publisherProvidedID != nil) {
            request.publisherProvidedID = publisherProvidedID;
        }
        NSDictionary *location = [_targeting objectForKey:@"location"];
        if (location != nil) {
            CGFloat latitude = [[location objectForKey:@"latitude"] doubleValue];
            CGFloat longitude = [[location objectForKey:@"longitude"] doubleValue];
            CGFloat accuracy = [[location objectForKey:@"accuracy"] doubleValue];
            [request setLocationWithLatitude:latitude longitude:longitude accuracy:accuracy];
        }
    }

    bannerView.adUnitID = _adUnitID;

    bannerView.validAdSizes = _validAdSizes;

    [bannerView loadRequest:request];

    [self addSubview:bannerView];

    _bannerView = bannerView;
}

- (void)loadBanner {
    [self createViewIfCan];
}

# pragma mark GADBannerViewDelegate

/// Tells the delegate an ad request loaded an ad.
- (void)adViewDidReceiveAd:(DFPBannerView *)adView
{
    if (self.onSizeChange) {
        self.onSizeChange(@{
                            @"type": @"banner",
                            @"width": @(adView.frame.size.width),
                            @"height": @(adView.frame.size.height) });
    }
    if (self.onAdLoaded) {
        self.onAdLoaded(@{
            @"type": @"banner",
            @"gadSize": NSValueFromGADAdSize(adView.adSize),
        });
    }
}

/// Tells the delegate an ad request failed.
- (void)adView:(DFPBannerView *)adView
didFailToReceiveAdWithError:(GADRequestError *)error
{
    if (self.onAdFailedToLoad) {
        self.onAdFailedToLoad(@{ @"error": @{ @"message": [error localizedDescription] } });
    }
    _bannerView.delegate = nil;
    _bannerView.adSizeDelegate = nil;
    _bannerView.appEventDelegate = nil;
    _bannerView.rootViewController = nil;
    _bannerView = nil;
}

/// Tells the delegate that a full screen view will be presented in response
/// to the user clicking on an ad.
- (void)adViewWillPresentScreen:(DFPBannerView *)adView
{
    if (self.onAdOpened) {
        self.onAdOpened(@{});
    }
}

/// Tells the delegate that the full screen view will be dismissed.
- (void)adViewWillDismissScreen:(__unused DFPBannerView *)adView
{
    if (self.onAdClosed) {
        self.onAdClosed(@{});
    }
}

/// Tells the delegate that a user click will open another app (such as
/// the App Store), backgrounding the current app.
- (void)adViewWillLeaveApplication:(DFPBannerView *)adView
{
    if (self.onAdLeftApplication) {
        self.onAdLeftApplication(@{});
    }
}

# pragma mark GADAdSizeDelegate

- (void)adView:(GADBannerView *)bannerView willChangeAdSizeTo:(GADAdSize)size
{
    CGSize adSize = CGSizeFromGADAdSize(size);
    self.onSizeChange(@{
                        @"type": @"banner",
                        @"width": @(adSize.width),
                        @"height": @(adSize.height) });
}

# pragma mark GADAppEventDelegate

- (void)adView:(GADBannerView *)banner didReceiveAppEvent:(NSString *)name withInfo:(NSString *)info
{
    if (self.onAppEvent) {
        self.onAppEvent(@{ @"name": name, @"info": info });
    }
}

@end
