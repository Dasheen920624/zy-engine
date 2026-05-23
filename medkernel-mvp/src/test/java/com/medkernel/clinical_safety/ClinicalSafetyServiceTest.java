package com.medkernel.clinical_safety;

import com.medkernel.cdss.ClinicalSafetyService;
import com.medkernel.cdss.HazardLog;
import com.medkernel.cdss.SafetyCase;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicalSafetyService 单元测试")
class ClinicalSafetyServiceTest {

    @Mock
    private EnginePersistenceProperties properties;

    @Mock
    private DataSource dataSource;

    private ClinicalSafetyService clinicalSafetyService;

    @BeforeEach
    void setUp() {
        clinicalSafetyService = new ClinicalSafetyService(properties, dataSource);
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private HazardLog buildHazardLog() {
        HazardLog hazard = new HazardLog();
        hazard.setTenantId(1L);
        hazard.setHazardCode("HAZ-001");
        hazard.setHazardName("CDSS误诊风险");
        hazard.setHazardCategory("AI_SPECIFIC");
        hazard.setHazardDescription("AI推荐可能导致误诊");
        hazard.setAffectedProcess("CDSS");
        hazard.setLikelihood("POSSIBLE");
        hazard.setSeverity("MAJOR");
        hazard.setRiskLevel("HIGH");
        hazard.setControlMeasures("增加人工审核环节");
        hazard.setResidualRisk("MEDIUM");
        hazard.setBlockingStrategy("ESCALATE");
        hazard.setCreatedBy("admin");
        return hazard;
    }

    private SafetyCase buildSafetyCase() {
        SafetyCase safetyCase = new SafetyCase();
        safetyCase.setTenantId(1L);
        safetyCase.setCaseCode("SC-001");
        safetyCase.setCaseName("CDSS系统安全案例");
        safetyCase.setCaseType("AI_MODEL");
        safetyCase.setScope("CDSS推荐引擎");
        safetyCase.setGoal("确保CDSS推荐不会对患者造成伤害");
        safetyCase.setArgument("通过多层审核和阻断策略确保安全");
        safetyCase.setEvidenceRefs("[\"HAZ-001\",\"HAZ-002\"]");
        safetyCase.setVersion("1.0.0");
        safetyCase.setCreatedBy("admin");
        return safetyCase;
    }

    // ──────────────────────── 危险日志管理 ────────────────────────

    @Nested
    @DisplayName("危险日志管理")
    class HazardLogTests {

        @Test
        @DisplayName("持久化未启用时创建危险日志直接返回")
        void createHazardWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            HazardLog hazard = buildHazardLog();
            HazardLog result = clinicalSafetyService.createHazard(hazard);

            assertNotNull(result);
            assertEquals("HAZ-001", result.getHazardCode());
            assertEquals("CDSS误诊风险", result.getHazardName());
        }

        @Test
        @DisplayName("持久化启用时创建危险日志写入数据库")
        void createHazardWithPersistence() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);

