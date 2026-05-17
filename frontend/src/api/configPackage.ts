import { get, post } from "./client";
import type {
  AssetType,
  ConfigPackageDetail,
  ConfigPackageReview,
  ConfigPackageSummary,
  PackageStatus,
  PublishRequest,
  ScopeLevel,
} from "./types";

/**
 * 配置包中心 API 模块 (FE-004)
 *
 * 对应后端 /api/config-packages/* 端点。
 * - listPackages: 配置包列表查询
 * - getPackageDetail: 配置包详情
 * - reviewPackage: 审核校验
 * - publishPackage: 发布
 * - exportPackage: 导出 snapshot
 */

export interface ListPackagesParams {
  assetType?: AssetType;
  status?: PackageStatus;
  scopeLevel?: ScopeLevel;
  scopeCode?: string;
  keyword?: string;
}

export async function listPackages(params: ListPackagesParams = {}): Promise<ConfigPackageSummary[]> {
  const qs = new URLSearchParams();
  if (params.assetType) qs.set("assetType", params.assetType);
  if (params.status) qs.set("status", params.status);
  if (params.scopeLevel) qs.set("scopeLevel", params.scopeLevel);
  if (params.scopeCode) qs.set("scopeCode", params.scopeCode);
  const query = qs.toString();
  return get<ConfigPackageSummary[]>(`/config-packages${query ? `?${query}` : ""}`);
}

export async function getPackageDetail(
  packageCode: string,
  packageVersion: string,
): Promise<ConfigPackageDetail> {
  return get<ConfigPackageDetail>(`/config-packages/${encodeURIComponent(packageCode)}/${encodeURIComponent(packageVersion)}`);
}

export async function reviewPackage(
  packageCode: string,
  packageVersion: string,
  reviewedBy?: string,
): Promise<ConfigPackageReview> {
  const body = reviewedBy ? { reviewed_by: reviewedBy } : undefined;
  return post<ConfigPackageReview>(
    `/config-packages/${encodeURIComponent(packageCode)}/${encodeURIComponent(packageVersion)}/review`,
    body,
  );
}

export async function publishPackage(
  packageCode: string,
  packageVersion: string,
  request: PublishRequest,
): Promise<ConfigPackageDetail> {
  return post<ConfigPackageDetail>(
    `/config-packages/${encodeURIComponent(packageCode)}/${encodeURIComponent(packageVersion)}/publish`,
    request,
  );
}

export async function exportPackage(
  packageCode: string,
  packageVersion: string,
): Promise<ConfigPackageDetail & { exported_time: string; export_format: string }> {
  return post(
    `/config-packages/${encodeURIComponent(packageCode)}/${encodeURIComponent(packageVersion)}/export`,
  );
}
