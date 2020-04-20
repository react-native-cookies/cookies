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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CookieManagerModule extends ReactContextBaseJavaModule {

    private static final boolean USES_LEGACY_STORE = Build.VERSION.SDK_INT < 21;

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
    public void set(String url, ReadableMap cookie, Boolean useWebKit, final Promise promise) {
        String cookieString = makeCookieString(url, cookie);

        if (cookieString == null) {
            promise.reject(new Exception("Unable to add cookie - invalid values"));
            return;
        }

        addCookies(url, cookieString, promise);
    }

    @ReactMethod
    public void setFromResponse(String url, String cookie, final Promise promise) {
        if (cookie == null) {
            promise.reject(new Exception("Unable to add cookie - invalid values"));
            return;
        }

        addCookies(url, cookie, promise);
    }

    @ReactMethod
    public void getFromResponse(String url, Promise promise) throws URISyntaxException, IOException {
        promise.resolve(url);
    }

    @ReactMethod
    public void getAll(Boolean useWebKit, Promise promise) {
        promise.reject(new Exception("Get all cookies not supported for Android (iOS only)"));
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
    public void clearByName(String url, String name, Boolean useWebKit, final Promise promise) {
        promise.reject(new Exception("Cannot remove a single cookie by name on Android"));
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

        Map<String, Cookie> cookieObjects = getCookieObjects(url);

        WritableMap allCookiesMap = Arguments.createMap();

        Set<String> keys = cookieObjects.keySet();
        for (String key : keys) {
            Cookie cookie = cookieObjects.get(key);
            if (cookie != null) {
                WritableMap cookieMap = Arguments.createMap();
                cookieMap.putString("name", cookie.name());
                cookieMap.putString("value", cookie.value());
                cookieMap.putString("domain", cookie.domain());
                cookieMap.putString("path", cookie.path());
                cookieMap.putBoolean("secure", cookie.secure());
                cookieMap.putBoolean("httpOnly", cookie.httpOnly());

                long expires = cookie.expiresAt();
                cookieMap.putString("expiration", new Date(expires).toString());

                allCookiesMap.putMap(cookie.name(), cookieMap);
            }
        }

        return allCookiesMap;
    }

    private Map<String, Cookie> getCookieObjects(String url) throws Exception {
        if (url == null || url.equals("")) {
            throw new Exception("Cannot get cookies without a url");
        }

        Map<String, Cookie> allCookiesMap = new HashMap<>();

        String cookiesString = mCookieManager.getCookie(url);
        if (cookiesString != null && !cookiesString.equals("")) {
            String[] cookieHeaders = cookiesString.split(";");
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl != null) {
                for (String cookieString : cookieHeaders) {
                    Cookie cookie = Cookie.parse(httpUrl, cookieString);
                    if (cookie != null) {
                        String name = cookie.name();
                        String value = cookie.value();
                        if (name != null && name != "" && value != null && value != "") {
                            allCookiesMap.put(name, cookie);
                        }
                    }
                }
            }
        }
        return allCookiesMap;
    }

    private String makeCookieString(String url, ReadableMap cookie) {
        Cookie.Builder cookieBuilder = buildCookie(url, cookie);

        if (cookieBuilder == null) {
            return null;
        }

        return cookieBuilder.build().toString();
    }

    private Cookie.Builder buildCookie(String url, ReadableMap cookie) {
        Date date = null;
        try {
            date = SimpleDateFormat.getDateTimeInstance().parse(cookie.getString("expiration"));
        } catch (Exception ignored) {

        }

        String extractedDomain = null;
        try {
            URL parsedUrl = new URL(url);
            extractedDomain = parsedUrl.getHost();
        } catch (Exception e) {

        }

        Cookie.Builder cookieBuilder = new Cookie.Builder().name(cookie.getString("name"))
                .value(cookie.getString("value"));

        if (cookie.hasKey("domain") && cookie.getString("domain") != null && !cookie.getString("domain").isEmpty()) {
            String domain = cookie.getString("domain");
            // take off leading dot as Android doesn't like it
            if (domain.startsWith(".")) {
                domain = domain.substring(1);
            }
            cookieBuilder.domain(domain);
        } else if (extractedDomain != null && !extractedDomain.isEmpty()) {
            cookieBuilder.domain(extractedDomain);
        } else {
            // assume something went terribly wrong here and no cookie can be created
            return null;
        }

        if (cookie.hasKey("path") && cookie.getString("path") != null && !cookie.getString("path").isEmpty()) {
            cookieBuilder.path(cookie.getString("path"));
        }
        // unlike iOS, Android will handle no path gracefully and assume "/""

        if (date != null) {
            cookieBuilder.expiresAt(date.getTime());
        }

        if (cookie.hasKey("secure") && cookie.getBoolean("secure")) {
            cookieBuilder.secure();
        }

        if (cookie.hasKey("httpOnly") && cookie.getBoolean("httpOnly")) {
            cookieBuilder.httpOnly();
        }

        return cookieBuilder;
    }
}
