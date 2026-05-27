package com.medkernel.engine.evaluation;

/**
 * 评估对象类型枚举。
 *
 * <p>覆盖患者、病历、科室、医生、疾病、路径、医保病例和随访对象等质控评估主体。
 */
public enum EvaluationSubjectType {
    PATIENT,
    MEDICAL_RECORD,
    DEPARTMENT,
    DOCTOR,
    DISEASE,
    PATHWAY,
    CLAIM,
    FOLLOWUP
}
