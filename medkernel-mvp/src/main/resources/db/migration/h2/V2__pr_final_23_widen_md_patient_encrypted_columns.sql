-- PR-FINAL-25 + PR-FINAL-23: 扩展 md_patient 加密字段列宽。
--
-- 背景：PR-FINAL-23 将 patient_name / id_card_no / phone / address 改为 SM4 密文存储。
-- 密文长度 = Base64(版本头 7B + IV 16B + 明文 + PKCS5 padding) ≈ (23 + 明文) × 4/3。
-- 明文 100B → 密文 ~196B；明文 500B → ~712B。统一开 256 / 1024 留足裕量。
--
-- H2 ALTER TABLE 语法：ALTER TABLE ... ALTER COLUMN ... VARCHAR(N)

ALTER TABLE md_patient ALTER COLUMN patient_name VARCHAR(256) NOT NULL;
ALTER TABLE md_patient ALTER COLUMN id_card_no VARCHAR(256);
ALTER TABLE md_patient ALTER COLUMN phone VARCHAR(256);
ALTER TABLE md_patient ALTER COLUMN address VARCHAR(1024);
