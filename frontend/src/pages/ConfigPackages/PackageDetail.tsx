import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Empty,
  Input,
  Modal,
  Row,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from "antd";
import {
  CheckCircleOutlined,
  CloudUploadOutlined,
  DiffOutlined,
  DownloadOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  WarningOutlined,
} from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import {
  getPackageDetail,
  reviewPackage,
  publishPackage,
  exportPackage,
} from "@/api/configPackage";
import type {
  ApiError,
  ConfigPackageDetail,
  ConfigPackageReview,
  ConfigPackageSummary,
  ReviewIssue,
} from "@/api/types";
import { StatusBadge } from "@/components/StatusBadge";
import { SourceInfo } from "@/components/SourceInfo";
import styles from "./PackageDetail.module.css";

const { Text } = Typography;

function shortHash(hash: string): string {
  if (!hash) return "—";
  const clean = hash.replace("sha256:", "");
  return clean.length > 10 ? `${clean.slice(0, 6)}…` : clean;
}

function formatTime(time?: string): string {
  if (!time) return "—";
  try {
    const d = new Date(time);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
  } catch {
    return time;
  }
}

function changeTypeColor(ct?: string): string {
  switch (ct) {
    case "ADDED":
      return "success";
    case "MODIFIED":
      return "processing";
    case "REMOVED":
      return "error";
    case "UNCHANGED":
    default:
      return "default";
  }
}

function changeTypeLabel(ct?: string): string {
  switch (ct) {
    case "ADDED":
      return "新增";
    case "MODIFIED":
      return "修改";
    case "REMOVED":
      return "移除";
    case "UNCHANGED":
    default:
      return "未变";
  }
}

// diffLineColor() / diffLineBackground() 已废弃 → 改由 PackageDetail.module.css 中
// `.diffLineAdd / .diffLineDel / .diffLineNeutral` token class 接管（PR-V3-INLINE-STYLE）

function issueIcon(severity: string) {
  switch (severity) {
    case "ERROR":
      return <ExclamationCircleOutlined className={styles.iconDanger} />;
    case "WARNING":
      return <WarningOutlined className={styles.iconWarning} />;
    default:
      return <CheckCircleOutlined className={styles.iconSuccess} />;
  }
}

/** 校验检查列表 */
function ReviewCheckList({ issues }: { issues: ReviewIssue[] }) {
  if (!issues || issues.length === 0) {
    return (
      <div className={styles.reviewPass}>
        <Space>
          <CheckCircleOutlined className={styles.iconSuccess} />
          <Text className={styles.textSuccess}>全部检查通过</Text>
        </Space>
      </div>
    );
  }

  return (
    <ul className={styles.reviewList}>
      {issues.map((issue, i) => (
        <li
          key={i}
          className={styles.reviewItem}
          style={{
            borderBottom:
              i < issues.length - 1 ? "var(--mk-border-width) solid var(--mk-border-divider)" : "none",
          }}
        >
          <span className={styles.reviewItemIcon}>{issueIcon(issue.severity)}</span>
          <div>
            <strong>{issue.field}</strong>
            <br />
            <Text type="secondary" className={styles.reviewMessage}>
              {issue.message}
            </Text>
          </div>
        </li>
      ))}
    </ul>
  );
}

