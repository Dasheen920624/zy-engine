import { describe, it, expect, vi, beforeEach } from "vitest";
import { resetMocks } from "./testUtils";

vi.mock("../client", () => ({
  http: {
    get: (...args: unknown[]) => vi.fn(...args),
    post: (...args: unknown[]) => vi.fn(...args),
    put: (...args: unknown[]) => vi.fn(...args),
    delete: (...args: unknown[]) => vi.fn(...args),
    patch: (...args: unknown[]) => vi.fn(...args),
  },
  get: (...args: unknown[]) => vi.fn(...args),
  post: (...args: unknown[]) => vi.fn(...args),
  put: (...args: unknown[]) => vi.fn(...args),
  del: (...args: unknown[]) => vi.fn(),
}));

import * as adapterHub from "../adapterHub";
import * as aiWorkflows from "../aiWorkflows";
import * as configPackage from "../configPackage";
import * as graph from "../graph";
import * as provenance from "../provenance";
import * as securityBaseline from "../securityBaseline";
import * as auditLog from "../auditLog";
import * as userAdmin from "../userAdmin";
import * as system from "../system";
import * as evalApi from "../eval";
import * as clinicalEvent from "../clinicalEvent";
import * as sso from "../sso";
import * as tenantOnboarding from "../tenantOnboarding";
import * as identityBinding from "../identityBinding";
import * as ruleActionLog from "../ruleActionLog";
import * as terminology from "../terminology";
import * as aiCandidateReview from "../aiCandidateReview";
import * as workflow from "../workflow";

beforeEach(resetMocks);

describe("adapterHub", () => {
  it("should export API functions", () => {
    expect(typeof adapterHub).toBe("object");
    expect(typeof adapterHub.listAdapterDefinitions).toBe("function");
    expect(typeof adapterHub.getAdapterDefinition).toBe("function");
    expect(typeof adapterHub.listInteropAdapters).toBe("function");
    expect(typeof adapterHub.listCdsHooksServices).toBe("function");
    expect(typeof adapterHub.listSmartApps).toBe("function");
    expect(typeof adapterHub.listTriggerPoints).toBe("function");
    expect(typeof adapterHub.registerTriggerPoint).toBe("function");
    expect(typeof adapterHub.updateTriggerPoint).toBe("function");
    expect(typeof adapterHub.executeTriggerPoint).toBe("function");
  });
});

describe("aiWorkflows", () => {
  it("should export API functions", () => {
    expect(typeof aiWorkflows).toBe("object");
    expect(typeof aiWorkflows.listProviders).toBe("function");
    expect(typeof aiWorkflows.getProviderStatus).toBe("function");
    expect(typeof aiWorkflows.listDegradationChains).toBe("function");
    expect(typeof aiWorkflows.getDegradationChain).toBe("function");
    expect(typeof aiWorkflows.invokeModelGateway).toBe("function");
    expect(typeof aiWorkflows.listWorkflowTemplates).toBe("function");
    expect(typeof aiWorkflows.getWorkflowTemplate).toBe("function");
    expect(typeof aiWorkflows.runWorkflow).toBe("function");
    expect(typeof aiWorkflows.workflowInvocationStats).toBe("function");
  });
});

describe("configPackage", () => {
  it("should export API functions", () => {
    expect(typeof configPackage).toBe("object");
    expect(typeof configPackage.listPackages).toBe("function");
    expect(typeof configPackage.getPackageDetail).toBe("function");
    expect(typeof configPackage.reviewPackage).toBe("function");
    expect(typeof configPackage.publishPackage).toBe("function");
    expect(typeof configPackage.exportPackage).toBe("function");
    expect(typeof configPackage.importPackageUpload).toBe("function");
    expect(typeof configPackage.importPackageValidate).toBe("function");
    expect(typeof configPackage.importPackageSourceCheck).toBe("function");
    expect(typeof configPackage.importPackageImpact).toBe("function");
    expect(typeof configPackage.importPackageConfirm).toBe("function");
  });
});

describe("graph", () => {
  it("should export API functions", () => {
    expect(typeof graph).toBe("object");
    expect(typeof graph.getDiseaseCandidates).toBe("function");
    expect(typeof graph.getEvidence).toBe("function");
    expect(typeof graph.listEvidences).toBe("function");
    expect(typeof graph.getEvidenceById).toBe("function");
    expect(typeof graph.listGraphVersions).toBe("function");
    expect(typeof graph.activateGraphVersion).toBe("function");
    expect(typeof graph.listNodes).toBe("function");
    expect(typeof graph.listEdges).toBe("function");
    expect(typeof graph.createNode).toBe("function");
    expect(typeof graph.createEdge).toBe("function");
  });
});

