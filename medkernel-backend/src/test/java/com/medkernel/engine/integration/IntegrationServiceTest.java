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

        // 自检：未接入真实外部连接器 → 配置合法只标 NOT_CONNECTED，不伪造 HEALTHY/网络 RTT，且不覆盖 configJson
        IntegrationAdapter pinged = service.pingAdapter(tenantId, "adp-1");
        assertEquals("NOT_CONNECTED", pinged.healthStatus());
        assertEquals(0L, pinged.rttMs());
        assertEquals("{}", pinged.configJson());
        assertFalse(pinged.configJson().contains("dataQuality"));
    }

    @Test
    void pingHonestlyReportsConfigStateNeverFakesHealthy() {
        // 配置合法 → NOT_CONNECTED（外部可达性未知，绝不伪造 HEALTHY）
        service.createAdapter(tenantId, new AdapterCreateDto("adp-ok", "LIS集成", "HL7", "{\"host\":\"lis.local\"}"));
        IntegrationAdapter okPing = service.pingAdapter(tenantId, "adp-ok");
        assertEquals("NOT_CONNECTED", okPing.healthStatus());
        assertNotEquals("HEALTHY", okPing.healthStatus());
        assertEquals(0L, okPing.rttMs());
        assertTrue(okPing.configJson().contains("lis.local"), "配置原值应被保留，不被体检报告覆盖");

        // 配置非法 → MISCONFIGURED
        service.createAdapter(tenantId, new AdapterCreateDto("adp-bad", "坏配置", "REST", "{not-json"));
        IntegrationAdapter badPing = service.pingAdapter(tenantId, "adp-bad");
        assertEquals("MISCONFIGURED", badPing.healthStatus());
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
        WebhookTestResultDto signResult = service.testWebhookSignature(tenantId, testDto);
        assertEquals("SUCCESS", signResult.status());
        assertNotNull(signResult.signature());
        assertNotNull(signResult.timestamp());
    }

    @Test
    void testMessageLogsAndRetry() {
        // 先手动插入一条失败的消息日志，物理报文 payload 非空
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

        // 执行重试：未接入真实外部连接器，即便 payload 合法也绝不伪造 SUCCESS（retryCount 1 < maxRetries 3 → FAILED）
        IntegrationMessageLog retried = service.retryMessage(tenantId, "msg-1");
        assertEquals(1, retried.retryCount());
        assertEquals("FAILED", retried.status());
        assertTrue(retried.errorMessage().contains("未接入真实外部连接器"));

        // 插入一条空 payload 的失败消息日志，测试其重试失败
        IntegrationMessageLog logEmpty = new IntegrationMessageLog(
            null,
            "msg-2",
            tenantId,
            "trace-2",
            "OUTBOUND",
            "EMR",
            "REST",
            "summary",
            "",
            "FAILED",
            0,
            3,
            "error",
            Instant.now(),
            "system",
            Instant.now(),
            "system"
        );
        logRepository.save(logEmpty);

        IntegrationMessageLog retriedEmpty = service.retryMessage(tenantId, "msg-2");
        assertEquals(1, retriedEmpty.retryCount());
        assertEquals("FAILED", retriedEmpty.status());
        assertTrue(retriedEmpty.errorMessage().contains("物理载荷报文为空"));

        // 重试次数累加到 maxRetries 时强制移入死信队列
        service.retryMessage(tenantId, "msg-2"); // retryCount = 2, FAILED
        IntegrationMessageLog retriedDead = service.retryMessage(tenantId, "msg-2"); // retryCount = 3, DEAD_LETTER
        assertEquals(3, retriedDead.retryCount());
        assertEquals("DEAD_LETTER", retriedDead.status());
        assertTrue(retriedDead.errorMessage().contains("投递重试超限"));

        // 删除日志
        service.deleteMessage(tenantId, "msg-1");
        Optional<IntegrationMessageLog> deleted = logRepository.findByMessageId("msg-1");
        assertFalse(deleted.isPresent());

        service.deleteMessage(tenantId, "msg-2");
        Optional<IntegrationMessageLog> deletedEmpty = logRepository.findByMessageId("msg-2");
        assertFalse(deletedEmpty.isPresent());
    }
}
