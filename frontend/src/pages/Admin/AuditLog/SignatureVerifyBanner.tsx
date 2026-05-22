import { Button, Space, Spin, Tooltip, message } from "antd";
import { CheckCircleOutlined, WarningOutlined, QuestionCircleOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AUDIT_TABLES,
  AUDIT_TABLE_LABELS,
  type AuditChainCheckpoint,
  type AuditChainStatusEntry,
  type AuditTableName,
  getAuditChainStatus,
  verifyAuditChain,
} from "../../../api/auditLog";
import styles from "./styles.module.css";

/**
 * 顶部审计链验签横幅（PR-FINAL-09，等保 2.0 三级要求）。
 *
 * - 默认展示 3 个受支持审计表的最近校验状态（绿/红/灰）
 * - 每个表有「立即校验」按钮，点击触发后端 verifyAuditChain
 * - 失败状态显著标红（broken_records > 0 或 chain_status === BROKEN）
 *
 * 注：后端当前 /audit-chain/status 实现返 NOT_VERIFIED 占位（SecurityAdminController L74-83），
 * 真实校验结果在 verifyAuditChain checkpoint 中。本组件兼容两端表达。
 */
export default function SignatureVerifyBanner() {
  const queryClient = useQueryClient();

  const statusQuery = useQuery({
    queryKey: ["audit-chain", "status"],
    queryFn: getAuditChainStatus,
    refetchInterval: 5 * 60_000,
  });

  const verifyMutation = useMutation({
    mutationFn: (tableName: AuditTableName) => verifyAuditChain(tableName),
    onSuccess: (checkpoint, tableName) => {
      const broken = checkpoint.broken_records ?? 0;
      if (broken > 0 || checkpoint.chain_status === "BROKEN") {
        message.error(
          `${AUDIT_TABLE_LABELS[tableName]} 链校验：发现 ${broken} 条断链记录（首破 id=${checkpoint.first_broken_id ?? "?"}）`,
        );
      } else {
        message.success(
          `${AUDIT_TABLE_LABELS[tableName]} 链校验通过（共 ${checkpoint.total_records ?? 0} 条）`,
        );
      }
      queryClient.invalidateQueries({ queryKey: ["audit-chain", "status"] });
    },
    onError: (error: Error, tableName) => {
      message.error(`${AUDIT_TABLE_LABELS[tableName]} 校验失败：${error.message}`);
    },
  });

  return (
    <div className={styles.banner} aria-label="audit-chain-banner">
      <div className={styles.bannerTitle}>
        审计链验签
        <span className={styles.bannerTitleHint}>
          等保 2.0 三级要求·定期校验防篡改链；点击「立即校验」会写一条 checkpoint 记录
        </span>
      </div>
      {statusQuery.isLoading ? (
        <Spin tip="加载中..." />
      ) : (
        AUDIT_TABLES.map((tableName) => {
          const entry = statusQuery.data?.[tableName];
          const checkpoint = verifyMutation.data;
          const isCurrentTarget = verifyMutation.variables === tableName;
          // 优先用最新 verify 结果展示；否则用 /status 端点结果
          const display = isCurrentTarget && checkpoint ? toEntry(checkpoint) : entry;
          return (
            <div key={tableName} className={styles.bannerItem}>
              <div className={styles.bannerItemHeader}>
                <span className={styles.bannerItemName}>{AUDIT_TABLE_LABELS[tableName]}</span>
                <StatusBadge status={display?.status} broken={display?.broken_records} />
              </div>
              <div className={styles.bannerItemMeta}>
                {display?.total_records !== undefined && (
                  <>共 {display.total_records} 条 · </>
                )}
                {display?.last_verified_time
                  ? `最近校验 ${display.last_verified_time}`
                  : "尚未校验"}
              </div>
              <Space size={4}>
                <Button
                  size="small"
                  type="link"
                  loading={isCurrentTarget && verifyMutation.isPending}
                  onClick={() => verifyMutation.mutate(tableName)}
                >
                  立即校验
                </Button>
                {display?.broken_records !== undefined && display.broken_records > 0 && (
                  <Tooltip title={`首破记录 id = ${display.first_broken_id ?? "未知"}`}>
                    <span className={styles.signatureInvalidTag}>
                      <WarningOutlined /> {display.broken_records} 断链
                    </span>
                  </Tooltip>
                )}
              </Space>
            </div>
          );
        })
      )}
    </div>
  );
}

function StatusBadge({ status, broken }: { status?: string; broken?: number }) {
  if (broken !== undefined && broken > 0) {
    return (
      <span className={styles.bannerItemStatusBroken} aria-label="status-broken">
        <WarningOutlined /> 断链
      </span>
    );
  }
  if (status === "VALID") {
    return (
      <span className={styles.bannerItemStatusValid} aria-label="status-valid">
        <CheckCircleOutlined /> 通过
      </span>
    );
  }
  if (status === "BROKEN") {
    return (
      <span className={styles.bannerItemStatusBroken} aria-label="status-broken">
        <WarningOutlined /> 断链
      </span>
    );
  }
  return (
    <span className={styles.bannerItemStatusUnknown} aria-label="status-unknown">
      <QuestionCircleOutlined /> {status ?? "未校验"}
    </span>
  );
}

function toEntry(checkpoint: AuditChainCheckpoint): AuditChainStatusEntry {
  return {
    status: checkpoint.chain_status,
    last_verified_time: checkpoint.checkpoint_time,
    total_records: checkpoint.total_records,
    broken_records: checkpoint.broken_records,
    first_broken_id: checkpoint.first_broken_id,
  };
}
