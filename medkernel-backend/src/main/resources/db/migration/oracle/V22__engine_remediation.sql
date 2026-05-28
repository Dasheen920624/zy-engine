-- MedKernel v1.0 GA · 引擎真实性彻底整治与门禁修复工程表结构变更（Oracle）

ALTER TABLE source_fragment ADD content_hash VARCHAR2(128);

COMMENT ON COLUMN source_fragment.content_hash IS '内容片段的物理 SHA-256 摘要去重指纹';
