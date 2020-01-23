/**		
  * Copyright (c) Joseph P. Ferraro		
  *		
  * This source code is licensed under the MIT license found in the		
  * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.		
  */

#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif

#import <WebKit/WebKit.h>

@interface RNCookieManagerIOS : NSObject <RCTBridgeModule>

@property (nonatomic, strong) NSDateFormatter *formatter;

@end
