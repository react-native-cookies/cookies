/**
  * Copyright (c) Joseph P. Ferraro
  *
  * This source code is licensed under the MIT license found in the
  * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.
  */

#import "RNCookieManagerIOS.h"
#if __has_include("RCTConvert.h")
#import "RCTConvert.h"
#else
#import <React/RCTConvert.h>
#endif

static NSString * const NOT_AVAILABLE_ERROR_MESSAGE = @"WebKit/WebKit-Components are only available with iOS11 and higher!";
static NSString * const INVALID_URL_MISSING_HTTP = @"Invalid URL: It may be missing a protocol (ex. http:// or https://).";

static inline BOOL isEmpty(id value)
{
    return value == nil
        || ([value respondsToSelector:@selector(length)] && [(NSData *)value length] == 0)
        || ([value respondsToSelector:@selector(count)] && [(NSArray *)value count] == 0);
}

@implementation RNCookieManagerIOS

- (instancetype)init
{
    self = [super init];
    if (self) {
        self.formatter = [NSDateFormatter new];
        [self.formatter setDateFormat:@"yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ"];
    }
    return self;
}

+ (BOOL)requiresMainQueueSetup
{
    return NO;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(
    set:(NSDictionary *)props
    useWebKit:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject)
{
    NSString *name = [RCTConvert NSString:props[@"name"]];
    NSString *value = [RCTConvert NSString:props[@"value"]];
    NSString *path = [RCTConvert NSString:props[@"path"]];
    NSString *domain = [RCTConvert NSString:props[@"domain"]];
    NSString *origin = [RCTConvert NSString:props[@"origin"]];
    NSString *version = [RCTConvert NSString:props[@"version"]];
    NSDate *expiration = [RCTConvert NSDate:props[@"expiration"]];

    NSMutableDictionary *cookieProperties = [NSMutableDictionary dictionary];
    [cookieProperties setObject:name forKey:NSHTTPCookieName];
    [cookieProperties setObject:value forKey:NSHTTPCookieValue];
    [cookieProperties setObject:path forKey:NSHTTPCookiePath];
    if (!isEmpty(domain)) {
        [cookieProperties setObject:domain forKey:NSHTTPCookieDomain];
    }
    if (!isEmpty(origin)) {
        [cookieProperties setObject:origin forKey:NSHTTPCookieOriginURL];
    }
    if (!isEmpty(version)) {
         [cookieProperties setObject:version forKey:NSHTTPCookieVersion];
    }
    if (!isEmpty(expiration)) {
         [cookieProperties setObject:expiration forKey:NSHTTPCookieExpires];
    }

    NSHTTPCookie *cookie = [NSHTTPCookie cookieWithProperties:cookieProperties];

    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                WKHTTPCookieStore *cookieStore = [[WKWebsiteDataStore defaultDataStore] httpCookieStore];
                [cookieStore setCookie:cookie completionHandler:nil];
                resolve(nil);
            });
        } else {
            reject(@"", NOT_AVAILABLE_ERROR_MESSAGE, nil);
        }
    } else {
        [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookie:cookie];
        resolve(nil);
    }
}

RCT_EXPORT_METHOD(setFromResponse:(NSURL *)url
    value:(NSString *)value
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject) {
    NSArray *cookies = [NSHTTPCookie cookiesWithResponseHeaderFields:@{@"Set-Cookie": value} forURL:url];
    [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookies:cookies forURL:url mainDocumentURL:nil];
    resolve(nil);
}

RCT_EXPORT_METHOD(getFromResponse:(NSURL *)url
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject) {
    NSURLRequest *request = [NSURLRequest requestWithURL:url];
    [NSURLConnection sendAsynchronousRequest:request  queue:[[NSOperationQueue alloc] init]
                           completionHandler:^(NSURLResponse *response, NSData *data, NSError *error) {

        NSHTTPURLResponse *httpResponse = (NSHTTPURLResponse *)response;
        NSArray *cookies = [NSHTTPCookie cookiesWithResponseHeaderFields:httpResponse.allHeaderFields forURL:response.URL];
        NSMutableDictionary *dics = [NSMutableDictionary dictionary];

        for (int i = 0; i < cookies.count; i++) {
            NSHTTPCookie *cookie = [cookies objectAtIndex:i];
            [dics setObject:cookie.value forKey:cookie.name];
            [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookie:cookie];
        }
        resolve(dics);
    }];
}

