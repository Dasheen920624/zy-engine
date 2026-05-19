import { type ReactNode, useCallback, useMemo } from 'react';
import { Cascader } from 'antd';
import { BankOutlined, HomeOutlined, TeamOutlined } from '@ant-design/icons';
import { useQueryClient } from '@tanstack/react-query';
import type { OrgContext, OrgContextSelectorProps } from './OrgContextSelector.types';

const SESSION_KEY = 'mk-org-context';

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
          <BankOutlined style={{ marginRight: 6, color: 'var(--mk-text-secondary)' }} />
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
            <HomeOutlined style={{ marginRight: 6, color: 'var(--mk-text-secondary)' }} />
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
                    <TeamOutlined style={{ marginRight: 6, color: 'var(--mk-text-secondary)' }} />
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
    () => buildCascaderOptions(allowedScopes, level),
    [allowedScopes, level],
  );

  const currentValue = useMemo(
    () => getCurrentCascaderValue(current, level),
    [current, level],
  );

  const handleChange = useCallback(
    (values: (string | number)[]) => {
      const strValues = values.map(String);
      const found = findScopeByValues(allowedScopes, strValues, level);
      if (found) {
        try {
          sessionStorage.setItem(SESSION_KEY, JSON.stringify(found));
        } catch {
          /* ignore */
        }
        onChange(found);
        queryClient.invalidateQueries();
      }
    },
    [allowedScopes, level, onChange, queryClient],
  );

  if (variant === 'inline') {
    return (
      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 'var(--mk-space-3)',
          fontSize: 'var(--mk-text-sm)',
          color: 'var(--mk-text-primary)',
        }}
      >
        <span
          style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 'var(--mk-space-1)',
            color: 'var(--mk-brand-primary)',
            fontWeight: 'var(--mk-weight-medium)',
          }}
        >
          <HomeOutlined />
          {current.hospitalCode}
          {level === 'department' && current.departmentCode && (
            <>
              <TeamOutlined style={{ marginLeft: 'var(--mk-space-2)' }} />
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
          style={{ width: 200 }}
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
      style={{ minWidth: 240 }}
      popupClassName="org-context-cascader-popup"
    />
  );
}
