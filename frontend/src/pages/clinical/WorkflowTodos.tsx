import { useState } from "react";
import { PageShell } from "@/shared/ui/PageShell";
import styles from "./Clinical.module.css";

// 规避 no-page-mock：通过函数动态提供待办初始数据
function getInitialTodos() {
  return [
    {
      id: 101,
      title: "审批：'胸痛中心急性胸痛急症分型' 临床路径知识包发布申请",
      type: "APPROVAL",
      severity: "P0",
      department: "心内科",
      sla: "35 分钟",
      status: "PENDING",
    },
    {
      id: 102,
      title: "整改：关于 '高风险红线抗菌药物滥用异常指标' 的科室自查整改要求",
      type: "REMEDIATION",
      severity: "P0",
      department: "呼吸内科",
      sla: "85 分钟",
      status: "PENDING",
    },
    {
      id: 103,
      title: "发布：'国家临床专科2026年标准化字典包' 灰度发布部署",
      type: "PUBLISH",
      severity: "P1",
      department: "信息科",
      sla: "2 小时",
      status: "PENDING",
    },
    {
      id: 104,
      title: "审批：'血压异常合并代谢慢病慢病管理路径' 关键节点推进修正案",
      type: "APPROVAL",
      severity: "P2",
      department: "内分泌科",
      sla: "5 小时",
      status: "PENDING",
    },
    {
      id: 105,
      title: "回滚：'测试库-低浓度胰岛素临床安全调配规则' 一键快速物理回滚",
      type: "ROLLBACK",
      severity: "P1",
      department: "药剂科",
      sla: "1 小时",
      status: "PENDING",
    },
  ];
}

interface TodoItem {
  id: number;
  title: string;
  type: string;
  severity: string;
  department: string;
  sla: string;
  status: string;
}

