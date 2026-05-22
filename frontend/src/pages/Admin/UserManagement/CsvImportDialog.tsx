import { useState } from "react";
import { Alert, Modal, Typography, Upload } from "antd";
import { InboxOutlined } from "@ant-design/icons";
import type { UploadFile } from "antd/es/upload/interface";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { importUsers, type ImportResult } from "../../../api/userAdmin";
import styles from "./styles.module.css";

const { Dragger } = Upload;
const { Text, Paragraph } = Typography;

interface Props {
  open: boolean;
  onClose: () => void;
}

/**
 * CSV 批量导入用户 Dialog（PR-FINAL-08）。
 *
 * 支持拖拽上传，文件发送到 /api/admin/users/import（multipart）。
 * 后端支持 GB18030 / UTF-8（卫健委系统常见 GB18030）。
 *
 * CSV 格式（首行表头，列顺序不敏感）：
 *   username, display_name, email, phone, user_type, employee_id, password
 *   password 可省略，省略时后端自动生成 8 位临时密码。
 */
export default function CsvImportDialog({ open, onClose }: Props) {
  const queryClient = useQueryClient();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [result, setResult] = useState<ImportResult | null>(null);

  const mutation = useMutation({
    mutationFn: (file: File) => importUsers(file),
    onSuccess: (data) => {
      setResult(data);
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
    },
  });

  const handleOk = () => {
    const file = fileList[0]?.originFileObj;
    if (!file) return;
    setResult(null);
    mutation.mutate(file);
  };

  const handleClose = () => {
    setFileList([]);
    setResult(null);
    mutation.reset();
    onClose();
  };

  const beforeUpload = (file: File) => {
    setFileList([{ uid: "-1", name: file.name, status: "done", originFileObj: file } as UploadFile]);
    return false; // 手动控制上传
  };

  return (
    <Modal
      title="批量导入用户（CSV）"
      open={open}
      onOk={handleOk}
      onCancel={handleClose}
      confirmLoading={mutation.isPending}
      okText="开始导入"
      cancelText="关闭"
      okButtonProps={{ disabled: fileList.length === 0 }}
      width={520}
      destroyOnClose
    >
      <Dragger
        accept=".csv"
        fileList={fileList}
        beforeUpload={beforeUpload}
        onRemove={() => setFileList([])}
        maxCount={1}
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">点击或拖拽 CSV 文件到此区域</p>
        <p className="ant-upload-hint">支持 GB18030 / UTF-8 编码，单次最多 2000 行</p>
      </Dragger>

      <div className={styles.uploadHint}>
        <Paragraph className={styles.uploadHintLine1}>
          <Text strong>必填列：</Text>{" "}
          <Text code>username</Text>、<Text code>display_name</Text>
        </Paragraph>
        <Paragraph className={styles.uploadHintLine2}>
          <Text strong>可选列：</Text>{" "}
          <Text code>email</Text>、<Text code>phone</Text>、<Text code>user_type</Text>（默认 STAFF）、
          <Text code>employee_id</Text>、<Text code>password</Text>（缺省自动生成临时密码）
        </Paragraph>
      </div>

      {mutation.isError && (
        <Alert
          type="error"
          message="导入失败"
          description={String(mutation.error)}
          className={styles.importAlert}
          showIcon
        />
      )}

      {result && (
        <div className={styles.importResultBox}>
          <div className={styles.importResultTitle}>
            导入完成：成功 {result.created} 条，跳过 {result.skipped} 条
          </div>
          {result.errors.length > 0 && (
            <div className={styles.importErrorList}>
              {result.errors.map((err, i) => (
                <div key={i} className={styles.importErrorItem}>
                  {err}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </Modal>
  );
}
