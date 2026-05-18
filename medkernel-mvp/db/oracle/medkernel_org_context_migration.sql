SET DEFINE OFF;
SET SERVEROUTPUT ON;
ALTER SESSION SET NLS_LENGTH_SEMANTICS=CHAR;

DECLARE
  PROCEDURE add_column_if_missing(p_table VARCHAR2, p_column VARCHAR2, p_definition VARCHAR2) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_tab_columns
     WHERE table_name = UPPER(p_table)
       AND column_name = UPPER(p_column);

    IF v_count = 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' ADD (' || p_column || ' ' || p_definition || ')';
      DBMS_OUTPUT.PUT_LINE('Added column ' || p_table || '.' || p_column);
    END IF;
  END;

  PROCEDURE create_index_if_missing(p_index VARCHAR2, p_sql VARCHAR2) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_indexes
     WHERE index_name = UPPER(p_index);

    IF v_count = 0 THEN
      EXECUTE IMMEDIATE p_sql;
      DBMS_OUTPUT.PUT_LINE('Created index ' || p_index);
    END IF;
  END;

  FUNCTION constraint_signature(p_constraint VARCHAR2) RETURN VARCHAR2 IS
    v_signature VARCHAR2(4000);
  BEGIN
    SELECT LISTAGG(column_name, ',') WITHIN GROUP (ORDER BY position)
      INTO v_signature
      FROM user_cons_columns
     WHERE constraint_name = UPPER(p_constraint);
    RETURN v_signature;
  END;

  PROCEDURE drop_constraint_if_exists(p_table VARCHAR2, p_constraint VARCHAR2) IS
    v_count NUMBER;
  BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM user_constraints
     WHERE table_name = UPPER(p_table)
       AND constraint_name = UPPER(p_constraint);

    IF v_count > 0 THEN
      EXECUTE IMMEDIATE 'ALTER TABLE ' || p_table || ' DROP CONSTRAINT ' || p_constraint;
      DBMS_OUTPUT.PUT_LINE('Dropped constraint ' || p_constraint);
    END IF;
  END;
BEGIN
  add_column_if_missing('pe_variation_record', 'tenant_id', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'group_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'hospital_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'campus_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'site_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'department_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'scope_level', 'VARCHAR2(32)');
  add_column_if_missing('pe_variation_record', 'scope_code', 'VARCHAR2(64)');
  add_column_if_missing('pe_variation_record', 'org_source', 'VARCHAR2(32)');

  add_column_if_missing('re_rule_exec_log', 'tenant_id', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'group_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'hospital_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'campus_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'site_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'department_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'scope_level', 'VARCHAR2(32)');
  add_column_if_missing('re_rule_exec_log', 'scope_code', 'VARCHAR2(64)');
  add_column_if_missing('re_rule_exec_log', 'org_source', 'VARCHAR2(32)');

  add_column_if_missing('engine_audit_log', 'tenant_id', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'group_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'hospital_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'campus_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'site_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'department_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'scope_level', 'VARCHAR2(32)');
  add_column_if_missing('engine_audit_log', 'scope_code', 'VARCHAR2(64)');
  add_column_if_missing('engine_audit_log', 'org_source', 'VARCHAR2(32)');

  UPDATE pe_variation_record
     SET tenant_id = NVL(tenant_id, 'default'),
         hospital_code = NVL(hospital_code, 'ZYHOSPITAL'),
         scope_level = NVL(scope_level, 'HOSPITAL'),
         scope_code = NVL(scope_code, NVL(hospital_code, 'ZYHOSPITAL')),
         org_source = NVL(org_source, 'MIGRATION')
   WHERE tenant_id IS NULL
      OR hospital_code IS NULL
      OR scope_level IS NULL
      OR scope_code IS NULL
      OR org_source IS NULL;

  UPDATE re_rule_exec_log
     SET tenant_id = NVL(tenant_id, 'default'),
         hospital_code = NVL(hospital_code, 'ZYHOSPITAL'),
         scope_level = NVL(scope_level, 'HOSPITAL'),
         scope_code = NVL(scope_code, NVL(hospital_code, 'ZYHOSPITAL')),
         org_source = NVL(org_source, 'MIGRATION')
   WHERE tenant_id IS NULL
      OR hospital_code IS NULL
      OR scope_level IS NULL
      OR scope_code IS NULL
      OR org_source IS NULL;

  UPDATE engine_audit_log
     SET tenant_id = NVL(tenant_id, 'default'),
         hospital_code = NVL(hospital_code, 'ZYHOSPITAL'),
         scope_level = NVL(scope_level, 'HOSPITAL'),
         scope_code = NVL(scope_code, NVL(hospital_code, 'ZYHOSPITAL')),
         org_source = NVL(org_source, 'MIGRATION')
   WHERE tenant_id IS NULL
      OR hospital_code IS NULL
      OR scope_level IS NULL
      OR scope_code IS NULL
      OR org_source IS NULL;

  IF NVL(constraint_signature('uk_pe_active_instance'), 'NONE')
        <> 'TENANT_ID,ORG_CODE,ENCOUNTER_ID,PATHWAY_CODE,STATUS' THEN
    drop_constraint_if_exists('pe_patient_instance', 'uk_pe_active_instance');
    EXECUTE IMMEDIATE 'ALTER TABLE pe_patient_instance ADD CONSTRAINT uk_pe_active_instance UNIQUE (tenant_id, org_code, encounter_id, pathway_code, status)';
    DBMS_OUTPUT.PUT_LINE('Created constraint uk_pe_active_instance');
  END IF;

  create_index_if_missing('idx_pe_variation_org',
      'CREATE INDEX idx_pe_variation_org ON pe_variation_record(tenant_id, hospital_code, scope_level, scope_code)');
  create_index_if_missing('idx_re_log_org',
      'CREATE INDEX idx_re_log_org ON re_rule_exec_log(tenant_id, hospital_code, scope_level, scope_code)');
  create_index_if_missing('idx_audit_org',
      'CREATE INDEX idx_audit_org ON engine_audit_log(tenant_id, hospital_code, scope_level, scope_code)');
END;
/

PROMPT MEDKERNEL org context migration is ready.