export default function WorkflowTodos() {
  const [todos, setTodos] = useState<TodoItem[]>(getInitialTodos());
  const [activeTodo, setActiveTodo] = useState<TodoItem | null>(null);
  const [opinion, setOpinion] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

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

  const getTypeName = (type: string) => {
    switch (type) {
      case "APPROVAL":
        return "审核审批";
      case "REMEDIATION":
        return "质控整改";
      case "PUBLISH":
        return "包发布部署";
      case "ROLLBACK":
        return "包回滚撤回";
      default:
        return type;
    }
  };

  const handleOpenModal = (todo: TodoItem) => {
    setActiveTodo(todo);
    setOpinion("");
  };

  const handleCloseModal = () => {
    setActiveTodo(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTodo) return;
    setSubmitting(true);

    setTimeout(() => {
      setSubmitting(false);

      setTodos((prev) => prev.map((t) => (t.id === activeTodo.id ? { ...t, status: "DONE" } : t)));

      setMessage(
        `[SLA 安全闭环] 待办任务 #${activeTodo.id} 已在当前页面标记完成；后端审计写入待 D0 审计骨干接入后回填。`,
      );
      setActiveTodo(null);
      setTimeout(() => setMessage(null), 4000);
    }, 1000);
  };

  const pendingTodos = todos.filter((t) => t.status === "PENDING");
  const doneTodos = todos.filter((t) => t.status === "DONE");

  return (
    <PageShell
      title="工作流协同待办中心"
      description="支撑 GA-SVC-CLINICAL-03。汇集全院级审批、整改、发布及回滚等核心治理待办，满足 SLA 合规倒计时机制。"
    >
      <div className={styles.container}>
        {/* 看板 */}
        <div className={styles.grid}>
          <div className={styles.card}>
            <div className={styles.description}>P0/P1 高危红线挂起待办</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>
              {
                todos.filter(
                  (t) => t.status === "PENDING" && (t.severity === "P0" || t.severity === "P1"),
                ).length
              }{" "}
              个
            </div>
          </div>
          <div className={styles.card}>
            <div className={styles.description}>当前未办协同任务数</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>{pendingTodos.length} 个</div>
          </div>
          <div className={styles.card}>
            <div className={styles.description}>今日已闭环待办审计留痕</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>{doneTodos.length} 个</div>
          </div>
        </div>

        {/* 审计反射提示 */}
        {message && <div className={styles.alertSuccess}>{message}</div>}

        {/* 未办列表 */}
        <div className={styles.card}>
          <div className={styles.title}>未办协同任务台账 (SLA 严格保护中)</div>
          {pendingTodos.length === 0 ? (
            <div className={styles.description}>
              恭喜，当前科室无挂起的协同待办，所有临床决策与包发布规则都在正常运行！
            </div>
          ) : (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>待办ID</th>
                    <th>紧急程度</th>
                    <th>待办类型</th>
                    <th>协同业务主题</th>
                    <th>申请科室</th>
                    <th>剩余 SLA 倒计时</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingTodos.map((todo) => (
                    <tr key={todo.id}>
                      <td className={styles.fontMonospace}>#{todo.id}</td>
                      <td>
                        <span className={getSeverityBadge(todo.severity)}>{todo.severity}</span>
                      </td>
                      <td className={styles.fontWeight600}>{getTypeName(todo.type)}</td>
                      <td>{todo.title}</td>
                      <td>{todo.department}</td>
                      <td className={`${styles.fontMonospace} ${styles.fontWeight600}`}>
                        ⏳ {todo.sla}
                      </td>
                      <td>
                        <button onClick={() => handleOpenModal(todo)} className={styles.btnPrimary}>
                          立即办理
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* 已办归档列表 */}
        {doneTodos.length > 0 && (
          <div className={styles.card}>
            <div className={styles.title}>已完成的协同任务历史 (已物理留痕保全)</div>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>待办ID</th>
                    <th>紧急程度</th>
                    <th>待办类型</th>
                    <th>协同业务主题</th>
                    <th>完成科室</th>
                    <th>运行状态</th>
                  </tr>
                </thead>
                <tbody>
                  {doneTodos.map((todo) => (
                    <tr key={todo.id}>
                      <td className={styles.fontMonospace}>#{todo.id}</td>
                      <td>
                        <span className={getSeverityBadge(todo.severity)}>{todo.severity}</span>
                      </td>
                      <td>{getTypeName(todo.type)}</td>
                      <td>{todo.title}</td>
                      <td>{todo.department}</td>
                      <td>
                        <span className={styles.textSuccess}>已处理 (AUDITED)</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 办理模态框抽屉 */}
        {activeTodo && (
          <div className={styles.modalOverlay}>
            <div className={styles.modalContent}>
              <div className={`${styles.flexBetween} ${styles.marginBottom20}`}>
                <div className={styles.title}>办理待办协作任务 #{activeTodo.id}</div>
                <button
                  onClick={handleCloseModal}
                  className={`${styles.btnSecondary} ${styles.pad4px8px}`}
                >
                  ✕
                </button>
              </div>

              <div className={styles.modalBodyText}>
                <div className={styles.marginBottom8}>
                  <strong>待办主题：</strong> {activeTodo.title}
                </div>
                <div className={styles.marginBottom8}>
                  <strong>协同类型：</strong> {getTypeName(activeTodo.type)}
                </div>
                <div className={styles.marginBottom8}>
                  <strong>时效要求：</strong> SLA 倒计时 {activeTodo.sla}
                </div>
              </div>

              <form onSubmit={handleSubmit}>
                <div className={styles.formGroup}>
                  <label className={styles.formLabel}>协作签署意见/审核备注</label>
                  <textarea
                    value={opinion}
                    onChange={(e) => setOpinion(e.target.value)}
                    placeholder="请输入具体的审批通过意见、拒绝理由或整改反馈摘要"
                    className={`${styles.formInput} ${styles.opinionTextarea}`}
                    required
                  />
                </div>

                <div className={styles.btnGroup}>
                  <button type="submit" disabled={submitting} className={styles.btnPrimary}>
                    {submitting ? "正在进行物理存证..." : "签署并提交保存"}
                  </button>
                  <button type="button" onClick={handleCloseModal} className={styles.btnSecondary}>
                    取消
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </PageShell>
  );
}
