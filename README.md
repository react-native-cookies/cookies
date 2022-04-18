# React Native Cookies - A Cookie Manager for React Native

Cookie Manager for React Native

<a href="https://discord.gg/CJHKVeW6sp">
<img src="https://img.shields.io/discord/764994995098615828?label=Discord&logo=Discord&style=for-the-badge"
            alt="chat on Discord"></a>

This module was ported from [joeferraro/react-native-cookies](https://github.com/joeferraro/react-native-cookies). This would not exist without the work of the original author, [Joe Ferraro](https://github.com/joeferraro).

## Important notices & Breaking Changes
- **v6.0.0**: Package name updated to `@react-native-cookies/cookies`.
- **v5.0.0**: Peer Dependency of >= React Native 0.60.2
- **v4.0.0**: Android SDK version bumpted to 21
- **v3.0.0**: Remove React Native Core dependencies, CookieManager.set() support for Android
- **v2.0.0**: Package name updated to `@react-native-community/cookies`.

## Maintainers

- [Jason Safaiyeh](https://github.com/safaiyeh) ([Twitter @safaiyeh](https://twitter.com/safaiyeh)) from [🪄 Magic Eden](https://magiceden.io)

## Platforms Supported

✅ iOS  
✅ Android  
❌ Expo is working on their own cookie support (https://github.com/expo/expo/issues/6756)

Currently lacking support for Windows, macOS, and web. Support for these platforms will be created when there is a need for them. Starts with a posted issue.

## Installation

```
yarn add @react-native-cookies/cookies
```

Then link the native iOS package

```
npx pod-install
```

## Usage

A cookie object can have one of the following fields:

```typescript
export interface Cookie {
  name: string;
  value: string;
  path?: string;
  domain?: string;
  version?: string;
  expires?: string;
  secure?: boolean;
  httpOnly?: boolean;
}

export interface Cookies {
  [key: string]: Cookie;
}
```

```javascript
import CookieManager from '@react-native-cookies/cookies';

// set a cookie
CookieManager.set('http://example.com', {
  name: 'myCookie',
  value: 'myValue',
  domain: 'some domain',
  path: '/',
  version: '1',
  expires: '2015-05-30T12:30:00.00-05:00'
}).then((done) => {
  console.log('CookieManager.set =>', done);
});

*NB:* When no `domain` is specified, url host will be used instead.
*NB:* When no `path` is specified, an empty path `/` will be assumed.

// Set cookies from a response header
// This allows you to put the full string provided by a server's Set-Cookie
// response header directly into the cookie store.
CookieManager.setFromResponse(
  'http://example.com',
  'user_session=abcdefg; path=/; expires=Thu, 1 Jan 2030 00:00:00 -0000; secure; HttpOnly')
    .then((success) => {
      console.log('CookieManager.setFromResponse =>', success);
    });

// Get cookies for a url
CookieManager.get('http://example.com')
  .then((cookies) => {
    console.log('CookieManager.get =>', cookies);
  });

// list cookies (IOS ONLY)
CookieManager.getAll()
  .then((cookies) => {
    console.log('CookieManager.getAll =>', cookies);
  });

// clear cookies
CookieManager.clearAll()
  .then((success) => {
    console.log('CookieManager.clearAll =>', success);
  });

// clear a specific cookie by its name (IOS ONLY)
CookieManager.clearByName('http://example.com', 'cookie_name')
  .then((success) => {
    console.log('CookieManager.clearByName =>', success);
  });

// flush cookies (ANDROID ONLY)
CookieManager.flush()
  .then((success) => {
    console.log('CookieManager.flush =>', success);
  });

// Remove session cookies (ANDROID ONLY)
// Session cookies are cookies with no expires set. Android typically does not
// remove these, it is up to the developer to decide when to remove them.
// The return value is true if any session cookies were removed.
// iOS handles removal of session cookies automatically on app open.
CookieManager.removeSessionCookies()
  .then((sessionCookiesRemoved) => {
    console.log('CookieManager.removeSessionCookies =>', sessionCookiesRemoved);
  });
```

### WebKit-Support (iOS only)

React Native comes with a WebView component, which uses UIWebView on iOS. Introduced in iOS 8 Apple implemented the WebKit-Support with all the performance boost.

Prior to WebKit-Support, cookies would have been stored in `NSHTTPCookieStorage` and sharedCookiesEnabled must be set on webviews to ensure access to them.

With WebKit-Support, cookies are stored in a separate webview store `WKHTTPCookieStore` and not necessarily shared with other http requests. Caveat is that this store is available upon mounting the component but not necessarily prior so any attempts to set a cookie too early may result in a false positive.

To use WebKit-Support, you should be able to simply make advantage of the react-native-webview as is OR alternatively use the webview component like [react-native-wkwebview](https://github.com/CRAlpha/react-native-wkwebview).

To use this _CookieManager_ with WebKit-Support we extended the interface with the attribute `useWebKit` (a boolean value, default: `FALSE`) for the following methods:

| Method      | WebKit-Support | Method-Signature                                                         |
| ----------- | -------------- | ------------------------------------------------------------------------ |
| getAll      | Yes            | `CookieManager.getAll(useWebKit:boolean)`                                |
| clearAll    | Yes            | `CookieManager.clearAll(useWebKit:boolean)`                              |
| clearByName | Yes            | `CookieManager.clearByName(url:string, name: string, useWebKit:boolean)` |
| get         | Yes            | `CookieManager.get(url:string, useWebKit:boolean)`                       |
| set         | Yes            | `CookieManager.set(url:string, cookie:object, useWebKit:boolean)`        |

##### Usage

```javascript
import CookieManager from '@react-native-cookies/cookies';

const useWebKit = true;

// list cookies (IOS ONLY)
CookieManager.getAll(useWebKit)
	.then((cookies) => {
		console.log('CookieManager.getAll from webkit-view =>', cookies);
	});

// clear cookies
CookieManager.clearAll(useWebKit)
	.then((succcess) => {
		console.log('CookieManager.clearAll from webkit-view =>', succcess);
	});

// clear cookies with name (IOS ONLY)
CookieManager.clearByName('http://example.com', 'cookie name', useWebKit)
	.then((succcess) => {
		console.log('CookieManager.clearByName from webkit-view =>', succcess);
  });

// Get cookies as a request header string
CookieManager.get('http://example.com', useWebKit)
	.then((cookies) => {
		console.log('CookieManager.get from webkit-view =>', cookies);
	});

// set a cookie
const newCookie: = {
	name: 'myCookie',
	value: 'myValue',
	domain: 'some domain',
	path: '/',
	version: '1',
	expires: '2015-05-30T12:30:00.00-05:00'
};

CookieManager.set('http://example.com', newCookie, useWebKit)
	.then((res) => {
		console.log('CookieManager.set from webkit-view =>', res);
	});
```