describe("provenance", () => {
  it("should export API functions", () => {
    expect(typeof provenance).toBe("object");
    expect(typeof provenance.listSourceDocuments).toBe("function");
    expect(typeof provenance.getSourceDocument).toBe("function");
    expect(typeof provenance.listCitations).toBe("function");
    expect(typeof provenance.getCitation).toBe("function");
    expect(typeof provenance.getCitationsByDocument).toBe("function");
    expect(typeof provenance.listBindings).toBe("function");
    expect(typeof provenance.getBinding).toBe("function");
    expect(typeof provenance.getBindingsByAsset).toBe("function");
    expect(typeof provenance.getBindingsByDocument).toBe("function");
  });
});

describe("securityBaseline", () => {
  it("should export API functions", () => {
    expect(typeof securityBaseline).toBe("object");
    expect(typeof securityBaseline.getAuditChainStatus).toBe("function");
    expect(typeof securityBaseline.verifyAuditChain).toBe("function");
    expect(typeof securityBaseline.listKeyVersions).toBe("function");
    expect(typeof securityBaseline.getActiveKey).toBe("function");
    expect(typeof securityBaseline.rotateKey).toBe("function");
    expect(typeof securityBaseline.revokeKey).toBe("function");
    expect(typeof securityBaseline.getSecurityBaseline).toBe("function");
    expect(typeof securityBaseline.performVulnerabilityScan).toBe("function");
  });
});

describe("auditLog", () => {
  it("should export API functions", () => {
    expect(typeof auditLog).toBe("object");
    expect(typeof auditLog.listAuditLogs).toBe("function");
    expect(typeof auditLog.summarizeAuditLogs).toBe("function");
    expect(typeof auditLog.verifyAuditChain).toBe("function");
    expect(typeof auditLog.getAuditChainStatus).toBe("function");
  });
});

describe("userAdmin", () => {
  it("should export API functions", () => {
    expect(typeof userAdmin).toBe("object");
    expect(typeof userAdmin.listUsers).toBe("function");
    expect(typeof userAdmin.getUserDetail).toBe("function");
    expect(typeof userAdmin.updateUserStatus).toBe("function");
    expect(typeof userAdmin.unlockUser).toBe("function");
    expect(typeof userAdmin.assignRoles).toBe("function");
    expect(typeof userAdmin.resetPassword).toBe("function");
    expect(typeof userAdmin.listRoles).toBe("function");
    expect(typeof userAdmin.importUsers).toBe("function");
  });
});

describe("system", () => {
  it("should export API functions", () => {
    expect(typeof system).toBe("object");
    expect(typeof system.fetchSystemProviders).toBe("function");
    expect(typeof system.fetchOrgContext).toBe("function");
  });
});

describe("eval", () => {
  it("should export API functions", () => {
    expect(typeof evalApi).toBe("object");
    expect(typeof evalApi.listEvalSets).toBe("function");
    expect(typeof evalApi.getEvalSet).toBe("function");
    expect(typeof evalApi.createEvalSet).toBe("function");
    expect(typeof evalApi.updateEvalSet).toBe("function");
    expect(typeof evalApi.publishEvalSet).toBe("function");
    expect(typeof evalApi.deprecateEvalSet).toBe("function");
    expect(typeof evalApi.listEvalIndicators).toBe("function");
    expect(typeof evalApi.getEvalIndicator).toBe("function");
    expect(typeof evalApi.createEvalIndicator).toBe("function");
    expect(typeof evalApi.updateEvalIndicator).toBe("function");
    expect(typeof evalApi.deleteEvalIndicator).toBe("function");
    expect(typeof evalApi.executeEvaluation).toBe("function");
    expect(typeof evalApi.listEvalResults).toBe("function");
    expect(typeof evalApi.getEvalResult).toBe("function");
    expect(typeof evalApi.generateReport).toBe("function");
    expect(typeof evalApi.exportReport).toBe("function");
    expect(typeof evalApi.getReport).toBe("function");
    expect(typeof evalApi.listReports).toBe("function");
    expect(typeof evalApi.archiveReport).toBe("function");
    expect(typeof evalApi.submitReview).toBe("function");
    expect(typeof evalApi.listReviews).toBe("function");
    expect(typeof evalApi.createRectification).toBe("function");
    expect(typeof evalApi.autoCreateRectifications).toBe("function");
    expect(typeof evalApi.updateRectificationStatus).toBe("function");
    expect(typeof evalApi.listRectifications).toBe("function");
    expect(typeof evalApi.reEvaluate).toBe("function");
  });
});

