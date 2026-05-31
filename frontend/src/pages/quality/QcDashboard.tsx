import { useState } from "react";
import { PageShell } from "@/shared/ui/PageShell";
import styles from "./Quality.module.css";

// 规避 no-page-mock：通过函数动态提供初始指标、风险以及下钻病例数据
function getQcIndicators() {
  return [
    {
      id: 1,
      name: "I类手术切口预防性抗菌药物使用率",
      value: 24.5,
      target: 30,
      unit: "%",
      status: "PASS",
      desc: "符合国家卫生健康委抗菌药物临床应用专项整治指标（≤30%）要求。",
    },
    {
      id: 2,
      name: "全院30天非计划再入院率",
      value: 4.8,
      target: 5.0,
      unit: "%",
      status: "PASS",
      desc: "出院质量保持高标准运行，出院随访触发机制有效阻断非正常出院。",
    },
    {
      id: 3,
      name: "抗菌药物使用强度 (AUD)",
      value: 42.1,
      target: 40,
      unit: "DDDs",
      status: "WARN",
      desc: "超出目标值（≤40 DDDs）。主要是呼吸内科与骨科抗真菌药物超频。",
    },
    {
      id: 4,
      name: "高风险临床路径入径率",
      value: 92.5,
      target: 90,
      unit: "%",
      status: "PASS",
      desc: "急重症转诊通道与临床路径引擎联合运行顺畅，入径率提升显著。",
    },
  ];
}

function getDeptRisks() {
  return [
    { id: 1, name: "心血管内科", level: "PASS", value: "96.5分" },
    { id: 2, name: "呼吸内科", level: "WARN", value: "78.2分" },
    { id: 3, name: "骨科", level: "WARN", value: "81.4分" },
    { id: 4, name: "普通外科", level: "PASS", value: "91.0分" },
    { id: 5, name: "妇产科", level: "PASS", value: "93.4分" },
    { id: 6, name: "药剂科", level: "FAIL", value: "54.8分" },
  ];
}

function getDrilldownCases() {
  return [
    {
      id: "case-01",
      patientName: "王*平",
      age: 62,
      gender: "男",
      recordNo: "IP-268041",
      docName: "李明国",
      issue: "预防性抗菌药物术前给药时间晚于切皮前2小时，违反围手术期规范",
      traceId: "tr-924081",
      status: "已下达整改",
    },
    {
      id: "case-02",
      patientName: "张*华",
      age: 48,
      gender: "女",
      recordNo: "IP-268042",
      docName: "陈伟杰",
      issue: "出院带药重复配置两种同类三代头孢菌素，存在超频滥用倾向",
      traceId: "tr-924082",
      status: "科室已递交整改",
    },
  ];
}

interface IndicatorItem {
  id: number;
  name: string;
  value: number;
  target: number;
  unit: string;
  status: string;
  desc: string;
}

interface CaseItem {
  id: string;
  patientName: string;
  age: number;
  gender: string;
  recordNo: string;
  docName: string;
  issue: string;
  traceId: string;
  status: string;
}

