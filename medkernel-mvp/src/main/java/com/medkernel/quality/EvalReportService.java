package com.medkernel.quality;

import com.medkernel.organization.OrganizationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评估报告、复核和整改闭环服务。
 * 支持评估报告生成/导出、人工复核、整改任务创建/跟踪、再评估和归档。
 */
@Service
public class EvalReportService {
    private final EvalScoringService evalScoringService;

    private static final AtomicLong REPORT_SEQ = new AtomicLong(1);
    private static final AtomicLong REVIEW_SEQ = new AtomicLong(1);
    private static final AtomicLong RECTIFICATION_SEQ = new AtomicLong(1);

    private final Map<String, EvalReport> reportStore = new ConcurrentHashMap<String, EvalReport>();
    private final Map<String, EvalReview> reviewStore = new ConcurrentHashMap<String, EvalReview>();
    private final Map<String, EvalRectification> rectificationStore = new ConcurrentHashMap<String, EvalRectification>();

    public EvalReportService(EvalScoringService evalScoringService) {
        this.evalScoringService = evalScoringService;
    }

    // ==================== 评估报告 ====================

    /**
     * 基于评估结果生成评估报告。
     */
    public EvalReport generateReport(String evalId, OrganizationContext orgContext) {
        EvalResult result = evalScoringService.getResult(evalId, orgContext);
        if (result == null) {
            throw new IllegalArgumentException("Evaluation result not found: " + evalId);
        }

        EvalReport report = new EvalReport();
        report.setReportId("EVAL-RPT-" + String.format("%04d", REPORT_SEQ.getAndIncrement()));
        report.setEvalId(evalId);
        report.setTenantId(orgContext.getTenantId());
        report.setSetCode(result.getSetCode());
        report.setSubjectType(result.getSubjectType());
        report.setSubjectId(result.getSubjectId());
        report.setSubjectName(result.getSubjectName());
        report.setTotalScore(result.getTotalScore());
        report.setMaxPossibleScore(result.getMaxPossibleScore());
        report.setRiskLevel(result.getRiskLevel());
        report.setAbnormalFacts(result.getAbnormalFacts());
        report.setMissingFacts(result.getMissingFacts());
        report.setIndicatorScores(result.getIndicatorScores());
        report.setStatus("DRAFT");
        report.setGeneratedAt(LocalDateTime.now().toString());
        report.setOrgContext(buildOrgContextView(orgContext));

        // 生成整改建议
        report.setRecommendations(generateRecommendations(result));

        reportStore.put(report.getReportId(), report);
        return report;
    }

    /**
     * 导出评估报告（结构化 Map）。
     */
    public Map<String, Object> exportReport(String reportId, OrganizationContext orgContext) {
        EvalReport report = getReport(reportId, orgContext);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        return report.toExportView();
    }

    /**
     * 获取评估报告。
     */
    public EvalReport getReport(String reportId, OrganizationContext orgContext) {
        EvalReport report = reportStore.get(reportId);
        if (report == null || !orgContext.getTenantId().equals(report.getTenantId())) {
            return null;
        }
        return report;
    }

    /**
     * 列出评估报告。
     */
    public List<EvalReport> listReports(String status, String subjectType, OrganizationContext orgContext) {
        List<EvalReport> reports = new ArrayList<EvalReport>();
        for (EvalReport report : reportStore.values()) {
            if (!orgContext.getTenantId().equals(report.getTenantId())) continue;
            if (status != null && !status.equals(report.getStatus())) continue;
            if (subjectType != null && !subjectType.equalsIgnoreCase(report.getSubjectType())) continue;
            reports.add(report);
        }
        return reports;
    }

