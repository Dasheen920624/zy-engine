/**
 * 患者路径实例详情（路由 /pathway/patients/:instanceId）。
 *
 * 主区：基本信息 + 节点进度时间轴 + 当前节点任务列表
 * 侧栏：变异列表 + 记录变异按钮
 */

import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  Alert,
  Button,
  Result,
  Space,
  Spin,
  Tag,
  Typography,
  message,
} from "antd";
import {
  ArrowLeftOutlined,
  ExclamationCircleOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  completeNode,
  completeTask,
  getNodeState,
  getPatientPathwayInstance,
  listVariations,
  skipTask,
  type PatientNodeState,
  type PatientTaskState,
  type PatientPathwayInstance,
  type PathwayVariationRecord,
} from "../../../api/pathway";
import NodeProgressTimeline from "./components/NodeProgressTimeline";
import TaskList from "./components/TaskList";
import VariationCard from "./components/VariationCard";
import VariationDialog from "./VariationDialog";
import {
  describeInstanceStatus,
  INSTANCE_STATUS_COLOR,
  maskPatientId,
} from "../helpers/pathwayFormatters";
import styles from "../styles.module.css";

const { Title, Text } = Typography;

/** 把后端实例详情 Map 适配为 PatientPathwayInstance + 节点数组 */
interface InstanceDetailEnvelope {
  instance: PatientPathwayInstance;
  nodes: PatientNodeState[];
}

function adaptInstanceDetail(raw: Record<string, unknown> | undefined): InstanceDetailEnvelope | null {
  if (!raw) return null;
  const instance = (raw.instance ?? raw) as PatientPathwayInstance;
  const nodes = (raw.nodes as PatientNodeState[]) ?? [];
  return { instance, nodes };
}