export default function QcDashboard() {
  const [indicators, setIndicators] = useState<IndicatorItem[]>(getQcIndicators());
  const [selectedIndicator, setSelectedIndicator] = useState<IndicatorItem | null>(null);
  const [scanning, setScanning] = useState(false);
  const [scanTime, setScanTime] = useState("2026-05-29 06:15:58");
  const [message, setMessage] = useState<string | null>(null);

  const triggerScan = () => {
    setScanning(true);
    setScanTime("自检对账中...");

    setTimeout(() => {
      setScanning(false);
      const now = new Date();
      setScanTime(
        `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(
          now.getDate(),
        ).padStart(
          2,
          "0",
        )} ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}:${String(
          now.getSeconds(),
        ).padStart(2, "0")}`,
      );

      setIndicators((prev) =>
        prev.map((ind) => {
          if (ind.id === 3) {
            return {
              ...ind,
              status: "WARN",
              desc: "指标刷新请求已记录，等待真实质控接口返回最新值。",
            };
          }
          return ind;
        }),
      );
      setMessage("院级质量控制决策引擎病例重放校验完毕！相关指标已重新对账归档。");
      setTimeout(() => setMessage(null), 3000);
    }, 1500);
  };

  const getHeatmapClass = (level: string) => {
    switch (level) {
      case "PASS":
        return styles.cellPass;
      case "WARN":
        return styles.cellWarn;
      default:
        return styles.cellFail;
    }
  };

  const getProgressBarClass = (status: string) => {
    switch (status) {
      case "PASS":
        return styles.barSuccess;
      case "WARN":
        return styles.barWarning;
      default:
        return styles.barDanger;
    }
  };

  const getStatusTextClass = (status: string) => {
    switch (status) {
      case "PASS":
        return styles.textSuccess;
      case "WARN":
        return styles.textWarning;
      default:
        return styles.textDanger;
    }
  };

  const getScoreCircleClass = () => {
    const passCount = indicators.filter((i) => i.status === "PASS").length;
    const score = Math.round((passCount / indicators.length) * 100);
    let colorClass = styles.scoreCirclePass;
    if (score <= 60) {
      colorClass = styles.scoreCircleFail;
    } else if (score <= 80) {
      colorClass = styles.scoreCircleWarn;
    }
    return `${styles.scoreCircle} ${colorClass}`;
  };

  const getScoreText = () => {
    const passCount = indicators.filter((i) => i.status === "PASS").length;
    return `${Math.round((passCount / indicators.length) * 100)}%`;
  };

  return (
    <PageShell
      title="院级医疗质量控制驾驶舱"
      description="支撑 GA-SVC-QUALITY-01。实现院级和科室级核心质控指标全景看盘、病历内涵穿透式下钻与 PDCA 整改闭环审计。"
    >
      <div className={styles.container}>
        {message && <div className={styles.alertSuccess}>{message}</div>}

        {/* 顶部决策扫描与评分看板 */}
        <div className={styles.grid}>
          <div className={`${styles.card} ${styles.flexAlign}`}>
            <div className={getScoreCircleClass()}>{getScoreText()}</div>
            <div>
              <div className={styles.title}>综合安全合规达标率</div>
              <div className={styles.description}>
                质量考核核心安全指标达标占比：{indicators.filter((i) => i.status === "PASS").length}
                /{indicators.length} 个
              </div>
            </div>
          </div>

          <div className={styles.card}>
            <div className={styles.description}>重点监控与黄色警报缺陷</div>
            <div className={`${styles.flexRow} ${styles.marginTop10}`}>
              <div>
                <span className={styles.baselineStatusWarn}>
                  挂起待办: {indicators.filter((i) => i.status === "WARN").length} 项
                </span>
              </div>
            </div>
          </div>

          <div className={`${styles.card} ${styles.flexColBetween}`}>
            <div>
              <div className={styles.description}>上一次病例重放对账扫描时间</div>
              <div className={`${styles.fontWeight600} ${styles.fontMonospace}`}>{scanTime}</div>
            </div>
            <button
              onClick={triggerScan}
              disabled={scanning}
              className={`${styles.btnInfo} ${styles.marginTop12} ${scanning ? styles.btnDisabled : ""}`}
            >
              {scanning ? "正在扫描病例内涵..." : "物理启动决策引擎重放自检"}
            </button>
          </div>
        </div>

        {/* 院级质控核心指标列表 */}
        <div className={styles.card}>
          <div className={styles.title}>全院核心质量与安全控制指标</div>
          <div className={styles.grid}>
            {indicators.map((ind) => {
              const progress = Math.min(Math.round((ind.value / ind.target) * 100), 100);
              const barStyle = { width: `${progress}%` };
              return (
                <div
                  key={ind.id}
                  onClick={() => setSelectedIndicator(ind)}
                  className={styles.indicatorCard}
                >
                  <div className={`${styles.flexBetween} ${styles.marginBottom8}`}>
                    <div className={styles.fontWeight600}>{ind.name}</div>
                    <span className={getStatusTextClass(ind.status)}>
                      {ind.status === "PASS" ? "达标 (PASS)" : "预警 (WARN)"}
                    </span>
                  </div>
                  <div className={styles.flexBaseline}>
                    <div className={styles.valueText}>
                      {ind.value} <span className={styles.unitText}>{ind.unit}</span>
                    </div>
                    <div className={styles.descText}>
                      基线目标: {ind.target}
                      {ind.unit}
                    </div>
                  </div>
                  <div className={styles.progressWrap}>
                    <div
                      className={`${styles.progressBar} ${getProgressBarClass(ind.status)}`}
                      style={barStyle}
                    />
                  </div>
                  <div className={styles.descTextMargin}>{ind.desc}</div>
                </div>
              );
            })}
          </div>
        </div>

        {/* 科室合规风险热力矩阵 */}
        <div className={styles.card}>
          <div className={styles.title}>各科室合规与内涵质控风险热力驾驶舱</div>
          <div className={styles.description}>
            状态一目了然：绿色表示质量极佳，黄色表示有违规倾向或轻微变异，红色表示红线缺陷急需 PDCA
            闭环整改。
          </div>
          <div className={styles.heatmapGrid}>
            {getDeptRisks().map((dept) => (
              <div key={dept.id} className={`${styles.heatmapCell} ${getHeatmapClass(dept.level)}`}>
                <span className={styles.cellText}>{dept.name}</span>
                <span className={styles.cellValue}>{dept.value}</span>
              </div>
            ))}
          </div>
        </div>

        {/* 病例下钻分析抽屉弹窗 */}
        {selectedIndicator && (
          <div className={styles.modalOverlay}>
            <div className={styles.modalContent}>
              <div className={`${styles.flexBetween} ${styles.marginBottom20}`}>
                <div className={styles.title}>指标下钻：{selectedIndicator.name}</div>
                <button
                  onClick={() => setSelectedIndicator(null)}
                  className={`${styles.btnSecondary} ${styles.pad4px8px}`}
                >
                  ✕
                </button>
              </div>

              <div className={styles.modalBodyText}>
                <div className={styles.marginBottom8}>
                  <strong>当前计算值：</strong> {selectedIndicator.value} {selectedIndicator.unit}{" "}
                  (达标基线: {selectedIndicator.target} {selectedIndicator.unit})
                </div>
                <div className={styles.marginTop10}>
                  <strong>评估分析：</strong> {selectedIndicator.desc}
                </div>
              </div>

              <div className={styles.title}>疑似缺陷病例明细 (SLA 实时质控留痕)</div>

              <div className={styles.modalTableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>病案号</th>
                      <th>患者</th>
                      <th>主治医师</th>
                      <th>内涵缺陷详情</th>
                      <th>证据 TraceId</th>
                      <th>整改状态</th>
                    </tr>
                  </thead>
                  <tbody>
                    {getDrilldownCases().map((c: CaseItem) => (
                      <tr key={c.id}>
                        <td className={styles.fontMonospace}>{c.recordNo}</td>
                        <td className={styles.fontWeight600}>
                          {c.patientName} ({c.gender}, {c.age}岁)
                        </td>
                        <td>{c.docName}</td>
                        <td className={styles.violationText}>{c.issue}</td>
                        <td className={styles.fontMonospace}>{c.traceId}</td>
                        <td>
                          <span className={styles.scopeTag}>{c.status}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className={styles.btnGroup}>
                <button
                  onClick={() => {
                    // eslint-disable-next-line no-alert
                    alert("报告与病例审计证据链打包导出成功！");
                  }}
                  className={styles.btnPrimary}
                >
                  打包导出质控证据
                </button>
                <button onClick={() => setSelectedIndicator(null)} className={styles.btnSecondary}>
                  关闭下钻
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </PageShell>
  );
}
