/*
 * Copyright (c) Joseph P. Ferraro
 * <p>
 * This source code is licensed under the MIT license found in the
 * LICENSE file here: https://github.com/joeferraro/react-native-cookies/blob/master/LICENSE.md.
 */

package com.reactnativecommunity.cookies;

import android.os.Build;
import android.util.Log;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CookieManagerModule extends ReactContextBaseJavaModule {

    private static final boolean USES_LEGACY_STORE = Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    private static final boolean HTTP_ONLY_SUPPORTED = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    private static final String INVALID_URL_MISSING_HTTP = "Invalid URL: It may be missing a protocol (ex. http:// or https://).";
    private static final String INVALID_COOKIE_VALUES = "Unable to add cookie - invalid values";
    private static final String GET_ALL_NOT_SUPPORTED = "Get all cookies not supported for Android (iOS only)";
    private static final String CLEAR_BY_NAME_NOT_SUPPORTED = "Cannot remove a single cookie by name on Android";
    private static final String INVALID_DOMAINS = "Cookie URL host %s and domain %s mismatched. The cookie won't set correctly.";

    private CookieSyncManager mCookieSyncManager;

    CookieManagerModule(ReactApplicationContext context) {
        super(context);
        this.mCookieSyncManager = CookieSyncManager.createInstance(context);
    }

    private CookieManager getCookieManager() throws Exception {
        try {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            return cookieManager;
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public String getName() {
        return "RNCookieManagerAndroid";
    }

    @ReactMethod
    public void set(String url, ReadableMap cookie, Boolean useWebKit, final Promise promise) {
        String cookieString = null;
        try {
            cookieString = toRFC6265string(makeHTTPCookieObject(url, cookie));
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
    public void flush(Promise promise) {
        try {
            getCookieManager().flush();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void removeSessionCookies(final Promise promise) {
        try {
            getCookieManager().removeSessionCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean data) {
                    promise.resolve(data);
                }
            });
        } catch (Exception e) {
            promise.reject(e);
        }
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
        if (isEmpty(url)) {
            promise.reject(new Exception(INVALID_URL_MISSING_HTTP));
            return;
        }
        try {
            String cookiesString = getCookieManager().getCookie(url);

            WritableMap cookieMap = createCookieList(cookiesString);
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
        try {
            CookieManager cookieManager = getCookieManager();
            if (USES_LEGACY_STORE) {
                cookieManager.removeAllCookie();
                cookieManager.removeSessionCookie();
                mCookieSyncManager.sync();
                promise.resolve(true);
            } else {
                cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        promise.resolve(value);
                    }
                });
                cookieManager.flush();
            }
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    private void addCookies(String url, String cookieString, final Promise promise) {
        try {
            CookieManager cookieManager = getCookieManager();
            if (USES_LEGACY_STORE) {
                cookieManager.setCookie(url, cookieString);
                mCookieSyncManager.sync();
                promise.resolve(true);
            } else {
                cookieManager.setCookie(url, cookieString, new ValueCallback<Boolean>() {
                    @Override
                    public void onReceiveValue(Boolean value) {
                        promise.resolve(value);
                    }
                });
                cookieManager.flush();
            }
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    private WritableMap createCookieList(String allCookies) throws Exception {
        WritableMap allCookiesMap = Arguments.createMap();

        if (!isEmpty(allCookies)) {
            String[] cookieHeaders = allCookies.split(";");
            for (String singleCookie : cookieHeaders) {
                List<HttpCookie> cookies = HttpCookie.parse(singleCookie);
                for (HttpCookie cookie : cookies) {
                    if (cookie != null) {
                        String name = cookie.getName();
                        String value = cookie.getValue();
                        if (!isEmpty(name) && !isEmpty(value)) {
                            WritableMap cookieMap = createCookieData(cookie);
                            allCookiesMap.putMap(name, cookieMap);
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
            throw new Exception(INVALID_URL_MISSING_HTTP);
        }

        String topLevelDomain = parsedUrl.getHost();

        if (isEmpty(topLevelDomain)) {
            // assume something went terribly wrong here and no cookie can be created
            throw new Exception(INVALID_URL_MISSING_HTTP);
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

        if (cookie.hasKey("expires") && !isEmpty(cookie.getString("expires"))) {
            Date date = parseDate(cookie.getString("expires"));
            if (date != null) {
                cookieBuilder.setMaxAge(date.getTime());
            }
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

        // if persistent the max Age will be -1
        long expires = cookie.getMaxAge();
        if (expires > 0) {
            String expiry = formatDate(new Date(expires));
            if (!isEmpty(expiry)) {
                cookieMap.putString("expires", expiry);
            }
        }
        return cookieMap;
    }

    /**
     * As HttpCookie is designed specifically for headers, it only gives us 2 formats on toString
     * dependent on the cookie version: 0 = Netscape; 1 = RFC 2965/2109, both without leading "Cookie:" token.
     * For our purposes RFC 6265 is the right way to go.
     * This is a convenience method to give us the right formatting.
     */
    private String toRFC6265string(HttpCookie cookie) {
        StringBuilder builder = new StringBuilder();

        builder.append(cookie.getName())
                .append('=')
                .append(cookie.getValue());

        if (!cookie.hasExpired()) {
            long expiresAt = cookie.getMaxAge();
            if (expiresAt > 0) {
                String dateString = formatDate(new Date(expiresAt), true);
                if (!isEmpty(dateString)) {
                    builder.append("; expires=").append(dateString);
                }
            }
        }

        if (!isEmpty(cookie.getDomain())) {
            builder.append("; domain=")
                    .append(cookie.getDomain());
        }

        if (!isEmpty(cookie.getPath())) {
            builder.append("; path=")
                    .append(cookie.getPath());
        }

        if (cookie.getSecure()) {
            builder.append("; secure");
        }

        if (HTTP_ONLY_SUPPORTED && cookie.isHttpOnly()) {
            builder.append("; httponly");
        }

        return builder.toString();
    }

    private boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Used for pushing cookies expiry time in and out of the library.
     *
     * @return simple date formatter
     */
    private DateFormat dateFormatter() {
        // suggested formatting -> return DateTimeFormatter.RFC_1123_DATE_TIME;
        // matching ios format as yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    /**
     * Used building the correctly formatted date for a cookie string in RFC_1123_DATE_TIME format
     *
     * @return simple date formatter
     */
    private DateFormat RFC1123dateFormatter() {
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    }

    private Date parseDate(String dateString) {
        return parseDate(dateString, false);
    }

    private Date parseDate(String dateString, boolean rfc1123) {
        Date date = null;
        try {
            date = (rfc1123 ? RFC1123dateFormatter() : dateFormatter()).parse(dateString);
        } catch (Exception e) {
            String message = e.getMessage();
            Log.i("Cookies", message != null ? message : "Unable to parse date");
        }
        return date;
    }

    private String formatDate(Date date) {
        return formatDate(date, false);
    }

    private String formatDate(Date date, boolean rfc1123) {
        String dateString = null;
        try {
            dateString = (rfc1123 ? RFC1123dateFormatter() : dateFormatter()).format(date);
        } catch (Exception e) {
            String message = e.getMessage();
            Log.i("Cookies", message != null ? message : "Unable to format date");
        }
        return dateString;
    }
}