RCT_EXPORT_METHOD(
    get:(NSURL *) url
    useWebKit:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject)
{
    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                NSString *topLevelDomain = url.host;

                if (topLevelDomain == nil) {
                    reject(@"", INVALID_URL_MISSING_HTTP, nil);
                    return;
                }

                WKHTTPCookieStore *cookieStore = [[WKWebsiteDataStore defaultDataStore] httpCookieStore];
                [cookieStore getAllCookies:^(NSArray<NSHTTPCookie *> *allCookies) {
                    NSMutableDictionary *cookies = [NSMutableDictionary dictionary];
                    for (NSHTTPCookie *cookie in allCookies) {
                        if ([topLevelDomain containsString:cookie.domain]) {
                            [cookies setObject:[self createCookieData:cookie] forKey:cookie.name];
                        }
                    }
                    resolve(cookies);
                }];
            });
        } else {
            reject(@"", NOT_AVAILABLE_ERROR_MESSAGE, nil);
        }
    } else {
        NSMutableDictionary *cookies = [NSMutableDictionary dictionary];
        for (NSHTTPCookie *cookie in [[NSHTTPCookieStorage sharedHTTPCookieStorage] cookiesForURL:url]) {
            [cookies setObject:[self createCookieData:cookie] forKey:cookie.name];
        }
        resolve(cookies);
    }
}

RCT_EXPORT_METHOD(
    clearAll:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject)
{
    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                // https://stackoverflow.com/questions/46465070/how-to-delete-cookies-from-wkhttpcookiestore#answer-47928399
                NSSet *websiteDataTypes = [NSSet setWithArray:@[WKWebsiteDataTypeCookies]];
                NSDate *dateFrom = [NSDate dateWithTimeIntervalSince1970:0];
                [[WKWebsiteDataStore defaultDataStore] removeDataOfTypes:websiteDataTypes
                                                        modifiedSince:dateFrom
                                                        completionHandler:^() {
                                                            resolve(nil);
                                                        }];
            });
        } else {
            reject(@"", NOT_AVAILABLE_ERROR_MESSAGE, nil);
        }
    } else {
        NSHTTPCookieStorage *cookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
        for (NSHTTPCookie *c in cookieStorage.cookies) {
            [cookieStorage deleteCookie:c];
        }
        resolve(nil);
    }
}

RCT_EXPORT_METHOD(clearByName:(NSString *) name
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject) {
    NSHTTPCookieStorage *cookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
    for (NSHTTPCookie *c in cookieStorage.cookies) {
      if ([[c name] isEqualToString:name]) {
        [cookieStorage deleteCookie:c];
      }
    }
    resolve(nil);
}

RCT_EXPORT_METHOD(
    getAll:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject)
{
    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                WKHTTPCookieStore *cookieStore = [[WKWebsiteDataStore defaultDataStore] httpCookieStore];
                [cookieStore getAllCookies:^(NSArray<NSHTTPCookie *> *allCookies) {
                    resolve([self createCookieList: allCookies]);
                }];
            });
        } else {
            reject(@"", NOT_AVAILABLE_ERROR_MESSAGE, nil);
        }
    } else {
        NSHTTPCookieStorage *cookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
        resolve([self createCookieList:cookieStorage.cookies]);
    }
}

-(NSDictionary *)createCookieList:(NSArray<NSHTTPCookie *>*)cookies
{
    NSMutableDictionary *cookieList = [NSMutableDictionary dictionary];
    for (NSHTTPCookie *cookie in cookies) {
        [cookieList setObject:[self createCookieData:cookie] forKey:cookie.name];
    }
    return cookieList;
}

-(NSDictionary *)createCookieData:(NSHTTPCookie *)cookie
{
    NSMutableDictionary *cookieData = [NSMutableDictionary dictionary];
    [cookieData setObject:cookie.name forKey:@"name"];
    [cookieData setObject:cookie.value forKey:@"value"];
    [cookieData setObject:cookie.path forKey:@"path"];
    [cookieData setObject:cookie.domain forKey:@"domain"];
    [cookieData setObject:[NSString stringWithFormat:@"%@", @(cookie.version)] forKey:@"version"];
    if (!isEmpty(cookie.expiresDate)) {
        [cookieData setObject:[self.formatter stringFromDate:cookie.expiresDate] forKey:@"expiration"];
    }
    return cookieData;
}

@end
