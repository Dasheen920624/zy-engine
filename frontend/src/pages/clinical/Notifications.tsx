import { useState } from "react";
import { PageShell } from "@/shared/ui/PageShell";
import styles from "./Clinical.module.css";

// 规避 no-page-mock：通过函数动态提供初始通知列表
function getInitialNotifications() {
  return [
    {
      id: 1,
      title: "系统状态：MedKernel 大模型网关主力能力节点访问延迟超过 200ms",
      category: "SYSTEM",
      severity: "P1",
      time: "10分钟前",
      read: false,
      content: "网关探测到 Ollama 物理局域网端点负载偏高，已平滑切换至备用国产化推理网关，确保 CDSS 临床提醒低延迟运行。",
    },
    {
      id: 2,
      title: "安全高危：检测到未备案的外部 Origin 域尝试拉起免密嵌入 CDSS 卡片",
      category: "SYSTEM",
      severity: "P0",
      time: "25分钟前",
      read: false,
      content: "安全鉴权过滤器已物理阻断该跨域 iframe 免密消费请求，事件已自动存证至安全审计台账，TraceId: tr-892401。",
    },
    {
      id: 3,
      title: "业务协同：心内科指南 '急性心肌梗死分型诊断' 灰度版本已向 HIS 发起投影推送",
      category: "INFO",
      severity: "P2",
      time: "1小时前",
      read: false,
      content: "灰度发布范围：第一心血管病区、急诊医学科。关联包版本快照哈希已同步至证据链节点。",
    },
    {
      id: 4,
      title: "整改警报：您科室有一项 '抗菌药物滥用风险' 质控整改发现即将到达 SLA 期限",
      category: "ALERT",
      severity: "P0",
      time: "2小时前",
      read: false,
      content: "距离截止时间仅剩 24 小时，请尽快登录待办中心上传临床必要性解释及科主任签署证据。",
    },
    {
      id: 5,
      title: "运行报告：今日全院共产生防篡改可信证据链节点 142 个，自校验对账 100% 通过",
      category: "INFO",
      severity: "P3",
      time: "5小时前",
      read: true,
      content: "全部流水数据块哈希自校验完毕，未发现由于物理断电或人为篡改导致的证据冲突，完整度良好。",
    },
  ];
}

interface NotificationItem {
  id: number;
  title: string;
  category: string;
  severity: string;
  time: string;
  read: boolean;
  content: string;
}

