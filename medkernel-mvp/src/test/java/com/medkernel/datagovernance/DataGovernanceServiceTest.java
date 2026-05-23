package com.medkernel.datagovernance;

import com.medkernel.datagovernance.entity.DepartmentEntity;
import com.medkernel.datagovernance.entity.DoctorEntity;
import com.medkernel.datagovernance.entity.PatientEntity;
import com.medkernel.datagovernance.entity.QualityCheckEntity;
import com.medkernel.datagovernance.entity.QualityRuleEntity;
import com.medkernel.datagovernance.repository.DepartmentRepository;
import com.medkernel.datagovernance.repository.DoctorRepository;
import com.medkernel.datagovernance.repository.PatientRepository;
import com.medkernel.datagovernance.repository.QualityCheckRepository;
import com.medkernel.datagovernance.repository.QualityRuleRepository;
import com.medkernel.datagovernance.service.DataGovernanceService;
import com.medkernel.datagovernance.service.DepartmentService;
import com.medkernel.datagovernance.service.DoctorService;
import com.medkernel.datagovernance.service.PatientService;
import com.medkernel.datagovernance.service.QualityReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("数据治理服务单元测试")
class DataGovernanceServiceTest {

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private QualityRuleRepository qualityRuleRepository;
    @Mock
    private QualityCheckRepository qualityCheckRepository;

    private PatientService patientService;
    private DoctorService doctorService;
    private DepartmentService departmentService;
    private DataGovernanceService dataGovernanceService;
    private QualityReportService qualityReportService;

    private static final String TENANT_ID = "tenant_001";

    @BeforeEach
    void setUp() {
        patientService = new PatientService(patientRepository);
        doctorService = new DoctorService(doctorRepository);
        departmentService = new DepartmentService(departmentRepository);
        dataGovernanceService = new DataGovernanceService(
                patientService, doctorService, departmentService,
                qualityRuleRepository, qualityCheckRepository);
        qualityReportService = new QualityReportService(qualityRuleRepository, qualityCheckRepository);
    }

    // ──────────────────────── 辅助方法 ────────────────────────

    private PatientEntity buildPatient(String patientId, String name, String gender, LocalDate birthDate) {
        PatientEntity p = new PatientEntity();
        p.setTenantId(TENANT_ID);
        p.setPatientId(patientId);
        p.setPatientName(name);
        p.setGender(gender);
        p.setBirthDate(birthDate);
        p.setStatus("ACTIVE");
        return p;
    }

    private QualityRuleEntity buildRule(String ruleCode, String ruleName, String ruleType,
                                        String targetEntity, String targetField, String severity, String status) {
        QualityRuleEntity rule = new QualityRuleEntity();
        rule.setTenantId(TENANT_ID);
        rule.setRuleCode(ruleCode);
        rule.setRuleName(ruleName);
        rule.setRuleType(ruleType);
        rule.setTargetEntity(targetEntity);
        rule.setTargetField(targetField);
        rule.setSeverity(severity);
        rule.setStatus(status);
        return rule;
    }

    private QualityCheckEntity buildCheckEntity(String ruleCode, String targetEntity,
                                                 String targetId, String result, String errorMessage) {
        QualityCheckEntity check = new QualityCheckEntity();
        check.setTenantId(TENANT_ID);
        check.setCheckId("chk_" + targetId);
        check.setRuleCode(ruleCode);
        check.setTargetEntity(targetEntity);
        check.setTargetId(targetId);
        check.setCheckResult(result);
        check.setErrorMessage(errorMessage);
        check.setCheckTime(LocalDateTime.now());
        check.setCreatedTime(LocalDateTime.now());
        return check;
    }

    // ──────────────────────── DataGovernanceService 测试 ────────────────────────

    @Nested
    @DisplayName("数据治理概览")
    class OverviewTests {

