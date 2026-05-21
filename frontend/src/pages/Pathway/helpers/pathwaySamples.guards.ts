/**
 * 路径模块的小工具守卫：放在样本文件旁边便于跨组件复用。
 */

export function safeParseJson(text: string): { ok: true; value: unknown } | { ok: false; error: string } {
  try {
    return { ok: true, value: JSON.parse(text) };
  } catch (e) {
    return { ok: false, error: (e as Error).message };
  }
}
