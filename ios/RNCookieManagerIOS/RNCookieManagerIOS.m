/**
  * Copyright (c) Joseph P. Ferraro
  *
  * This source code is licensed under the MIT license found in the
  * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.
  */

#import "RNCookieManagerIOS.h"
#import <React/RCTConvert.h>

static NSString * const NOT_AVAILABLE_ERROR_MESSAGE = @"WebKit/WebKit-Components are only available with iOS11 and higher!";
static NSString * const INVALID_URL_MISSING_HTTP = @"Invalid URL: It may be missing a protocol (ex. http:// or https://).";
static NSString * const INVALID_DOMAINS = @"Cookie URL host %@ and domain %@ mismatched. The cookie won't set correctly.";

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
    set:(NSURL *)url
    cookie:(NSDictionary *)props
    useWebKit:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject)
{
    NSHTTPCookie *cookie;
    @try {
        cookie = [self makeHTTPCookieObject:url props:props];
    }
    @catch ( NSException *e ) {
        reject(@"", [e reason], nil);
        return;
    }

    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                WKHTTPCookieStore *cookieStore = [[WKWebsiteDataStore defaultDataStore] httpCookieStore];
                [cookieStore setCookie:cookie completionHandler:^() {
                    resolve(@(YES));
                }];
            });
        } else {
            reject(@"", NOT_AVAILABLE_ERROR_MESSAGE, nil);
        }
    } else {
        [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookie:cookie];
        resolve(@(YES));
    }
}

RCT_EXPORT_METHOD(
    setFromResponse:(NSURL *)url
    cookie:(NSString *)cookie
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject) {

    NSArray *cookies = [NSHTTPCookie cookiesWithResponseHeaderFields:@{@"Set-Cookie": cookie} forURL:url];
    [[NSHTTPCookieStorage sharedHTTPCookieStorage] setCookies:cookies forURL:url mainDocumentURL:nil];
    resolve(@(YES));
}

