import { useCallback, useEffect, useState } from 'react';
import { ConfigProvider, theme } from 'antd';
import { EmbeddedAlert } from '../components/EmbeddedAlert';
import type { EmbeddedAlertAction } from '../components/EmbeddedAlert';
import { ClinicalEventClient, executeAlertAction } from '../api/clinicalEvent';
import type { EmbedAlert, EmbedConfig } from '../api/clinicalEvent';
import { OrderSafetyBlocker } from './OrderSafetyBlocker';
import type { OrderSafetyDecision } from './OrderSafetyBlocker';
import styles from './embedApp.module.css';

interface EmbedAppProps {
  config: EmbedConfig;
  patientId?: string;
  encounterId?: string;
}

export default function EmbedApp({ config, patientId, encounterId }: EmbedAppProps) {
  const [alerts, setAlerts] = useState<EmbedAlert[]>([]);
  const [_connected, setConnected] = useState(false);
  const [blockerAlert, setBlockerAlert] = useState<EmbedAlert | null>(null);

  // WebSocket 连接
  useEffect(() => {
    const client = new ClinicalEventClient(config);

    const unsubAlert = client.onAlert((alert) => {
      // 过滤患者
      if (patientId && alert.actions.some((a) => a.payload?.patient_id !== patientId)) {
        return;
      }
      
      // 检查是否为BLOCK模式的拦截告警
      if (alert.action_mode === 'BLOCK') {
        setBlockerAlert(alert);
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

  // 处理拦截弹窗决策
  const handleBlockerDecision = useCallback(
    async (decision: OrderSafetyDecision, data?: { reason?: string; informedConsent?: boolean; familyNotified?: boolean }) => {
      if (!blockerAlert) return;
      
      // 执行对应的动作
      const action = blockerAlert.actions.find((a) => {
        if (decision === 'CANCEL') return a.action_type === 'CANCEL_ORDER';
        if (decision === 'MODIFY') return a.action_type === 'MODIFY_DOSE';
        if (decision === 'CONTINUE') return a.action_type === 'CONTINUE_ORDER';
        return false;
      });
      
      if (action) {
        await executeAlertAction(blockerAlert.alert_id, action.action_type, {
          ...action.payload,
          patient_id: patientId,
          encounter_id: encounterId,
          decision,
          reason: data?.reason,
          informed_consent: data?.informedConsent,
          family_notified: data?.familyNotified,
        });
      }
      
      setBlockerAlert(null);
    },
    [blockerAlert, patientId, encounterId]
  );

  // 关闭拦截弹窗
  const handleBlockerClose = useCallback(() => {
    setBlockerAlert(null);
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

  if (alerts.length === 0 && !blockerAlert) {
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
      <div className={styles.alertStack}>
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

      {/* 医嘱安全拦截弹窗 */}
      {blockerAlert && (
        <OrderSafetyBlocker
          open={true}
          ruleCode={blockerAlert.rule_code || ''}
          ruleVersion={blockerAlert.rule_version || ''}
          patientId={patientId || ''}
          encounterId={encounterId || ''}
          orderId={blockerAlert.order_id || ''}
          actionMode="BLOCK"
          severity={blockerAlert.severity === 'danger' ? 'CRITICAL' : 'HIGH'}
          title={blockerAlert.title}
          patientInfo={{
            name: blockerAlert.patient_name || '患者',
            age: blockerAlert.patient_age || 0,
            gender: blockerAlert.patient_gender || '',
            patientId: patientId || '',
          }}
          orderInfo={{
            name: blockerAlert.order_name || '医嘱',
            dose: blockerAlert.order_dose || '',
            frequency: blockerAlert.order_frequency || '',
          }}
          riskDescription={blockerAlert.risk_description || blockerAlert.title}
          evidence={blockerAlert.evidence}
          source={blockerAlert.source}
          onDecision={handleBlockerDecision}
          onClose={handleBlockerClose}
        />
      )}
    </ConfigProvider>
  );
}
