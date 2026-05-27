CREATE TABLE demo_table (
  id BIGINT PRIMARY KEY,
  status VARCHAR(32) NOT NULL
);
COMMENT ON TABLE demo_table IS '示例：合规的中文表注释';
