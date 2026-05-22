-- PR-FINAL-25 + PR-FINAL-23: 扩展 md_patient 加密字段列宽（Oracle / DM 共用）。
--
-- 密文长度 = Base64(版本头 7B + IV 16B + 明文 + PKCS5 padding) ≈ (23 + 明文) × 4/3。
-- 明文 100B → 密文 ~196B；明文 500B → ~712B。统一开 256 / 1024 留足裕量。
--
-- Oracle ALTER 语法：ALTER TABLE ... MODIFY (col VARCHAR2(N))

ALTER TABLE md_patient MODIFY (patient_name VARCHAR2(256));
ALTER TABLE md_patient MODIFY (id_card_no VARCHAR2(256));
ALTER TABLE md_patient MODIFY (phone VARCHAR2(256));
ALTER TABLE md_patient MODIFY (address VARCHAR2(1024));
