/**
  * Copyright (c) Joseph P. Ferraro
  *
  * This source code is licensed under the MIT license found in the
  * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.
  */

#import <React/RCTBridgeModule.h>

#if __has_include(<WebKit/WebKit.h>)
#import <WebKit/WebKit.h>
#endif

@interface RNCookieManagerIOS : NSObject <RCTBridgeModule>

@property (nonatomic, strong) NSDateFormatter *formatter;

@end
