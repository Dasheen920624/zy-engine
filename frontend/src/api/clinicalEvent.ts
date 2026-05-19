import type { AlertSeverity } from '../components/EmbeddedAlert';

// ─── 临床事件相关类型 ───────────────────────────────────────────────

export type ClinicalEventType =
  | 'ECG_ABNORMAL'
  | 'LAB_CRITICAL'
  | 'DRUG_INTERACTION'
  | 'PATHWAY_RECOMMEND'
  | 'QC_ALERT'
  | 'INSURANCE_ALERT';

export interface ClinicalEvent {
  event_id: string;
  event_type: ClinicalEventType;
  patient_id: string;
  encounter_id: string;
  timestamp: string;
  data: Record<string, unknown>;
}

export interface EmbedAlert {
  alert_id: string;
  event_id: string;
  severity: AlertSeverity;
  title: string;
  evidence: string;
  source?: {
    documentName: string;
    section?: string;
    publishYear?: number;
  };
  confidence?: number;
  actions: Array<{
    text: string;
    intent: 'primary' | 'secondary' | 'tertiary';
    action_type: string;
    payload?: Record<string, unknown>;
  }>;
  created_at: string;
  expires_at?: string;
}

export interface EmbedConfig {
  ws_url: string;
  reconnect_interval_ms: number;
  max_alerts: number;
  auto_dismiss_success_ms: number;
}

// ─── WebSocket 客户端 ──────────────────────────────────────────────

type AlertHandler = (alert: EmbedAlert) => void;
type ErrorHandler = (error: Event) => void;
type ConnectHandler = () => void;

export class ClinicalEventClient {
  private ws: WebSocket | null = null;
  private url: string;
  private reconnectInterval: number;
  private alertHandlers: AlertHandler[] = [];
  private errorHandlers: ErrorHandler[] = [];
  private connectHandlers: ConnectHandler[] = [];
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private isConnecting = false;

  constructor(config: EmbedConfig) {
    this.url = config.ws_url;
    this.reconnectInterval = config.reconnect_interval_ms;
  }

  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN || this.isConnecting) {
      return;
    }

    this.isConnecting = true;

    try {
      this.ws = new WebSocket(this.url);

      this.ws.onopen = () => {
        this.isConnecting = false;
        this.connectHandlers.forEach((h) => h());
      };

      this.ws.onmessage = (event) => {
        try {
          const alert = JSON.parse(event.data) as EmbedAlert;
          this.alertHandlers.forEach((h) => h(alert));
        } catch (e) {
          console.error('[ClinicalEventClient] Failed to parse message:', e);
        }
      };

      this.ws.onerror = (event) => {
        this.isConnecting = false;
        this.errorHandlers.forEach((h) => h(event));
      };

      this.ws.onclose = () => {
        this.isConnecting = false;
        this.scheduleReconnect();
      };
    } catch (e) {
      this.isConnecting = false;
      console.error('[ClinicalEventClient] Connection failed:', e);
      this.scheduleReconnect();
    }
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }

  onAlert(handler: AlertHandler): () => void {
    this.alertHandlers.push(handler);
    return () => {
      this.alertHandlers = this.alertHandlers.filter((h) => h !== handler);
    };
  }

  onError(handler: ErrorHandler): () => void {
    this.errorHandlers.push(handler);
    return () => {
      this.errorHandlers = this.errorHandlers.filter((h) => h !== handler);
    };
  }

  onConnect(handler: ConnectHandler): () => void {
    this.connectHandlers.push(handler);
    return () => {
      this.connectHandlers = this.connectHandlers.filter((h) => h !== handler);
    };
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, this.reconnectInterval);
  }
}

// ─── REST API ──────────────────────────────────────────────────────

import { get, post } from './client';

/**
 * 获取嵌入器配置
 */
export async function fetchEmbedConfig(): Promise<EmbedConfig> {
  return get<EmbedConfig>('/embed/config');
}

/**
 * 获取历史告警
 */
export async function fetchEmbedAlerts(params?: {
  patient_id?: string;
  encounter_id?: string;
  limit?: number;
}): Promise<EmbedAlert[]> {
  const queryParams = new URLSearchParams();
  if (params?.patient_id) queryParams.set('patient_id', params.patient_id);
  if (params?.encounter_id) queryParams.set('encounter_id', params.encounter_id);
  if (params?.limit) queryParams.set('limit', params.limit.toString());
  const url = `/embed/alerts${queryParams.toString() ? `?${queryParams.toString()}` : ''}`;
  return get<EmbedAlert[]>(url);
}

/**
 * 执行告警动作（如一键入径）
 */
export async function executeAlertAction(
  alertId: string,
  actionType: string,
  payload?: Record<string, unknown>,
): Promise<void> {
  await post(`/embed/alerts/${alertId}/action`, { action_type: actionType, ...payload });
}