RCT_EXPORT_METHOD(
    getFromResponse:(NSURL *)url
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
    get:(NSURL *)url
    useWebKit:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject)
{
    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                NSString *topLevelDomain = url.host;

                if (isEmpty(topLevelDomain)) {
                    reject(@"", INVALID_URL_MISSING_HTTP, nil);
                    return;
                }

                WKHTTPCookieStore *cookieStore = [[WKWebsiteDataStore defaultDataStore] httpCookieStore];
                [cookieStore getAllCookies:^(NSArray<NSHTTPCookie *> *allCookies) {
                    NSMutableDictionary *cookies = [NSMutableDictionary dictionary];
                    for (NSHTTPCookie *cookie in allCookies) {
                        if ([topLevelDomain containsString:cookie.domain] ||
                            [cookie.domain isEqualToString: topLevelDomain]) {
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
                                                            resolve(@(YES));
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
        [[NSUserDefaults standardUserDefaults] synchronize];
        resolve(@(YES));
    }
}

RCT_EXPORT_METHOD(
    clearByName:(NSURL *)url
    name:(NSString *)name
    useWebKit:(BOOL)useWebKit
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject) {
    __block NSNumber * foundCookies = @NO;
    NSMutableArray<NSHTTPCookie *> * foundCookiesList = [NSMutableArray new];

    if (useWebKit) {
        if (@available(iOS 11.0, *)) {
            dispatch_async(dispatch_get_main_queue(), ^(){
                NSString *topLevelDomain = url.host;

                if (isEmpty(topLevelDomain)) {
                    reject(@"", INVALID_URL_MISSING_HTTP, nil);
                    return;
                }

                WKHTTPCookieStore *cookieStore = [[WKWebsiteDataStore defaultDataStore] httpCookieStore];
                [cookieStore getAllCookies:^(NSArray<NSHTTPCookie *> *allCookies) {
                    for (NSHTTPCookie *cookie in allCookies) {
                        if ([name isEqualToString:cookie.name] && [self isMatchingDomain:topLevelDomain cookieDomain:cookie.domain]) {
                             [foundCookiesList addObject:cookie];
                             foundCookies = @YES;
                        }
                    }
                    for (NSHTTPCookie *fCookie in foundCookiesList) {
                        [cookieStore deleteCookie:fCookie completionHandler:nil];
                    }
                    resolve(foundCookies);
                }];
            });
        } else {
            reject(@"", NOT_AVAILABLE_ERROR_MESSAGE, nil);
        }
    } else {
           NSHTTPCookieStorage *cookieStorage = [NSHTTPCookieStorage sharedHTTPCookieStorage];
           for (NSHTTPCookie *c in cookieStorage.cookies) {
               if ([[c name] isEqualToString:name] && [self isMatchingDomain:url.host cookieDomain:c.domain]) {
                   [cookieStorage deleteCookie:c];
                   foundCookies = @YES;
               }
           }
           resolve(foundCookies);
    }
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

-(NSHTTPCookie *)makeHTTPCookieObject:(NSURL *)url
    props:(NSDictionary *)props
{
    NSString *topLevelDomain = url.host;

    if (isEmpty(topLevelDomain)){
        NSException* myException = [NSException
            exceptionWithName:@"Exception"
            reason:INVALID_URL_MISSING_HTTP
            userInfo:nil];
        @throw myException;
    }

    NSString *name = [RCTConvert NSString:props[@"name"]];
    NSString *value = [RCTConvert NSString:props[@"value"]];
    NSString *path = [RCTConvert NSString:props[@"path"]];
    NSString *domain = [RCTConvert NSString:props[@"domain"]];
    NSString *version = [RCTConvert NSString:props[@"version"]];
    NSDate *expires = [RCTConvert NSDate:props[@"expires"]];
    NSNumber *secure = [RCTConvert NSNumber:props[@"secure"]];
    NSNumber *httpOnly = [RCTConvert NSNumber:props[@"httpOnly"]];

    NSMutableDictionary *cookieProperties = [NSMutableDictionary dictionary];
    [cookieProperties setObject:name forKey:NSHTTPCookieName];
    [cookieProperties setObject:value forKey:NSHTTPCookieValue];

    if (!isEmpty(path)) {
        [cookieProperties setObject:path forKey:NSHTTPCookiePath];
    } else {
        [cookieProperties setObject:@"/" forKey:NSHTTPCookiePath];
    }
    if (!isEmpty(domain)) {
        // Stripping the leading . to ensure the following check is accurate
        NSString *strippedDomain = domain;
         if ([strippedDomain hasPrefix:@"."]) {
            strippedDomain = [strippedDomain substringFromIndex:1];
        }

        if (![topLevelDomain containsString:strippedDomain] &&
            ![topLevelDomain isEqualToString: strippedDomain]) {
                NSException* myException = [NSException
                    exceptionWithName:@"Exception"
                    reason: [NSString stringWithFormat:INVALID_DOMAINS, topLevelDomain, domain]
                    userInfo:nil];
                @throw myException;
        }

        [cookieProperties setObject:domain forKey:NSHTTPCookieDomain];
    } else {
        [cookieProperties setObject:topLevelDomain forKey:NSHTTPCookieDomain];
    }
    if (!isEmpty(version)) {
         [cookieProperties setObject:version forKey:NSHTTPCookieVersion];
    }
    if (!isEmpty(expires)) {
         [cookieProperties setObject:expires forKey:NSHTTPCookieExpires];
    }
    if ([secure boolValue]) {
        [cookieProperties setObject:secure forKey:NSHTTPCookieSecure];
    }
    if ([httpOnly boolValue]) {
        [cookieProperties setObject:httpOnly forKey:@"HttpOnly"];
    }

    NSHTTPCookie *cookie = [NSHTTPCookie cookieWithProperties:cookieProperties];

    return cookie;
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
        [cookieData setObject:[self.formatter stringFromDate:cookie.expiresDate] forKey:@"expires"];
    }
    [cookieData setObject:[NSNumber numberWithBool:(BOOL)cookie.secure] forKey:@"secure"];
    [cookieData setObject:[NSNumber numberWithBool:(BOOL)cookie.HTTPOnly] forKey:@"httpOnly"];
    return cookieData;
}

-(BOOL)isMatchingDomain:(NSString *)originDomain
      cookieDomain:(NSString *)cookieDomain
{
    if ([originDomain isEqualToString: cookieDomain]) {
        return @YES;
    }
    NSString *parentDomain = [cookieDomain hasPrefix:@"."] ? cookieDomain : [@"." stringByAppendingString: cookieDomain];
    return [originDomain hasSuffix:parentDomain];
}

@end
