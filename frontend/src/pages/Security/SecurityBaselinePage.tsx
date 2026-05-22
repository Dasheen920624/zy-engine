import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Card,
  Col,
  Collapse,
  Descriptions,
  Form,
  Input,
  Modal,
  Row,
  Space,
  Spin,
  Statistic,
  Table,
  Tag,
  message,
  Popconfirm,
} from "antd";
import {
  SafetyCertificateOutlined,
  KeyOutlined,
  ScanOutlined,
  CheckCircleOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import {
  getSecurityBaseline,
  verifyAuditChain,
  listKeyVersions,
  rotateKey,
  revokeKey,
  performVulnerabilityScan,
  type SecurityBaselineStatus,
  type AuditChainVerifyResult,
  type KeyVersion,
  type VulnerabilityScanResult,
} from "../../api/securityBaseline";
import styles from "./securityBaseline.module.css";

const KEY_STATUS_MAP: Record<string, { color: string; label: string }> = {
  ACTIVE: { color: "green", label: "活跃" },
  GRACE: { color: "orange", label: "宽限期" },
  RETIRED: { color: "default", label: "已退役" },
  REVOKED: { color: "red", label: "已撤销" },
};

const SecurityBaselinePage: React.FC = () => {
  const [baseline, setBaseline] = useState<SecurityBaselineStatus | null>(null);
  const [verifyResult, setVerifyResult] = useState<AuditChainVerifyResult | null>(null);
  const [keys, setKeys] = useState<KeyVersion[]>([]);
  const [scanResult, setScanResult] = useState<VulnerabilityScanResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [rotateModalVisible, setRotateModalVisible] = useState(false);
  const [rotateForm] = Form.useForm();

  const fetchBaseline = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getSecurityBaseline();
      setBaseline(data);
    } catch {
      message.error("获取安全基线状态失败");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchKeys = useCallback(async () => {
    try {
      const data = await listKeyVersions();
      setKeys(data);
    } catch {
      message.error("获取密钥版本失败");
    }
  }, []);

  useEffect(() => {
    fetchBaseline();
    fetchKeys();
  }, [fetchBaseline, fetchKeys]);

  const handleVerify = async () => {
    setLoading(true);
    try {
      const result = await verifyAuditChain();
      setVerifyResult(result);
      message.success(result.chain_intact ? "审计链完整性校验通过" : "审计链存在异常");
    } catch {
      message.error("校验失败");
    } finally {
      setLoading(false);
    }
  };

  const handleRotate = async (values: Record<string, unknown>) => {
    try {
      await rotateKey(values as Parameters<typeof rotateKey>[0]);
      message.success("密钥轮换成功");
      setRotateModalVisible(false);
      rotateForm.resetFields();
      fetchKeys();
      fetchBaseline();
    } catch {
      message.error("密钥轮换失败");
    }
  };

  const handleRevoke = async (keyId: number) => {
    try {
      await revokeKey(keyId, { revoked_by: "admin" });
      message.success("密钥已撤销");
      fetchKeys();
    } catch {
      message.error("撤销失败");
    }
  };

  const handleScan = async () => {
    setLoading(true);
    try {
      const result = await performVulnerabilityScan();
      setScanResult(result);
      message.success("漏洞扫描完成");
    } catch {
      message.error("扫描失败");
    } finally {
      setLoading(false);
    }
  };

  const keyColumns = [
    { title: "密钥ID", dataIndex: "key_id", key: "key_id", width: 80 },
    { title: "别名", dataIndex: "key_alias", key: "key_alias", width: 160 },
    { title: "指纹", dataIndex: "key_hash", key: "key_hash", width: 120 },
    { title: "算法", dataIndex: "algorithm", key: "algorithm", width: 80 },
    {
      title: "状态",
      dataIndex: "status",
      key: "status",
      width: 100,
      render: (v: string) => {
        const cfg = KEY_STATUS_MAP[v] ?? { color: "default", label: v };
        return <Tag color={cfg.color}>{cfg.label}</Tag>;
      },
    },
    { title: "创建时间", dataIndex: "created_at", key: "created_at", width: 160 },
    { title: "操作人", dataIndex: "rotated_by", key: "rotated_by", width: 100 },
    {
      title: "操作",
      key: "action",
      width: 100,
      render: (_: unknown, record: KeyVersion) => {
        if (record.status === "GRACE") {
          return (
            <Popconfirm title="确定撤销此密钥？" onConfirm={() => handleRevoke(record.key_id)}>
              <Button size="small" danger>紧急撤销</Button>
            </Popconfirm>
          );
        }
        return null;
      },
    },
  ];

  const findingColumns = [
    { title: "依赖", dataIndex: "dependency", key: "dependency", width: 160 },
    { title: "版本", dataIndex: "version", key: "version", width: 100 },
    {
      title: "严重程度",
      dataIndex: "severity",
      key: "severity",
      width: 100,
      render: (v: string) => {
        const colorMap: Record<string, string> = { CRITICAL: "red", HIGH: "orange", MEDIUM: "gold", LOW: "blue" };
        return <Tag color={colorMap[v] ?? "default"}>{v}</Tag>;
      },
    },
    { title: "CVE", dataIndex: "cve", key: "cve", width: 160 },
    { title: "建议", dataIndex: "recommendation", key: "recommendation" },
  ];

  return (
    <div className={styles.page}>
      <Spin spinning={loading}>
        {/* 安全基线概览 */}
        <Card title={<Space><SafetyCertificateOutlined />安全基线概览</Space>} className={styles.sectionCard}>
          {baseline && (
            <Row gutter={16}>
              <Col span={6}>
                <Statistic
                  title="JWT 签名"
                  value={baseline.jwt_algorithm ? "已满足" : "缺证据"}
                  valueStyle={{ color: baseline.jwt_algorithm ? "var(--mk-success)" : "var(--mk-warning)" }}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="密钥管理"
                  value={baseline.active_key_id ? "已满足" : "需整改"}
                  valueStyle={{ color: baseline.active_key_id ? "var(--mk-success)" : "var(--mk-danger)" }}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="传输加密"
                  value={baseline.tls_min_version ? "已满足" : "需整改"}
                  valueStyle={{ color: baseline.tls_min_version ? "var(--mk-success)" : "var(--mk-danger)" }}
                />
              </Col>
              <Col span={6}>
                <Statistic
                  title="密钥版本"
                  value={baseline.total_key_versions}
                />
              </Col>
            </Row>
          )}
          <Collapse
            ghost
            size="small"
            items={[{
              key: "advanced-params",
              label: "高级选项",
              children: baseline ? (
                <Descriptions column={3} size="small" bordered>
                  <Descriptions.Item label="JWT 算法">{baseline.jwt_algorithm}</Descriptions.Item>
                  <Descriptions.Item label="密码哈希算法">{baseline.password_hash_algorithm}</Descriptions.Item>
                  <Descriptions.Item label="TLS 最低版本">{baseline.tls_min_version}</Descriptions.Item>
                  <Descriptions.Item label="HSTS">{baseline.hsts_enabled ? "已启用" : "未启用"}</Descriptions.Item>
                  <Descriptions.Item label="SBOM 格式">{baseline.sbom_format}</Descriptions.Item>
                  <Descriptions.Item label="活跃密钥ID">{baseline.active_key_id}</Descriptions.Item>
                  <Descriptions.Item label="宽限期密钥">{baseline.grace_keys}</Descriptions.Item>
                  <Descriptions.Item label="已退役密钥">{baseline.retired_keys}</Descriptions.Item>
                  <Descriptions.Item label="已撤销密钥">{baseline.revoked_keys}</Descriptions.Item>
                </Descriptions>
              ) : null,
            }]}
          />
        </Card>

        <Row gutter={16}>
          {/* 审计链完整性 */}
          <Col span={12}>
            <Card
              title={<Space><CheckCircleOutlined />审计链完整性</Space>}
              extra={<Button type="primary" onClick={handleVerify} loading={loading}>校验链完整性</Button>}
              className={styles.sectionCard}
            >
              {baseline?.audit_chain && (
                <Descriptions column={1} size="small" bordered>
                  <Descriptions.Item label="总记录数">{baseline.audit_chain.total_records}</Descriptions.Item>
                  <Descriptions.Item label="最后哈希">
                    <code className={styles.monoCode}>{baseline.audit_chain.last_record_hash}</code>
                  </Descriptions.Item>
                  <Descriptions.Item label="哈希算法">{baseline.audit_chain.hash_algorithm}</Descriptions.Item>
                </Descriptions>
              )}
              {verifyResult && (
                <div className={styles.marginTop}>
                  <Space>
                    <Tag color={verifyResult.chain_intact ? "green" : "red"} icon={verifyResult.chain_intact ? <CheckCircleOutlined /> : <WarningOutlined />}>
                      {verifyResult.chain_intact ? "链完整" : "链异常"}
                    </Tag>
                    <span>通过 {verifyResult.passed}/{verifyResult.total}</span>
                    {verifyResult.failed > 0 && <Tag color="red">失败 {verifyResult.failed}</Tag>}
                  </Space>
                </div>
              )}
            </Card>
          </Col>

          {/* 漏洞扫描 */}
          <Col span={12}>
            <Card
              title={<Space><ScanOutlined />漏洞扫描</Space>}
              extra={<Button onClick={handleScan} loading={loading}>执行扫描</Button>}
              className={styles.sectionCard}
            >
              {scanResult ? (
                <>
                  <Row gutter={8}>
                    <Col span={6}><Statistic title="依赖总数" value={scanResult.total_dependencies} /></Col>
                    <Col span={6}><Statistic title="严重" value={scanResult.critical_count} valueStyle={{ color: "var(--mk-danger)" }} /></Col>
                    <Col span={6}><Statistic title="高危" value={scanResult.high_count} valueStyle={{ color: "var(--mk-warning)" }} /></Col>
                    <Col span={6}><Statistic title="中危" value={scanResult.medium_count} /></Col>
                  </Row>
                  {scanResult.findings.length > 0 && (
                    <Table
                      rowKey="cve"
                      columns={findingColumns}
                      dataSource={scanResult.findings}
                      pagination={false}
                      size="small"
                      className={styles.findingTable}
                    />
                  )}
                </>
              ) : (
                <div className={styles.emptyHint}>
                  点击"执行扫描"开始漏洞扫描
                </div>
              )}
            </Card>
          </Col>
        </Row>

        {/* 密钥版本管理 */}
        <Card
          title={<Space><KeyOutlined />密钥版本管理</Space>}
          extra={
            <Button type="primary" onClick={() => setRotateModalVisible(true)}>
              密钥轮换
            </Button>
          }
        >
          <Table
            rowKey="key_id"
            columns={keyColumns}
            dataSource={keys}
            pagination={false}
            size="small"
          />
        </Card>
      </Spin>

      {/* 密钥轮换弹窗 */}
      <Modal
        title="密钥轮换"
        open={rotateModalVisible}
        onCancel={() => { setRotateModalVisible(false); rotateForm.resetFields(); }}
        onOk={() => rotateForm.submit()}
      >
        <Form form={rotateForm} layout="vertical" onFinish={handleRotate}>
          <Form.Item name="key_alias" label="密钥别名">
            <Input placeholder="如：key-v2（留空自动生成）" />
          </Form.Item>
          <Form.Item
            name="key_material"
            label="新密钥材料"
            rules={[{ required: true, message: "请输入新密钥材料" }]}
          >
            <Input.Password placeholder="请输入新的密钥字符串（至少32字符）" />
          </Form.Item>
          <Form.Item name="rotated_by" label="操作人">
            <Input placeholder="请输入操作人姓名" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default SecurityBaselinePage;
