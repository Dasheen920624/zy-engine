package com.medkernel.engine.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.medkernel.engine.integration.domain.*;
import com.medkernel.engine.integration.dto.*;
import com.medkernel.engine.integration.repository.*;
import com.medkernel.engine.integration.service.IntegrationService;
import com.medkernel.shared.api.error.ApiException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntegrationServiceTest {

    @Autowired
    private IntegrationService service;

    @Autowired
    private IntegrationAdapterRepository adapterRepository;

    @Autowired
    private IntegrationWebhookConfigRepository webhookRepository;

    @Autowired
    private IntegrationMessageLogRepository logRepository;

    private final String tenantId = "tenant-001";



    @Test
    void testAdapterLifecycle() {
        AdapterCreateDto createDto = new AdapterCreateDto("adp-1", "HIS集成", "HL7", "{}");
        IntegrationAdapter created = service.createAdapter(tenantId, createDto);

        assertNotNull(created.id());
        assertEquals("adp-1", created.adapterId());
        assertEquals("HIS集成", created.name());
        assertEquals("HL7", created.protocolType());
        assertEquals("ACTIVE", created.status());

        // 获取列表
        List<IntegrationAdapter> list = service.getAdapters(tenantId);
        assertEquals(1, list.size());

        // 更新适配器
        AdapterUpdateDto updateDto = new AdapterUpdateDto("HIS集成系统大版本", "REST", "{}", "SUSPENDED");
        IntegrationAdapter updated = service.updateAdapter(tenantId, "adp-1", updateDto);
        assertEquals("HIS集成系统大版本", updated.name());
        assertEquals("REST", updated.protocolType());
        assertEquals("SUSPENDED", updated.status());

        // 自检测 ping
        IntegrationAdapter pinged = service.pingAdapter(tenantId, "adp-1");
        assertEquals("HEALTHY", pinged.healthStatus());
        assertTrue(pinged.rttMs() >= 2L);
        assertTrue(pinged.configJson().contains("dataQuality"));
    }

    @Test
    void testAdapterCreateConflict() {
        AdapterCreateDto createDto = new AdapterCreateDto("adp-1", "HIS集成", "HL7", "{}");
        service.createAdapter(tenantId, createDto);

        assertThrows(ApiException.class, () -> {
            service.createAdapter(tenantId, createDto);
        });
    }

    @Test
    void testWebhookLifecycleAndSignature() {
        WebhookCreateDto createDto = new WebhookCreateDto("whk-1", "出院回执", "http://localhost/callback", "OUTPATIENT_DIAGNOSIS");
        IntegrationWebhookConfig created = service.createWebhook(tenantId, createDto);

        assertNotNull(created.id());
        assertEquals("whk-1", created.webhookId());
        assertTrue(created.secretKey().startsWith("sec_key_"));

        List<IntegrationWebhookConfig> webhooks = service.getWebhooks(tenantId);
        assertEquals(1, webhooks.size());

        // 签名生成与验证测试
        WebhookTestDto testDto = new WebhookTestDto("whk-1", "{\"patientId\":\"P-101\"}");
        Map<String, Object> signResult = service.testWebhookSignature(tenantId, testDto);
        assertEquals("SUCCESS", signResult.get("status"));
        assertNotNull(signResult.get("signature"));
        assertNotNull(signResult.get("timestamp"));
    }

    @Test
    void testMessageLogsAndRetry() {
        // 先手动插入一条失败的消息日志
        IntegrationMessageLog log = new IntegrationMessageLog(
            null,
            "msg-1",
            tenantId,
            "trace-1",
            "OUTBOUND",
            "EMR",
            "REST",
            "summary",
            "payload",
            "FAILED",
            0,
            3,
            "error",
            Instant.now(),
            "system",
            Instant.now(),
            "system"
        );
        logRepository.save(log);

        List<IntegrationMessageLog> logsPage = service.getMessageLogs(tenantId, 0, 10);
        assertEquals(1, logsPage.size());
        assertEquals("msg-1", logsPage.get(0).messageId());

        // 执行重试
        IntegrationMessageLog retried = service.retryMessage(tenantId, "msg-1");
        assertEquals(1, retried.retryCount());
        assertTrue("SUCCESS".equals(retried.status()) || "FAILED".equals(retried.status()) || "DEAD_LETTER".equals(retried.status()));

        // 删除日志
        service.deleteMessage(tenantId, "msg-1");
        Optional<IntegrationMessageLog> deleted = logRepository.findByMessageId("msg-1");
        assertFalse(deleted.isPresent());
    }
}
