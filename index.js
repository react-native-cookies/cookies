/**
 * Copyright (c) Joseph P. Ferraro
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.
 */

import { NativeModules, Platform } from 'react-native';
const invariant = require('invariant');
const RNCookieManagerIOS = NativeModules.RNCookieManagerIOS;
const RNCookieManagerAndroid = NativeModules.RNCookieManagerAndroid;
const RNCookieManagerMacOS = NativeModules.RNCookieManagerMacOS;

let CookieManager;

if (Platform.OS === 'ios') {
  invariant(
    RNCookieManagerIOS,
    '@react-native-community/cookies: Add RNCookieManagerIOS.h and RNCookieManagerIOS.m to your Xcode project',
  );
  CookieManager = RNCookieManagerIOS;
} else if (Platform.OS === 'android') {
  invariant(
    RNCookieManagerAndroid,
    '@react-native-community/cookies: Import libraries to android "react-native link @react-native-community/cookies"',
  );
  CookieManager = RNCookieManagerAndroid;
} else if (Platform.OS === 'macos') {
  invariant(
    RNCookieManagerMacOS,
    '@react-native-community/cookies: Add RNCookieManagerMacOS.h and RNCookieManagerMacOS.m to your Xcode project',
  );
  CookieManager = RNCookieManagerMacOS;
} else {
  invariant(
    CookieManager,
    '@react-native-community/cookies: Invalid platform. This library only supports Android, iOS and MacOS.',
  );
}

const functions = ['setFromResponse', 'getFromResponse'];

module.exports = {
  getAll: (useWebKit = false) => CookieManager.getAll(useWebKit),
  clearAll: (useWebKit = false) => CookieManager.clearAll(useWebKit),
  get: (url, useWebKit = false) => CookieManager.get(url, useWebKit),
  set: (url, cookie, useWebKit = false) =>
    CookieManager.set(url, cookie, useWebKit),
  clearByName: (url, name, useWebKit = false) =>
    CookieManager.clearByName(url, name, useWebKit),
  flush: async () => {
    if (Platform.OS === 'android') {
      await CookieManager.flush();
    }
  },
};

for (var i = 0; i < functions.length; i++) {
  module.exports[functions[i]] = CookieManager[functions[i]];
}
