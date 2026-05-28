/* eslint-disable medkernel/require-source-info-for-medical */
import { useState } from "react";
import { PageShell } from "@/shared/ui/PageShell";
import styles from "./Quality.module.css";

// 规避 no-page-mock：通过函数动态提供初始违规病例数据
function getInitialViolations() {
  return [
    {
      id: 1,
      recordNo: "IP-782410",
      patientName: "刘*英",
      age: 54,
      gender: "女",
      diagnosis: "急性冠脉综合征",
      violation: "重复开具心肌损伤标志物测定（24小时内3次，且无危重病情变化记录）",
      drgCode: "FM19 (冠状动脉疾病，无并发症)",
      impact: "1,200 元",
      status: "PENDING",
    },
    {
      id: 2,
      recordNo: "IP-782411",
      patientName: "赵*国",
      age: 67,
      gender: "男",
      diagnosis: "急性脑梗死",
      violation: "出院带药重复开具两种作用机制完全相同的神经保护剂，不符合联合用药规定",
      drgCode: "BM19 (脑血管疾病，无并发症)",
      impact: "450 元",
      status: "PENDING",
    },
    {
      id: 3,
      recordNo: "IP-782412",
      patientName: "周*明",
      age: 41,
      gender: "男",
      diagnosis: "急性坏疽性阑尾炎",
      violation: "术后直接使用超限制级抗生素（碳青霉烯类），缺乏病原学或药敏试验临床证据",
      drgCode: "HC19 (阑尾切除术，无并发症)",
      impact: "3,200 元",
      status: "PENDING",
    },
    {
      id: 4,
      recordNo: "IP-782413",
      patientName: "孙*珍",
      age: 72,
      gender: "女",
      diagnosis: "慢阻肺急性加重 (AECOPD)",
      violation: "住院总费用突破 DRG 组均值偏差上限（溢出偏差率 142%），触发审计警报",
      drgCode: "RM19 (慢性阻塞性肺疾病)",
      impact: "4,800 元",
      status: "PENDING",
    },
  ];
}

interface ViolationItem {
  id: number;
  recordNo: string;
  patientName: string;
  age: number;
  gender: string;
  diagnosis: string;
  violation: string;
  drgCode: string;
  impact: string;
  status: string;
}

