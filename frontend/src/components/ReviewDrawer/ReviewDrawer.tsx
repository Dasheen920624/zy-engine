import { useState } from "react";
import { Drawer, Button, Space, Input, message } from "antd";
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  SwapOutlined,
  LoadingOutlined,
} from "@ant-design/icons";
import type { ReviewDrawerProps } from "./ReviewDrawer.types";

const { TextArea } = Input;

export default function ReviewDrawer({
  visible,
  onClose,
  title,
  reviewStatus,
  onApprove,
  onReject,
  onTransfer,
  loading = false,
  children,
  width = 480,
  showApprove = true,
  showReject = true,
  showTransfer = false,
  rejectReasonRequired = true,
  transferReasonRequired = false,
}: ReviewDrawerProps) {
  const [rejectReason, setRejectReason] = useState("");
  const [transferReason, setTransferReason] = useState("");
  const [showRejectForm, setShowRejectForm] = useState(false);
  const [showTransferForm, setShowTransferForm] = useState(false);

  const handleApprove = () => {
    onApprove();
  };

  const handleReject = () => {
    if (rejectReasonRequired && !rejectReason.trim()) {
      message.error("请填写驳回理由");
      return;
    }
    onReject(rejectReason);
    setRejectReason("");
    setShowRejectForm(false);
  };

  const handleTransfer = () => {
    if (transferReasonRequired && !transferReason.trim()) {
      message.error("请填写转人工理由");
      return;
    }
    onTransfer();
    setTransferReason("");
    setShowTransferForm(false);
  };

  const handleClose = () => {
    setShowRejectForm(false);
    setShowTransferForm(false);
    setRejectReason("");
    setTransferReason("");
    onClose();
  };

  const isDisabled = reviewStatus !== "pending" || loading;

  return (
    <Drawer
      title={title}
      placement="right"
      width={width}
      onClose={handleClose}
      open={visible}
      footer={
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            padding: "12px 0",
            borderTop: "1px solid var(--mk-border-divider)",
          }}
        >
          <Space>
            {showReject && (
              <>
                {showRejectForm ? (
                  <Space direction="vertical" style={{ width: "100%" }}>
                    <TextArea
                      placeholder="请输入驳回理由"
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      rows={3}
                      style={{ width: 300 }}
                    />
                    <Space>
                      <Button
                        type="primary"
                        danger
                        icon={<CloseCircleOutlined />}
                        onClick={handleReject}
                        loading={loading}
                      >
                        确认驳回
                      </Button>
                      <Button onClick={() => setShowRejectForm(false)}>取消</Button>
                    </Space>
                  </Space>
                ) : (
                  <Button
                    danger
                    icon={<CloseCircleOutlined />}
                    onClick={() => setShowRejectForm(true)}
                    disabled={isDisabled}
                  >
                    驳回
                  </Button>
                )}
              </>
            )}

            {showTransfer && (
              <>
                {showTransferForm ? (
                  <Space direction="vertical" style={{ width: "100%" }}>
                    <TextArea
                      placeholder="请输入转人工理由（可选）"
                      value={transferReason}
                      onChange={(e) => setTransferReason(e.target.value)}
                      rows={3}
                      style={{ width: 300 }}
                    />
                    <Space>
                      <Button
                        type="primary"
                        icon={<SwapOutlined />}
                        onClick={handleTransfer}
                        loading={loading}
                      >
                        确认转人工
                      </Button>
                      <Button onClick={() => setShowTransferForm(false)}>取消</Button>
                    </Space>
                  </Space>
                ) : (
                  <Button
                    icon={<SwapOutlined />}
                    onClick={() => setShowTransferForm(true)}
                    disabled={isDisabled}
                  >
                    转人工
                  </Button>
                )}
              </>
            )}

            {showApprove && (
              <Button
                type="primary"
                icon={loading ? <LoadingOutlined /> : <CheckCircleOutlined />}
                onClick={handleApprove}
                disabled={isDisabled}
                loading={loading}
              >
                通过
              </Button>
            )}
          </Space>
        </div>
      }
    >
      {children}
    </Drawer>
  );
}