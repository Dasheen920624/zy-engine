import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

import commonZhCN from "./zh-CN/common.json";
import qualityZhCN from "./zh-CN/quality.json";
import cdssZhCN from "./zh-CN/cdss.json";

const resources = {
  "zh-CN": {
    common: commonZhCN,
    quality: qualityZhCN,
    cdss: cdssZhCN,
  },
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: "zh-CN",
    defaultNS: "common",
    ns: ["common", "quality", "cdss"],
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ["localStorage", "navigator"],
      lookupLocalStorage: "medkernel_locale",
      caches: ["localStorage"],
    },
  });

export default i18n;