/** Manifest 资产清单表格 */
function ManifestTable({ manifest }: { manifest?: Record<string, unknown> }) {
  const items = useMemo(() => {
    if (!manifest?.items || !Array.isArray(manifest.items)) return [];
    return manifest.items as Array<{
      asset_code: string;
      asset_type: string;
      version?: string;
      change_type?: string;
    }>;
  }, [manifest]);

  if (items.length === 0) {
    return <Empty description="无资产清单" image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  const cols: ColumnsType<(typeof items)[0]> = [
    { title: "资产编码", dataIndex: "asset_code", key: "code", render: (v: string) => <code className={styles.codeFontSmall}>{v}</code> },
    { title: "类型", dataIndex: "asset_type", key: "type", width: 80 },
    { title: "版本", dataIndex: "version", key: "version", width: 60 },
    {
      title: "变更",
      dataIndex: "change_type",
      key: "change",
      width: 80,
      render: (v: string) => <Tag color={changeTypeColor(v)}>{changeTypeLabel(v)}</Tag>,
    },
  ];

  return <Table dataSource={items} columns={cols} rowKey="asset_code" size="small" pagination={false} />;
}

/** Diff 展示 */
function DiffView({ diff }: { diff?: Record<string, unknown> }) {
  if (!diff || Object.keys(diff).length === 0) {
    return <Text type="secondary">无 diff 数据</Text>;
  }

  const lines: Array<{ text: string; type: "add" | "del" | "neutral" }> = [];
  const rules = diff.rules as Array<Record<string, unknown>> | undefined;
  if (rules) {
    for (const rule of rules) {
      if (rule.change === "REMOVED") {
        lines.push({ text: `- ${rule.rule_code}`, type: "del" });
      } else if (rule.version_change) {
        lines.push({ text: `  ${rule.rule_code}: ${rule.version_change}`, type: "neutral" });
        const added = rule.added_fields as string[] | undefined;
        if (added && added.length > 0) {
          for (const f of added) {
            lines.push({ text: `+   ${f}`, type: "add" });
          }
        }
      }
    }
  }

  if (lines.length === 0) {
    return (
      <pre className={styles.diffView}>
        {JSON.stringify(diff, null, 2)}
      </pre>
    );
  }

  return (
    <pre className={styles.diffViewDark}>
      {lines.map((line, i) => (
        <div
          key={i}
          className={`${styles.diffLine} ${
            line.type === "add" ? styles.diffLineAdd :
            line.type === "del" ? styles.diffLineDel :
            styles.diffLineNeutral
          }`}
        >
          {line.text}
        </div>
      ))}
    </pre>
  );
}

interface PackageDetailProps {
  selectedPkg: ConfigPackageSummary | null;
}

export default function PackageDetail({ selectedPkg }: PackageDetailProps) {
  const queryClient = useQueryClient();

  const [publishModalOpen, setPublishModalOpen] = useState(false);
  const [confirmPackageName, setConfirmPackageName] = useState("");
  const [publishReason, setPublishReason] = useState("");

  const selectedPackageKey = useMemo(
    () =>
      selectedPkg
        ? {
            code: selectedPkg.package_code,
            version: selectedPkg.package_version,
          }
        : null,
    [selectedPkg],
  );

  function requirePackageKey() {
    if (!selectedPackageKey) {
      throw new Error("selected package is required");
    }
    return selectedPackageKey;
  }

  const {
    data: pkgDetail,
    isLoading: detailLoading,
  } = useQuery<ConfigPackageDetail, ApiError>({
    queryKey: ["config-package-detail", selectedPackageKey?.code, selectedPackageKey?.version],
    queryFn: () => {
      const key = requirePackageKey();
      return getPackageDetail(key.code, key.version);
    },
    enabled: Boolean(selectedPackageKey),
  });

  const {
    data: pkgReview,
    isLoading: reviewLoading,
  } = useQuery<ConfigPackageReview, ApiError>({
    queryKey: ["config-package-review", selectedPackageKey?.code, selectedPackageKey?.version],
    queryFn: () => {
      const key = requirePackageKey();
      return reviewPackage(key.code, key.version);
    },
    enabled: Boolean(selectedPackageKey),
  });

  const reviewMutation = useMutation<ConfigPackageReview, ApiError>({
    mutationFn: () => {
      const key = requirePackageKey();
      return reviewPackage(key.code, key.version);
    },
    onSuccess: (data) => {
      message.success(`校验完成 · ${data.ready_to_publish ? "可发布" : "存在问题"}`);
      queryClient.invalidateQueries({ queryKey: ["config-package-review"] });
      queryClient.invalidateQueries({ queryKey: ["config-packages"] });
    },
    onError: (err) => message.error(`校验失败: ${err.message}`),
  });

  const publishMut = useMutation<ConfigPackageDetail, ApiError, { approved_by: string; approved_note?: string }>({
    mutationFn: (req) => {
      const key = requirePackageKey();
      return publishPackage(key.code, key.version, req);
    },
    onSuccess: () => {
      message.success("发布成功");
      setPublishModalOpen(false);
      setConfirmPackageName("");
      setPublishReason("");
      queryClient.invalidateQueries({ queryKey: ["config-packages"] });
      queryClient.invalidateQueries({ queryKey: ["config-package-detail"] });
      queryClient.invalidateQueries({ queryKey: ["config-package-review"] });
    },
    onError: (err) => message.error(`发布失败: ${err.message}`),
  });

  const exportMut = useMutation({
    mutationFn: () => {
      const key = requirePackageKey();
      return exportPackage(key.code, key.version);
    },
    onSuccess: (data) => {
      const key = requirePackageKey();
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${key.code}_${key.version}_snapshot.json`;
      a.click();
      URL.revokeObjectURL(url);
      message.success("导出成功");
    },
    onError: (err) => message.error(`导出失败: ${(err as Error).message}`),
  });

  const handlePublish = () => {
    if (!selectedPkg) return;
    if (confirmPackageName !== selectedPkg.package_code) {
      message.warning("请输入正确的包编码确认发布");
      return;
    }
    if (!publishReason.trim()) {
      message.warning("请输入发布原因");
      return;
    }
    publishMut.mutate({
      approved_by: confirmPackageName,
      approved_note: publishReason.trim(),
    });
  };

  if (!selectedPkg) {
    return (
      <Card size="small">
        <Empty description="选择配置包查看详情" image={Empty.PRESENTED_IMAGE_SIMPLE} />
      </Card>
    );
  }

  return (
    <>
      <Card
        title={
          <span>
            {selectedPkg.package_code} @ {selectedPkg.package_version} ·{" "}
            <StatusBadge status={selectedPkg.status.toLowerCase() as never} size="sm" />
          </span>
        }
        size="small"
        extra={
          <Space>
            <Button
              size="small"
              icon={<ReloadOutlined />}
              loading={reviewMutation.isPending}
              onClick={() => reviewMutation.mutate()}
            >
              重新校验
            </Button>
            <Button
              size="small"
              icon={<DownloadOutlined />}
              loading={exportMut.isPending}
              onClick={() => exportMut.mutate()}
            >
              导出
            </Button>
            {pkgReview?.ready_to_publish && selectedPkg.status !== "PUBLISHED" && selectedPkg.status !== "ACTIVE" && (
              <Button
                size="small"
                type="primary"
                danger
                icon={<CloudUploadOutlined />}
                onClick={() => setPublishModalOpen(true)}
              >
                发布
              </Button>
            )}
          </Space>
        }
      >
        {detailLoading || reviewLoading ? (
          <div className={styles.loadingContainer}>
            加载中...
          </div>
        ) : (
          <Row gutter={16}>
            {/* 左栏：基础信息 + manifest */}
            <Col xs={24} lg={12}>
              <h4 className={styles.sectionTitle}>基础信息</h4>
              <Descriptions
                column={1}
                size="small"
                labelStyle={{ color: "var(--mk-text-tertiary)", width: 140 }}
                style={{ marginBottom: 16 }}
              >
                <Descriptions.Item label="包编码">
                  <code className={styles.codeFont}>{selectedPkg.package_code}</code>
                </Descriptions.Item>
                <Descriptions.Item label="版本">
                  <code className={styles.codeFont}>{selectedPkg.package_version}</code>
                </Descriptions.Item>
                <Descriptions.Item label="资产类型">
                  <Tag color="blue">{selectedPkg.asset_type}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="组织范围">{selectedPkg.scope_reference || `${selectedPkg.scope_level} · ${selectedPkg.scope_code}`}</Descriptions.Item>
                <Descriptions.Item label="基础版本">
                  <code className={styles.codeFont}>{selectedPkg.base_version || "—"}</code>
                </Descriptions.Item>
                <Descriptions.Item label="内容哈希">
                  <Tooltip title={selectedPkg.content_hash}>
                    <code className={styles.codeFontSmall}>{shortHash(selectedPkg.content_hash)}</code>
                  </Tooltip>
                </Descriptions.Item>
                <Descriptions.Item label="创建人">{selectedPkg.created_by || "—"} · {formatTime(selectedPkg.created_time)}</Descriptions.Item>
                <Descriptions.Item label="审核人">{selectedPkg.reviewed_by || "—"} · {formatTime(selectedPkg.reviewed_time)}</Descriptions.Item>
                <Descriptions.Item label="审批人">
                  {selectedPkg.approved_by ? (
                    <span>{selectedPkg.approved_by} · {formatTime(selectedPkg.published_time)}</span>
                  ) : (
                    <Text type="secondary">— 未发布</Text>
                  )}
                </Descriptions.Item>
              </Descriptions>

              <Divider className={styles.dividerCompact} />

              <h4 className={styles.sectionTitle}>资产清单</h4>
              <ManifestTable manifest={pkgDetail?.manifest} />
            </Col>

            {/* 右栏：校验检查 */}
            <Col xs={24} lg={12}>
              <h4 className={styles.sectionTitle}>校验检查</h4>
              <ReviewCheckList issues={pkgReview?.issues || []} />

              {/* 来源完整性 — 使用 SourceInfo compact */}
              {pkgReview?.source_review && pkgReview.source_review.enabled && (
                <div className={styles.sourceReview}>
                  <Text type="secondary" className={styles.sourceReviewLabel}>
                    来源审核：
                  </Text>
                  {pkgReview.source_review.missing_count === 0 &&
                  pkgReview.source_review.expired_count === 0 &&
                  pkgReview.source_review.unreviewed_count === 0 ? (
                    <SourceInfo
                      variant="compact"
                      source={{ documentName: "全部来源已审核", documentId: "_all" }}
                      review={{ status: "reviewed" }}
                      version="1"
                    />
                  ) : (
                    <SourceInfo
                      variant="compact"
                      source={{ documentName: `缺 ${pkgReview.source_review.missing_count} 条来源`, documentId: "_missing" }}
                      review={{ status: "missing" }}
                      version="—"
                    />
                  )}
                </div>
              )}

              {/* 校验摘要 */}
              {pkgReview?.summary && (
                <div className={styles.reviewSummary}>
                  <h4 className={styles.sectionTitle}>校验摘要</h4>
                  <Descriptions
                    column={1}
                    size="small"
                    labelStyle={{ color: "var(--mk-text-tertiary)", width: 140 }}
                  >
                    <Descriptions.Item label="资产数量">{pkgReview.summary.asset_count}</Descriptions.Item>
                    <Descriptions.Item label="完整快照">{pkgReview.summary.full_snapshot_present ? <Tag color="success">✓</Tag> : <Tag color="error">✗</Tag>}</Descriptions.Item>
                    <Descriptions.Item label="差异">{pkgReview.summary.diff_present ? <Tag color="success">✓</Tag> : <Tag color="default">无</Tag>}</Descriptions.Item>
                    <Descriptions.Item label="范围存在">{pkgReview.summary.scope_exists ? <Tag color="success">✓</Tag> : <Tag color="error">✗</Tag>}</Descriptions.Item>
                  </Descriptions>
                </div>
              )}

              {/* ready_to_publish 状态 */}
              <div
                className={`${styles.publishStatus} ${
                  pkgReview?.ready_to_publish ? styles.publishStatusReady : styles.publishStatusNotReady
                }`}
              >
                <Space>
                  {pkgReview?.ready_to_publish ? (
                    <CheckCircleOutlined className={styles.iconSuccess} />
                  ) : (
                    <ExclamationCircleOutlined className={styles.iconDanger} />
                  )}
                  <Text
                    strong
                    className={pkgReview?.ready_to_publish ? styles.textSuccess : styles.textDanger}
                  >
                    {pkgReview?.ready_to_publish ? "可发布" : "不可发布"}
                  </Text>
                </Space>
              </div>
            </Col>

            {/* diff 全宽 */}
            {pkgDetail?.diff && Object.keys(pkgDetail.diff).length > 0 && (
              <Col span={24}>
                <Divider className={styles.dividerNormal} />
                <h4 className={styles.sectionTitle}>
                  <DiffOutlined className={styles.marginRightSmall} />
                  版本差异（基础 {selectedPkg.base_version || "—"} → 目标 {selectedPkg.package_version}）
                </h4>
                <DiffView diff={pkgDetail.diff} />
              </Col>
            )}
          </Row>
        )}
      </Card>

      {/* 发布确认弹窗 — 要求输入包名确认 + 必填原因 */}
      <Modal
        title={
          <span className={styles.publishModalTitle}>
            发布配置包: {selectedPkg?.package_code}@{selectedPkg?.package_version}
          </span>
        }
        open={publishModalOpen}
        onCancel={() => {
          setPublishModalOpen(false);
          setConfirmPackageName("");
          setPublishReason("");
        }}
        footer={
          <Space>
            <Button onClick={() => {
              setPublishModalOpen(false);
              setConfirmPackageName("");
              setPublishReason("");
            }}>
              取消
            </Button>
            <Button
              type="primary"
              danger
              loading={publishMut.isPending}
              onClick={handlePublish}
              disabled={confirmPackageName !== selectedPkg?.package_code || !publishReason.trim()}
            >
              确认发布
            </Button>
          </Space>
        }
      >
        <div className={styles.publishWarning}>
          <Text className={styles.textDanger}>
            <ExclamationCircleOutlined className={styles.marginRightSmall} />
            此操作不可撤销！发布后将写入 ENGINE_AUDIT_LOG（PKG/PUBLISH）。
          </Text>
        </div>

        <div className={styles.publishFormSection}>
          <Text type="secondary" className={styles.publishFormLabel}>
            请输入包编码确认 <span className={styles.textDanger}>*</span>
          </Text>
          <Input
            placeholder={`请输入 "${selectedPkg?.package_code}" 以确认`}
            value={confirmPackageName}
            onChange={(e) => setConfirmPackageName(e.target.value)}
            status={confirmPackageName && confirmPackageName !== selectedPkg?.package_code ? "error" : undefined}
          />
          {confirmPackageName && confirmPackageName !== selectedPkg?.package_code && (
            <Text type="danger" className={styles.publishFormError}>包编码不匹配</Text>
          )}
        </div>

        <div>
          <Text type="secondary" className={styles.publishFormLabel}>
            发布原因 <span className={styles.textDanger}>*</span>
          </Text>
          <Input.TextArea
            placeholder="必填 · 例：医学审核已通过，建议本周二上线"
            value={publishReason}
            onChange={(e) => setPublishReason(e.target.value)}
            rows={3}
          />
        </div>
      </Modal>
    </>
  );
}
