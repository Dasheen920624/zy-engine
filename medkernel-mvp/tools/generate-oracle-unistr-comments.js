const fs = require("fs");

const input = "db/oracle/medkernel_core_ddl_with_comments.sql";
const output = "db/oracle/medkernel_comments_unistr.sql";

function toUnistr(value) {
  let result = "";
  for (let i = 0; i < value.length; i += 1) {
    result += "\\" + value.charCodeAt(i).toString(16).toUpperCase().padStart(4, "0");
  }
  return result;
}

const source = fs.readFileSync(input, "utf8");
const lines = [
  "SET DEFINE OFF;",
  "SET SERVEROUTPUT ON;",
  "DECLARE",
  "BEGIN"
];

source.split(/\r?\n/).forEach((line) => {
  const match = line.match(/^COMMENT ON (TABLE|COLUMN) ([A-Za-z0-9_\\.]+) IS '(.*)';\s*$/);
  if (!match) {
    return;
  }
  const kind = match[1];
  const target = match[2];
  const comment = match[3].replace(/''/g, "'");
  const prefix = `COMMENT ON ${kind} ${target} IS `;
  lines.push(`  EXECUTE IMMEDIATE '${prefix}''' || UNISTR('${toUnistr(comment)}') || '''';`);
});

lines.push("END;");
lines.push("/");
lines.push("PROMPT ZYENGINE unicode comments are ready.");

fs.writeFileSync(output, lines.join("\n") + "\n", "ascii");
console.log(output);
