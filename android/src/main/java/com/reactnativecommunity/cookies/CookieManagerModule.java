/**
 * Copyright (c) Joseph P. Ferraro
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.
 */

package com.reactnativecommunity.cookies;

import android.os.Build;
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
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CookieManagerModule extends ReactContextBaseJavaModule {

    private static final boolean USES_LEGACY_STORE = Build.VERSION.SDK_INT < 21;

    private CookieManager mCookieManager;
    private CookieSyncManager mCookieSyncManager;

    ForwardingCookieHandler handler;

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
    public void set(String url, ReadableMap cookie, Boolean useWebKit, final Promise promise) {
        String cookieString = makeCookieString(cookie);

        if (cookieString == null) {
            promise.reject(new Exception("Unable to add cookie - invalid values"));
        }

        addCookies(url, cookieString, promise);
    }

    @ReactMethod
    public void setFromResponse(String url, ReadableMap cookie, final Promise promise) {
        String cookieString = makeCookieString(cookie);

        if (cookieString == null) {
            promise.reject(new Exception("Unable to add cookie - invalid values"));
        }

        addCookies(url, cookieString, promise);
    }

    @ReactMethod
    public void getFromResponse(String url, Promise promise) throws URISyntaxException, IOException {
        promise.resolve(url);
    }

    @ReactMethod
    public void get(String url, Boolean useWebKit, Promise promise) {
        try {
            WritableMap cookieMap = getCookies(url);
            promise.resolve(cookieMap);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void getAll(Boolean useWebKit, Promise promise) {
        promise.reject(new Exception("Cannot get all available cookies in store for Android"));
    }

    @ReactMethod
    public void clearByName(String url, String name, Boolean useWebKit, final Promise promise) {
        if (url == null) {
            promise.reject(new Exception("Cannot remove cookie without domain / url"));
        }

        String cookieString = name + "=''";

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
        try {
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
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    private WritableMap getCookies(String url) throws Exception {
        if (url == null || url.equals("")) {
            throw new Exception("Cannot get cookies without a url");
        }

        WritableMap allCookiesMap = Arguments.createMap();

        String cookiesString = mCookieManager.getCookie(url);
        if (cookiesString != null && !cookiesString.equals("")) {
            String[] cookieHeaders = cookiesString.split(";");
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl != null) {
                for (String cookieString : cookieHeaders) {
                    Cookie cookie = Cookie.parse(httpUrl, cookieString);
                    if (cookie != null) {
                        WritableMap cookieMap = Arguments.createMap();
                        cookieMap.putString("name", cookie.name());
                        cookieMap.putString("value", cookie.value());
                        cookieMap.putString("domain", cookie.domain());
                        cookieMap.putString("path", cookie.path());
                        cookieMap.putString("origin", url);
                        // no version unavailable from this interface
                        // cookieMap.putInt("version", cookie.getVersion());
                        cookieMap.putBoolean("secure", cookie.secure());
                        cookieMap.putBoolean("httpOnly", cookie.httpOnly());

                        cookieMap.putString("expiration", new Date(cookie.expiresAt()).toString());

                        allCookiesMap.putMap(cookie.name(), cookieMap);
                    }
                }
            }
        }
        return allCookiesMap;
    }

    private String makeCookieString(ReadableMap cookie) {
        Date date = null;
        try {
            date = SimpleDateFormat.getDateTimeInstance().parse(cookie.getString("expiration"));
        } catch (Exception ignored) {

        }
        Cookie.Builder cookieBuilder = new Cookie.Builder()
                .name(cookie.getString("name"))
                .value(cookie.getString("value"));

        if (cookie.hasKey("path") && cookie.getBoolean("secure")) {
            cookieBuilder.secure();
        }

        if (cookie.hasKey("domain") && cookie.getString("domain") != null
                && !cookie.getString("domain").isEmpty()) {
            cookieBuilder.domain(cookie.getString("domain"));
        }

        if (date != null) {
            cookieBuilder.expiresAt(date.getTime());
        }

        if (cookie.hasKey("secure") && cookie.getBoolean("secure")) {
            cookieBuilder.secure();
        }

        if (cookie.hasKey("httpOnly") && cookie.getBoolean("httpOnly")) {
            cookieBuilder.httpOnly();
        }

        return cookieBuilder.build().toString();
    }
}
