declare module '@react-native-community/cookies' {
  export interface Cookie {
    name: string;
    value: string;
    path: string;
    domain?: string;
    origin?: string;
    version?: string;
    expiration?: string;
    secure?: boolean;
    httpOnly?: boolean;
  }

  export interface Cookies {
    [key: string]: string;
  }

  interface CookieManagerStatic {
    set(url: string, cookie: Cookie, useWebKit?: boolean): Promise<boolean>;
    setFromResponse(url: string, cookie: Cookie): Promise<boolean>;

    get(url: string, useWebKit?: boolean): Promise<Cookies>;
    getFromResponse(url: string): Promise<Cookies>;

    clearByName(url: string, name: string, useWebKit?: boolean): Promise<boolean>;
    clearAll(useWebKit?: boolean): Promise<boolean>;

    //iOS only
    getAll(useWebKit?: boolean): Promise<Cookies>;
  }

  const CookieManager: CookieManagerStatic;

  export default CookieManager;
}
