/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_ENABLE_MSW?: string;
  readonly VITE_DEFAULT_TENANT_ID?: string;
  readonly VITE_DEFAULT_GROUP_CODE?: string;
  readonly VITE_DEFAULT_HOSPITAL_CODE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
