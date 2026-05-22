import type { TerminologyItem, AiCandidate, MappingSummary } from "../../../api/types";
import type { ConceptType } from "../../../api/types";

export const MOCK_SUMMARY: MappingSummary = {
  totalUnmapped: 18,
  totalMapped: 156,
  totalConflict: 2,
  totalAiCandidate: 7,
  byConceptType: {
    DIAGNOSIS: 45,
    PROCEDURE: 32,
    DRUG: 68,
    LAB: 28,
    OBSERVATION: 12,
    DEVICE: 7,
  } as Record<ConceptType, number>,
};

export const MOCK_UNMAPPED: TerminologyItem[] = [
  {
    id: 1,
    sourceSystem: "HIS",
    sourceCode: "LIS_LDH_001",
    sourceName: "乳酸脱氢酶",
    conceptType: "LAB",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 1250,
  },
  {
    id: 2,
    sourceSystem: "HIS",
    sourceCode: "LIS_TNI_07",
    sourceName: "肌钙蛋白 I",
    conceptType: "LAB",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 980,
  },
  {
    id: 3,
    sourceSystem: "HIS",
    sourceCode: "DRUG_ASP_001",
    sourceName: "阿司匹林肠溶片",
    conceptType: "DRUG",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 3500,
  },
  {
    id: 4,
    sourceSystem: "HIS",
    sourceCode: "DX_AMI_001",
    sourceName: "急性心肌梗死",
    conceptType: "DIAGNOSIS",
    mappingStatus: "UNMAPPED",
    occurrenceCount: 420,
  },
];

export const MOCK_MAPPED: TerminologyItem[] = [
  {
    id: 101,
    sourceSystem: "HIS",
    sourceCode: "LIS_CRP_001",
    sourceName: "C反应蛋白",
    conceptType: "LAB",
    mappingStatus: "MAPPED",
    standardCode: "LOINC:1988-5",
    standardName: "C-reactive protein [Mass/volume] in Serum or Plasma",
    reviewedBy: "张信息",
    reviewedTime: "2026-05-18T10:00:00",
  },
];

export const MOCK_CONFLICT: TerminologyItem[] = [
  {
    id: 201,
    sourceSystem: "HIS",
    sourceCode: "LIS_WBC_001",
    sourceName: "白细胞计数",
    conceptType: "LAB",
    mappingStatus: "CONFLICT",
    reviewComment: "存在多个LOINC候选: 6690-2 (血液) vs 26464-8 (体液)",
  },
  {
    id: 202,
    sourceSystem: "HIS",
    sourceCode: "DX_HTN_001",
    sourceName: "高血压病",
    conceptType: "DIAGNOSIS",
    mappingStatus: "CONFLICT",
    reviewComment: "ICD-11 BA00 vs BA01 分型不明确",
  },
];

export const MOCK_AI_CANDIDATES: AiCandidate[] = [
  {
    sourceCode: "LIS_LDH_001",
    sourceName: "乳酸脱氢酶",
    conceptType: "LAB",
    proposedStandardCode: "LOINC:2532-0",
    proposedStandardName: "Lactate dehydrogenase [Enzymatic activity/volume] in Serum or Plasma",
    confidence: 0.95,
    mappingSource: "GPT-4-med",
  },
  {
    sourceCode: "LIS_TNI_07",
    sourceName: "肌钙蛋白 I",
    conceptType: "LAB",
    proposedStandardCode: "LOINC:6597-9",
    proposedStandardName: "Troponin I.cardiac [Mass/volume] in Serum or Plasma",
    confidence: 0.89,
    mappingSource: "GPT-4-med",
  },
  {
    sourceCode: "DRUG_ASP_001",
    sourceName: "阿司匹林肠溶片",
    conceptType: "DRUG",
    proposedStandardCode: "ATC:B01AC06",
    proposedStandardName: "Acetylsalicylic acid",
    confidence: 0.98,
    mappingSource: "GPT-4-med",
  },
  {
    sourceCode: "DX_AMI_001",
    sourceName: "急性心肌梗死",
    conceptType: "DIAGNOSIS",
    proposedStandardCode: "ICD-11:BA41",
    proposedStandardName: "Acute myocardial infarction",
    confidence: 0.92,
    mappingSource: "GPT-4-med",
  },
];