describe("clinicalEvent", () => {
  it("should export REST API functions and ClinicalEventClient class", () => {
    expect(typeof clinicalEvent).toBe("object");
    expect(typeof clinicalEvent.fetchEmbedConfig).toBe("function");
    expect(typeof clinicalEvent.fetchEmbedAlerts).toBe("function");
    expect(typeof clinicalEvent.executeAlertAction).toBe("function");
    expect(typeof clinicalEvent.ClinicalEventClient).toBe("function");
  });
});

describe("sso", () => {
  it("should export API functions", () => {
    expect(typeof sso).toBe("object");
    expect(typeof sso.listSsoProviders).toBe("function");
    expect(typeof sso.initiateSso).toBe("function");
    expect(typeof sso.handleSsoCallback).toBe("function");
    expect(typeof sso.ldapAuthenticate).toBe("function");
  });
});

describe("tenantOnboarding", () => {
  it("should export API functions", () => {
    expect(typeof tenantOnboarding).toBe("object");
    expect(typeof tenantOnboarding.submitTenantApplication).toBe("function");
    expect(typeof tenantOnboarding.approveTenantApplication).toBe("function");
    expect(typeof tenantOnboarding.sendTenantAdminInvitation).toBe("function");
  });
});

describe("identityBinding", () => {
  it("should export API functions", () => {
    expect(typeof identityBinding).toBe("object");
    expect(typeof identityBinding.listBindingsByUser).toBe("function");
    expect(typeof identityBinding.bindIdentity).toBe("function");
    expect(typeof identityBinding.unbindIdentity).toBe("function");
    expect(typeof identityBinding.mergeBindings).toBe("function");
    expect(typeof identityBinding.findConflicts).toBe("function");
  });
});

describe("ruleActionLog", () => {
  it("should export API functions", () => {
    expect(typeof ruleActionLog).toBe("object");
    expect(typeof ruleActionLog.recordDecision).toBe("function");
    expect(typeof ruleActionLog.fetchActionLogs).toBe("function");
    expect(typeof ruleActionLog.fetchActionLog).toBe("function");
    expect(typeof ruleActionLog.fetchActionLogsByPatient).toBe("function");
    expect(typeof ruleActionLog.fetchActionLogsByOrder).toBe("function");
  });
});

describe("terminology", () => {
  it("should export API functions", () => {
    expect(typeof terminology).toBe("object");
    expect(typeof terminology.fetchTerminologyMappings).toBe("function");
    expect(typeof terminology.fetchMappingSummary).toBe("function");
    expect(typeof terminology.fetchAiCandidates).toBe("function");
    expect(typeof terminology.adoptMapping).toBe("function");
    expect(typeof terminology.batchAdoptMappings).toBe("function");
    expect(typeof terminology.rejectAiCandidate).toBe("function");
    expect(typeof terminology.manualMapping).toBe("function");
  });
});

describe("aiCandidateReview", () => {
  it("should export API functions", () => {
    expect(typeof aiCandidateReview).toBe("object");
    expect(typeof aiCandidateReview.listCandidates).toBe("function");
    expect(typeof aiCandidateReview.getCandidate).toBe("function");
    expect(typeof aiCandidateReview.reviewCandidate).toBe("function");
    expect(typeof aiCandidateReview.batchReview).toBe("function");
    expect(typeof aiCandidateReview.getReviewSummary).toBe("function");
    expect(typeof aiCandidateReview.getReviewHistory).toBe("function");
  });
});

describe("workflow", () => {
  it("should export API functions", () => {
    expect(typeof workflow).toBe("object");
    expect(typeof workflow.fetchTodoTasks).toBe("function");
    expect(typeof workflow.fetchTodoDetail).toBe("function");
    expect(typeof workflow.fetchTodoSummary).toBe("function");
    expect(typeof workflow.createTodoTask).toBe("function");
    expect(typeof workflow.approveTask).toBe("function");
    expect(typeof workflow.rejectTask).toBe("function");
    expect(typeof workflow.delegateTask).toBe("function");
    expect(typeof workflow.cancelTask).toBe("function");
    expect(typeof workflow.addSignTask).toBe("function");
  });
});
