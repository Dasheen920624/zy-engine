import type { ConceptType } from "../../../api/types";

export const CONCEPT_TYPE_MAP: Record<ConceptType, { label: string; color: string }> = {
  DIAGNOSIS: { label: "诊断", color: "blue" },
  PROCEDURE: { label: "手术/操作", color: "cyan" },
  DRUG: { label: "药品", color: "green" },
  LAB: { label: "检验", color: "purple" },
  OBSERVATION: { label: "观察", color: "orange" },
  DEVICE: { label: "器械", color: "magenta" },
};

export const STANDARD_DICT_OPTIONS = [
  { value: "ICD-11", label: "ICD-11 国际疾病分类" },
  { value: "ICD-9-CM-3", label: "ICD-9-CM-3 手术操作" },
  { value: "LOINC", label: "LOINC 检验术语" },
  { value: "ATC", label: "ATC 药品分类" },
  { value: "SNOMED-CT", label: "SNOMED-CT 临床术语" },
];
