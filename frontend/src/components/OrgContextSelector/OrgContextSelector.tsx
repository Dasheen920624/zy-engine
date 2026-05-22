import { type ReactNode, useCallback, useMemo } from 'react';
import { Cascader } from 'antd';
import { BankOutlined, HomeOutlined, TeamOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { OrgContext, OrgContextSelectorProps } from './OrgContextSelector.types';
import type { OrgContext as StoreOrgContext } from '../../api/types';
import { setOrgContext } from '../../store/orgContext';
import styles from "./orgContextSelector.module.css";

const SESSION_KEY = 'mk-org-context';

/**
 * 将 OrgContextSelector 用的 camelCase 视图模型转回 store/请求 Header 用的 snake_case。
 * 必须与 {@link ../../api/client.ts} `applyOrgHeaders` 读到的 OrgContext 字段集严格对齐。
 */
function toStoreShape(view: OrgContext): StoreOrgContext {
  return {
    tenant_id: view.tenantId,
    group_code: view.groupCode,
    hospital_code: view.hospitalCode,
    campus_code: view.campusCode,
    site_code: view.siteCode,
    department_code: view.departmentCode,
  };
}

interface CascaderOption {
  value: string;
  label: ReactNode;
  children?: CascaderOption[];
}

function buildCascaderOptions(
  scopes: OrgContext[],
  level: 'hospital' | 'department',
): CascaderOption[] {
  const groupMap = new Map<string, Map<string, OrgContext[]>>();

  for (const scope of scopes) {
    const groupKey = scope.groupCode ?? '__no_group__';
    const hospitalKey = scope.hospitalCode;
    if (!groupMap.has(groupKey)) {
      groupMap.set(groupKey, new Map());
    }
    const hospitalMap = groupMap.get(groupKey) as Map<string, OrgContext[]>;
    if (!hospitalMap.has(hospitalKey)) {
      hospitalMap.set(hospitalKey, []);
    }
    (hospitalMap.get(hospitalKey) as OrgContext[]).push(scope);
  }

  const options: CascaderOption[] = [];

  for (const [groupKey, hospitalMap] of groupMap) {
    const groupLabel =
      groupKey === '__no_group__' ? '默认集团' : groupKey;
    const groupOption: CascaderOption = {
      value: groupKey,
      label: (
        <span>
          <BankOutlined className={styles.icon} />
          {groupLabel}
        </span>
      ),
      children: [],
    };

    for (const [hospitalCode, hospitalScopes] of hospitalMap) {
      const hospitalOption: CascaderOption = {
        value: hospitalCode,
        label: (
          <span>
            <HomeOutlined className={styles.icon} />
            {hospitalCode}
          </span>
        ),
      };

      if (level === 'department') {
        hospitalOption.children = [];
        const campusMap = new Map<string, OrgContext[]>();
        for (const s of hospitalScopes) {
          const campusKey = s.campusCode ?? '__no_campus__';
          if (!campusMap.has(campusKey)) {
            campusMap.set(campusKey, []);
          }
          (campusMap.get(campusKey) as OrgContext[]).push(s);
        }

        for (const [campusKey, campusScopes] of campusMap) {
          const campusOption: CascaderOption = {
            value: campusKey,
            label: campusKey === '__no_campus__' ? '默认院区' : campusKey,
          };

          const siteMap = new Map<string, OrgContext[]>();
          for (const cs of campusScopes) {
            const siteKey = cs.siteCode ?? '__no_site__';
            if (!siteMap.has(siteKey)) {
              siteMap.set(siteKey, []);
            }
            (siteMap.get(siteKey) as OrgContext[]).push(cs);
          }

          campusOption.children = [];
          for (const [siteKey, siteScopes] of siteMap) {
            const siteOption: CascaderOption = {
              value: siteKey,
              label: siteKey === '__no_site__' ? '默认站点' : siteKey,
              children: siteScopes.map((ss) => ({
                value: ss.departmentCode ?? '__no_dept__',
                label: (
                  <span>
                    <TeamOutlined className={styles.icon} />
                    {ss.departmentCode ?? '默认科室'}
                  </span>
                ),
              })),
            };
            campusOption.children.push(siteOption);
          }

          hospitalOption.children.push(campusOption);
        }
      }

      (groupOption.children as CascaderOption[]).push(hospitalOption);
    }

    options.push(groupOption);
  }

  return options;
}

function getCurrentCascaderValue(ctx: OrgContext, level: 'hospital' | 'department'): string[] {
  const values: string[] = [
    ctx.groupCode ?? '__no_group__',
    ctx.hospitalCode,
  ];
  if (level === 'department') {
    values.push(ctx.campusCode ?? '__no_campus__');
    values.push(ctx.siteCode ?? '__no_site__');
    values.push(ctx.departmentCode ?? '__no_dept__');
  }
  return values;
}

function findScopeByValues(
  scopes: OrgContext[],
  values: string[],
  level: 'hospital' | 'department',
): OrgContext | undefined {
  const [groupCode, hospitalCode, campusCode, siteCode, departmentCode] = values;
  return scopes.find((s) => {
    const gMatch = (s.groupCode ?? '__no_group__') === groupCode;
    const hMatch = s.hospitalCode === hospitalCode;
    if (level === 'hospital') return gMatch && hMatch;
    const cMatch = (s.campusCode ?? '__no_campus__') === campusCode;
    const sMatch = (s.siteCode ?? '__no_site__') === siteCode;
    const dMatch = (s.departmentCode ?? '__no_dept__') === departmentCode;
    return gMatch && hMatch && cMatch && sMatch && dMatch;
  });
}

export function OrgContextSelector({
  current,
  allowedScopes,
  onChange,
  level = 'hospital',
  variant = 'dropdown',
}: OrgContextSelectorProps) {
  const queryClient = useQueryClient();

  const options = useMemo(
    () => buildCascaderOptions(allowedScopes ?? [], level),
    [allowedScopes, level],
  );

  const currentValue = useMemo(
    () => current ? getCurrentCascaderValue(current, level) : [],
    [current, level],
  );

  const handleChange = useCallback(
    (values: (string | number)[]) => {
      const strValues = values.map(String);
      const found = findScopeByValues(allowedScopes ?? [], strValues, level);
      if (found) {
        // 1) 同步写入全局 store —— axios 拦截器 applyOrgHeaders 读的就是这里，
        //    保证下一个请求自动带上新的 X-Hospital-Code 等 Header（AUDIT §3.2 修复）。
        setOrgContext(toStoreShape(found));
        // 2) 兼容历史 sessionStorage 缓存键，避免直接读 sessionStorage 的旧代码失效；
        //    后续若确认无消费者，可在下一轮审计中清理。
        try {
          sessionStorage.setItem(SESSION_KEY, JSON.stringify(found));
        } catch {
          /* ignore */
        }
        onChange?.(found);
        queryClient.invalidateQueries();
      }
    },
    [allowedScopes, level, onChange, queryClient],
  );

  if (variant === 'inline') {
    return (
      <div className={styles.inlineContainer}>
        <span className={styles.hospitalCode}>
          <HomeOutlined />
          {current?.hospitalCode}
          {level === 'department' && current?.departmentCode && (
            <>
              <TeamOutlined className={styles.departmentIcon} />
              {current.departmentCode}
            </>
          )}
        </span>
        <Cascader
          options={options}
          value={currentValue}
          onChange={handleChange}
          changeOnSelect
          placeholder="切换组织"
          className={styles.inlineCascader}
          popupClassName="org-context-cascader-popup"
        />
      </div>
    );
  }

  return (
    <Cascader
      options={options}
      value={currentValue}
      onChange={handleChange}
      changeOnSelect
      placeholder="选择组织上下文"
      className={styles.dropdownCascader}
      popupClassName="org-context-cascader-popup"
    />
  );
}