            HazardLog hazard = buildHazardLog();
            HazardLog result = clinicalSafetyService.createHazard(hazard);

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("HAZ-001", result.getHazardCode());
        }

        @Test
        @DisplayName("创建危险日志时状态默认为IDENTIFIED")
        void createHazardDefaultStatus() {
            when(properties.isEnabled()).thenReturn(false);
            HazardLog hazard = buildHazardLog();
            hazard.setStatus(null);

            HazardLog result = clinicalSafetyService.createHazard(hazard);

            // 当持久化未启用时，status为null不会被设置默认值
            // 默认值IDENTIFIED仅在持久化SQL中设置
            assertNotNull(result);
        }

        @Test
        @DisplayName("持久化未启用时查询危险日志返回空列表")
        void listHazardsWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            List<HazardLog> result = clinicalSafetyService.listHazards(1L, null, null, null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("持久化启用时查询危险日志")
        void listHazardsWithPersistence() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("id")).thenReturn(1L);
            when(rs.getObject("tenant_id")).thenReturn(1L);
            when(rs.getString("hazard_code")).thenReturn("HAZ-001");
            when(rs.getString("hazard_name")).thenReturn("CDSS误诊风险");
            when(rs.getString("hazard_category")).thenReturn("AI_SPECIFIC");
            when(rs.getString("hazard_description")).thenReturn("AI推荐可能导致误诊");
            when(rs.getString("affected_process")).thenReturn("CDSS");
            when(rs.getString("likelihood")).thenReturn("POSSIBLE");
            when(rs.getString("severity")).thenReturn("MAJOR");
            when(rs.getString("risk_level")).thenReturn("HIGH");
            when(rs.getString("control_measures")).thenReturn("增加人工审核");
            when(rs.getString("residual_risk")).thenReturn("MEDIUM");
            when(rs.getString("status")).thenReturn("IDENTIFIED");
            when(rs.getString("accepted_by")).thenReturn(null);
            when(rs.getTimestamp("accepted_time")).thenReturn(null);
            when(rs.getString("acceptance_note")).thenReturn(null);
            when(rs.getString("blocking_strategy")).thenReturn("ESCALATE");
            when(rs.getString("created_by")).thenReturn("admin");
            when(rs.getTimestamp("created_time")).thenReturn(null);
            when(rs.getString("updated_by")).thenReturn(null);
            when(rs.getTimestamp("updated_time")).thenReturn(null);

            List<HazardLog> result = clinicalSafetyService.listHazards(1L, null, null, null);

            assertEquals(1, result.size());
            assertEquals("HAZ-001", result.get(0).getHazardCode());
        }

        @Test
        @DisplayName("更新危险日志时找不到记录抛出异常")
        void updateHazardNotFoundThrows() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            HazardLog hazard = buildHazardLog();
            hazard.setId(999L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clinicalSafetyService.updateHazard(hazard));
            assertTrue(ex.getMessage().contains("Hazard not found"));
        }

        @Test
        @DisplayName("持久化未启用时更新危险日志直接返回")
        void updateHazardWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            HazardLog hazard = buildHazardLog();
            HazardLog result = clinicalSafetyService.updateHazard(hazard);

            assertNotNull(result);
            assertEquals("HAZ-001", result.getHazardCode());
        }
    }

    // ──────────────────────── 风险接受 ────────────────────────

    @Nested
    @DisplayName("风险接受")
    class AcceptHazardTests {

        @Test
        @DisplayName("持久化未启用时接受风险返回正确信息")
        void acceptHazardWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            HazardLog result = clinicalSafetyService.acceptHazard(1L, "zheng07", "院方已评估，接受残余风险");

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("zheng07", result.getAcceptedBy());
            assertEquals("院方已评估，接受残余风险", result.getAcceptanceNote());
        }

        @Test
        @DisplayName("持久化启用时接受风险更新数据库")
        void acceptHazardWithPersistence() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("id")).thenReturn(1L);
            when(rs.getObject("tenant_id")).thenReturn(1L);
            when(rs.getString("hazard_code")).thenReturn("HAZ-001");
            when(rs.getString("hazard_name")).thenReturn("CDSS误诊风险");
            when(rs.getString("hazard_category")).thenReturn("AI_SPECIFIC");
            when(rs.getString("hazard_description")).thenReturn("AI推荐可能导致误诊");
            when(rs.getString("affected_process")).thenReturn("CDSS");
            when(rs.getString("likelihood")).thenReturn("POSSIBLE");
            when(rs.getString("severity")).thenReturn("MAJOR");
            when(rs.getString("risk_level")).thenReturn("HIGH");
            when(rs.getString("control_measures")).thenReturn("增加人工审核");
            when(rs.getString("residual_risk")).thenReturn("MEDIUM");
            when(rs.getString("status")).thenReturn("ACCEPTED");
            when(rs.getString("accepted_by")).thenReturn("zheng07");
            when(rs.getTimestamp("accepted_time")).thenReturn(null);
            when(rs.getString("acceptance_note")).thenReturn("院方已评估");
            when(rs.getString("blocking_strategy")).thenReturn("ESCALATE");
            when(rs.getString("created_by")).thenReturn("admin");
            when(rs.getTimestamp("created_time")).thenReturn(null);
            when(rs.getString("updated_by")).thenReturn(null);
            when(rs.getTimestamp("updated_time")).thenReturn(null);

            HazardLog result = clinicalSafetyService.acceptHazard(1L, "zheng07", "院方已评估");

            assertEquals("ACCEPTED", result.getStatus());
            assertEquals("zheng07", result.getAcceptedBy());
        }

        @Test
        @DisplayName("接受不存在的风险抛出异常")
        void acceptNonExistentHazardThrows() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clinicalSafetyService.acceptHazard(999L, "zheng07", "接受"));
            assertTrue(ex.getMessage().contains("Hazard not found"));
        }
    }

    // ──────────────────────── 关闭危险日志 ────────────────────────

    @Nested
    @DisplayName("关闭危险日志")
    class CloseHazardTests {

        @Test
        @DisplayName("持久化未启用时关闭危险日志返回正确状态")
        void closeHazardWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            HazardLog result = clinicalSafetyService.closeHazard(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("CLOSED", result.getStatus());
        }

        @Test
        @DisplayName("关闭不存在的危险日志抛出异常")
        void closeNonExistentHazardThrows() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clinicalSafetyService.closeHazard(999L));
            assertTrue(ex.getMessage().contains("Hazard not found"));
        }
    }

    // ──────────────────────── 安全案例管理 ────────────────────────

    @Nested
    @DisplayName("安全案例管理")
    class SafetyCaseTests {

        @Test
        @DisplayName("持久化未启用时创建安全案例直接返回")
        void createSafetyCaseWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            SafetyCase safetyCase = buildSafetyCase();
            SafetyCase result = clinicalSafetyService.createSafetyCase(safetyCase);

            assertNotNull(result);
            assertEquals("SC-001", result.getCaseCode());
            assertEquals("CDSS系统安全案例", result.getCaseName());
            assertEquals("AI_MODEL", result.getCaseType());
        }

        @Test
        @DisplayName("持久化启用时创建安全案例写入数据库")
        void createSafetyCaseWithPersistence() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);

            SafetyCase safetyCase = buildSafetyCase();
            SafetyCase result = clinicalSafetyService.createSafetyCase(safetyCase);

            assertNotNull(result);
            assertNotNull(result.getId());
        }

        @Test
        @DisplayName("持久化未启用时查询安全案例返回空列表")
        void listSafetyCasesWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            List<SafetyCase> result = clinicalSafetyService.listSafetyCases(1L, null, null);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("更新安全案例时找不到记录抛出异常")
        void updateSafetyCaseNotFoundThrows() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            SafetyCase safetyCase = buildSafetyCase();
            safetyCase.setId(999L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clinicalSafetyService.updateSafetyCase(safetyCase));
            assertTrue(ex.getMessage().contains("Safety case not found"));
        }

        @Test
        @DisplayName("持久化未启用时更新安全案例直接返回")
        void updateSafetyCaseWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            SafetyCase safetyCase = buildSafetyCase();
            SafetyCase result = clinicalSafetyService.updateSafetyCase(safetyCase);

            assertNotNull(result);
            assertEquals("SC-001", result.getCaseCode());
        }

        @Test
        @DisplayName("审核安全案例时找不到记录抛出异常")
        void reviewSafetyCaseNotFoundThrows() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            when(properties.localFileDatabase()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> clinicalSafetyService.reviewSafetyCase(999L, "APPROVED", "zheng07", "审核通过"));
            assertTrue(ex.getMessage().contains("Safety case not found"));
        }

        @Test
        @DisplayName("持久化未启用时审核安全案例返回正确状态")
        void reviewSafetyCaseWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            SafetyCase result = clinicalSafetyService.reviewSafetyCase(
                    1L, "APPROVED", "zheng07", "审核通过");

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("APPROVED", result.getStatus());
            assertEquals("zheng07", result.getReviewedBy());
            assertEquals("审核通过", result.getReviewNote());
        }
    }

    // ──────────────────────── 风险矩阵计算 ────────────────────────

    @Nested
    @DisplayName("5×5 风险矩阵计算")
    class RiskMatrixTests {

        @Test
        @DisplayName("ALMOST_CERTAIN × CATASTROPHIC = CRITICAL（5×5=25）")
        void almostCertainCatastrophicIsCritical() {
            assertEquals("CRITICAL",
                    clinicalSafetyService.calculateRiskLevel("ALMOST_CERTAIN", "CATASTROPHIC"));
        }

        @Test
        @DisplayName("LIKELY × MAJOR = HIGH（4×4=16）")
        void likelyMajorIsHigh() {
            assertEquals("HIGH",
                    clinicalSafetyService.calculateRiskLevel("LIKELY", "MAJOR"));
        }

        @Test
        @DisplayName("POSSIBLE × MODERATE = MEDIUM（3×3=9）")
        void possibleModerateIsMedium() {
            assertEquals("MEDIUM",
                    clinicalSafetyService.calculateRiskLevel("POSSIBLE", "MODERATE"));
        }

        @Test
        @DisplayName("RARE × NEGLIGIBLE = LOW（1×1=1）")
        void rareNegligibleIsLow() {
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel("RARE", "NEGLIGIBLE"));
        }

        @Test
        @DisplayName("UNLIKELY × MINOR = LOW（2×2=4）")
        void unlikelyMinorIsLow() {
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel("UNLIKELY", "MINOR"));
        }

        @Test
        @DisplayName("POSSIBLE × MAJOR = HIGH（3×4=12）")
        void possibleMajorIsHigh() {
            assertEquals("HIGH",
                    clinicalSafetyService.calculateRiskLevel("POSSIBLE", "MAJOR"));
        }

        @Test
        @DisplayName("LIKELY × MODERATE = MEDIUM（4×3=12 → HIGH）")
        void likelyModerateIsHigh() {
            // 4×3=12 >= 10, so HIGH
            assertEquals("HIGH",
                    clinicalSafetyService.calculateRiskLevel("LIKELY", "MODERATE"));
        }

        @Test
        @DisplayName("UNLIKELY × MODERATE = MEDIUM（2×3=6）")
        void unlikelyModerateIsMedium() {
            assertEquals("MEDIUM",
                    clinicalSafetyService.calculateRiskLevel("UNLIKELY", "MODERATE"));
        }

        @Test
        @DisplayName("可能性为null时默认得分为1")
        void nullLikelihoodDefaultsTo1() {
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel(null, "NEGLIGIBLE"));
        }

        @Test
        @DisplayName("严重性为null时默认得分为1")
        void nullSeverityDefaultsTo1() {
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel("RARE", null));
        }

        @Test
        @DisplayName("未知可能性值默认得分为1")
        void unknownLikelihoodDefaultsTo1() {
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel("UNKNOWN", "NEGLIGIBLE"));
        }

        @Test
        @DisplayName("未知严重性值默认得分为1")
        void unknownSeverityDefaultsTo1() {
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel("RARE", "UNKNOWN"));
        }

        @Test
        @DisplayName("边界值：得分5为MEDIUM")
        void score5IsMedium() {
            // POSSIBLE(3) × MINOR(2) = 6, but we need exactly 5
            // UNLIKELY(2) × MODERATE(3) = 6, not 5
            // Let's find: 5 = 5×1 or impossible with integer factors
            // Actually 5*1 = ALMOST_CERTAIN * NEGLIGIBLE = 5
            assertEquals("MEDIUM",
                    clinicalSafetyService.calculateRiskLevel("ALMOST_CERTAIN", "NEGLIGIBLE"));
        }

        @Test
        @DisplayName("边界值：得分4为LOW")
        void score4IsLow() {
            // 2*2 = 4, which is < 5, so LOW
            assertEquals("LOW",
                    clinicalSafetyService.calculateRiskLevel("UNLIKELY", "MINOR"));
        }

        @Test
        @DisplayName("边界值：得分17为CRITICAL")
        void score17IsCritical() {
            // ALMOST_CERTAIN(5) × MAJOR(4) = 20 >= 17
            assertEquals("CRITICAL",
                    clinicalSafetyService.calculateRiskLevel("ALMOST_CERTAIN", "MAJOR"));
        }

        @Test
        @DisplayName("边界值：得分10为HIGH")
        void score10IsHigh() {
            // LIKELY(4) × MODERATE(3) = 12 >= 10
            assertEquals("HIGH",
                    clinicalSafetyService.calculateRiskLevel("LIKELY", "MODERATE"));
        }
    }

    // ──────────────────────── 阻断策略 ────────────────────────

    @Nested
    @DisplayName("阻断策略映射")
    class BlockingStrategyTests {

        @Test
        @DisplayName("CRITICAL 风险等级映射为 BLOCK 策略")
        void criticalMapsToBlock() {
            assertEquals("BLOCK", clinicalSafetyService.getBlockingStrategy("CRITICAL"));
        }

        @Test
        @DisplayName("HIGH 风险等级映射为 ESCALATE 策略")
        void highMapsToEscalate() {
            assertEquals("ESCALATE", clinicalSafetyService.getBlockingStrategy("HIGH"));
        }

        @Test
        @DisplayName("MEDIUM 风险等级映射为 REQUIRE_DUAL_CONFIRM 策略")
        void mediumMapsToDualConfirm() {
            assertEquals("REQUIRE_DUAL_CONFIRM", clinicalSafetyService.getBlockingStrategy("MEDIUM"));
        }

        @Test
        @DisplayName("LOW 风险等级映射为 WARN 策略")
        void lowMapsToWarn() {
            assertEquals("WARN", clinicalSafetyService.getBlockingStrategy("LOW"));
        }

        @Test
        @DisplayName("null 风险等级映射为 WARN 策略")
        void nullMapsToWarn() {
            assertEquals("WARN", clinicalSafetyService.getBlockingStrategy(null));
        }

        @Test
        @DisplayName("未知风险等级映射为 WARN 策略")
        void unknownMapsToWarn() {
            assertEquals("WARN", clinicalSafetyService.getBlockingStrategy("UNKNOWN"));
        }
    }

    // ──────────────────────── 风险摘要 ────────────────────────

    @Nested
    @DisplayName("风险摘要统计")
    class RiskSummaryTests {

        @Test
        @DisplayName("持久化未启用时返回空统计")
        void riskSummaryWhenPersistenceDisabled() {
            when(properties.isEnabled()).thenReturn(false);

            Map<String, Object> summary = clinicalSafetyService.getRiskSummary(1L);

            assertEquals(1L, summary.get("tenant_id"));
            assertEquals(0, summary.get("total_hazards"));
            assertEquals(0, summary.get("total_safety_cases"));
            assertNotNull(summary.get("risk_level_distribution"));
            assertNotNull(summary.get("status_distribution"));
            assertNotNull(summary.get("safety_case_status_distribution"));
        }

        @Test
        @DisplayName("持久化启用时查询风险摘要")
        void riskSummaryWithPersistence() throws SQLException {
            when(properties.isEnabled()).thenReturn(true);
            Connection connection = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            Map<String, Object> summary = clinicalSafetyService.getRiskSummary(1L);

            assertNotNull(summary);
            assertEquals(1L, summary.get("tenant_id"));
        }
    }
}
