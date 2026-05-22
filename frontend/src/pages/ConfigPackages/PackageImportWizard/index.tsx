import { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Space, Steps, message } from "antd";
import { ArrowLeftOutlined } from "@ant-design/icons";
import { useNavigate, useSearchParams } from "react-router-dom";
import type { ConfigPackageDetail, ImportImpactResult, ImportSourceCheckResult, ImportUploadResult, ImportValidateResult } from "@/api/types";
import Step1Upload from "./steps/Step1Upload";
import Step2Validate from "./steps/Step2Validate";
import Step3SourceCheck from "./steps/Step3SourceCheck";
import Step4Impact from "./steps/Step4Impact";
import Step5Confirm from "./steps/Step5Confirm";
import {
  INITIAL_WIZARD_CONTEXT,
  WIZARD_STEPS,
  clearDraft,
  loadDraft,
  saveDraft,
} from "./types";
import type { WizardContext } from "./types";
import styles from "./PackageImportWizard.module.css";

export default function PackageImportWizard() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const stepFromUrl = parseInt(searchParams.get("step") || "1", 10);
  const [currentStep, setCurrentStep] = useState(() =>
    Math.max(1, Math.min(5, stepFromUrl || 1)),
  );

  const [context, setContext] = useState<WizardContext>(() => {
    const draft = loadDraft();
    if (draft && draft.uploadId) {
      return { ...INITIAL_WIZARD_CONTEXT, ...draft };
    }
    return { ...INITIAL_WIZARD_CONTEXT };
  });

  // 同步步骤到 URL
  useEffect(() => {
    setSearchParams({ step: String(currentStep) }, { replace: true });
  }, [currentStep, setSearchParams]);

  // 保存草稿
  useEffect(() => {
    if (context.uploadId) {
      saveDraft(context);
    }
  }, [context]);

  const canGoNext = useMemo(() => {
    switch (currentStep) {
      case 1:
        return !!context.uploadResult;
      case 2:
        return !!context.validateResult?.valid;
      case 3:
        return !!context.sourceCheckResult && context.sourceCheckResult.source_review.missing_count === 0;
      case 4:
        return !!context.impactResult;
      case 5:
        return false;
      default:
        return false;
    }
  }, [currentStep, context]);

  const handleNext = useCallback(() => {
    if (currentStep < 5 && canGoNext) {
      setCurrentStep((s) => s + 1);
    }
  }, [currentStep, canGoNext]);

  const handlePrev = useCallback(() => {
    if (currentStep > 1) {
      setCurrentStep((s) => s - 1);
    }
  }, [currentStep]);

  const handleUploadSuccess = useCallback((result: ImportUploadResult) => {
    setContext((prev) => ({
      ...prev,
      uploadId: result.upload_id,
      uploadResult: result,
    }));
  }, []);

  const handleValidateComplete = useCallback((result: ImportValidateResult) => {
    setContext((prev) => ({
      ...prev,
      validateResult: result,
    }));
  }, []);

  const handleSourceCheckComplete = useCallback((result: ImportSourceCheckResult) => {
    setContext((prev) => ({
      ...prev,
      sourceCheckResult: result,
    }));
  }, []);

  const handleImpactComplete = useCallback((result: ImportImpactResult) => {
    setContext((prev) => ({
      ...prev,
      impactResult: result,
    }));
  }, []);

  const handleTargetEnvironmentChange = useCallback((env: string) => {
    setContext((prev) => ({
      ...prev,
      targetEnvironment: env,
      impactResult: null,
    }));
  }, []);

  const handlePublishSuccess = useCallback((_result: ConfigPackageDetail) => {
    clearDraft();
    message.success("配置包发布成功！");
    navigate("/config/packages");
  }, [navigate]);

  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <Step1Upload
            context={context}
            onUploadSuccess={handleUploadSuccess}
          />
        );
      case 2:
        return (
          <Step2Validate
            context={context}
            onValidateComplete={handleValidateComplete}
          />
        );
      case 3:
        return (
          <Step3SourceCheck
            context={context}
            onSourceCheckComplete={handleSourceCheckComplete}
          />
        );
      case 4:
        return (
          <Step4Impact
            context={context}
            onImpactComplete={handleImpactComplete}
            onTargetEnvironmentChange={handleTargetEnvironmentChange}
          />
        );
      case 5:
        return (
          <Step5Confirm
            context={context}
            onPublishSuccess={handlePublishSuccess}
          />
        );
      default:
        return null;
    }
  };

  return (
    <div>
      {/* 头部 */}
      <div className={styles.header}>
        <div className={styles.titleGroup}>
          <Button
            icon={<ArrowLeftOutlined />}
            onClick={() => navigate("/config/packages")}
          >
            返回列表
          </Button>
          <h1 className={styles.title}>
            配置包导入
          </h1>
        </div>
      </div>

      {/* 步骤条 */}
      <Steps
        current={currentStep - 1}
        className={styles.steps}
        items={WIZARD_STEPS.map((s) => ({
          title: s.title,
          description: s.description,
        }))}
      />

      {/* 步骤内容 */}
      <Card className={styles.stepCard}>
        {renderStep()}
      </Card>

      {/* 底部导航 */}
      <div className={styles.footer}>
        <Button
          onClick={handlePrev}
          disabled={currentStep <= 1}
        >
          上一步
        </Button>
        <Space>
          {currentStep < 5 && (
            <Button
              type="primary"
              onClick={handleNext}
              disabled={!canGoNext}
            >
              下一步
            </Button>
          )}
        </Space>
      </div>
    </div>
  );
}
