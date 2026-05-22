import { Drawer, Spin, Tag } from "antd";
import { useQuery } from "@tanstack/react-query";
import dayjs from "dayjs";
import {
  getUserDetail,
  STATUS_LABELS,
  STATUS_COLORS,
  USER_TYPE_LABELS,
} from "../../../api/userAdmin";
import styles from "./styles.module.css";

interface Props {
  userId: number | null;
  open: boolean;
  onClose: () => void;
}

function attemptsClassName(attempts: number): string | undefined {
  if (attempts >= 5) return styles.attemptsDanger;
  if (attempts >= 3) return styles.attemptsWarn;
  return undefined;
}

/**
 * 用户详情 Drawer（PR-FINAL-08）。
 *
 * 展示：基本信息 / 登录状态（含锁定 + 失败次数）/ 角色列表 / 身份绑定。
 * 等保 2.0 三级合规：登录失败次数和锁定时间必须可见给管理员。
 */
export default function UserDetail({ userId, open, onClose }: Props) {
  const query = useQuery({
    queryKey: ["admin-user-detail", userId],
    queryFn: () => getUserDetail(userId as number),
    enabled: open && userId !== null,
  });

  const user = query.data;
  const attemptsClass = attemptsClassName(user?.login_attempts ?? 0);

  return (
    <Drawer
      title="用户详情"
      open={open}
      onClose={onClose}
      width={480}
      destroyOnClose
    >
      {query.isLoading && <Spin />}
      {user && (
        <>
          {/* 基本信息 */}
          <div className={styles.detailSection}>
            <div className={styles.detailSectionTitle}>基本信息</div>
            <div className={styles.detailGrid}>
              <span className={styles.detailLabel}>用户名</span>
              <span className={styles.detailValue}>{user.username}</span>
              <span className={styles.detailLabel}>显示名</span>
              <span className={styles.detailValue}>{user.display_name}</span>
              <span className={styles.detailLabel}>邮箱</span>
              <span className={user.email ? styles.detailValue : styles.detailMissing}>
                {user.email ?? "—"}
              </span>
              <span className={styles.detailLabel}>手机</span>
              <span className={user.phone ? styles.detailValue : styles.detailMissing}>
                {user.phone ?? "—"}
              </span>
              <span className={styles.detailLabel}>用户类型</span>
              <span className={styles.detailValue}>
                {USER_TYPE_LABELS[user.user_type] ?? user.user_type}
              </span>
              <span className={styles.detailLabel}>工号</span>
              <span className={user.employee_id ? styles.detailValue : styles.detailMissing}>
                {user.employee_id ?? "—"}
              </span>
              <span className={styles.detailLabel}>状态</span>
              <span className={styles.detailValue}>
                <Tag color={STATUS_COLORS[user.status] ?? "default"}>
                  {STATUS_LABELS[user.status] ?? user.status}
                </Tag>
              </span>
            </div>
          </div>

          {/* 登录安全（等保 2.0 三级：必须对管理员可见）*/}
          <div className={styles.detailSection}>
            <div className={styles.detailSectionTitle}>登录安全</div>
            <div className={styles.detailGrid}>
              <span className={styles.detailLabel}>最近登录</span>
              <span className={user.last_login_time ? styles.detailValue : styles.detailMissing}>
                {user.last_login_time
                  ? dayjs(user.last_login_time).format("YYYY-MM-DD HH:mm:ss")
                  : "从未登录"}
              </span>
              <span className={styles.detailLabel}>最近 IP</span>
              <span className={user.last_login_ip ? styles.detailValue : styles.detailMissing}>
                {user.last_login_ip ?? "—"}
              </span>
              <span className={styles.detailLabel}>连续失败</span>
              <span className={attemptsClass ?? styles.detailValue}>
                {user.login_attempts} 次
              </span>
              <span className={styles.detailLabel}>锁定至</span>
              <span className={user.locked_until ? styles.attemptsDanger : styles.detailMissing}>
                {user.locked_until
                  ? dayjs(user.locked_until).format("YYYY-MM-DD HH:mm:ss")
                  : "未锁定"}
              </span>
            </div>
          </div>

          {/* 角色 */}
          <div className={styles.detailSection}>
            <div className={styles.detailSectionTitle}>角色</div>
            {user.roles.length === 0 ? (
              <span className={styles.emptyText}>无角色</span>
            ) : (
              user.roles.map((r) => (
                <Tag key={r} color="blue" className={styles.roleTag}>
                  {r}
                </Tag>
              ))
            )}
          </div>

          {/* 身份绑定 */}
          {(user.identity_bindings ?? []).length > 0 && (
            <div className={styles.detailSection}>
              <div className={styles.detailSectionTitle}>院内身份绑定</div>
              {user.identity_bindings.map((b, i) => (
                <div key={i} className={`${styles.detailGrid} ${styles.bindingRow}`}>
                  <span className={styles.detailLabel}>外部标识</span>
                  <span className={styles.detailValue}>{b.external_subject}</span>
                  <span className={styles.detailLabel}>姓名</span>
                  <span className={b.external_display_name ? styles.detailValue : styles.detailMissing}>
                    {b.external_display_name ?? "—"}
                  </span>
                  <span className={styles.detailLabel}>科室编码</span>
                  <span className={b.external_org_code ? styles.detailValue : styles.detailMissing}>
                    {b.external_org_code ?? "—"}
                  </span>
                  <span className={styles.detailLabel}>绑定状态</span>
                  <span className={styles.detailValue}>
                    <Tag color={b.binding_status === "ACTIVE" ? "success" : "default"}>
                      {b.binding_status}
                    </Tag>
                  </span>
                  {b.last_verified_time && (
                    <>
                      <span className={styles.detailLabel}>最近验证</span>
                      <span className={styles.detailValue}>
                        {dayjs(b.last_verified_time).format("YYYY-MM-DD HH:mm")}
                      </span>
                    </>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </Drawer>
  );
}
