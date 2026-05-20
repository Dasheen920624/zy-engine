package com.medkernel.datagovernance.controller;

import com.medkernel.common.ApiResult;
import com.medkernel.datagovernance.entity.*;
import com.medkernel.datagovernance.service.*;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据治理API控制器
 */
@RestController
@RequestMapping("/api/data-governance")
public class DataGovernanceController {
    private final DataGovernanceService dataGovernanceService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;
    private final QualityReportService qualityReportService;
    private final OrganizationContextService organizationContextService;

    public DataGovernanceController(DataGovernanceService dataGovernanceService,
                                    PatientService patientService,
                                    DoctorService doctorService,
                                    DepartmentService departmentService,
                                    QualityReportService qualityReportService,
                                    OrganizationContextService organizationContextService) {
        this.dataGovernanceService = dataGovernanceService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.qualityReportService = qualityReportService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 获取数据治理概览
     */
    @GetMapping("/overview")
    public ApiResult<Map<String, Object>> getOverview(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(dataGovernanceService.getOverview(tenantId));
    }

    // ============================================================================
    // 患者主数据API
    // ============================================================================

    /**
     * 保存患者主数据
     */
    @PostMapping("/patients")
    public ApiResult<PatientEntity> savePatient(@RequestBody PatientEntity entity,
                                                HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(patientService.save(entity));
    }

    /**
     * 根据患者ID查找患者
     */
    @GetMapping("/patients/{patientId}")
    public ApiResult<PatientEntity> getPatient(@PathVariable String patientId,
                                               HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(patientService.findByPatientId(tenantId, patientId));
    }

    /**
     * 获取所有患者列表
     */
    @GetMapping("/patients")
    public ApiResult<List<PatientEntity>> listPatients(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(patientService.findAllByTenantId(tenantId));
    }

    // ============================================================================
    // 医生主数据API
    // ============================================================================

    /**
     * 保存医生主数据
     */
    @PostMapping("/doctors")
    public ApiResult<DoctorEntity> saveDoctor(@RequestBody DoctorEntity entity,
                                              HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(doctorService.save(entity));
    }

    /**
     * 根据医生ID查找医生
     */
    @GetMapping("/doctors/{doctorId}")
    public ApiResult<DoctorEntity> getDoctor(@PathVariable String doctorId,
                                             HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(doctorService.findByDoctorId(tenantId, doctorId));
    }

    /**
     * 获取所有医生列表
     */
    @GetMapping("/doctors")
    public ApiResult<List<DoctorEntity>> listDoctors(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(doctorService.findAllByTenantId(tenantId));
    }

    // ============================================================================
    // 科室主数据API
    // ============================================================================

    /**
     * 保存科室主数据
     */
    @PostMapping("/departments")
    public ApiResult<DepartmentEntity> saveDepartment(@RequestBody DepartmentEntity entity,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(departmentService.save(entity));
    }

    /**
     * 根据科室编码查找科室
     */
    @GetMapping("/departments/{deptCode}")
    public ApiResult<DepartmentEntity> getDepartment(@PathVariable String deptCode,
                                                     HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(departmentService.findByDeptCode(tenantId, deptCode));
    }

    /**
     * 获取所有科室列表
     */
    @GetMapping("/departments")
    public ApiResult<List<DepartmentEntity>> listDepartments(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(departmentService.findAllByTenantId(tenantId));
    }

    // ============================================================================
    // 数据质量API
    // ============================================================================

    /**
     * 执行数据质量检查
     */
    @PostMapping("/quality/check/{ruleCode}")
    public ApiResult<Map<String, Object>> executeQualityCheck(@PathVariable String ruleCode,
                                                              HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(dataGovernanceService.executeQualityCheck(tenantId, ruleCode));
    }

    /**
     * 获取数据质量报告
     */
    @GetMapping("/quality/report")
    public ApiResult<Map<String, Object>> getQualityReport(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(qualityReportService.generateReport(tenantId));
    }

    /**
     * 获取数据质量监控指标
     */
    @GetMapping("/quality/monitor")
    public ApiResult<Map<String, Object>> getQualityMonitor(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(qualityReportService.getMonitorMetrics(tenantId));
    }
}