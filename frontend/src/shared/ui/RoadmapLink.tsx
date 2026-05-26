import { useState } from "react";

import { LinkOutlined } from "@ant-design/icons";
import { Button, Modal, Space, Tag, Typography } from "antd";

const { Text } = Typography;

interface RoadmapLinkProps {
  taskIds: string[];
  label?: string;
}

export function RoadmapLink({ taskIds, label = "查看实施路线图" }: RoadmapLinkProps) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Button type="link" icon={<LinkOutlined />} onClick={() => setOpen(true)}>
        {label}
      </Button>
      <Modal
        title="引擎能力规划"
        open={open}
        onCancel={() => setOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Space direction="vertical" size="middle" className="mk-full-width">
          <Text>本页依赖以下 backlog 任务，引擎完成后激活：</Text>
          <Space wrap>
            {taskIds.map((id) => (
              <Tag key={id} color="processing">
                {id}
              </Tag>
            ))}
          </Space>
          <Text type="secondary">
            详细任务清单见{" "}
            <a href="/docs/backlog.md" target="_blank" rel="noreferrer">
              docs/backlog.md
            </a>
          </Text>
        </Space>
      </Modal>
    </>
  );
}
