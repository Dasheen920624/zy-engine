package com.medkernel.engine.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.medkernel.shared.context.OrgScope;
import com.medkernel.shared.context.RequestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 随访引擎 Service 单元测试。
 *
 * <p>使用 Mockito 隔离数据库依赖，验证业务逻辑：
 * <ul>
 *   <li>计划生成：验证返回的 planId 和任务列表</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class FollowupEngineServiceTest {

    @Mock
    private FollowupPlanRepository planRepository;
    @Mock
    private FollowupTaskRepository taskRepository;
    @Mock
    private FollowupQuestionnaireRepository questionnaireRepository;
    @Mock
    private FollowupEventRepository eventRepository;

    @InjectMocks
    private FollowupEngineService service;

    @BeforeEach
    void setUp() {
        RequestContext.restore(new RequestContext.Snapshot(
            "trace-123", OrgScope.tenant("tenant-1"), "user-1"
        ));
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void testGeneratePlan() {
        FollowupPlanGenerateRequest request = new FollowupPlanGenerateRequest(
            "PAT01", "ENC01", "PATH01", "D01", "HIGH", List.of("QUESTIONNAIRE")
        );
        
        FollowupPlan plan = new FollowupPlan(1L, "PLAN01", "tenant-1", "PAT01", "ENC01", "PATH01", "D01", "HIGH",
            FollowupPlanStatus.ACTIVE, Instant.now(), "sys", Instant.now(), "sys", "trace-123");
            
        when(planRepository.save(any(FollowupPlan.class))).thenReturn(plan);
        
        FollowupTask task = new FollowupTask(1L, "TASK01", "tenant-1", "PLAN01", FollowupTaskType.QUESTIONNAIRE,
            Instant.now(), FollowupTaskStatus.PENDING, null, null, Instant.now(), "sys", Instant.now(), "sys", "trace-123");
            
        when(taskRepository.save(any(FollowupTask.class))).thenReturn(task);

        FollowupPlanDetailResponse response = service.generatePlan(request);
        
        assertNotNull(response);
        assertEquals("PLAN01", response.planId());
        assertEquals(1, response.tasks().size());
        assertEquals(FollowupTaskType.QUESTIONNAIRE, response.tasks().get(0).taskType());
    }
}
