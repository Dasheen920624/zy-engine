import { useMemo, useState } from "react";
import { Button, Empty, Input, Select, Space, Spin, Typography } from "antd";
import { SearchOutlined } from "@ant-design/icons";
import { ETHNICITIES } from "../../api/mpi";
import type { Ethnicity, PatientIdentityStatus } from "../../api/mpi";
import type { MpiPatientRecord } from "./helpers";
import PatientCard from "./components/PatientCard";
import styles from "./styles.module.css";

const { Text } = Typography;

export interface PatientSearchPayload {
  keyword: string;
  identity_type: string;
  source_system: string;
}

export interface PatientListProps {
  patients: MpiPatientRecord[];
  selectedPatientId?: string;
  loading?: boolean;
  showSensitive?: boolean;
  onSearch: (payload: PatientSearchPayload) => void;
  onSelectPatient: (platform_patient_id: string) => void;
}

const SEARCH_TYPES = [
  { value: "PLATFORM_PATIENT_ID", label: "平台患者 ID" },
  { value: "ID_CARD", label: "居民身份证" },
  { value: "MOBILE_PHONE", label: "手机号" },
  { value: "HIS_PATIENT_ID", label: "HIS 患者号" },
  { value: "EMR_PATIENT_ID", label: "EMR 患者号" },
  { value: "INSURANCE_ID", label: "医保号" },
];

const STATUS_OPTIONS: Array<{ value: PatientIdentityStatus | ""; label: string }> = [
  { value: "", label: "全部状态" },
  { value: "ACTIVE", label: "有效" },
  { value: "CONFLICT", label: "冲突" },
  { value: "MERGED", label: "已合并" },
  { value: "INACTIVE", label: "停用" },
];

export default function PatientList({
  patients,
  selectedPatientId,
  loading = false,
  showSensitive = false,
  onSearch,
  onSelectPatient,
}: PatientListProps) {
  const [keyword, setKeyword] = useState("");
  const [identity_type, setIdentityType] = useState("PLATFORM_PATIENT_ID");
  const [source_system, setSourceSystem] = useState("HIS");
  const [status, setStatus] = useState<PatientIdentityStatus | "">("");
  const [ethnicity, setEthnicity] = useState<Ethnicity | "">("");

  const filteredPatients = useMemo(
    () =>
      patients.filter((patient) => {
        const matchesStatus = !status || patient.status === status;
        const matchesEthnicity = !ethnicity || patient.ethnicity === ethnicity;
        return matchesStatus && matchesEthnicity;
      }),
    [ethnicity, patients, status],
  );

  const submitSearch = () => {
    onSearch({
      keyword: keyword.trim(),
      identity_type,
      source_system: source_system.trim() || "HIS",
    });
  };

  let listContent;
  if (loading && !filteredPatients.length) {
    listContent = (
      <div className={styles.loadingState}>
        <Spin />
      </div>
    );
  } else if (filteredPatients.length) {
    listContent = (
      <div className={styles.patientList}>
        {filteredPatients.map((patient) => (
          <PatientCard
            key={patient.platform_patient_id}
            patient={patient}
            selected={patient.platform_patient_id === selectedPatientId}
            showSensitive={showSensitive}
            onSelect={onSelectPatient}
          />
        ))}
      </div>
    );
  } else {
    listContent = (
      <Empty
        className={styles.emptyState}
        description={
          <Space direction="vertical" size="small">
            <span>暂无患者主索引结果</span>
            <Text type="secondary">请先按平台患者 ID 或外部标识搜索。</Text>
          </Space>
        }
      />
    );
  }

  return (
    <section className={styles.listPanel} aria-label="患者列表">
      <div className={styles.panelHeader}>
        <div>
          <h2>患者主索引</h2>
          <Text type="secondary">按平台患者 ID 或外部标识定位患者，结果默认脱敏。</Text>
        </div>
      </div>

      <div className={styles.searchBar} role="search">
        <Select
          className={styles.searchType}
          value={identity_type}
          options={SEARCH_TYPES}
          onChange={setIdentityType}
          aria-label="搜索标识类型"
        />
        <Input
          className={styles.searchInput}
          prefix={<SearchOutlined />}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          onPressEnter={submitSearch}
          placeholder="输入患者 ID、证件号或外部号"
          aria-label="患者搜索关键字"
        />
        {identity_type !== "PLATFORM_PATIENT_ID" && (
          <Input
            className={styles.sourceInput}
            value={source_system}
            onChange={(event) => setSourceSystem(event.target.value)}
            placeholder="来源系统"
            aria-label="来源系统"
          />
        )}
        <Button type="primary" icon={<SearchOutlined />} loading={loading} onClick={submitSearch}>
          搜索
        </Button>
      </div>

      <div className={styles.filterBar}>
        <Select
          className={styles.filterSelect}
          value={status}
          options={STATUS_OPTIONS}
          onChange={setStatus}
          aria-label="状态筛选"
        />
        <Select
          showSearch
          allowClear
          className={styles.filterSelect}
          value={ethnicity || undefined}
          placeholder="民族筛选"
          options={ETHNICITIES.map((item) => ({ value: item, label: item }))}
          onChange={(value) => setEthnicity((value || "") as Ethnicity | "")}
          aria-label="民族筛选"
        />
        <span className={styles.resultCount}>共 {filteredPatients.length} 条</span>
      </div>

      {listContent}
    </section>
  );
}
