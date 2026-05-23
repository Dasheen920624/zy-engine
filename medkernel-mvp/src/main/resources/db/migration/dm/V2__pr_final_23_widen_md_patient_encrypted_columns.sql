-- DM8+ (达梦): 扩展 md_patient 加密字段列宽
-- PR-FINAL-23: SM4 密文存储后字段长度远超明文
ALTER TABLE md_patient MODIFY (patient_name VARCHAR(256));
ALTER TABLE md_patient MODIFY (id_card_no VARCHAR(256));
ALTER TABLE md_patient MODIFY (phone VARCHAR(256));
ALTER TABLE md_patient MODIFY (address VARCHAR(1024));
