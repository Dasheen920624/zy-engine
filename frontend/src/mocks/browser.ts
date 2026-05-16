import { setupWorker } from "msw/browser";
import { handlers } from "./handlers";

// 浏览器侧 MSW worker。仅在 VITE_ENABLE_MSW=true 时启用（见 main.tsx）。
export const worker = setupWorker(...handlers);
