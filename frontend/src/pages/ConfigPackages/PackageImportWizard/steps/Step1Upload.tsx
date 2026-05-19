import { useState } from "react";
import { Upload, Typography, Alert } from "antd";
import { InboxOutlined } from "@ant-design/icons";
import { importPackageUpload } from "@/api/configPackage";
import type { ImportUploadResult } from "@/api/types";
import type { WizardContext } from "../types";

const { Text } = Typography;
const { Dragger } = Upload;

interface Step1UploadProps {
  context: WizardContext;
  onUploadSuccess: (result: ImportUploadResult) => void;
}

export default function Step1Upload({ context, onUploadSuccess }: Step1UploadProps) {
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (context.uploadResult) {
    return (
      <div>
        <Alert
          type="success"
          showIcon
          message="文件上传成功"
          description={
            <div>
              <div>文件名：{context.uploadResult.filename}</div>
              <div>大小：{(context.uploadResult.file_size / 1024).toFixed(1)} KB</div>
              <div>上传 ID：{context.uploadResult.upload_id}</div>
            </div>
          }
        />
      </div>
    );
  }

  return (
    <div>
      {error && (
        <Alert
          type="error"
          showIcon
          closable
          style={{ marginBottom: 16 }}
          message="上传失败"
          description={error}
          onClose={() => setError(null)}
        />
      )}

      <Dragger
        accept=".zip,.json"
        multiple={false}
        showUploadList={false}
        customRequest={async ({ file, onSuccess, onError }) => {
          setUploading(true);
          setError(null);
          try {
            const result = await importPackageUpload(file as File);
            onSuccess?.(result);
            onUploadSuccess(result);
          } catch (err) {
            const msg = err instanceof Error ? err.message : "上传失败";
            setError(msg);
            onError?.(new Error(msg));
          } finally {
            setUploading(false);
          }
        }}
        disabled={uploading}
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined style={{ color: "var(--mk-brand-primary)", fontSize: 48 }} />
        </p>
        <p className="ant-upload-text" style={{ fontSize: 16 }}>
          点击或拖拽文件到此区域上传
        </p>
        <p className="ant-upload-hint">
          <Text type="secondary">支持 .zip 和 .json 格式的配置包文件</Text>
        </p>
      </Dragger>
    </div>
  );
}
