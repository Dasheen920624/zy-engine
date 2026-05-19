import { useEffect } from "react";
import { useBlocker } from "react-router-dom";
import { Modal } from "antd";

interface UnsavedChangesGuardProps {
  dirty: boolean;
}

const UnsavedChangesGuard: React.FC<UnsavedChangesGuardProps> = ({ dirty }) => {
  const blocker = useBlocker(dirty);

  // beforeunload for browser close / refresh
  useEffect(() => {
    if (!dirty) return;

    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };

    window.addEventListener("beforeunload", handler);
    return () => window.removeEventListener("beforeunload", handler);
  }, [dirty]);

  // React Router navigation blocker
  useEffect(() => {
    if (blocker.state === "blocked") {
      Modal.confirm({
        title: "未保存的更改",
        content: "有未保存的更改，确定要离开吗？",
        okText: "离开",
        cancelText: "留下",
        onOk: () => {
          blocker.proceed?.();
        },
        onCancel: () => {
          blocker.reset?.();
        },
      });
    }
  }, [blocker]);

  return null;
};

export default UnsavedChangesGuard;
