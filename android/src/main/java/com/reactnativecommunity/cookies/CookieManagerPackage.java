/**		
  * Copyright (c) Joseph P. Ferraro		
  *		
  * This source code is licensed under the MIT license found in the		
  * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.		
  */

package com.reactnativecommunity.cookies;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CookieManagerPackage implements ReactPackage {

    public CookieManagerPackage() {}

    @Override
    public List<NativeModule> createNativeModules(
            ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();

        modules.add(new CookieManagerModule(reactContext));
        return modules;
    }

    // Deprecated
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList();
    }
}
