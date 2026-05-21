export type LoginTabKey = "sms" | "password" | "sso" | "ldap";

const allowedTabs = new Set<LoginTabKey>(["sms", "password", "sso", "ldap"]);

function normalizeTab(value: string | undefined): LoginTabKey {
  if (value && allowedTabs.has(value as LoginTabKey)) {
    return value as LoginTabKey;
  }
  return "sms";
}

function readNumber(value: string | undefined, fallback: number): number {
  if (!value) return fallback;
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export const loginRuntimeConfig = {
  defaultMethod: normalizeTab(import.meta.env.VITE_LOGIN_DEFAULT_METHOD),
  icpNumber: import.meta.env.VITE_COMPLIANCE_ICP_NUMBER || "京ICP备20260521号-1",
  psbNumber: import.meta.env.VITE_COMPLIANCE_PSB_NUMBER || "京公网安备 11000002026021 号",
  profile: import.meta.env.VITE_APP_PROFILE || "demo",
  cryptoSuite: import.meta.env.VITE_SECURITY_CRYPTO_SUITE || "SM2",
  appVersion: import.meta.env.VITE_APP_VERSION || "v0.3-demo",
  lockThreshold: readNumber(import.meta.env.VITE_LOGIN_LOCK_THRESHOLD, 5),
  sessionTimeoutMinutes: readNumber(import.meta.env.VITE_SESSION_TIMEOUT_MINUTES, 30),
};

export function isDemoProfile() {
  return loginRuntimeConfig.profile === "demo";
}

export function resolveInitialTab(
  initialTab: LoginTabKey | undefined,
  search: string,
): LoginTabKey {
  const method = new URLSearchParams(search).get("method") ?? undefined;
  return normalizeTab(initialTab ?? method ?? loginRuntimeConfig.defaultMethod);
}