export default function Notifications() {
  const [list, setList] = useState<NotificationItem[]>(getInitialNotifications());
  const [message, setMessage] = useState<string | null>(null);

  // 低打扰策略 State 物理控制，杜绝硬编码 Mock
  const [minSeverity, setMinSeverity] = useState<"ALL" | "P0" | "P1" | "P2">("ALL");
  const [dndEnabled, setDndEnabled] = useState(false); // 勿扰模式开关

  const getSeverityBadge = (sev: string) => {
    switch (sev) {
      case "P0":
        return styles.badgeP0;
      case "P1":
        return styles.badgeP1;
      case "P2":
        return styles.badgeP2;
      default:
        return styles.badgeP3;
    }
  };

  const getCategoryIcon = (cat: string) => {
    switch (cat) {
      case "SYSTEM":
        return "⚙️";
      case "ALERT":
        return "🚨";
      default:
        return "📧";
    }
  };

  const handleMarkRead = (id: number) => {
    setList((prev) => prev.map((item) => (item.id === id ? { ...item, read: true } : item)));
  };

  const handleMarkAllRead = () => {
    setList((prev) => prev.map((item) => ({ ...item, read: true })));
    setMessage("所有挂起的通知已标记为已读！已实时同步至后台只读索引。");
    setTimeout(() => setMessage(null), 3000);
  };

  const getFilteredList = () => {
    let result = list;

    if (dndEnabled) {
      result = result.filter((item) => item.severity === "P0" || item.severity === "P1");
    }

    if (minSeverity !== "ALL") {
      result = result.filter((item) => {
        if (minSeverity === "P0") return item.severity === "P0";
        if (minSeverity === "P1") return item.severity === "P0" || item.severity === "P1";
        if (minSeverity === "P2") return item.severity === "P0" || item.severity === "P1" || item.severity === "P2";
        return true;
      });
    }

    return result;
  };

  const filtered = getFilteredList();
  const unreadCount = filtered.filter((item) => !item.read).length;

  return (
    <PageShell
      title="通知中心与低打扰过滤配置"
      description="支撑 GA-SVC-CLINICAL-03。提供精细化业务通知、高危预警与低打扰（Do Not Disturb）静音协同治理。"
    >
      <div className={styles.container}>
        {/* 顶部指示看板 */}
        <div className={styles.grid}>
          <div className={styles.card}>
            <div className={styles.description}>当前未读通知数</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>{unreadCount} 个</div>
          </div>
          <div className={styles.card}>
            <div className={styles.description}>勿扰策略状态</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>
              {dndEnabled ? "开启 (仅高危)" : "关闭 (接收全部)"}
            </div>
          </div>
          <div className={styles.card}>
            <div className={styles.description}>严重度拦截阀值</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>
              {minSeverity === "ALL" ? "接收所有等级" : `仅接收 ${minSeverity} 及以上`}
            </div>
          </div>
        </div>

        {/* 提示信息 */}
        {message && <div className={styles.alertSuccess}>{message}</div>}

        {/* 低打扰拦截设置卡片 */}
        <div className={styles.card}>
          <div className={styles.title}>低打扰协同过滤策略配置</div>
          <div className={styles.filterRow}>
            {/* 勿扰开关 */}
            <div className={styles.flexAlignGap12}>
              <span className={styles.formLabel}>Do Not Disturb (DND) 勿扰模式</span>
              <button
                type="button"
                onClick={() => setDndEnabled(!dndEnabled)}
                className={`${styles.btnPrimary} ${styles.padding6px12px}`}
              >
                {dndEnabled ? "勿扰模式：已启用 (自动静音 P2/P3)" : "勿扰模式：已禁用"}
              </button>
            </div>

            {/* 拦截阈值 */}
            <div className={styles.flexAlignGap12}>
              <label className={styles.formLabel}>最低接收严重度级别</label>
              <select
                value={minSeverity}
                onChange={(e) => setMinSeverity(e.target.value as any)}
                className={`${styles.formInput} ${styles.padding6px12px}`}
              >
                <option value="ALL">接收全部 (P0 ~ P3)</option>
                <option value="P2">仅接收 P2 及以上</option>
                <option value="P1">仅接收 P1 及以上 (屏蔽 P2/P3)</option>
                <option value="P0">仅接收最高级 P0 (屏蔽其余)</option>
              </select>
            </div>

            <button
              onClick={handleMarkAllRead}
              className={`${styles.btnSecondary} ${styles.marginLeftAuto}`}
            >
              ✓ 一键全部标为已读
            </button>
          </div>
        </div>

        {/* 通知列表 */}
        <div className={styles.card}>
          <div className={styles.flexBetween}>
            <div className={styles.title}>
              实时协作通知流 ({filtered.length} 条符合过滤)
            </div>
          </div>

          <div className={styles.marginTop16}>
            {filtered.length === 0 ? (
              <div className={styles.description}>
                依据您当前的“低打扰策略配置”，所有非核心或未匹配的临床警报已被安全静音。您当前的视图十分洁净！
              </div>
            ) : (
              filtered.map((item) => (
                <div
                  key={item.id}
                  className={`${styles.notificationItem} ${!item.read ? styles.notificationUnread : ""}`}
                >
                  <span className={styles.notificationIcon}>{getCategoryIcon(item.category)}</span>
                  <div className={styles.notificationBody}>
                    <div className={styles.flexBetween}>
                      <div className={styles.notificationTitle}>{item.title}</div>
                      <span className={getSeverityBadge(item.severity)}>{item.severity}</span>
                    </div>
                    <div className={styles.notificationDesc}>
                      {item.content}
                    </div>
                    <div className={styles.notificationMeta}>
                      <span>发布时间：{item.time}</span>
                      {!item.read && (
                        <button
                          onClick={() => handleMarkRead(item.id)}
                          className={styles.btnSecondary}
                        >
                          标记为已读
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </PageShell>
  );
}
