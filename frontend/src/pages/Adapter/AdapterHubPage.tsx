import { Tabs, Typography } from "antd";
import { ClusterOutlined } from "@ant-design/icons";
import AdapterDefinitionList from "./components/AdapterDefinitionList";
import InteropAdapterList from "./components/InteropAdapterList";
import TriggerPointList from "./components/TriggerPointList";
import styles from "./styles.module.css";

const { Title, Paragraph } = Typography;

/**
 * 适配器中心主页（PR-FINAL-12 /adapter/hub）。
 *
 * 3 Tab 结构：
 *  Tab 1「业务适配器」：HIS/EMR/LIS 等院内系统的具名查询定义（/api/adapters/definitions）
 *  Tab 2「互联互通」：HL7/FHIR/CDA/IHE/CDS Hooks/SMART/DICOM 标准适配（/api/interop/*）
 *  Tab 3「CDSS 触发点」：业务事件 → 接入策略 → 规则/路径绑定（/api/cdss/triggers）
 *
 * 全部端点已在 develop 就绪（AdapterHubController / InteropController / TriggerPointController）。
 */
export default function AdapterHubPage() {
  return (
    <div>
      <div className={`mk-page-header ${styles.pageHeader}`}>
        <Title level={3} className={styles.pageTitle}>
          <ClusterOutlined /> 适配器中心
        </Title>
        <Paragraph type="secondary" className={styles.pageHint}>
          ADAPT-001 / INTEROP-001：HIS/EMR/LIS 适配器、互联互通标准（HL7/FHIR/CDS Hooks）、CDSS 触发点统一治理
        </Paragraph>
      </div>

      <Tabs
        defaultActiveKey="adapters"
        items={[
          {
            key: "adapters",
            label: "业务适配器",
            children: <AdapterDefinitionList />,
          },
          {
            key: "interop",
            label: "互联互通",
            children: <InteropAdapterList />,
          },
          {
            key: "triggers",
            label: "CDSS 触发点",
            children: <TriggerPointList />,
          },
        ]}
      />
    </div>
  );
}
