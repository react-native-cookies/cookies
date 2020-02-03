/**		
  * Copyright (c) Joseph P. Ferraro		
  *		
  * This source code is licensed under the MIT license found in the		
  * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.		
  */

package com.reactnativecommunity.cookies;

import android.os.Build;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.network.ForwardingCookieHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CookieManagerModule extends ReactContextBaseJavaModule {

    private static final boolean USES_LEGACY_STORE = Build.VERSION.SDK_INT < 21;

    private ForwardingCookieHandler cookieHandler;

    private CookieManager mCookieManager;
    private CookieSyncManager mCookieSyncManager;

    CookieManagerModule(ReactApplicationContext context) {
        super(context);
        this.mCookieSyncManager = CookieSyncManager.createInstance(context);
        this.mCookieManager = CookieManager.getInstance();
        mCookieManager.setAcceptCookie(true);
    }

    public String getName() {
        return "RNCookieManagerAndroid";
    }

    @ReactMethod
    public void set(String url, ReadableMap cookie, Boolean useWebKit, final Promise promise) throws Exception {
        String cookieString = makeCookieString(cookie);

        if (cookieString == null) {
            throw new Exception("Unable to add cookie - invalid values");
        }

        addCookies(url, cookieString, promise);
    }

    @ReactMethod
    public void setFromResponse(String url, ReadableMap cookie, final Promise promise)
            throws URISyntaxException, IOException, Exception {
        String cookieString = makeCookieString(cookie);

        if (cookieString == null) {
            throw new Exception("Unable to add cookie - invalid values");
        }

        addCookies(url, cookieString, promise);
    }

    @ReactMethod
    public void getFromResponse(String url, Promise promise) throws URISyntaxException, IOException {
        promise.resolve(url);
    }

    @ReactMethod
    public void getAll(boolean useWebKit, Promise promise) throws Exception {
        throw new Exception("Cannot get all cookies on android, try getCookieHeader(url)");
    }

    @ReactMethod
    public void get(String url, boolean useWebKit, Promise promise) throws URISyntaxException, IOException {
        URI uri = new URI(url);

        Map<String, List<String>> cookieMap = this.cookieHandler.get(uri, new HashMap());
        // If only the variables were public
        List<String> cookieList = cookieMap.get("Cookie");
        WritableMap map = Arguments.createMap();
        if (cookieList != null) {
            String[] cookies = cookieList.get(0).split(";");
            for (int i = 0; i < cookies.length; i++) {
                String[] cookie = cookies[i].split("=", 2);
                if (cookie.length > 1) {
                  map.putString(cookie[0].trim(), cookie[1]);
                }
            }
        }
        promise.resolve(map);
    }

    @ReactMethod
    public void clearByName(String url, String name, final Promise promise) throws Exception{
        if(url==null){
            throw new Exception("Cannot remove cookie without domain / url");
        }

        String cookieString = name + "=''";

        if (USES_LEGACY_STORE) {
            mCookieManager.setCookie(url, cookieString);
            mCookieSyncManager.sync();
            promise.resolve(true);
        } else {
            mCookieManager.setCookie(url, cookieString,new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    promise.resolve(value);
                }
            });
            mCookieManager.flush();
        }
    }

    @ReactMethod
    public void clearAll(Boolean useWebKit, final Promise promise) {
        if (USES_LEGACY_STORE) {
            mCookieManager.removeAllCookie();
            mCookieManager.removeSessionCookie();
            mCookieSyncManager.sync();
            promise.resolve(true);
        } else {
            mCookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    promise.resolve(value);
                }
            });
            mCookieManager.flush();
        }
    }

    private void addCookies(String url, String cookieString, final Promise promise) {
        if (USES_LEGACY_STORE) {
            mCookieManager.setCookie(url, cookieString);
            mCookieSyncManager.sync();
            promise.resolve(true);
        } else {
            mCookieManager.setCookie(url, cookieString, new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    promise.resolve(value);
                }
            });
        }
    }

    private String makeCookieString(ReadableMap cookie) {
        return null;
    }
}