        @Test
        @DisplayName("获取概览 - 正常返回主数据统计和质量统计")
        void getOverview_returnsCorrectStats() {
            when(patientRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Arrays.asList(buildPatient("P001", "张三", "男", LocalDate.of(1990, 1, 1))));
            when(doctorRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.emptyList());
            when(departmentRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.emptyList());

            QualityRuleEntity activeRule = buildRule("R001", "患者姓名完整性", "COMPLETENESS",
                    "md_patient", "patient_name", "HIGH", "ACTIVE");
            QualityRuleEntity inactiveRule = buildRule("R002", "患者性别完整性", "COMPLETENESS",
                    "md_patient", "gender", "MEDIUM", "INACTIVE");
            when(qualityRuleRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Arrays.asList(activeRule, inactiveRule));

            Map<String, Object> overview = dataGovernanceService.getOverview(TENANT_ID);

            assertNotNull(overview);
            assertEquals(1, ((Map<?, ?>) overview.get("master_data")).get("patient_count"));
            assertEquals(0, ((Map<?, ?>) overview.get("master_data")).get("doctor_count"));
            assertEquals(2, ((Map<?, ?>) overview.get("data_quality")).get("rule_count"));
            assertEquals(1L, ((Map<?, ?>) overview.get("data_quality")).get("active_rule_count"));
            assertNotNull(overview.get("generated_time"));
        }

