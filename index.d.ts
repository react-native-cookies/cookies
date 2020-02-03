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
    clearAll(useWebKit?: boolean): Promise<void>;
    set(url: string, cookie: Cookie, useWebKit?: boolean): Promise<boolean>;
    setFromResponse(url: string, cookie: Cookie): Promise<boolean>;
    get(url: string, useWebKit?: boolean): Promise<Cookies>;

    // iOS only.
    getAll(
      useWebKit?: boolean,
    ): Promise<{
      [key: string]: Cookie;
    }>;
    getFromResponse(url: string): Promise<Cookies>;
    clearByName(name: string): Promise<void>;
  }

  const CookieManager: CookieManagerStatic;

  export default CookieManager;
}
