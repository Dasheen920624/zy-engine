import React, { useCallback } from "react";
import { Dropdown, Modal, message } from "antd";
import {
  DeleteOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  RollbackOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import type { PathwaySummary } from "../../../api/types";
import { deletePathway, publishPathway, rollbackPathway } from "../../../api/pathway";

interface ActionMenuProps {
  row: PathwaySummary;
  onRefresh?: () => void;
  onEdit?: (code: string) => void;
  onView?: (code: string) => void;
}

const ActionMenu: React.FC<ActionMenuProps> = ({ row, onRefresh, onEdit, onView }) => {
  const isDraft = row.status === "DRAFT" && row.published_versions.length === 0;
  const isPublished = row.published_versions.length > 0;
  const hasMultipleVersions = row.published_versions.length > 1;

  const handleDelete = useCallback(() => {
    Modal.confirm({
      title: "确认删除",
      icon: <ExclamationCircleOutlined />,
      content: `确定要删除路径模板「${row.pathway_name}」吗？此操作不可恢复。`,
      okText: "删除",
      okType: "danger",
      cancelText: "取消",
      onOk: async () => {
        try {
          await deletePathway(row.pathway_code);
          message.success("删除成功");
          onRefresh?.();
        } catch (err) {
          message.error(err instanceof Error ? err.message : "删除失败");
        }
      },
    });
  }, [row, onRefresh]);

  const handlePublish = useCallback(async () => {
    try {
      await publishPathway(row.pathway_code, {});
      message.success("发布成功");
      onRefresh?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : "发布失败");
    }
  }, [row, onRefresh]);

  const handleRollback = useCallback(async () => {
    if (!hasMultipleVersions) return;
    const targetVersion = row.published_versions[row.published_versions.length - 2];
    try {
      await rollbackPathway(row.pathway_code, { target_version: targetVersion });
      message.success("回滚成功");
      onRefresh?.();
    } catch (err) {
      message.error(err instanceof Error ? err.message : "回滚失败");
    }
  }, [row, hasMultipleVersions, onRefresh]);

  const items: MenuProps["items"] = [
    {
      key: "view",
      icon: <EyeOutlined />,
      label: "查看详情",
      onClick: () => onView?.(row.pathway_code),
    },
  ];

  if (isDraft) {
    items.push({
      key: "edit",
      icon: <EditOutlined />,
      label: "编辑",
      onClick: () => onEdit?.(row.pathway_code),
    });
    items.push({
      key: "publish",
      label: "发布",
      onClick: handlePublish,
    });
    items.push({
      type: "divider",
    });
    items.push({
      key: "delete",
      icon: <DeleteOutlined />,
      label: "删除",
      danger: true,
      disabled: !isDraft,
      onClick: handleDelete,
    });
  }

  if (hasMultipleVersions) {
    items.push({
      key: "rollback",
      icon: <RollbackOutlined />,
      label: "回滚到上一版本",
      onClick: handleRollback,
    });
  }

  if (!isDraft && isPublished) {
    items.push({
      type: "divider",
    });
    items.push({
      key: "delete_disabled",
      icon: <DeleteOutlined />,
      label: "删除",
      disabled: true,
    });
  }

  return <Dropdown menu={{ items }} trigger={["click"]}><a onClick={(e) => e.preventDefault()}>⋮</a></Dropdown>;
};

export default ActionMenu;