        @Test
        @DisplayName("获取概览 - 无数据时返回零值统计")
        void getOverview_returnsZeroWhenNoData() {
            when(patientRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
            when(doctorRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
            when(departmentRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
            when(qualityRuleRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

            Map<String, Object> overview = dataGovernanceService.getOverview(TENANT_ID);

            assertEquals(0, ((Map<?, ?>) overview.get("master_data")).get("patient_count"));
            assertEquals(0, ((Map<?, ?>) overview.get("data_quality")).get("rule_count"));
            assertEquals(0L, ((Map<?, ?>) overview.get("data_quality")).get("active_rule_count"));
        }
    }

    @Nested
    @DisplayName("数据质量检查")
    class QualityCheckTests {

        @Test
        @DisplayName("执行完整性检查 - 患者姓名缺失时检查失败")
        void executeQualityCheck_completeness_patientNameMissing() {
            QualityRuleEntity rule = buildRule("R001", "患者姓名完整性", "COMPLETENESS",
                    "md_patient", "patient_name", "HIGH", "ACTIVE");
            when(qualityRuleRepository.findByRuleCode(TENANT_ID, "R001")).thenReturn(rule);

            PatientEntity patientWithoutName = buildPatient("P001", null, "男", LocalDate.of(1990, 1, 1));
            when(patientRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.singletonList(patientWithoutName));

            Map<String, Object> result = dataGovernanceService.executeQualityCheck(TENANT_ID, "R001");

            assertEquals("R001", result.get("rule_code"));
            assertEquals(1, result.get("total_checked"));
            assertEquals(0L, result.get("passed"));
            assertEquals(1L, result.get("failed"));
            verify(qualityCheckRepository).save(any(QualityCheckEntity.class));
        }

        @Test
        @DisplayName("执行完整性检查 - 患者姓名完整时检查通过")
        void executeQualityCheck_completeness_patientNamePresent() {
            QualityRuleEntity rule = buildRule("R001", "患者姓名完整性", "COMPLETENESS",
                    "md_patient", "patient_name", "HIGH", "ACTIVE");
            when(qualityRuleRepository.findByRuleCode(TENANT_ID, "R001")).thenReturn(rule);

            PatientEntity patient = buildPatient("P001", "张三", "男", LocalDate.of(1990, 1, 1));
            when(patientRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.singletonList(patient));

            Map<String, Object> result = dataGovernanceService.executeQualityCheck(TENANT_ID, "R001");

            assertEquals(1, result.get("total_checked"));
            assertEquals(1L, result.get("passed"));
            assertEquals(0L, result.get("failed"));
        }

        @Test
        @DisplayName("执行完整性检查 - 性别字段缺失")
        void executeQualityCheck_completeness_genderMissing() {
            QualityRuleEntity rule = buildRule("R002", "患者性别完整性", "COMPLETENESS",
                    "md_patient", "gender", "MEDIUM", "ACTIVE");
            when(qualityRuleRepository.findByRuleCode(TENANT_ID, "R002")).thenReturn(rule);

            PatientEntity patient = buildPatient("P001", "张三", null, LocalDate.of(1990, 1, 1));
            when(patientRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.singletonList(patient));

            Map<String, Object> result = dataGovernanceService.executeQualityCheck(TENANT_ID, "R002");

            assertEquals(0L, result.get("passed"));
            assertEquals(1L, result.get("failed"));
        }

        @Test
        @DisplayName("执行完整性检查 - 出生日期缺失")
        void executeQualityCheck_completeness_birthDateMissing() {
            QualityRuleEntity rule = buildRule("R003", "患者出生日期完整性", "COMPLETENESS",
                    "md_patient", "birth_date", "HIGH", "ACTIVE");
            when(qualityRuleRepository.findByRuleCode(TENANT_ID, "R003")).thenReturn(rule);

            PatientEntity patient = buildPatient("P001", "张三", "男", null);
            when(patientRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.singletonList(patient));

            Map<String, Object> result = dataGovernanceService.executeQualityCheck(TENANT_ID, "R003");

            assertEquals(0L, result.get("passed"));
            assertEquals(1L, result.get("failed"));
        }

        @Test
        @DisplayName("执行质量检查 - 规则不存在时抛出异常")
        void executeQualityCheck_ruleNotFound_throwsException() {
            when(qualityRuleRepository.findByRuleCode(TENANT_ID, "INVALID_CODE")).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> dataGovernanceService.executeQualityCheck(TENANT_ID, "INVALID_CODE"));
            assertTrue(ex.getMessage().contains("Quality rule not found"));
        }

        @Test
        @DisplayName("执行质量检查 - 多个患者混合通过和失败")
        void executeQualityCheck_completeness_mixedResults() {
            QualityRuleEntity rule = buildRule("R001", "患者姓名完整性", "COMPLETENESS",
                    "md_patient", "patient_name", "HIGH", "ACTIVE");
            when(qualityRuleRepository.findByRuleCode(TENANT_ID, "R001")).thenReturn(rule);

            PatientEntity p1 = buildPatient("P001", "张三", "男", LocalDate.of(1990, 1, 1));
            PatientEntity p2 = buildPatient("P002", "", "女", LocalDate.of(1985, 5, 15));
            PatientEntity p3 = buildPatient("P003", "王五", "男", LocalDate.of(2000, 3, 20));
            when(patientRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Arrays.asList(p1, p2, p3));

            Map<String, Object> result = dataGovernanceService.executeQualityCheck(TENANT_ID, "R001");

            assertEquals(3, result.get("total_checked"));
            assertEquals(2L, result.get("passed"));
            assertEquals(1L, result.get("failed"));
        }
    }

    // ──────────────────────── PatientService 测试 ────────────────────────

    @Nested
    @DisplayName("患者主数据服务")
    class PatientServiceTests {

        @Test
        @DisplayName("保存患者 - 新增时自动设置创建时间和更新时间")
        void save_newPatient_setsTimestamps() {
            PatientEntity patient = buildPatient("P001", "张三", "男", LocalDate.of(1990, 1, 1));

            PatientEntity result = patientService.save(patient);

            assertNotNull(result.getCreatedTime());
            assertNotNull(result.getUpdatedTime());
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("保存患者 - 已有创建时间时保留原值")
        void save_existingPatient_preservesCreatedTime() {
            LocalDateTime existingCreatedTime = LocalDateTime.of(2025, 1, 1, 0, 0);
            PatientEntity patient = buildPatient("P001", "张三", "男", LocalDate.of(1990, 1, 1));
            patient.setCreatedTime(existingCreatedTime);

            PatientEntity result = patientService.save(patient);

            assertEquals(existingCreatedTime, result.getCreatedTime());
            assertNotNull(result.getUpdatedTime());
        }

        @Test
        @DisplayName("根据患者ID查找患者")
        void findByPatientId_returnsPatient() {
            PatientEntity patient = buildPatient("P001", "张三", "男", LocalDate.of(1990, 1, 1));
            when(patientRepository.findByPatientId(TENANT_ID, "P001")).thenReturn(patient);

            PatientEntity result = patientService.findByPatientId(TENANT_ID, "P001");

            assertNotNull(result);
            assertEquals("P001", result.getPatientId());
            assertEquals("张三", result.getPatientName());
        }

        @Test
        @DisplayName("根据患者ID查找 - 不存在时返回null")
        void findByPatientId_notFound_returnsNull() {
            when(patientRepository.findByPatientId(TENANT_ID, "P999")).thenReturn(null);

            PatientEntity result = patientService.findByPatientId(TENANT_ID, "P999");

            assertEquals(null, result);
        }

        @Test
        @DisplayName("批量导入患者数据 - 全部成功")
        void batchImport_allSuccess() {
            List<Map<String, Object>> dataList = new ArrayList<>();
            Map<String, Object> data1 = new java.util.LinkedHashMap<>();
            data1.put("patient_id", "P001");
            data1.put("patient_name", "张三");
            data1.put("gender", "男");
            dataList.add(data1);

            Map<String, Object> data2 = new java.util.LinkedHashMap<>();
            data2.put("patient_id", "P002");
            data2.put("patient_name", "李四");
            data2.put("gender", "女");
            dataList.add(data2);

            int count = patientService.batchImport(TENANT_ID, dataList);

            assertEquals(2, count);
        }

        @Test
        @DisplayName("批量导入患者数据 - 部分失败时返回成功数量")
        void batchImport_partialFailure() {
            List<Map<String, Object>> dataList = new ArrayList<>();
            Map<String, Object> validData = new java.util.LinkedHashMap<>();
            validData.put("patient_id", "P001");
            validData.put("patient_name", "张三");
            validData.put("gender", "男");
            dataList.add(validData);

            Map<String, Object> invalidData = new java.util.LinkedHashMap<>();
            invalidData.put("patient_id", null);
            invalidData.put("patient_name", null);
            invalidData.put("gender", null);
            dataList.add(invalidData);

            int count = patientService.batchImport(TENANT_ID, dataList);

            assertTrue(count >= 1);
        }
    }

    // ──────────────────────── DoctorService 测试 ────────────────────────

    @Nested
    @DisplayName("医生主数据服务")
    class DoctorServiceTests {

        @Test
        @DisplayName("保存医生 - 自动设置时间戳")
        void save_setsTimestamps() {
            DoctorEntity doctor = new DoctorEntity();
            doctor.setTenantId(TENANT_ID);
            doctor.setDoctorId("D001");
            doctor.setDoctorName("李医生");

            DoctorEntity result = doctorService.save(doctor);

            assertNotNull(result.getCreatedTime());
            assertNotNull(result.getUpdatedTime());
            verify(doctorRepository).save(doctor);
        }

        @Test
        @DisplayName("根据医生ID查找医生")
        void findByDoctorId_returnsDoctor() {
            DoctorEntity doctor = new DoctorEntity();
            doctor.setDoctorId("D001");
            doctor.setDoctorName("李医生");
            when(doctorRepository.findByDoctorId(TENANT_ID, "D001")).thenReturn(doctor);

            DoctorEntity result = doctorService.findByDoctorId(TENANT_ID, "D001");

            assertNotNull(result);
            assertEquals("D001", result.getDoctorId());
        }
    }

    // ──────────────────────── DepartmentService 测试 ────────────────────────

    @Nested
    @DisplayName("科室主数据服务")
    class DepartmentServiceTests {

        @Test
        @DisplayName("保存科室 - 自动设置时间戳")
        void save_setsTimestamps() {
            DepartmentEntity dept = new DepartmentEntity();
            dept.setTenantId(TENANT_ID);
            dept.setDeptCode("DEPT001");
            dept.setDeptName("内科");

            DepartmentEntity result = departmentService.save(dept);

            assertNotNull(result.getCreatedTime());
            assertNotNull(result.getUpdatedTime());
            verify(departmentRepository).save(dept);
        }

        @Test
        @DisplayName("根据科室编码查找科室")
        void findByDeptCode_returnsDepartment() {
            DepartmentEntity dept = new DepartmentEntity();
            dept.setDeptCode("DEPT001");
            dept.setDeptName("内科");
            when(departmentRepository.findByDeptCode(TENANT_ID, "DEPT001")).thenReturn(dept);

            DepartmentEntity result = departmentService.findByDeptCode(TENANT_ID, "DEPT001");

            assertNotNull(result);
            assertEquals("DEPT001", result.getDeptCode());
        }
    }

    // ──────────────────────── QualityReportService 测试 ────────────────────────

    @Nested
    @DisplayName("数据质量报告服务")
    class QualityReportServiceTests {

        @Test
        @DisplayName("生成质量报告 - 包含规则统计和检查统计")
        void generateReport_containsRuleAndCheckStats() {
            QualityRuleEntity activeRule = buildRule("R001", "患者姓名完整性", "COMPLETENESS",
                    "md_patient", "patient_name", "HIGH", "ACTIVE");
            QualityRuleEntity inactiveRule = buildRule("R002", "患者性别完整性", "COMPLETENESS",
                    "md_patient", "gender", "MEDIUM", "INACTIVE");
            when(qualityRuleRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Arrays.asList(activeRule, inactiveRule));

            QualityCheckEntity passCheck = buildCheckEntity("R001", "md_patient", "P001", "PASS", null);
            QualityCheckEntity failCheck = buildCheckEntity("R001", "md_patient", "P002", "FAIL", "Missing field: patient_name");
            when(qualityCheckRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Arrays.asList(passCheck, failCheck));

            Map<String, Object> report = qualityReportService.generateReport(TENANT_ID);

            assertNotNull(report);
            assertEquals(TENANT_ID, report.get("tenant_id"));

            Map<?, ?> ruleStats = (Map<?, ?>) report.get("rule_statistics");
            assertEquals(2, ruleStats.get("total"));
            assertEquals(1L, ruleStats.get("active"));
            assertEquals(1L, ruleStats.get("inactive"));

            Map<?, ?> checkStats = (Map<?, ?>) report.get("check_statistics");
            assertEquals(2, checkStats.get("total_checks"));
            assertEquals(1L, checkStats.get("passed"));
            assertEquals(1L, checkStats.get("failed"));
        }

        @Test
        @DisplayName("生成质量报告 - 无检查记录时通过率为0%")
        void generateReport_noChecks_zeroPassRate() {
            when(qualityRuleRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
            when(qualityCheckRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

            Map<String, Object> report = qualityReportService.generateReport(TENANT_ID);

            Map<?, ?> checkStats = (Map<?, ?>) report.get("check_statistics");
            assertEquals("0.00%", checkStats.get("pass_rate"));
        }

        @Test
        @DisplayName("获取监控指标 - 包含今日/本周/本月统计")
        void getMonitorMetrics_containsTimePeriodStats() {
            QualityCheckEntity recentCheck = buildCheckEntity("R001", "md_patient", "P001", "PASS", null);
            when(qualityCheckRepository.findAllByTenantId(TENANT_ID))
                    .thenReturn(Collections.singletonList(recentCheck));

            Map<String, Object> metrics = qualityReportService.getMonitorMetrics(TENANT_ID);

            assertNotNull(metrics.get("today"));
            assertNotNull(metrics.get("week"));
            assertNotNull(metrics.get("month"));
            assertNotNull(metrics.get("trend"));
        }
    }
}
