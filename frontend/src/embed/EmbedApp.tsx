import { useCallback, useEffect, useState } from 'react';
import { ConfigProvider, theme } from 'antd';
import { EmbeddedAlert } from '../components/EmbeddedAlert';
import type { EmbeddedAlertAction } from '../components/EmbeddedAlert';
import { ClinicalEventClient, executeAlertAction } from '../api/clinicalEvent';
import type { EmbedAlert, EmbedConfig } from '../api/clinicalEvent';

interface EmbedAppProps {
  config: EmbedConfig;
  patientId?: string;
  encounterId?: string;
}

export default function EmbedApp({ config, patientId, encounterId }: EmbedAppProps) {
  const [alerts, setAlerts] = useState<EmbedAlert[]>([]);
  const [connected, setConnected] = useState(false);

  // WebSocket 连接
  useEffect(() => {
    const client = new ClinicalEventClient(config);

    const unsubAlert = client.onAlert((alert) => {
      // 过滤患者
      if (patientId && alert.actions.some((a) => a.payload?.patient_id !== patientId)) {
        return;
      }
      setAlerts((prev) => {
        const next = [alert, ...prev];
        // 限制最大数量
        return next.slice(0, 10);
      });
    });

    const unsubConnect = client.onConnect(() => setConnected(true));
    const unsubError = client.onError(() => setConnected(false));

    client.connect();

    return () => {
      unsubAlert();
      unsubConnect();
      unsubError();
      client.disconnect();
    };
  }, [config, patientId]);

  // 处理动作点击
  const handleAction = useCallback(
    (alert: EmbedAlert, action: EmbedAlert['actions'][0]) => {
      executeAlertAction(alert.alert_id, action.action_type, {
        ...action.payload,
        patient_id: patientId,
        encounter_id: encounterId,
      }).then(() => {
        // 成功后移除该告警
        setAlerts((prev) => prev.filter((a) => a.alert_id !== alert.alert_id));
      });
    },
    [patientId, encounterId],
  );

  // 关闭告警
  const handleClose = useCallback((alertId: string) => {
    setAlerts((prev) => prev.filter((a) => a.alert_id !== alertId));
  }, []);

  // 转换 actions 为组件格式
  const toEmbeddedActions = useCallback(
    (alert: EmbedAlert): EmbeddedAlertAction[] =>
      alert.actions.map((a) => ({
        text: a.text,
        intent: a.intent,
        onClick: () => handleAction(alert, a),
      })),
    [handleAction],
  );

  if (alerts.length === 0) {
    return null;
  }

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: 'var(--mk-primary)',
          borderRadius: 4,
          fontSize: 13,
        },
      }}
    >
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          zIndex: 9999,
          display: 'flex',
          flexDirection: 'column',
          gap: 4,
          padding: '4px 8px',
        }}
      >
        {alerts.map((alert) => (
          <EmbeddedAlert
            key={alert.alert_id}
            severity={alert.severity}
            title={alert.title}
            evidence={alert.evidence}
            source={alert.source}
            confidence={alert.confidence}
            actions={toEmbeddedActions(alert)}
            onClose={() => handleClose(alert.alert_id)}
            autoHide={
              alert.severity === 'success' ? config.auto_dismiss_success_ms : undefined
            }
          />
        ))}
      </div>
    </ConfigProvider>
  );
}
