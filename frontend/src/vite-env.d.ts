/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_ENABLE_MSW?: string;
  readonly VITE_APP_PROFILE?: string;
  readonly VITE_APP_VERSION?: string;
  readonly VITE_COMPLIANCE_ICP_NUMBER?: string;
  readonly VITE_COMPLIANCE_PSB_NUMBER?: string;
  readonly VITE_LOGIN_DEFAULT_METHOD?: string;
  readonly VITE_LOGIN_LOCK_THRESHOLD?: string;
  readonly VITE_SECURITY_CRYPTO_SUITE?: string;
  readonly VITE_SESSION_TIMEOUT_MINUTES?: string;
  readonly VITE_DEFAULT_TENANT_ID?: string;
  readonly VITE_DEFAULT_GROUP_CODE?: string;
  readonly VITE_DEFAULT_HOSPITAL_CODE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
