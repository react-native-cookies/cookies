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
import java.net.HttpCookie;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CookieManagerModule extends ReactContextBaseJavaModule {

    private static final boolean USES_LEGACY_STORE = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    private static final boolean HTTP_ONLY_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    private static final String INVALID_URL_MISSING_HTTP = "Invalid URL: It may be missing a protocol (ex. http:// or https://).";
    private static final String INVALID_COOKIE_VALUES = "Unable to add cookie - invalid values";
    private static final String GET_ALL_NOT_SUPPORTED = "Get all cookies not supported for Android (iOS only)";
    private static final String CLEAR_BY_NAME_NOT_SUPPORTED = "Cannot remove a single cookie by name on Android";
    private static final String INVALID_DOMAINS = "Cookie URL host %s and domain %s mismatched. The cookie won't set correctly.";

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
        String cookieString = null;
        try {
            cookieString = makeHTTPCookieObject(url, cookie).toString();
        } catch (Exception e) {
            promise.reject(e);
            return;
        }

        if (cookieString == null) {
            promise.reject(new Exception(INVALID_COOKIE_VALUES));
            return;
        }

        addCookies(url, cookieString, promise);
    }

    @ReactMethod
    public void setFromResponse(String url, String cookie, final Promise promise) {
        if (cookie == null) {
            promise.reject(new Exception(INVALID_COOKIE_VALUES));
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
        promise.reject(new Exception(GET_ALL_NOT_SUPPORTED));
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
        promise.reject(new Exception(CLEAR_BY_NAME_NOT_SUPPORTED));
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

        Map<String, HttpCookie> cookieObjects = getCookieObjects(url);

        WritableMap allCookiesMap = Arguments.createMap();

        Set<String> keys = cookieObjects.keySet();
        for (String key : keys) {
            HttpCookie cookie = cookieObjects.get(key);
            if (cookie != null) {
                WritableMap cookieMap = createCookieData(cookie);
                allCookiesMap.putMap(cookie.getName(), cookieMap);
            }
        }

        return allCookiesMap;
    }

    private Map<String, HttpCookie> getCookieObjects(String url) throws Exception {
        if (isEmpty(url)) {
            throw new Exception("Cannot get cookies without a url");
        }

        Map<String, HttpCookie> allCookiesMap = new HashMap<>();

        String cookiesString = mCookieManager.getCookie(url);
        if (cookiesString != null && !cookiesString.equals("")) {
            String[] cookieHeaders = cookiesString.split(";");
            for (String singleCookie : cookieHeaders) {
                List<HttpCookie> cookies = HttpCookie.parse(singleCookie);
                for (HttpCookie cookie : cookies) {
                    if (cookie != null) {
                        String name = cookie.getName();
                        String value = cookie.getValue();
                        if (name != null && !name.equals("") && value != null && !value.equals("")) {
                            allCookiesMap.put(name, cookie);
                        }
                    }
                }
            }
        }
        return allCookiesMap;
    }

    private HttpCookie makeHTTPCookieObject(String url, ReadableMap cookie) throws Exception {
        URL parsedUrl = null;
        try {
            parsedUrl = new URL(url);
        } catch (Exception e) {
            throw new Exception("Invalid URL");
        }

        String topLevelDomain = url.getHost();

        if (isEmpty(topLevelDomain)) {
            // assume something went terribly wrong here and no cookie can be created
            throw new Exception("Cookie URL contains no valid host");
        }

        HttpCookie cookieBuilder = new HttpCookie(cookie.getString("name"), cookie.getString("value"));

        if (cookie.hasKey("domain") && !isEmpty(cookie.getString("domain"))) {
            String domain = cookie.getString("domain");
            // strip the leading . as Android doesn't take it
            // but will include subdomains by default
            if (domain.startsWith(".")) {
                domain = domain.substring(1);
            }

            if (!topLevelDomain.contains(domain) && !topLevelDomain.equals(domain)) {
                throw new Exception(String.format(INVALID_DOMAINS, topLevelDomain, domain));
            }

            cookieBuilder.setDomain(domain);
        } else {
            cookieBuilder.setDomain(topLevelDomain);
        }

        // unlike iOS, Android will handle no path gracefully and assume "/""
        if (cookie.hasKey("path") && !isEmpty(cookie.getString("path"))) {
            cookieBuilder.setPath(cookie.getString("path"));
        }

        Date date = null;
        try {
            date = SimpleDateFormat.getDateTimeInstance().parse(cookie.getString("expiration"));
        } catch (Exception ignored) {

        }

        if (date != null) {
            cookieBuilder.setMaxAge(date.getTime());
        }

        if (cookie.hasKey("secure") && cookie.getBoolean("secure")) {
            cookieBuilder.setSecure(true);
        }

        if (HTTP_ONLY_SUPPORTED) {
            if (cookie.hasKey("httpOnly") && cookie.getBoolean("httpOnly")) {
                cookieBuilder.setHttpOnly(true);
            }
        }

        return cookieBuilder;
    }

    private WritableMap createCookieData(HttpCookie cookie) {
        WritableMap cookieMap = Arguments.createMap();
        cookieMap.putString("name", cookie.getName());
        cookieMap.putString("value", cookie.getValue());
        cookieMap.putString("domain", cookie.getDomain());
        cookieMap.putString("path", cookie.getPath());
        cookieMap.putBoolean("secure", cookie.getSecure());
        if (HTTP_ONLY_SUPPORTED) {
            cookieMap.putBoolean("httpOnly", cookie.isHttpOnly());
        }

        // if persistent the library will set expiry to 31 Dec 9999
        // which we don't want to display to the developer
        long persistentExpiry = 253402214400L;
        long expires = cookie.getMaxAge();
        if (expires > 0 && expires < persistentExpiry) {
            cookieMap.putString("expiration", new Date(expires).toString());
        }
        return cookieMap;
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
