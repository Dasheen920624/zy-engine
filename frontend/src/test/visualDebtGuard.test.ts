import { describe, expect, it } from "vitest";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join, relative } from "node:path";

const SRC_ROOT = join(process.cwd(), "src");
const INLINE_STYLE_PATTERN = /style=\{\{/;
const DIRECT_BROWSER_STORAGE_PATTERN = /\b(?:window\.)?(?:localStorage|sessionStorage)\s*\./;
const CONSOLE_PATTERN = /\bconsole\./;

const ALLOWED_DIRECT_STORAGE_FILES = new Set(["shared/lib/browserStorage.ts"]);
const IGNORED_FILE_PATTERN = /\.(test|spec)\.(ts|tsx)$/;

function listSourceFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const fullPath = join(dir, entry);
    const stat = statSync(fullPath);
    if (stat.isDirectory()) return listSourceFiles(fullPath);
    if (!/\.(ts|tsx)$/.test(entry)) return [];
    if (IGNORED_FILE_PATTERN.test(entry)) return [];
    return [fullPath];
  });
}

function findViolations(pattern: RegExp, allowList = new Set<string>()) {
  return listSourceFiles(SRC_ROOT)
    .map((file) => ({
      file: relative(SRC_ROOT, file),
      content: readFileSync(file, "utf8"),
    }))
    .filter(({ file, content }) => !allowList.has(file) && pattern.test(content))
    .map(({ file }) => file)
    .sort();
}

describe("前端视觉债与浏览器存储门禁", () => {
  it("生产 TSX 不再包含 JSX inline style", () => {
    expect(findViolations(INLINE_STYLE_PATTERN)).toEqual([]);
  });

  it("生产代码不直接访问浏览器存储或 console", () => {
    expect(findViolations(DIRECT_BROWSER_STORAGE_PATTERN, ALLOWED_DIRECT_STORAGE_FILES)).toEqual(
      [],
    );
    expect(findViolations(CONSOLE_PATTERN)).toEqual([]);
  });
});
