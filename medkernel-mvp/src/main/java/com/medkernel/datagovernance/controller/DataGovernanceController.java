package com.medkernel.datagovernance.controller;

import com.medkernel.common.ApiResult;
import com.medkernel.common.dataclass.MinimalDisplayService;
import com.medkernel.datagovernance.entity.*;
import com.medkernel.datagovernance.service.*;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据治理API控制器
 *
 * <p>GA-DATA-01：所有返回含 {@link com.medkernel.common.dataclass.Encrypted} 字段实体的接口，
 * 在响应前自动调用 {@link MinimalDisplayService#applyMinimalDisplay} 进行基于角色的脱敏，
 * 确保非授权用户仅看到脱敏数据，持有 VIEW_FULL_PII 权限的用户可查看完整数据。
 */
@Tag(name = "Data Governance")
@RestController
@RequestMapping("/api/data-governance")
public class DataGovernanceController {
    private final DataGovernanceService dataGovernanceService;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final DepartmentService departmentService;
    private final QualityReportService qualityReportService;
    private final OrganizationContextService organizationContextService;
    private final MinimalDisplayService minimalDisplayService;

    public DataGovernanceController(DataGovernanceService dataGovernanceService,
                                    PatientService patientService,
                                    DoctorService doctorService,
                                    DepartmentService departmentService,
                                    QualityReportService qualityReportService,
                                    OrganizationContextService organizationContextService,
                                    MinimalDisplayService minimalDisplayService) {
        this.dataGovernanceService = dataGovernanceService;
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.departmentService = departmentService;
        this.qualityReportService = qualityReportService;
        this.organizationContextService = organizationContextService;
        this.minimalDisplayService = minimalDisplayService;
    }

    /**
     * 获取数据治理概览
     */
    @Operation(summary = "Get overview")
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
    @Operation(summary = "Save patient")
    @PostMapping("/patients")
    public ApiResult<PatientEntity> savePatient(@RequestBody PatientEntity entity,
                                                HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        PatientEntity saved = patientService.save(entity);
        minimalDisplayService.applyMinimalDisplay(saved);
        return ApiResult.success(saved);
    }

    /**
     * 根据患者ID查找患者
     */
    @Operation(summary = "Get patient")
    @GetMapping("/patients/{patientId}")
    public ApiResult<PatientEntity> getPatient(@PathVariable String patientId,
                                               HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        PatientEntity patient = patientService.findByPatientId(tenantId, patientId);
        minimalDisplayService.applyMinimalDisplay(patient);
        return ApiResult.success(patient);
    }

    /**
     * 获取所有患者列表
     */
    @Operation(summary = "List patients")
    @GetMapping("/patients")
    public ApiResult<List<PatientEntity>> listPatients(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        List<PatientEntity> patients = patientService.findAllByTenantId(tenantId);
        for (PatientEntity p : patients) {
            minimalDisplayService.applyMinimalDisplay(p);
        }
        return ApiResult.success(patients);
    }

    // ============================================================================
    // 医生主数据API
    // ============================================================================

    /**
     * 保存医生主数据
     */
    @Operation(summary = "Save doctor")
    @PostMapping("/doctors")
    public ApiResult<DoctorEntity> saveDoctor(@RequestBody DoctorEntity entity,
                                              HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        DoctorEntity saved = doctorService.save(entity);
        minimalDisplayService.applyMinimalDisplay(saved);
        return ApiResult.success(saved);
    }

    /**
     * 根据医生ID查找医生
     */
    @Operation(summary = "Get doctor")
    @GetMapping("/doctors/{doctorId}")
    public ApiResult<DoctorEntity> getDoctor(@PathVariable String doctorId,
                                             HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        DoctorEntity doctor = doctorService.findByDoctorId(tenantId, doctorId);
        minimalDisplayService.applyMinimalDisplay(doctor);
        return ApiResult.success(doctor);
    }

    /**
     * 获取所有医生列表
     */
    @Operation(summary = "List doctors")
    @GetMapping("/doctors")
    public ApiResult<List<DoctorEntity>> listDoctors(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        List<DoctorEntity> doctors = doctorService.findAllByTenantId(tenantId);
        for (DoctorEntity d : doctors) {
            minimalDisplayService.applyMinimalDisplay(d);
        }
        return ApiResult.success(doctors);
    }

    // ============================================================================
    // 科室主数据API
    // ============================================================================

    /**
     * 保存科室主数据
     */
    @Operation(summary = "Save department")
    @PostMapping("/departments")
    public ApiResult<DepartmentEntity> saveDepartment(@RequestBody DepartmentEntity entity,
                                                      HttpServletRequest httpRequest) {
        organizationContextService.resolve(httpRequest);
        return ApiResult.success(departmentService.save(entity));
    }

    /**
     * 根据科室编码查找科室
     */
    @Operation(summary = "Get department")
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
    @Operation(summary = "List departments")
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
    @Operation(summary = "Execute quality check")
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
    @Operation(summary = "Get quality report")
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
    @Operation(summary = "Get quality monitor")
    @GetMapping("/quality/monitor")
    public ApiResult<Map<String, Object>> getQualityMonitor(HttpServletRequest httpRequest) {
        Map<String, String> filters = new LinkedHashMap<>();
        organizationContextService.applyExplicitFilters(filters, httpRequest);
        String tenantId = filters.getOrDefault("tenantId", "default");
        return ApiResult.success(qualityReportService.getMonitorMetrics(tenantId));
    }
}