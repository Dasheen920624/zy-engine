import { useCallback, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Button,
  Card,
  Col,
  Empty,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  FilterOutlined,
  ReloadOutlined,
  RobotOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import { AiBadge } from '../../components/AiBadge';
import { DangerConfirm } from '../../components/DangerConfirm';
import type { DangerLevel } from '../../components/DangerConfirm';
import {
  fetchPendingMappings,
  fetchMappedItems,
  fetchConflictItems,
  fetchAiCandidates,
  acceptMapping,
  batchAcceptMappings,
  rejectAiCandidate,
} from '../../api/terminology';
import type {
  TerminologyItem,
  AiCandidate,
  TermType,
  MappingRequest,
} from '../../api/terminology';

const { Text } = Typography;

// ─── 常量 ──────────────────────────────────────────────────────────

const TERM_TYPE_OPTIONS: { value: TermType; label: string }[] = [
  { value: 'DIAGNOSIS', label: '诊断' },
  { value: 'PROCEDURE', label: '手术/操作' },
  { value: 'MEDICATION', label: '药品' },
  { value: 'LAB_TEST', label: '检验' },
  { value: 'ORDER', label: '医嘱' },
  { value: 'OTHER', label: '其他' },
];

type TabKey = 'pending' | 'mapped' | 'conflict' | 'ai';

const TAB_LIST = [
  { key: 'pending', tab: '未映射' },
  { key: 'mapped', tab: '已映射' },
  { key: 'conflict', tab: '冲突' },
  { key: 'ai', tab: 'AI 候选' },
] as const;

// ─── 工具函数 ──────────────────────────────────────────────────────

function formatTime(time?: string): string {
  if (!time) return '—';
  try {
    const d = new Date(time);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  } catch {
    return time;
  }
}

function termTypeLabel(t?: TermType): string {
  const opt = TERM_TYPE_OPTIONS.find((o) => o.value === t);
  return opt?.label ?? '—';
}

// ─── 主页面 ──────────────────────────────────────────────────────────

export default function MappingWorkbench() {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<TabKey>('pending');
  const [termType, setTermType] = useState<TermType | undefined>();
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const pageSize = 20;

  // 选中的未映射项（用于批量采纳）
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);

  // 单个采纳确认
  const [confirmTarget, setConfirmTarget] = useState<TerminologyItem | null>(null);
  // 批量采纳确认
  const [batchConfirmOpen, setBatchConfirmOpen] = useState(false);
  // AI候选拒绝原因弹窗
  const [rejectTarget, setRejectTarget] = useState<AiCandidate | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  // ─── 数据查询 ────────────────────────────────────────────────

  const { data: pendingData, isLoading: pendingLoading } = useQuery({
    queryKey: ['terminology', 'pending', termType, page, pageSize],
    queryFn: () => fetchPendingMappings({ type: termType, page, size: pageSize }),
    enabled: activeTab === 'pending',
  });

  const { data: mappedData, isLoading: mappedLoading } = useQuery({
    queryKey: ['terminology', 'mapped', termType, page, pageSize],
    queryFn: () => fetchMappedItems({ type: termType, page, size: pageSize }),
    enabled: activeTab === 'mapped',
  });

  const { data: conflictData, isLoading: conflictLoading } = useQuery({
    queryKey: ['terminology', 'conflict', termType, page, pageSize],
    queryFn: () => fetchConflictItems({ type: termType, page, size: pageSize }),
    enabled: activeTab === 'conflict',
  });

  const { data: aiData, isLoading: aiLoading } = useQuery({
    queryKey: ['terminology', 'ai-candidates', page, pageSize],
    queryFn: () => fetchAiCandidates({ page, size: pageSize }),
    enabled: activeTab === 'ai',
  });

  // ─── 操作 mutation ──────────────────────────────────────────

  const acceptMutation = useMutation({
    mutationFn: (req: MappingRequest) => acceptMapping(req),
    onSuccess: () => {
      message.success('采纳成功');
      setConfirmTarget(null);
      queryClient.invalidateQueries({ queryKey: ['terminology'] });
    },
    onError: (err: Error) => message.error(`采纳失败: ${err.message}`),
  });

  const batchAcceptMutation = useMutation({
    mutationFn: (req: { mappings: MappingRequest[] }) =>
      batchAcceptMappings({ mappings: req.mappings, reviewer: 'current_user' }),
    onSuccess: () => {
      message.success('批量采纳成功');
      setBatchConfirmOpen(false);
      setSelectedRowKeys([]);
      queryClient.invalidateQueries({ queryKey: ['terminology'] });
    },
    onError: (err: Error) => message.error(`批量采纳失败: ${err.message}`),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ candidateId, reason }: { candidateId: string; reason: string }) =>
      rejectAiCandidate(candidateId, reason),
    onSuccess: () => {
      message.success('已拒绝');
      setRejectTarget(null);
      setRejectReason('');
      queryClient.invalidateQueries({ queryKey: ['terminology'] });
    },
    onError: (err: Error) => message.error(`拒绝失败: ${err.message}`),
  });

  // ─── 事件处理 ────────────────────────────────────────────────

  const handleTabChange = useCallback((key: string) => {
    setActiveTab(key as TabKey);
    setPage(1);
    setSelectedRowKeys([]);
  }, []);

  const handleAcceptSingle = useCallback((item: TerminologyItem) => {
    setConfirmTarget(item);
  }, []);

  const handleConfirmAccept = useCallback(async () => {
    if (!confirmTarget) return;
    acceptMutation.mutate({
      terminology_id: confirmTarget.id,
      standard_code: confirmTarget.standard_code || '',
      standard_name: confirmTarget.standard_name || '',
      standard_system: confirmTarget.standard_system || '',
      reviewer: 'current_user',
    });
  }, [confirmTarget, acceptMutation]);

  const handleBatchAccept = useCallback(() => {
    if (selectedRowKeys.length === 0) {
      message.warning('请先选择要采纳的项');
      return;
    }
    setBatchConfirmOpen(true);
  }, [selectedRowKeys]);

  const handleConfirmBatchAccept = useCallback(async () => {
    const items = (pendingData?.items || []).filter((i) => selectedRowKeys.includes(i.id));
    const mappings: MappingRequest[] = items.map((item) => ({
      terminology_id: item.id,
      standard_code: item.standard_code || '',
      standard_name: item.standard_name || '',
      standard_system: item.standard_system || '',
      reviewer: 'current_user',
    }));
    batchAcceptMutation.mutate({ mappings });
  }, [selectedRowKeys, pendingData, batchAcceptMutation]);

  const handleRejectAi = useCallback((candidate: AiCandidate) => {
    setRejectTarget(candidate);
  }, []);

  const handleConfirmReject = useCallback(async () => {
    if (!rejectTarget) return;
    if (!rejectReason.trim()) {
      message.warning('请填写拒绝原因');
      return;
    }
    rejectMutation.mutate({ candidateId: rejectTarget.id, reason: rejectReason.trim() });
  }, [rejectTarget, rejectReason, rejectMutation]);

  // ─── 客户端关键词过滤 ──────────────────────────────────────

  const filterByKeyword = useCallback(
    <T extends { internal_code?: string; internal_name?: string }>(items: T[]): T[] => {
      if (!keyword) return items;
      const kw = keyword.toLowerCase();
      return items.filter(
        (item) =>
          (item.internal_code || '').toLowerCase().includes(kw) ||
          (item.internal_name || '').toLowerCase().includes(kw),
      );
    },
    [keyword],
  );

  // ─── 未映射表格列 ──────────────────────────────────────────

  const pendingColumns: ColumnsType<TerminologyItem> = [
    {
      title: '院内编码',
      dataIndex: 'internal_code',
      key: 'code',
      width: 160,
      render: (v: string) => (
        <Text strong style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 13 }}>
          {v}
        </Text>
      ),
    },
    {
      title: '院内名称',
      dataIndex: 'internal_name',
      key: 'name',
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'term_type',
      key: 'type',
      width: 80,
      render: (v: TermType) => <Tag>{termTypeLabel(v)}</Tag>,
    },
    {
      title: 'AI 建议',
      key: 'ai',
      width: 240,
      render: (_: unknown, r: TerminologyItem) =>
        r.standard_code ? (
          <Space direction="vertical" size={0}>
            <Text style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 12 }}>
              {r.standard_code}
            </Text>
            {r.confidence != null && (
              <Text type="secondary" style={{ fontSize: 11 }}>
                置信 {Math.round(r.confidence * 100)}%
              </Text>
            )}
          </Space>
        ) : (
          <Text type="secondary">—</Text>
        ),
    },
    {
      title: '操作',
      key: 'action',
      width: 140,
      render: (_: unknown, r: TerminologyItem) => (
        <Space size="small">
          <Button
            size="small"
            type="primary"
            disabled={!r.standard_code}
            onClick={() => handleAcceptSingle(r)}
          >
            采纳
          </Button>
          <Button size="small">手动</Button>
        </Space>
      ),
    },
  ];

  // ─── 已映射表格列 ──────────────────────────────────────────

  const mappedColumns: ColumnsType<TerminologyItem> = [
    {
      title: '院内编码',
      dataIndex: 'internal_code',
      key: 'code',
      width: 160,
      render: (v: string) => (
        <Text strong style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 13 }}>
          {v}
        </Text>
      ),
    },
    {
      title: '院内名称',
      dataIndex: 'internal_name',
      key: 'name',
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'term_type',
      key: 'type',
      width: 80,
      render: (v: TermType) => <Tag>{termTypeLabel(v)}</Tag>,
    },
    {
      title: '标准编码',
      dataIndex: 'standard_code',
      key: 'std_code',
      width: 140,
      render: (v: string) => (
        <Text style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 12 }}>{v}</Text>
      ),
    },
    {
      title: '标准名称',
      dataIndex: 'standard_name',
      key: 'std_name',
      ellipsis: true,
    },
    {
      title: '审核人',
      dataIndex: 'reviewer',
      key: 'reviewer',
      width: 100,
    },
    {
      title: '审核时间',
      dataIndex: 'reviewed_at',
      key: 'reviewed_at',
      width: 150,
      render: (v: string) => (
        <Text type="secondary" style={{ fontSize: 12 }}>
          {formatTime(v)}
        </Text>
      ),
    },
  ];

  // ─── 冲突表格列 ──────────────────────────────────────────

  const conflictColumns: ColumnsType<TerminologyItem> = [
    {
      title: '院内编码',
      dataIndex: 'internal_code',
      key: 'code',
      width: 160,
      render: (v: string) => (
        <Text strong style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 13 }}>
          {v}
        </Text>
      ),
    },
    {
      title: '院内名称',
      dataIndex: 'internal_name',
      key: 'name',
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'term_type',
      key: 'type',
      width: 80,
      render: (v: TermType) => <Tag>{termTypeLabel(v)}</Tag>,
    },
    {
      title: '冲突原因',
      dataIndex: 'conflict_reason',
      key: 'reason',
      render: (v: string) => (
        <Tag color="error">{v || '多候选冲突'}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: () => (
        <Button size="small" type="primary">
          解决冲突
        </Button>
      ),
    },
  ];

  // ─── AI候选表格列 ──────────────────────────────────────────

  const aiColumns: ColumnsType<AiCandidate> = [
    {
      title: '关联术语',
      dataIndex: 'terminology_id',
      key: 'term',
      width: 160,
      render: (v: string) => (
        <Text style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 12 }}>{v}</Text>
      ),
    },
    {
      title: '标准编码',
      dataIndex: 'standard_code',
      key: 'std_code',
      width: 140,
      render: (v: string) => (
        <Text strong style={{ fontFamily: 'var(--mk-font-mono)', fontSize: 13 }}>
          {v}
        </Text>
      ),
    },
    {
      title: '标准名称',
      dataIndex: 'standard_name',
      key: 'std_name',
      ellipsis: true,
    },
    {
      title: '标准体系',
      dataIndex: 'standard_system',
      key: 'system',
      width: 100,
    },
    {
      title: 'AI 标识',
      key: 'ai_badge',
      width: 200,
      render: (_: unknown, r: AiCandidate) => (
        <AiBadge
          confidence={r.confidence}
          model={r.model}
          generatedAt={formatTime(r.generated_at)}
          reviewStatus={r.review_status === 'PENDING' ? 'pending' : r.review_status.toLowerCase() as 'accepted' | 'rejected' | 'modified'}
          variant="badge"
        />
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 200,
      render: (_: unknown, r: AiCandidate) => (
        <Space size="small">
          <Tooltip title="采纳此候选映射">
            <Button
              size="small"
              type="primary"
              onClick={() => {
                // 跳转到采纳流程：这里简化为直接弹确认
                Modal.confirm({
                  title: '采纳 AI 候选',
                  content: `确认采纳 ${r.standard_code} (${r.standard_name})？`,
                  okText: '确认采纳',
                  okButtonProps: { danger: true },
                  onOk: () => {
                    acceptMutation.mutate({
                      terminology_id: r.terminology_id,
                      standard_code: r.standard_code,
                      standard_name: r.standard_name,
                      standard_system: r.standard_system,
                      reviewer: 'current_user',
                    });
                  },
                });
              }}
            >
              采纳
            </Button>
          </Tooltip>
          <Button size="small" onClick={() => handleRejectAi(r)}>
            拒绝
          </Button>
        </Space>
      ),
    },
  ];

  // ─── 获取当前Tab的数据和列 ──────────────────────────────────

  const currentData = useMemo(() => {
    switch (activeTab) {
      case 'pending':
        return { items: filterByKeyword(pendingData?.items || []), total: pendingData?.total || 0, loading: pendingLoading };
      case 'mapped':
        return { items: filterByKeyword(mappedData?.items || []), total: mappedData?.total || 0, loading: mappedLoading };
      case 'conflict':
        return { items: filterByKeyword(conflictData?.items || []), total: conflictData?.total || 0, loading: conflictLoading };
      case 'ai':
        return { items: aiData?.items || [], total: aiData?.total || 0, loading: aiLoading };
    }
  }, [activeTab, pendingData, mappedData, conflictData, aiData, pendingLoading, mappedLoading, conflictLoading, aiLoading, filterByKeyword]);

  // ─── Tab 额外内容 ──────────────────────────────────────────

  const tabExtra = useMemo(() => {
    return (
      <Space>
        <Select
          allowClear
          placeholder="全部类型"
          style={{ width: 130 }}
          size="small"
          value={termType}
          onChange={(v) => {
            setTermType(v);
            setPage(1);
          }}
          options={TERM_TYPE_OPTIONS}
        />
        <Input
          size="small"
          placeholder="搜索编码/名称"
          prefix={<SearchOutlined style={{ color: 'var(--mk-text-tertiary)' }} />}
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          allowClear
          style={{ width: 180 }}
        />
        {activeTab === 'pending' && selectedRowKeys.length > 0 && (
          <Button
            size="small"
            type="primary"
            icon={<CheckCircleOutlined />}
            onClick={handleBatchAccept}
          >
            批量采纳 ({selectedRowKeys.length})
          </Button>
        )}
        <Button
          size="small"
          icon={<ReloadOutlined />}
          onClick={() => queryClient.invalidateQueries({ queryKey: ['terminology'] })}
        >
          刷新
        </Button>
      </Space>
    );
  }, [activeTab, termType, keyword, selectedRowKeys, queryClient, handleBatchAccept]);

  // ─── 未映射行选择 ──────────────────────────────────────────

  const rowSelection =
    activeTab === 'pending'
      ? {
          selectedRowKeys,
          onChange: (keys: React.Key[]) => setSelectedRowKeys(keys as string[]),
          getCheckboxProps: (record: TerminologyItem) => ({
            disabled: !record.standard_code,
          }),
        }
      : undefined;

  // ─── 表格分页 ──────────────────────────────────────────

  const pagination = useMemo(
    () => ({
      current: page,
      pageSize,
      total: currentData.total,
      showSizeChanger: false,
      showTotal: (total: number) => `共 ${total} 条`,
      onChange: (p: number) => setPage(p),
    }),
    [page, currentData.total],
  );

  // ─── 渲染 ──────────────────────────────────────────────────

  return (
    <div>
      {/* 页面头部 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          marginBottom: 16,
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>字典映射工作台</h1>
          <Text type="secondary" style={{ fontSize: 13 }}>
            院内术语 → 标准字典映射 · AI 辅助标注 · 批量采纳
          </Text>
        </div>
      </div>

      {/* 主卡片 */}
      <Card
        tabList={TAB_LIST}
        activeTabKey={activeTab}
        onTabChange={handleTabChange}
        tabBarExtraContent={tabExtra}
        size="small"
      >
        {/* 当前Tab标题 */}
        <div style={{ marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            标准字典：ICD-11
          </Text>
        </div>

        {/* 空状态 */}
        {!currentData.loading && currentData.items.length === 0 && (
          <Empty
            description={
              activeTab === 'pending'
                ? '暂无未映射术语'
                : activeTab === 'mapped'
                ? '暂无已映射术语'
                : activeTab === 'conflict'
                ? '暂无冲突项'
                : '暂无 AI 候选'
            }
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        )}

        {/* 表格 */}
        {activeTab === 'pending' && (
          <Table<TerminologyItem>
            dataSource={currentData.items as TerminologyItem[]}
            columns={pendingColumns}
            rowKey="id"
            loading={currentData.loading}
            size="small"
            pagination={pagination}
            rowSelection={rowSelection}
          />
        )}

        {activeTab === 'mapped' && (
          <Table<TerminologyItem>
            dataSource={currentData.items as TerminologyItem[]}
            columns={mappedColumns}
            rowKey="id"
            loading={currentData.loading}
            size="small"
            pagination={pagination}
          />
        )}

        {activeTab === 'conflict' && (
          <Table<TerminologyItem>
            dataSource={currentData.items as TerminologyItem[]}
            columns={conflictColumns}
            rowKey="id"
            loading={currentData.loading}
            size="small"
            pagination={pagination}
          />
        )}

        {activeTab === 'ai' && (
          <Table<AiCandidate>
            dataSource={currentData.items as AiCandidate[]}
            columns={aiColumns}
            rowKey="id"
            loading={currentData.loading}
            size="small"
            pagination={pagination}
          />
        )}
      </Card>

      {/* 单个采纳确认 (low 危险) */}
      {confirmTarget && (
        <DangerConfirm
          level="low"
          title="采纳映射"
          description={`确认将 ${confirmTarget.internal_code} (${confirmTarget.internal_name}) 映射到 ${confirmTarget.standard_code}？`}
          consequences={[
            '该项将从未映射移入已映射',
            '映射记录将写入审计日志',
          ]}
          onConfirm={handleConfirmAccept}
          onCancel={() => setConfirmTarget(null)}
          loading={acceptMutation.isPending}
        />
      )}

      {/* 批量采纳确认 (low 危险) */}
      {batchConfirmOpen && (
        <DangerConfirm
          level="low"
          title="批量采纳映射"
          description={`确认批量采纳 ${selectedRowKeys.length} 项映射？`}
          consequences={[
            `将采纳 ${selectedRowKeys.length} 项映射`,
            '所有项将从未映射移入已映射',
            '映射记录将写入审计日志',
          ]}
          onConfirm={handleConfirmBatchAccept}
          onCancel={() => setBatchConfirmOpen(false)}
          loading={batchAcceptMutation.isPending}
        />
      )}

      {/* AI候选拒绝原因弹窗 */}
      <Modal
        title="拒绝 AI 候选"
        open={!!rejectTarget}
        onCancel={() => {
          setRejectTarget(null);
          setRejectReason('');
        }}
        onOk={handleConfirmReject}
        okText="确认拒绝"
        okButtonProps={{ danger: true }}
        confirmLoading={rejectMutation.isPending}
      >
        {rejectTarget && (
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
              拒绝候选：{rejectTarget.standard_code} ({rejectTarget.standard_name})
            </Text>
            <div>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                拒绝原因 <span style={{ color: 'var(--mk-danger)' }}>*</span>
              </Text>
              <Input.TextArea
                rows={3}
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                placeholder="请填写拒绝原因"
              />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
