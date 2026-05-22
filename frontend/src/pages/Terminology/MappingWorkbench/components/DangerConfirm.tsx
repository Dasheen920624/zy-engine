import React from "react";
import { Modal } from "antd";

export default function DangerConfirm({
  level = "low",
  onConfirm,
  children,
  title,
  description,
}: {
  level?: "low" | "medium" | "high";
  onConfirm: () => void;
  children: React.ReactNode;
  title: string;
  description?: string;
}) {
  const handleClick = () => {
    Modal.confirm({
      title,
      content: description || "确认执行此操作？",
      okText: "确认",
      cancelText: "取消",
      okButtonProps: {
        danger: level === "high",
        type: level === "low" ? "primary" : "default",
      },
      onOk: onConfirm,
    });
  };

  return <span onClick={handleClick}>{children}</span>;
}
