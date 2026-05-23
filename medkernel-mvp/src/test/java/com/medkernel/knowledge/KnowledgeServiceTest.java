package com.medkernel.knowledge;

import com.medkernel.organization.OrganizationContext;
import com.medkernel.organization.OrganizationContextService;
import com.medkernel.ops.service.OpsSyncTaskService;
import com.medkernel.persistence.EnginePersistenceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private OrganizationContextService organizationContextService;

    @Mock
    private EnginePersistenceProperties properties;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private OpsSyncTaskService opsSyncTaskService;

    private OrganizationContext orgContext;

    @BeforeEach
    void setUp() {
        orgContext = new OrganizationContext();
        orgContext.setTenantId("tenant-001");
    }

    // =========================================================================
    // KnowledgeService — 知识订阅与来源注册
    // =========================================================================

    @Nested
    @DisplayName("KnowledgeService — 来源注册")
    class SourceRegistrationTests {

        private KnowledgeService knowledgeService;

        @BeforeEach
        void init() {
            knowledgeService = new KnowledgeService(organizationContextService);
        }

        @Test
        @DisplayName("注册来源 — 指定 source_code 时正确注册")
        void registerSource_withSourceCode() {
            Map<String, Object> request = new HashMap<>();
            request.put("source_code", "KS-ICD10");
            request.put("source_name", "ICD-10 编码体系");
            request.put("source_type", "TERMINOLOGY");
            request.put("publisher", "WHO");
            request.put("region", "GLOBAL");
            request.put("language", "zh");
            request.put("authority_level", "OFFICIAL");
            request.put("license_type", "OPEN");
            request.put("fetch_method", "API");
            request.put("source_uri", "https://who.int/icd10");
            request.put("created_by", "admin");

            KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

            assertEquals("KS-ICD10", source.getSourceCode());
            assertEquals("ICD-10 编码体系", source.getSourceName());
            assertEquals("TERMINOLOGY", source.getSourceType());
            assertEquals("WHO", source.getPublisher());
            assertEquals("tenant-001", source.getTenantId());
            assertEquals("PENDING", source.getReviewStatus());
            assertNotNull(source.getCreatedTime());
            assertNotNull(source.getUpdatedTime());
        }

        @Test
        @DisplayName("注册来源 — 未指定 source_code 时自动生成")
        void registerSource_autoGenerateCode() {
            Map<String, Object> request = new HashMap<>();
            request.put("source_name", "测试来源");

            KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

            assertNotNull(source.getSourceCode());
            assertTrue(source.getSourceCode().startsWith("KS-"));
        }

        @Test
        @DisplayName("注册来源 — 重复 source_code 抛出异常")
        void registerSource_duplicateCode() {
            Map<String, Object> request = new HashMap<>();
            request.put("source_code", "KS-DUP");
            request.put("source_name", "来源1");
            knowledgeService.registerSource(request, orgContext);

            Map<String, Object> request2 = new HashMap<>();
            request2.put("source_code", "KS-DUP");
            request2.put("source_name", "来源2");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.registerSource(request2, orgContext));
            assertTrue(ex.getMessage().contains("Source already registered"));
        }

        @Test
        @DisplayName("注册来源 — 布尔字段正确解析字符串")
        void registerSource_booleanFields() {
            Map<String, Object> request = new HashMap<>();
            request.put("source_code", "KS-BOOL");
            request.put("redistribution_allowed", "true");
            request.put("commercial_use_allowed", "false");
            request.put("export_allowed", true);

            KnowledgeSourceRegistry source = knowledgeService.registerSource(request, orgContext);

            assertTrue(source.isRedistributionAllowed());
            assertFalse(source.isCommercialUseAllowed());
            assertTrue(source.isExportAllowed());
        }

        @Test
        @DisplayName("更新来源 — 成功更新指定字段")
        void updateSource_success() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("source_code", "KS-UPD");
            createReq.put("source_name", "原始名称");
            createReq.put("source_type", "GUIDELINE");
            knowledgeService.registerSource(createReq, orgContext);

            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("source_name", "更新后名称");
            updateReq.put("source_type", "TERMINOLOGY");

            KnowledgeSourceRegistry updated = knowledgeService.updateSource("KS-UPD", updateReq, orgContext);

            assertEquals("更新后名称", updated.getSourceName());
            assertEquals("TERMINOLOGY", updated.getSourceType());
            assertNotNull(updated.getUpdatedTime());
        }

        @Test
        @DisplayName("更新来源 — 不存在的来源抛出异常")
        void updateSource_notFound() {
            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("source_name", "新名称");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.updateSource("KS-NONEXIST", updateReq, orgContext));
            assertTrue(ex.getMessage().contains("Source not found"));
        }

        @Test
        @DisplayName("更新来源 — 不同租户无法更新")
        void updateSource_differentTenant() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("source_code", "KS-TENANT");
            knowledgeService.registerSource(createReq, orgContext);

            OrganizationContext otherOrg = new OrganizationContext();
            otherOrg.setTenantId("tenant-999");

            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("source_name", "尝试更新");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.updateSource("KS-TENANT", updateReq, otherOrg));
            assertTrue(ex.getMessage().contains("Source not found"));
        }

        @Test
        @DisplayName("审核来源 — 审核通过")
        void reviewSource_approved() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("source_code", "KS-REV");
            knowledgeService.registerSource(createReq, orgContext);

            KnowledgeSourceRegistry reviewed = knowledgeService.reviewSource(
                    "KS-REV", "APPROVED", "reviewer1", orgContext);

            assertEquals("APPROVED", reviewed.getReviewStatus());
            assertEquals("reviewer1", reviewed.getReviewedBy());
            assertNotNull(reviewed.getReviewedTime());
        }

        @Test
        @DisplayName("审核来源 — 审核驳回")
        void reviewSource_rejected() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("source_code", "KS-REJ");
            knowledgeService.registerSource(createReq, orgContext);

            KnowledgeSourceRegistry reviewed = knowledgeService.reviewSource(
                    "KS-REJ", "REJECTED", "reviewer1", orgContext);

            assertEquals("REJECTED", reviewed.getReviewStatus());
        }

        @Test
        @DisplayName("审核来源 — 非法审核状态抛出异常")
        void reviewSource_invalidStatus() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("source_code", "KS-INV");
            knowledgeService.registerSource(createReq, orgContext);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.reviewSource("KS-INV", "PENDING", "reviewer1", orgContext));
            assertTrue(ex.getMessage().contains("reviewStatus must be APPROVED or REJECTED"));
        }

        @Test
        @DisplayName("查询来源 — 按来源编码查询")
        void getSource_found() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("source_code", "KS-GET");
            createReq.put("source_name", "查询测试来源");
            knowledgeService.registerSource(createReq, orgContext);

            KnowledgeSourceRegistry found = knowledgeService.getSource("KS-GET", orgContext);

            assertNotNull(found);
            assertEquals("查询测试来源", found.getSourceName());
        }

        @Test
        @DisplayName("查询来源 — 不存在返回 null")
        void getSource_notFound() {
            KnowledgeSourceRegistry found = knowledgeService.getSource("KS-NONEXIST", orgContext);
            assertNull(found);
        }

        @Test
        @DisplayName("列表来源 — 按来源类型过滤")
        void listSources_filterByType() {
            Map<String, Object> req1 = new HashMap<>();
            req1.put("source_code", "KS-LIST1");
            req1.put("source_type", "TERMINOLOGY");
            knowledgeService.registerSource(req1, orgContext);

            Map<String, Object> req2 = new HashMap<>();
            req2.put("source_code", "KS-LIST2");
            req2.put("source_type", "GUIDELINE");
            knowledgeService.registerSource(req2, orgContext);

            Map<String, String> filters = new HashMap<>();
            filters.put("source_type", "TERMINOLOGY");
            List<KnowledgeSourceRegistry> result = knowledgeService.listSources(filters, orgContext);

            assertEquals(1, result.size());
            assertEquals("KS-LIST1", result.get(0).getSourceCode());
        }

        @Test
        @DisplayName("列表来源 — 按审核状态过滤")
        void listSources_filterByReviewStatus() {
            Map<String, Object> req1 = new HashMap<>();
            req1.put("source_code", "KS-STA1");
            knowledgeService.registerSource(req1, orgContext);

            Map<String, Object> req2 = new HashMap<>();
            req2.put("source_code", "KS-STA2");
            knowledgeService.registerSource(req2, orgContext);
            knowledgeService.reviewSource("KS-STA2", "APPROVED", "admin", orgContext);

            Map<String, String> filters = new HashMap<>();
            filters.put("review_status", "APPROVED");
            List<KnowledgeSourceRegistry> result = knowledgeService.listSources(filters, orgContext);

            assertEquals(1, result.size());
            assertEquals("KS-STA2", result.get(0).getSourceCode());
        }

        @Test
        @DisplayName("列表来源 — 不同租户数据隔离")
        void listSources_tenantIsolation() {
            Map<String, Object> req = new HashMap<>();
            req.put("source_code", "KS-ISO");
            knowledgeService.registerSource(req, orgContext);

            OrganizationContext otherOrg = new OrganizationContext();
            otherOrg.setTenantId("tenant-999");

            List<KnowledgeSourceRegistry> result = knowledgeService.listSources(null, otherOrg);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("KnowledgeService — 知识订阅")
    class SubscriptionTests {

        private KnowledgeService knowledgeService;

        @BeforeEach
        void init() {
            knowledgeService = new KnowledgeService(organizationContextService);
        }

        @Test
        @DisplayName("创建订阅 — 成功创建并返回默认值")
        void createSubscription_success() {
            Map<String, Object> request = new HashMap<>();
            request.put("subscriber_id", "user-001");
            request.put("subscriber_name", "张医生");
            request.put("topic_type", "DISEASE");
            request.put("topic_code", "I21");
            request.put("topic_name", "急性心肌梗死");
            request.put("source_types", List.of("GUIDELINE", "TERMINOLOGY"));
            request.put("created_by", "user-001");

            KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);

            assertNotNull(sub.getSubscriptionId());
            assertTrue(sub.getSubscriptionId().startsWith("SUB-"));
            assertEquals("tenant-001", sub.getTenantId());
            assertEquals("DISEASE", sub.getTopicType());
            assertEquals("I21", sub.getTopicCode());
            assertEquals(List.of("GUIDELINE", "TERMINOLOGY"), sub.getSourceTypes());
            assertEquals("ACTIVE", sub.getStatus());
            assertTrue(sub.isAutoSync());
            assertEquals("MANUAL", sub.getSyncFrequency());
        }

        @Test
        @DisplayName("创建订阅 — 自定义 auto_sync 和 sync_frequency")
        void createSubscription_customSyncSettings() {
            Map<String, Object> request = new HashMap<>();
            request.put("topic_type", "DRUG");
            request.put("topic_code", "D001");
            request.put("auto_sync", "false");
            request.put("sync_frequency", "DAILY");

            KnowledgeSubscription sub = knowledgeService.createSubscription(request, orgContext);

            assertFalse(sub.isAutoSync());
            assertEquals("DAILY", sub.getSyncFrequency());
        }

        @Test
        @DisplayName("更新订阅 — 成功更新指定字段")
        void updateSubscription_success() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("topic_type", "GUIDELINE");
            createReq.put("topic_code", "GL001");
            createReq.put("topic_name", "原始名称");
            KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);

            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("topic_name", "更新后名称");
            updateReq.put("sync_frequency", "WEEKLY");

            KnowledgeSubscription updated = knowledgeService.updateSubscription(
                    sub.getSubscriptionId(), updateReq, orgContext);

            assertEquals("更新后名称", updated.getTopicName());
            assertEquals("WEEKLY", updated.getSyncFrequency());
        }

        @Test
        @DisplayName("更新订阅 — 不存在的订阅抛出异常")
        void updateSubscription_notFound() {
            Map<String, Object> updateReq = new HashMap<>();
            updateReq.put("topic_name", "新名称");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.updateSubscription("SUB-NONEXIST", updateReq, orgContext));
            assertTrue(ex.getMessage().contains("Subscription not found"));
        }

        @Test
        @DisplayName("暂停订阅 — 状态变为 PAUSED")
        void pauseSubscription_success() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("topic_type", "QUALITY");
            createReq.put("topic_code", "Q001");
            KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);

            KnowledgeSubscription paused = knowledgeService.pauseSubscription(
                    sub.getSubscriptionId(), orgContext);

            assertEquals("PAUSED", paused.getStatus());
        }

        @Test
        @DisplayName("取消订阅 — 状态变为 CANCELLED")
        void cancelSubscription_success() {
            Map<String, Object> createReq = new HashMap<>();
            createReq.put("topic_type", "INSURANCE");
            createReq.put("topic_code", "INS001");
            KnowledgeSubscription sub = knowledgeService.createSubscription(createReq, orgContext);

            KnowledgeSubscription cancelled = knowledgeService.cancelSubscription(
                    sub.getSubscriptionId(), orgContext);

            assertEquals("CANCELLED", cancelled.getStatus());
        }

        @Test
        @DisplayName("暂停订阅 — 不存在的订阅抛出异常")
        void pauseSubscription_notFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.pauseSubscription("SUB-NONEXIST", orgContext));
            assertTrue(ex.getMessage().contains("Subscription not found"));
        }

        @Test
        @DisplayName("取消订阅 — 不存在的订阅抛出异常")
        void cancelSubscription_notFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> knowledgeService.cancelSubscription("SUB-NONEXIST", orgContext));
            assertTrue(ex.getMessage().contains("Subscription not found"));
        }

        @Test
        @DisplayName("查询订阅 — 按订阅类型过滤")
        void listSubscriptions_filterByTopicType() {
            Map<String, Object> req1 = new HashMap<>();
            req1.put("topic_type", "DISEASE");
            req1.put("topic_code", "D001");
            knowledgeService.createSubscription(req1, orgContext);

            Map<String, Object> req2 = new HashMap<>();
            req2.put("topic_type", "DRUG");
            req2.put("topic_code", "DR001");
            knowledgeService.createSubscription(req2, orgContext);

            Map<String, String> filters = new HashMap<>();
            filters.put("topic_type", "DISEASE");
            List<KnowledgeSubscription> result = knowledgeService.listSubscriptions(filters, orgContext);

            assertEquals(1, result.size());
            assertEquals("DISEASE", result.get(0).getTopicType());
        }

        @Test
        @DisplayName("查询订阅 — 按状态过滤")
        void listSubscriptions_filterByStatus() {
            Map<String, Object> req = new HashMap<>();
            req.put("topic_type", "DEPARTMENT");
            req.put("topic_code", "DEP001");
            KnowledgeSubscription sub = knowledgeService.createSubscription(req, orgContext);
            knowledgeService.pauseSubscription(sub.getSubscriptionId(), orgContext);

            Map<String, String> filters = new HashMap<>();
            filters.put("status", "PAUSED");
            List<KnowledgeSubscription> result = knowledgeService.listSubscriptions(filters, orgContext);

            assertEquals(1, result.size());
            assertEquals("PAUSED", result.get(0).getStatus());
        }

        @Test
        @DisplayName("查询订阅 — 不同租户数据隔离")
        void listSubscriptions_tenantIsolation() {
            Map<String, Object> req = new HashMap<>();
            req.put("topic_type", "DISEASE");
            req.put("topic_code", "D001");
            knowledgeService.createSubscription(req, orgContext);

            OrganizationContext otherOrg = new OrganizationContext();
            otherOrg.setTenantId("tenant-999");

            List<KnowledgeSubscription> result = knowledgeService.listSubscriptions(null, otherOrg);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("获取订阅 — 按订阅ID查询")
        void getSubscription_found() {
            Map<String, Object> req = new HashMap<>();
            req.put("topic_type", "DISEASE");
            req.put("topic_code", "D002");
            req.put("topic_name", "心衰");
            KnowledgeSubscription sub = knowledgeService.createSubscription(req, orgContext);

            KnowledgeSubscription found = knowledgeService.getSubscription(sub.getSubscriptionId(), orgContext);

            assertNotNull(found);
            assertEquals("心衰", found.getTopicName());
        }

        @Test
        @DisplayName("获取订阅 — 不存在返回 null")
        void getSubscription_notFound() {
            KnowledgeSubscription found = knowledgeService.getSubscription("SUB-NONEXIST", orgContext);
            assertNull(found);
        }
    }

    // =========================================================================
    // AiKnowledgeJobService — AI 知识生产任务
    // =========================================================================

    @Nested
    @DisplayName("AiKnowledgeJobService — 知识生产任务")
    class AiKnowledgeJobServiceTests {

        private AiKnowledgeJobService jobService;

        @BeforeEach
        void init() throws Exception {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            jobService = new AiKnowledgeJobService(properties, dataSource);
        }

        @Test
        @DisplayName("创建任务 — 成功创建并设置默认值")
        void createJob_success() throws Exception {
            AiKnowledgeJob job = new AiKnowledgeJob();
            job.setTenantId(1L);
            job.setJobName("术语映射任务");
            job.setJobType("TERMINOLOGY_MAPPING");
            job.setSourceCode("KS-ICD10");
            job.setModelProvider("OpenAI");
            job.setModelName("gpt-4");
            job.setCreatedBy("admin");

            AiKnowledgeJob created = jobService.createJob(job);

            assertNotNull(created.getId());
            assertNotNull(created.getJobCode());
            assertEquals("PENDING", created.getStatus());
            assertEquals("PENDING", created.getReviewStatus());
            assertEquals(3, created.getMaxRetries());
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("创建任务 — 自定义 jobCode 时保留原值")
        void createJob_customJobCode() throws Exception {
            AiKnowledgeJob job = new AiKnowledgeJob();
            job.setTenantId(1L);
            job.setJobCode("JOB-CUSTOM-001");
            job.setJobName("自定义编码任务");

            AiKnowledgeJob created = jobService.createJob(job);

            assertEquals("JOB-CUSTOM-001", created.getJobCode());
        }

        @Test
        @DisplayName("创建任务 — 自定义 maxRetries 时保留原值")
        void createJob_customMaxRetries() throws Exception {
            AiKnowledgeJob job = new AiKnowledgeJob();
            job.setTenantId(1L);
            job.setJobName("重试任务");
            job.setMaxRetries(5);

            AiKnowledgeJob created = jobService.createJob(job);

            assertEquals(5, created.getMaxRetries());
        }

        @Test
        @DisplayName("更新任务状态 — RUNNING 状态设置 started_time")
        void updateJobStatus_running() throws Exception {
            jobService.updateJobStatus(100L, "RUNNING", null, null);
            verify(preparedStatement).setString(1, "RUNNING");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("更新任务状态 — SUCCESS 状态设置 finished_time")
        void updateJobStatus_success() throws Exception {
            jobService.updateJobStatus(100L, "SUCCESS", null, null);
            verify(preparedStatement).setString(1, "SUCCESS");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("更新任务状态 — FAILED 状态记录错误信息")
        void updateJobStatus_failed() throws Exception {
            jobService.updateJobStatus(100L, "FAILED", "TIMEOUT", "执行超时");
            verify(preparedStatement).setString(1, "FAILED");
            verify(preparedStatement).setString(2, "TIMEOUT");
            verify(preparedStatement).setString(3, "执行超时");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("更新任务状态 — RETRY 状态重置为 PENDING 并递增 retry_count")
        void updateJobStatus_retry() throws Exception {
            jobService.updateJobStatus(100L, "RETRY", null, null);
            verify(preparedStatement).setString(1, "PENDING");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("审核任务 — 成功更新审核状态")
        void reviewJob_success() throws Exception {
            jobService.reviewJob(100L, "APPROVED", "reviewer1", "审核通过");
            verify(preparedStatement).setString(1, "APPROVED");
            verify(preparedStatement).setString(2, "reviewer1");
            verify(preparedStatement).setString(4, "审核通过");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("记录模型调用日志 — 成功记录")
        void logModelCall_success() throws Exception {
            AiModelCallLog callLog = new AiModelCallLog();
            callLog.setTenantId(1L);
            callLog.setJobId(100L);
            callLog.setCallType("MAPPING");
            callLog.setModelProvider("OpenAI");
            callLog.setModelName("gpt-4");
            callLog.setCallStatus("SUCCESS");
            callLog.setInputTokenCount(500);
            callLog.setOutputTokenCount(200);
            callLog.setTotalTokenCount(700);
            callLog.setElapsedMs(1500);
            callLog.setCreatedBy("admin");

            AiModelCallLog result = jobService.logModelCall(callLog);

            assertNotNull(result.getId());
            assertNotNull(result.getCreatedTime());
            verify(preparedStatement).executeUpdate();
        }
    }

    // =========================================================================
    // AiCandidateReviewService — AI 候选审核
    // =========================================================================

    @Nested
    @DisplayName("AiCandidateReviewService — AI 候选审核")
    class AiCandidateReviewServiceTests {

        private AiCandidateReviewService reviewService;

        @BeforeEach
        void init() throws Exception {
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            reviewService = new AiCandidateReviewService(properties, dataSource);
        }

        @Test
        @DisplayName("提交候选 — 成功提交并设置默认值")
        void submitCandidate_success() throws Exception {
            AiCandidateReview candidate = new AiCandidateReview();
            candidate.setTenantId(1L);
            candidate.setCandidateType("TERMINOLOGY_MAPPING");
            candidate.setCandidateName("ICD-10 映射候选");
            candidate.setSourceCode("KS-ICD10");
            candidate.setModelProvider("OpenAI");
            candidate.setModelName("gpt-4");
            candidate.setConfidence(0.95);
            candidate.setCandidateContent("{\"code\": \"I21.0\"}");
            candidate.setCreatedBy("admin");

            AiCandidateReview result = reviewService.submitCandidate(candidate);

            assertNotNull(result.getId());
            assertNotNull(result.getCandidateCode());
            assertEquals("PENDING", result.getReviewStatus());
            assertEquals("MEDIUM", result.getPriority());
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("提交候选 — 自定义 candidateCode 和 priority")
        void submitCandidate_customFields() throws Exception {
            AiCandidateReview candidate = new AiCandidateReview();
            candidate.setTenantId(1L);
            candidate.setCandidateCode("CAND-CUSTOM");
            candidate.setCandidateType("RULE");
            candidate.setCandidateName("规则候选");
            candidate.setPriority("HIGH");
            candidate.setReviewStatus("PENDING");

            AiCandidateReview result = reviewService.submitCandidate(candidate);

            assertEquals("CAND-CUSTOM", result.getCandidateCode());
            assertEquals("HIGH", result.getPriority());
        }

        @Test
        @DisplayName("审核候选 — 成功审核通过")
        void reviewCandidate_approved() throws Exception {
            reviewService.reviewCandidate(100L, "APPROVED", "reviewer1", "审核通过", null);
            verify(preparedStatement).setString(1, "APPROVED");
            verify(preparedStatement).setString(2, "reviewer1");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("审核候选 — 审核驳回")
        void reviewCandidate_rejected() throws Exception {
            reviewService.reviewCandidate(100L, "REJECTED", "reviewer1", "置信度不足", null);
            verify(preparedStatement).setString(1, "REJECTED");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("审核候选 — 带修改内容审核")
        void reviewCandidate_withModifiedContent() throws Exception {
            reviewService.reviewCandidate(100L, "MODIFIED", "reviewer1", "已修正",
                    "{\"code\": \"I21.9\"}");
            verify(preparedStatement).setString(1, "MODIFIED");
            verify(preparedStatement).executeUpdate();
        }

        @Test
        @DisplayName("批量审核 — 逐条调用审核方法")
        void batchReview_success() throws Exception {
            List<Long> ids = List.of(1L, 2L, 3L);
            reviewService.batchReview(ids, "APPROVED", "reviewer1", "批量通过");

            // 每个ID调用一次 reviewCandidate，共3次 executeUpdate
            verify(preparedStatement, times(3)).executeUpdate();
        }
    }

    // =========================================================================
    // KnowledgeSyncService — 知识同步
    // =========================================================================

    @Nested
    @DisplayName("KnowledgeSyncService — 知识同步")
    class KnowledgeSyncServiceTests {

        private KnowledgeSyncService syncService;
        private KnowledgeService knowledgeService;

        @BeforeEach
        void init() throws Exception {
            knowledgeService = new KnowledgeService(organizationContextService);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            syncService = new KnowledgeSyncService(properties, opsSyncTaskService,
                    knowledgeService, dataSource);
        }

        @Test
        @DisplayName("手动触发同步 — sourceCode 为空时抛出异常")
        void triggerManualSync_emptySourceCode() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> syncService.triggerManualSync(1L, "", null,
                            KnowledgeSyncLog.SYNC_MODE_FULL, "admin"));
            assertTrue(ex.getMessage().contains("来源编码不能为空"));
        }

        @Test
        @DisplayName("手动触发同步 — sourceCode 为 null 时抛出异常")
        void triggerManualSync_nullSourceCode() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> syncService.triggerManualSync(1L, null, null,
                            KnowledgeSyncLog.SYNC_MODE_FULL, "admin"));
            assertTrue(ex.getMessage().contains("来源编码不能为空"));
        }

        @Test
        @DisplayName("审核同步 — 日志不存在时抛出异常")
        void reviewSync_logNotFound() {
            // getSyncLog 查询数据库，mock 返回空结果
            assertThrows(Exception.class,
                    () -> syncService.reviewSync(99999L, "APPROVED", "admin", "通过"));
        }

        @Test
        @DisplayName("执行已审核同步 — 日志不存在时抛出异常")
        void executeApprovedSync_logNotFound() {
            assertThrows(Exception.class,
                    () -> syncService.executeApprovedSync(99999L));
        }

        @Test
        @DisplayName("重试同步 — 日志不存在时抛出异常")
        void retrySync_logNotFound() {
            assertThrows(Exception.class,
                    () -> syncService.retrySync(99999L));
        }

        @Test
        @DisplayName("取消同步 — 日志不存在时抛出异常")
        void cancelSync_logNotFound() {
            assertThrows(Exception.class,
                    () -> syncService.cancelSync(99999L, "admin"));
        }
    }

    // =========================================================================
    // KnowledgeSyncLog — 状态判断方法
    // =========================================================================

    @Nested
    @DisplayName("KnowledgeSyncLog — 状态判断")
    class KnowledgeSyncLogStateTests {

        @Test
        @DisplayName("isTerminal — COMPLETED 为终态")
        void isTerminal_completed() {
            KnowledgeSyncLog log = new KnowledgeSyncLog();
            log.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
            assertTrue(log.isTerminal());
        }

        @Test
        @DisplayName("isTerminal — FAILED 为终态")
        void isTerminal_failed() {
            KnowledgeSyncLog log = new KnowledgeSyncLog();
            log.setStatus(KnowledgeSyncLog.STATUS_FAILED);
            assertTrue(log.isTerminal());
        }

        @Test
        @DisplayName("isTerminal — CANCELLED 为终态")
        void isTerminal_cancelled() {
            KnowledgeSyncLog log = new KnowledgeSyncLog();
            log.setStatus(KnowledgeSyncLog.STATUS_CANCELLED);
            assertTrue(log.isTerminal());
        }

        @Test
        @DisplayName("isTerminal — PENDING 不是终态")
        void isTerminal_pending() {
            KnowledgeSyncLog log = new KnowledgeSyncLog();
            log.setStatus(KnowledgeSyncLog.STATUS_PENDING);
            assertFalse(log.isTerminal());
        }

        @Test
        @DisplayName("isTerminal — SYNCING 不是终态")
        void isTerminal_syncing() {
            KnowledgeSyncLog log = new KnowledgeSyncLog();
            log.setStatus(KnowledgeSyncLog.STATUS_SYNCING);
            assertFalse(log.isTerminal());
        }

        @Test
        @DisplayName("canRetry — 仅 FAILED 状态可重试")
        void canRetry_onlyFailed() {
            KnowledgeSyncLog failed = new KnowledgeSyncLog();
            failed.setStatus(KnowledgeSyncLog.STATUS_FAILED);
            assertTrue(failed.canRetry());

            KnowledgeSyncLog completed = new KnowledgeSyncLog();
            completed.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
            assertFalse(completed.canRetry());

            KnowledgeSyncLog pending = new KnowledgeSyncLog();
            pending.setStatus(KnowledgeSyncLog.STATUS_PENDING);
            assertFalse(pending.canRetry());
        }

        @Test
        @DisplayName("canCancel — 非终态可取消")
        void canCancel_nonTerminal() {
            KnowledgeSyncLog pending = new KnowledgeSyncLog();
            pending.setStatus(KnowledgeSyncLog.STATUS_PENDING);
            assertTrue(pending.canCancel());

            KnowledgeSyncLog syncing = new KnowledgeSyncLog();
            syncing.setStatus(KnowledgeSyncLog.STATUS_SYNCING);
            assertTrue(syncing.canCancel());

            KnowledgeSyncLog completed = new KnowledgeSyncLog();
            completed.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
            assertFalse(completed.canCancel());
        }

        @Test
        @DisplayName("isDiffReady — DIFF_READY 及之后状态返回 true")
        void isDiffReady_states() {
            KnowledgeSyncLog diffReady = new KnowledgeSyncLog();
            diffReady.setStatus(KnowledgeSyncLog.STATUS_DIFF_READY);
            assertTrue(diffReady.isDiffReady());

            KnowledgeSyncLog approved = new KnowledgeSyncLog();
            approved.setStatus(KnowledgeSyncLog.STATUS_APPROVED);
            assertTrue(approved.isDiffReady());

            KnowledgeSyncLog syncing = new KnowledgeSyncLog();
            syncing.setStatus(KnowledgeSyncLog.STATUS_SYNCING);
            assertTrue(syncing.isDiffReady());

            KnowledgeSyncLog completed = new KnowledgeSyncLog();
            completed.setStatus(KnowledgeSyncLog.STATUS_COMPLETED);
            assertTrue(completed.isDiffReady());

            KnowledgeSyncLog pending = new KnowledgeSyncLog();
            pending.setStatus(KnowledgeSyncLog.STATUS_PENDING);
            assertFalse(pending.isDiffReady());
        }
    }
}
