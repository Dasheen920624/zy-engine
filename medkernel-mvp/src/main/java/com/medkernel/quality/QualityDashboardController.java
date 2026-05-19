package com.medkernel.quality;

import com.medkernel.common.ApiResult;
import com.medkernel.organization.OrganizationContextService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 院级质控驾驶舱 API
 * 提供 4 KPI 聚合、科室排名、钻取数据
 */
@RestController
@RequestMapping("/api/quality/dashboard")
public class QualityDashboardController {
    private final QualityDashboardService dashboardService;
    private final OrganizationContextService organizationContextService;

    public QualityDashboardController(QualityDashboardService dashboardService,
                                       OrganizationContextService organizationContextService) {
        this.dashboardService = dashboardService;
        this.organizationContextService = organizationContextService;
    }

    /**
     * 获取驾驶舱 4 KPI 聚合数据
     * 包含：路径执行、规则命中、质控问题、医保风险
     */
    @GetMapping
    public ApiResult<Map<String, Object>> getDashboard(
            @RequestParam(required = false, defaultValue = "month") String period,
            @RequestParam(required = false) String departmentCode,
            HttpServletRequest request) {
        String tenantId = organizationContextService.resolveTenantId(request);
        return ApiResult.success(dashboardService.getDashboardKpis(tenantId, period, departmentCode));
    }

    /**
     * 获取科室排名列表
     */
    @GetMapping("/departments")
    public ApiResult<Map<String, Object>> getDepartmentRanking(
            @RequestParam(required = false, defaultValue = "month") String period,
            HttpServletRequest request) {
        String tenantId = organizationContextService.resolveTenantId(request);
        return ApiResult.success(dashboardService.getDepartmentRanking(tenantId, period));
    }

    /**
     * 科室钻取详情
     */
    @GetMapping("/department/{deptCode}")
    public ApiResult<Map<String, Object>> getDepartmentDetail(
            @PathVariable String deptCode,
            @RequestParam(required = false, defaultValue = "month") String period,
            HttpServletRequest request) {
        String tenantId = organizationContextService.resolveTenantId(request);
        return ApiResult.success(dashboardService.getDepartmentDetail(tenantId, deptCode, period));
    }

    /**
     * 趋势数据（最近 N 天）
     */
    @GetMapping("/trend")
    public ApiResult<Map<String, Object>> getTrend(
            @RequestParam(required = false, defaultValue = "30") int days,
            @RequestParam(required = false) String departmentCode,
            HttpServletRequest request) {
        String tenantId = organizationContextService.resolveTenantId(request);
        return ApiResult.success(dashboardService.getTrendData(tenantId, days, departmentCode));
    }
}