    /**
     * 归档评估报告。
     */
    public EvalReport archiveReport(String reportId, OrganizationContext orgContext) {
        EvalReport report = getReport(reportId, orgContext);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }
        if (!"REVIEWED".equals(report.getStatus())) {
            throw new IllegalArgumentException("Report must be REVIEWED before archiving. Current: " + report.getStatus());
        }
        report.setStatus("ARCHIVED");
        report.setArchivedAt(LocalDateTime.now().toString());
        return report;
    }

    // ==================== 人工复核 ====================

    /**
     * 提交复核意见。
     */
    public EvalReview submitReview(String reportId, Map<String, Object> request, OrganizationContext orgContext) {
        EvalReport report = getReport(reportId, orgContext);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        EvalReview review = new EvalReview();
        review.setReviewId("EVAL-REV-" + String.format("%04d", REVIEW_SEQ.getAndIncrement()));
        review.setReportId(reportId);
        review.setTenantId(orgContext.getTenantId());
        review.setReviewerId((String) request.getOrDefault("reviewer_id", "unknown"));
        review.setReviewerName((String) request.getOrDefault("reviewer_name", "unknown"));
        review.setReviewResult((String) request.getOrDefault("review_result", "APPROVED"));
        review.setReviewComment((String) request.getOrDefault("review_comment", ""));
        review.setReviewedAt(LocalDateTime.now().toString());

        // 校验复核结果
        if (!"APPROVED".equals(review.getReviewResult()) && !"REJECTED".equals(review.getReviewResult())
                && !"CONDITIONALLY_APPROVED".equals(review.getReviewResult())) {
            throw new IllegalArgumentException("Invalid review_result: must be APPROVED, REJECTED or CONDITIONALLY_APPROVED");
        }

        // 更新报告状态
        if ("APPROVED".equals(review.getReviewResult())) {
            report.setStatus("REVIEWED");
        } else if ("REJECTED".equals(review.getReviewResult())) {
            report.setStatus("REVIEW_REJECTED");
        } else {
            report.setStatus("CONDITIONALLY_APPROVED");
        }

        reviewStore.put(review.getReviewId(), review);
        return review;
    }

    /**
     * 列出报告的复核记录。
     */
    public List<EvalReview> listReviews(String reportId, OrganizationContext orgContext) {
        List<EvalReview> reviews = new ArrayList<EvalReview>();
        for (EvalReview review : reviewStore.values()) {
            if (review.getReportId().equals(reportId)
                    && orgContext.getTenantId().equals(review.getTenantId())) {
                reviews.add(review);
            }
        }
        return reviews;
    }

    // ==================== 整改任务 ====================

    /**
     * 基于评估报告创建整改任务。
     */
    public EvalRectification createRectification(String reportId, Map<String, Object> request, OrganizationContext orgContext) {
        EvalReport report = getReport(reportId, orgContext);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        EvalRectification rect = new EvalRectification();
        rect.setRectId("EVAL-RECT-" + String.format("%04d", RECTIFICATION_SEQ.getAndIncrement()));
        rect.setReportId(reportId);
        rect.setTenantId(orgContext.getTenantId());
        rect.setTitle((String) request.getOrDefault("title", "整改任务"));
        rect.setDescription((String) request.getOrDefault("description", ""));
        rect.setAssigneeId((String) request.getOrDefault("assignee_id", ""));
        rect.setAssigneeName((String) request.getOrDefault("assignee_name", ""));
        rect.setPriority((String) request.getOrDefault("priority", "MEDIUM"));
        rect.setDueDate((String) request.getOrDefault("due_date", ""));
        rect.setRelatedFacts((String) request.getOrDefault("related_facts", ""));
        rect.setStatus("PENDING");
        rect.setCreatedAt(LocalDateTime.now().toString());
        rect.setOrgContext(buildOrgContextView(orgContext));

        rectificationStore.put(rect.getRectId(), rect);
        return rect;
    }

    /**
     * 基于评估报告自动生成整改任务（从异常和缺失事实中提取）。
     */
    public List<EvalRectification> autoCreateRectifications(String reportId, OrganizationContext orgContext) {
        EvalReport report = getReport(reportId, orgContext);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        List<EvalRectification> created = new ArrayList<EvalRectification>();

        // 从异常事实生成整改任务
        if (report.getAbnormalFacts() != null) {
            for (EvalResult.EvalFact fact : report.getAbnormalFacts()) {
                Map<String, Object> req = new LinkedHashMap<String, Object>();
                req.put("title", "整改异常指标：" + fact.getIndicatorName());
                req.put("description", fact.getDescription());
                req.put("priority", "HIGH".equals(fact.getSeverity()) || "CRITICAL".equals(fact.getSeverity()) ? "HIGH" : "MEDIUM");
                req.put("related_facts", fact.getIndicatorCode() + ":" + fact.getFactType());
                created.add(createRectification(reportId, req, orgContext));
            }
        }

        // 从缺失事实生成整改任务
        if (report.getMissingFacts() != null) {
            for (EvalResult.EvalFact fact : report.getMissingFacts()) {
                Map<String, Object> req = new LinkedHashMap<String, Object>();
                req.put("title", "补齐缺失指标：" + fact.getIndicatorName());
                req.put("description", fact.getDescription());
                req.put("priority", "HIGH");
                req.put("related_facts", fact.getIndicatorCode() + ":" + fact.getFactType());
                created.add(createRectification(reportId, req, orgContext));
            }
        }

        return created;
    }

    /**
     * 更新整改任务状态。
     */
    public EvalRectification updateRectificationStatus(String rectId, Map<String, Object> request, OrganizationContext orgContext) {
        EvalRectification rect = rectificationStore.get(rectId);
        if (rect == null || !orgContext.getTenantId().equals(rect.getTenantId())) {
            throw new IllegalArgumentException("Rectification task not found: " + rectId);
        }

        String newStatus = (String) request.get("status");
        if (newStatus == null) {
            throw new IllegalArgumentException("status is required");
        }

        // 状态流转校验
        String currentStatus = rect.getStatus();
        if ("COMPLETED".equals(currentStatus)) {
            throw new IllegalArgumentException("Completed task cannot be updated");
        }

        if ("IN_PROGRESS".equals(newStatus) && !"PENDING".equals(currentStatus)) {
            throw new IllegalArgumentException("Only PENDING task can start. Current: " + currentStatus);
        }
        if ("COMPLETED".equals(newStatus) && !"IN_PROGRESS".equals(currentStatus)) {
            throw new IllegalArgumentException("Only IN_PROGRESS task can complete. Current: " + currentStatus);
        }

        rect.setStatus(newStatus);
        rect.setUpdatedBy((String) request.getOrDefault("updated_by", ""));
        rect.setUpdateNote((String) request.getOrDefault("update_note", ""));
        rect.setUpdatedAt(LocalDateTime.now().toString());

        if ("COMPLETED".equals(newStatus)) {
            rect.setCompletedAt(LocalDateTime.now().toString());
        }

        return rect;
    }

    /**
     * 获取整改任务。
     */
    public EvalRectification getRectification(String rectId, OrganizationContext orgContext) {
        EvalRectification rect = rectificationStore.get(rectId);
        if (rect == null || !orgContext.getTenantId().equals(rect.getTenantId())) {
            return null;
        }
        return rect;
    }

    /**
     * 列出整改任务。
     */
    public List<EvalRectification> listRectifications(String reportId, String status, OrganizationContext orgContext) {
        List<EvalRectification> results = new ArrayList<EvalRectification>();
        for (EvalRectification rect : rectificationStore.values()) {
            if (!orgContext.getTenantId().equals(rect.getTenantId())) continue;
            if (reportId != null && !reportId.equals(rect.getReportId())) continue;
            if (status != null && !status.equals(rect.getStatus())) continue;
            results.add(rect);
        }
        return results;
    }

    // ==================== 再评估 ====================

    /**
     * 基于原评估结果执行再评估。
     */
    public EvalResult reEvaluate(String evalId, Map<String, Object> inputData, OrganizationContext orgContext) {
        EvalResult original = evalScoringService.getResult(evalId, orgContext);
        if (original == null) {
            throw new IllegalArgumentException("Original evaluation result not found: " + evalId);
        }

        // 使用原指标集和新的输入数据重新评估
        return evalScoringService.evaluate(
                original.getSetCode(),
                original.getSubjectId(),
                original.getSubjectName(),
                inputData,
                orgContext
        );
    }

    // ==================== 辅助方法 ====================

    private List<String> generateRecommendations(EvalResult result) {
        List<String> recommendations = new ArrayList<String>();

        // 基于风险等级生成建议
        if ("CRITICAL".equals(result.getRiskLevel())) {
            recommendations.add("评估结果为严重风险，建议立即启动整改流程");
        } else if ("HIGH".equals(result.getRiskLevel())) {
            recommendations.add("评估结果为高风险，建议限期整改");
        } else if ("MEDIUM".equals(result.getRiskLevel())) {
            recommendations.add("评估结果为中等风险，建议关注并持续改进");
        }

        // 基于异常事实生成建议
        if (result.getAbnormalFacts() != null && !result.getAbnormalFacts().isEmpty()) {
            recommendations.add("共 " + result.getAbnormalFacts().size() + " 项指标异常，建议逐项分析原因并制定整改措施");
        }

        // 基于缺失事实生成建议
        if (result.getMissingFacts() != null && !result.getMissingFacts().isEmpty()) {
            recommendations.add("共 " + result.getMissingFacts().size() + " 项指标数据缺失，建议补齐数据后重新评估");
        }

        // 基于得分百分比生成建议
        double percentage = result.getMaxPossibleScore() > 0
                ? (result.getTotalScore() / result.getMaxPossibleScore()) * 100 : 0;
        if (percentage < 60) {
            recommendations.add("综合得分低于 60%，建议全面整改后重新评估");
        } else if (percentage < 80) {
            recommendations.add("综合得分低于 80%，建议针对薄弱环节重点改进");
        }

        return recommendations;
    }

    private Map<String, Object> buildOrgContextView(OrganizationContext ctx) {
        Map<String, Object> view = new LinkedHashMap<String, Object>();
        view.put("group_code", ctx.getGroupCode());
        view.put("hospital_code", ctx.getHospitalCode());
        view.put("campus_code", ctx.getCampusCode());
        view.put("site_code", ctx.getSiteCode());
        view.put("department_code", ctx.getDepartmentCode());
        view.put("scope_level", ctx.getScopeLevel());
        view.put("scope_code", ctx.getScopeCode());
        view.put("org_source", ctx.getOrgSource());
        return view;
    }

    // ==================== 内部模型 ====================

    /**
     * 评估报告。
     */
    public static class EvalReport {
        private String reportId;
        private String evalId;
        private String tenantId;
        private String setCode;
        private String subjectType;
        private String subjectId;
        private String subjectName;
        private double totalScore;
        private double maxPossibleScore;
        private String riskLevel;
        private List<EvalResult.IndicatorScore> indicatorScores;
        private List<EvalResult.EvalFact> abnormalFacts;
        private List<EvalResult.EvalFact> missingFacts;
        private List<String> recommendations;
        private String status; // DRAFT, REVIEWED, REVIEW_REJECTED, CONDITIONALLY_APPROVED, ARCHIVED
        private String generatedAt;
        private String archivedAt;
        private Map<String, Object> orgContext;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("report_id", reportId);
            view.put("eval_id", evalId);
            view.put("tenant_id", tenantId);
            view.put("set_code", setCode);
            view.put("subject_type", subjectType);
            view.put("subject_id", subjectId);
            view.put("subject_name", subjectName);
            view.put("total_score", totalScore);
            view.put("max_possible_score", maxPossibleScore);
            view.put("score_percentage", maxPossibleScore > 0 ? Math.round(totalScore / maxPossibleScore * 1000.0) / 10.0 : 0);
            view.put("risk_level", riskLevel);
            view.put("recommendations", recommendations);
            view.put("status", status);
            view.put("generated_at", generatedAt);
            view.put("archived_at", archivedAt);
            view.put("abnormal_fact_count", abnormalFacts != null ? abnormalFacts.size() : 0);
            view.put("missing_fact_count", missingFacts != null ? missingFacts.size() : 0);
            view.put("org_context", orgContext);
            return view;
        }

        public Map<String, Object> toExportView() {
            Map<String, Object> view = toView();
            if (indicatorScores != null) {
                List<Map<String, Object>> scoreViews = new ArrayList<Map<String, Object>>();
                for (EvalResult.IndicatorScore score : indicatorScores) {
                    scoreViews.add(score.toView());
                }
                view.put("indicator_scores", scoreViews);
            }
            if (abnormalFacts != null) {
                List<Map<String, Object>> factViews = new ArrayList<Map<String, Object>>();
                for (EvalResult.EvalFact fact : abnormalFacts) {
                    factViews.add(fact.toView());
                }
                view.put("abnormal_facts", factViews);
            }
            if (missingFacts != null) {
                List<Map<String, Object>> factViews = new ArrayList<Map<String, Object>>();
                for (EvalResult.EvalFact fact : missingFacts) {
                    factViews.add(fact.toView());
                }
                view.put("missing_facts", factViews);
            }
            return view;
        }

        // Getters and Setters
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getEvalId() { return evalId; }
        public void setEvalId(String evalId) { this.evalId = evalId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getSetCode() { return setCode; }
        public void setSetCode(String setCode) { this.setCode = setCode; }
        public String getSubjectType() { return subjectType; }
        public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
        public String getSubjectId() { return subjectId; }
        public void setSubjectId(String subjectId) { this.subjectId = subjectId; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
        public double getMaxPossibleScore() { return maxPossibleScore; }
        public void setMaxPossibleScore(double maxPossibleScore) { this.maxPossibleScore = maxPossibleScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public List<EvalResult.IndicatorScore> getIndicatorScores() { return indicatorScores; }
        public void setIndicatorScores(List<EvalResult.IndicatorScore> indicatorScores) { this.indicatorScores = indicatorScores; }
        public List<EvalResult.EvalFact> getAbnormalFacts() { return abnormalFacts; }
        public void setAbnormalFacts(List<EvalResult.EvalFact> abnormalFacts) { this.abnormalFacts = abnormalFacts; }
        public List<EvalResult.EvalFact> getMissingFacts() { return missingFacts; }
        public void setMissingFacts(List<EvalResult.EvalFact> missingFacts) { this.missingFacts = missingFacts; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(String generatedAt) { this.generatedAt = generatedAt; }
        public String getArchivedAt() { return archivedAt; }
        public void setArchivedAt(String archivedAt) { this.archivedAt = archivedAt; }
        public Map<String, Object> getOrgContext() { return orgContext; }
        public void setOrgContext(Map<String, Object> orgContext) { this.orgContext = orgContext; }
    }

    /**
     * 评估复核记录。
     */
    public static class EvalReview {
        private String reviewId;
        private String reportId;
        private String tenantId;
        private String reviewerId;
        private String reviewerName;
        private String reviewResult; // APPROVED, REJECTED, CONDITIONALLY_APPROVED
        private String reviewComment;
        private String reviewedAt;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("review_id", reviewId);
            view.put("report_id", reportId);
            view.put("tenant_id", tenantId);
            view.put("reviewer_id", reviewerId);
            view.put("reviewer_name", reviewerName);
            view.put("review_result", reviewResult);
            view.put("review_comment", reviewComment);
            view.put("reviewed_at", reviewedAt);
            return view;
        }

        // Getters and Setters
        public String getReviewId() { return reviewId; }
        public void setReviewId(String reviewId) { this.reviewId = reviewId; }
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getReviewerId() { return reviewerId; }
        public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
        public String getReviewerName() { return reviewerName; }
        public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
        public String getReviewResult() { return reviewResult; }
        public void setReviewResult(String reviewResult) { this.reviewResult = reviewResult; }
        public String getReviewComment() { return reviewComment; }
        public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
        public String getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }
    }

    /**
     * 整改任务。
     */
    public static class EvalRectification {
        private String rectId;
        private String reportId;
        private String tenantId;
        private String title;
        private String description;
        private String assigneeId;
        private String assigneeName;
        private String priority; // HIGH, MEDIUM, LOW
        private String dueDate;
        private String relatedFacts;
        private String status; // PENDING, IN_PROGRESS, COMPLETED
        private String updatedBy;
        private String updateNote;
        private String createdAt;
        private String updatedAt;
        private String completedAt;
        private Map<String, Object> orgContext;

        public Map<String, Object> toView() {
            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("rect_id", rectId);
            view.put("report_id", reportId);
            view.put("tenant_id", tenantId);
            view.put("title", title);
            view.put("description", description);
            view.put("assignee_id", assigneeId);
            view.put("assignee_name", assigneeName);
            view.put("priority", priority);
            view.put("due_date", dueDate);
            view.put("related_facts", relatedFacts);
            view.put("status", status);
            view.put("updated_by", updatedBy);
            view.put("update_note", updateNote);
            view.put("created_at", createdAt);
            view.put("updated_at", updatedAt);
            view.put("completed_at", completedAt);
            view.put("org_context", orgContext);
            return view;
        }

        // Getters and Setters
        public String getRectId() { return rectId; }
        public void setRectId(String rectId) { this.rectId = rectId; }
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAssigneeId() { return assigneeId; }
        public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }
        public String getAssigneeName() { return assigneeName; }
        public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }
        public String getRelatedFacts() { return relatedFacts; }
        public void setRelatedFacts(String relatedFacts) { this.relatedFacts = relatedFacts; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getUpdatedBy() { return updatedBy; }
        public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
        public String getUpdateNote() { return updateNote; }
        public void setUpdateNote(String updateNote) { this.updateNote = updateNote; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
        public Map<String, Object> getOrgContext() { return orgContext; }
        public void setOrgContext(Map<String, Object> orgContext) { this.orgContext = orgContext; }
    }
}