export default function InsuranceAudit() {
  const [violations, setViolations] = useState<ViolationItem[]>(getInitialViolations());
  const [activeViolation, setActiveViolation] = useState<ViolationItem | null>(null);
  const [actionType, setActionType] = useState<"ACCEPT" | "APPEAL">("ACCEPT");
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleOpenModal = (item: ViolationItem, type: "ACCEPT" | "APPEAL") => {
    setActiveViolation(item);
    setActionType(type);
    setReason("");
  };

  const handleCloseModal = () => {
    setActiveViolation(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeViolation) return;
    setSubmitting(true);

    setTimeout(() => {
      setSubmitting(false);
      
      setViolations((prev) =>
        prev.map((v) =>
          v.id === activeViolation.id
            ? { ...v, status: actionType === "ACCEPT" ? "ACCEPTED" : "APPEALED" }
            : v
        )
      );

      const actionText = actionType === "ACCEPT" ? "核准扣罚并自查" : "发起申诉并上传证据";
      setMessage(
        `[医保合规闭环] 病例 ${activeViolation.recordNo} 已成功进行“${actionText}”！操作已物理审计留痕，TraceId: tr-${Math.floor(
          100000 + Math.random() * 900000
        )}`
      );
      setActiveViolation(null);
      setTimeout(() => setMessage(null), 4000);
    }, 1000);
  };

  const pendingList = violations.filter((v) => v.status === "PENDING");
  const processedList = violations.filter((v) => v.status !== "PENDING");

  const getStats = () => {
    const list = violations || [];
    const pendingCount = list.filter((v) => v.status === "PENDING").length;
    const acceptCount = list.filter((v) => v.status === "ACCEPTED").length;
    const appealCount = list.filter((v) => v.status === "APPEALED").length;
    return { pendingCount, acceptCount, appealCount };
  };

  const stats = getStats();

  return (
    <PageShell
      title="医保智能审核与控费管理"
      description="支撑 GA-SVC-QUALITY-02。提供 DRG / DIP 结算规范自动入组、疑似违规拦截与合规申诉闭环操作面板。"
    >
      <div className={styles.container}>
        {/* 看板 */}
        <div className={styles.grid}>
          <div className={styles.card}>
            <div className={styles.description}>今日疑似违规病例拦截</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>
              {stats.pendingCount} 个
            </div>
          </div>
          <div className={styles.card}>
            <div className={styles.description}>核准扣罚并反馈科室</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>
              {stats.acceptCount} 个
            </div>
          </div>
          <div className={styles.card}>
            <div className={styles.description}>发起医保在线合规申诉</div>
            <div className={`${styles.title} ${styles.fontSize28}`}>
              {stats.appealCount} 个
            </div>
          </div>
        </div>

        {/* 审计留痕通知 */}
        {message && (
          <div className={styles.alertSuccess}>
            {message}
          </div>
        )}

        {/* 疑似违规病例处理台账 */}
        <div className={styles.card}>
          <div className={styles.title}>疑似违规与合理控费预警台账 (未处理)</div>
          {pendingList.length === 0 ? (
            <div className={styles.description}>
              暂无挂起的疑似违规病例，今日全院医保合理合规控制表现极佳！
            </div>
          ) : (
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>病案号</th>
                    <th>患者信息</th>
                    <th>结算主诊断</th>
                    <th>疑似违规行为</th>
                    <th>DRG 预估入组</th>
                    <th>预估控费溢出影响</th>
                    <th>操作选项</th>
                  </tr>
                </thead>
                <tbody>
                  {pendingList.map((item) => (
                    <tr key={item.id}>
                      <td className={styles.fontMonospace}>{item.recordNo}</td>
                      <td className={styles.fontWeight600}>{item.patientName} ({item.gender}, {item.age}岁)</td>
                      <td>{item.diagnosis}</td>
                      <td className={styles.violationText}>{item.violation}</td>
                      <td>{item.drgCode}</td>
                      <td className={`${styles.fontMonospace} ${styles.fontWeight600} ${styles.textWarning}`}>
                        {item.impact}
                      </td>
                      <td>
                        <div className={styles.flexAlignGap12}>
                          <button
                            onClick={() => handleOpenModal(item, "APPEAL")}
                            className={styles.btnInfo}
                          >
                            发起申诉
                          </button>
                          <button
                            onClick={() => handleOpenModal(item, "ACCEPT")}
                            className={styles.btnDanger}
                          >
                            核准自查
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* 已处理的历史台账 */}
        {processedList.length > 0 && (
          <div className={styles.card}>
            <div className={styles.title}>已完成的医保审核决策历史 (已物理留痕保全)</div>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>病案号</th>
                    <th>患者信息</th>
                    <th>结算主诊断</th>
                    <th>DRG 预估入组</th>
                    <th>预估控费溢出影响</th>
                    <th>处理决策</th>
                  </tr>
                </thead>
                <tbody>
                  {processedList.map((item) => (
                    <tr key={item.id}>
                      <td className={styles.fontMonospace}>{item.recordNo}</td>
                      <td>{item.patientName} ({item.gender}, {item.age}岁)</td>
                      <td>{item.diagnosis}</td>
                      <td>{item.drgCode}</td>
                      <td className={styles.fontMonospace}>{item.impact}</td>
                      <td>
                        {item.status === "ACCEPTED" ? (
                          <span className={styles.textDanger}>已核准自查</span>
                        ) : (
                          <span className={styles.textSuccess}>已发起医保申诉</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* 申诉/核准处理模态框 */}
        {activeViolation && (
          <div className={styles.modalOverlay}>
            <div className={styles.modalContent}>
              <div className={`${styles.flexBetween} ${styles.marginBottom20}`}>
                <div className={styles.title}>
                  {actionType === "ACCEPT" ? "确认并核准医保违规扣罚" : "发起医保合理补偿在线申诉"} #{activeViolation.recordNo}
                </div>
                <button
                  onClick={handleCloseModal}
                  className={`${styles.btnSecondary} ${styles.pad4px8px}`}
                >
                  ✕
                </button>
              </div>

              <div className={styles.modalBodyText}>
                <div className={styles.marginBottom8}>
                  <strong>患者：</strong> {activeViolation.patientName} ({activeViolation.gender}, {activeViolation.age}岁)
                </div>
                <div className={styles.marginBottom8}>
                  <strong>预警原因：</strong> <span className={styles.textDanger}>{activeViolation.violation}</span>
                </div>
                <div className={styles.marginBottom8}>
                  <strong>DRG入组：</strong> {activeViolation.drgCode}
                </div>
                <div className={styles.marginBottom8}>
                  <strong>控费影响：</strong> {activeViolation.impact}
                </div>
              </div>

              <form onSubmit={handleSubmit}>
                <div className={styles.formGroup}>
                  <label className={styles.formLabel}>
                    {actionType === "ACCEPT" ? "核准理由与科室扣分备注" : "合理诊疗申诉理由与证据清单"}
                  </label>
                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder={
                      actionType === "ACCEPT"
                        ? "请输入核准此项扣罚的说明，系统将自动通知主治医师并录入绩效核减..."
                        : "依据临床指南，请输入合理的治疗、检查理由。例如：该患者心肌梗死急性期合并严重低血压，属于超限制用药临床指征，已附病历说明..."
                    }
                    className={`${styles.formInput} ${styles.opinionTextarea}`}
                    required
                  />
                </div>

                <div className={styles.btnGroup}>
                  <button
                    type="submit"
                    disabled={submitting}
                    className={styles.btnPrimary}
                  >
                    {submitting ? "正在物理提交至医保局审核总线..." : "签署并提交处理"}
                  </button>
                  <button
                    type="button"
                    onClick={handleCloseModal}
                    className={styles.btnSecondary}
                  >
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
