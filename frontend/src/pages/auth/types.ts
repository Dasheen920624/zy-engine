export interface PasswordLoginValues {
  username: string;
  password: string;
  captcha?: string;
  mfaMethod?: string;
}

export interface SmsLoginValues {
  phone: string;
  code: string;
}

export interface LdapLoginValues {
  providerId?: number;
  username: string;
  password: string;
  domain?: string;
}
