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
} else {
  invariant(
    CookieManager,
    '@react-native-community/cookies: Invalid platform. This library only supports Android and iOS.',
  );
}

module.exports = {
  getAll: (useWebKit = false) => CookieManager.getAll(useWebKit),
  clearAll: (useWebKit = false) => CookieManager.clearAll(useWebKit),
  get: (url, useWebKit = false) => CookieManager.get(url, useWebKit),
  getFromResponse: (url, useWebKit = false) =>
	CookieManager.getFromResponse(url, useWebKit),
  set: (url, cookie, useWebKit = false) =>
    CookieManager.set(url, cookie, useWebKit),
  setFromResponse: (url, cookie, useWebKit = false) =>
  	CookieManager.setFromResponse(url, cookie, useWebKit),
  clearByName: (url, name, useWebKit = false) =>
    CookieManager.clearByName(url, name, useWebKit),
  flush: async () => {
    if (Platform.OS === 'android') {
      await CookieManager.flush();
    }
  },
};