export default function PatientPathwayDetail() {
  const params = useParams<{ instanceId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const instanceId = params.instanceId ?? "";

  const detailQuery = useQuery({
    queryKey: ["patient-pathway-instance", instanceId],
    queryFn: () => getPatientPathwayInstance(instanceId),
    enabled: Boolean(instanceId),
  });

  const envelope = useMemo(() => adaptInstanceDetail(detailQuery.data), [detailQuery.data]);

  const currentNodeCode = envelope?.instance.current_node_code;

  const currentNodeQuery = useQuery<PatientNodeState>({
    queryKey: ["patient-pathway-node", instanceId, currentNodeCode],
    queryFn: () => getNodeState(instanceId, currentNodeCode as string),
    enabled: Boolean(instanceId && currentNodeCode),
  });

  const variationsQuery = useQuery<PathwayVariationRecord[]>({
    queryKey: ["patient-pathway-variations", instanceId],
    queryFn: () => listVariations({ instance_id: instanceId, limit: 50 }),
    enabled: Boolean(instanceId),
  });

  const [variationDialogOpen, setVariationDialogOpen] = useState(false);

  const invalidateAll = () => {
    queryClient.invalidateQueries({ queryKey: ["patient-pathway-instance", instanceId] });
    queryClient.invalidateQueries({ queryKey: ["patient-pathway-node", instanceId] });
    queryClient.invalidateQueries({ queryKey: ["patient-pathway-variations", instanceId] });
  };

  const completeTaskMutation = useMutation({
    mutationFn: (task: PatientTaskState) =>
      completeTask(instanceId, task.node_code, task.task_code),
    onSuccess: () => {
      message.success("任务已完成");
      invalidateAll();
    },
    onError: (err: Error) => message.error(`完成失败：${err.message}`),
  });

  const skipTaskMutation = useMutation({
    mutationFn: (task: PatientTaskState) => skipTask(instanceId, task.node_code, task.task_code),
    onSuccess: () => {
      message.success("任务已跳过");
      invalidateAll();
    },
    onError: (err: Error) => message.error(`跳过失败：${err.message}`),
  });

  const completeNodeMutation = useMutation({
    mutationFn: () =>
      completeNode(instanceId, currentNodeCode as string, { auto_advance: true }),
    onSuccess: () => {
      message.success("节点已完成，路径已推进");
      invalidateAll();
    },
    onError: (err: Error) => message.error(`节点完成失败：${err.message}`),
  });

  if (detailQuery.isLoading) {
    return (
      <div className={styles.page}>
        <Spin tip="加载实例详情..." />
      </div>
    );
  }

  if (detailQuery.isError || !envelope) {
    return (
      <Result
        status="404"
        title="实例未找到"
        subTitle={`未找到实例 ${instanceId}`}
        extra={
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/pathway/patients")}>
            返回列表
          </Button>
        }
      />
    );
  }

  const { instance, nodes } = envelope;
  const currentNode = currentNodeQuery.data;
  const tasks: PatientTaskState[] = currentNode?.tasks ?? [];
  const variations = variationsQuery.data ?? [];

  return (
    <div className={styles.page}>
      <header className={styles.pageHeader}>
        <div>
          <Button
            type="link"
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate("/pathway/patients")}
          >
            返回患者路径列表
          </Button>
          <Title level={3} className={styles.pageTitle}>
            {instance.pathway_code}
            {instance.version_no ? ` · v${instance.version_no}` : ""}
          </Title>
          <Space size="middle" wrap>
            <Tag color={INSTANCE_STATUS_COLOR[instance.status] ?? "default"}>
              {describeInstanceStatus(instance.status)}
            </Tag>
            <Text>
              <UserOutlined /> 患者 {maskPatientId(instance.patient_id)}
            </Text>
            <Text type="secondary">就诊 {instance.encounter_id}</Text>
            {instance.current_node_code && (
              <Tag color="processing">当前节点 {instance.current_node_code}</Tag>
            )}
          </Space>
        </div>
        <div className={styles.headerActions}>
          <Button
            icon={<ExclamationCircleOutlined />}
            onClick={() => setVariationDialogOpen(true)}
          >
            记录变异
          </Button>
        </div>
      </header>

      <div className={styles.detailGrid}>
        <div className={styles.detailMain}>
          {/* ─── 节点进度时间轴 ─── */}
          <section className={styles.sectionCard} aria-label="patient-pathway-nodes">
            <h3 className={styles.sectionTitle}>节点进度</h3>
            <NodeProgressTimeline nodes={nodes} currentNodeCode={currentNodeCode} />
          </section>

          {/* ─── 当前节点任务列表 ─── */}
          <section className={styles.sectionCard} aria-label="patient-pathway-tasks">
            <h3 className={styles.sectionTitle}>
              当前节点任务{currentNode?.node_name ? ` · ${currentNode.node_name}` : ""}
            </h3>
            {currentNodeQuery.isLoading ? (
              <Spin />
            ) : !currentNodeCode ? (
              <Alert
                showIcon
                type="info"
                message="该实例没有当前节点（路径已完成 / 终止）。"
              />
            ) : (
              <>
                <TaskList
                  tasks={tasks}
                  onComplete={(task) => completeTaskMutation.mutate(task)}
                  onSkip={(task) => skipTaskMutation.mutate(task)}
                  disabled={completeTaskMutation.isPending || skipTaskMutation.isPending}
                />
                {tasks.length > 0 && (
                  <div className={styles.taskActions}>
                    <Button
                      type="primary"
                      loading={completeNodeMutation.isPending}
                      onClick={() => completeNodeMutation.mutate()}
                      aria-label="complete-current-node"
                    >
                      完成当前节点（推进路径）
                    </Button>
                  </div>
                )}
              </>
            )}
          </section>
        </div>

        <aside className={styles.detailSide}>
          {/* ─── 变异记录 ─── */}
          <section className={styles.sectionCard} aria-label="patient-pathway-variations">
            <h3 className={styles.sectionTitle}>变异记录</h3>
            {variationsQuery.isLoading ? (
              <Spin />
            ) : variations.length === 0 ? (
              <Text type="secondary">尚无变异</Text>
            ) : (
              <div className={styles.variationList} role="list">
                {variations.map((v) => (
                  <VariationCard key={v.variation_id} variation={v} />
                ))}
              </div>
            )}
          </section>
        </aside>
      </div>

      <VariationDialog
        open={variationDialogOpen}
        instanceId={instanceId}
        currentNodeCode={currentNodeCode}
        onClose={() => setVariationDialogOpen(false)}
        onRecorded={invalidateAll}
      />
    </div>
  );
}
