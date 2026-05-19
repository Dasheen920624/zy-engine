import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import EmbedApp from './EmbedApp';
import type { EmbedConfig } from '../api/clinicalEvent';

// 从 URL 参数或 window 全局变量获取配置
function getConfig(): EmbedConfig {
  const params = new URLSearchParams(window.location.search);
  const wsUrl = params.get('ws_url') || (window as Record<string, unknown>).__EMBED_WS_URL__ as string || 'ws://localhost:8080/ws/embed/alerts';

  return {
    ws_url: wsUrl,
    reconnect_interval_ms: Number(params.get('reconnect_ms')) || 5000,
    max_alerts: Number(params.get('max_alerts')) || 10,
    auto_dismiss_success_ms: Number(params.get('auto_dismiss_ms')) || 5000,
  };
}

function getPatientContext() {
  const params = new URLSearchParams(window.location.search);
  return {
    patientId: params.get('patient_id') || (window as Record<string, unknown>).__EMBED_PATIENT_ID__ as string,
    encounterId: params.get('encounter_id') || (window as Record<string, unknown>).__EMBED_ENCOUNTER_ID__ as string,
  };
}

const config = getConfig();
const { patientId, encounterId } = getPatientContext();

const root = createRoot(document.getElementById('embed-root')!);
root.render(
  <StrictMode>
    <EmbedApp config={config} patientId={patientId} encounterId={encounterId} />
  </StrictMode>,
);
