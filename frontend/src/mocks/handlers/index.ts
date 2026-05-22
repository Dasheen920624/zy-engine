import { authHandlers } from "./auth";
import { ssoHandlers, systemHandlers } from "./system";
import { ruleEngineHandlers } from "./ruleEngine";
import { configPackageHandlers } from "./configPackage";

export const handlers = [
  ...authHandlers,
  ...ssoHandlers,
  ...systemHandlers,
  ...ruleEngineHandlers,
  ...configPackageHandlers,
];
