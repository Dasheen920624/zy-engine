-- KingbaseES (人大金仓): 扩展 md_patient 加密字段列宽
-- PR-FINAL-23: SM4 密文存储后字段长度远超明文
ALTER TABLE md_patient ALTER COLUMN patient_name TYPE VARCHAR(256);
ALTER TABLE md_patient ALTER COLUMN id_card_no TYPE VARCHAR(256);
ALTER TABLE md_patient ALTER COLUMN phone TYPE VARCHAR(256);
ALTER TABLE md_patient ALTER COLUMN address TYPE VARCHAR(1024);
