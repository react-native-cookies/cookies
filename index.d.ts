declare module '@react-native-community/cookies' {
  export interface Cookie {
    name: string;
    value: string;
    path: string;
    domain?: string;
    origin?: string;
    version?: string;
    expiration?: string;
  }

  export interface Cookies {
    [key: string]: string;
  }

  interface CookieManagerStatic {
    setFromResponse(url: string, value: string): Promise<boolean | undefined>;
    clearAll(useWebKit?: boolean): Promise<void>;
    get(url: string, useWebKit?: boolean): Promise<Cookies>;

    // iOS only.
    getAll(
      useWebKit?: boolean,
    ): Promise<{
      [key: string]: Cookie;
    }>;
    set(cookie: Cookie, useWebKit?: boolean): Promise<void>;
    getFromResponse(url: string): Promise<Cookies>;
    clearByName(name: string): Promise<void>;
  }

  const CookieManager: CookieManagerStatic;

  export default CookieManager;
}
