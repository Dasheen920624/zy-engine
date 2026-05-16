import { setupServer } from "msw/node";
import { handlers } from "./handlers";

// Node 侧 MSW server（用于 vitest 单测）
export const server = setupServer(...handlers);
