import { useEffect, useState } from "react";
import { Checkbox, Modal, Spin } from "antd";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  assignRoles,
  listRoles,
  ROLE_TYPE_LABELS,
  type AdminUser,
} from "../../../api/userAdmin";
import styles from "./styles.module.css";

interface Props {
  user: AdminUser | null;
  open: boolean;
  onClose: () => void;
}

/**
 * 角色分配 Dialog（幂等替换）。
 *
 * 按 role_type 分组展示（SYSTEM / PLATFORM / HOSPITAL / DEPARTMENT），
 * 确认后幂等替换用户全部角色。
 */
export default function RoleAssignDialog({ user, open, onClose }: Props) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState<string[]>([]);

  const rolesQuery = useQuery({
    queryKey: ["admin-roles"],
    queryFn: listRoles,
    enabled: open,
  });

  // 打开时把用户当前角色预填
  useEffect(() => {
    if (open && user) {
      setSelected(user.roles ?? []);
    }
  }, [open, user]);

  const mutation = useMutation({
    mutationFn: ({ id, codes }: { id: number; codes: string[] }) =>
      assignRoles(id, codes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      onClose();
    },
  });

  const handleOk = () => {
    if (!user) return;
    mutation.mutate({ id: user.id, codes: selected });
  };

  // 按 role_type 分组
  type RoleGroup = Record<string, NonNullable<typeof rolesQuery.data>>;
  const grouped = (rolesQuery.data ?? []).reduce<RoleGroup>(
    (acc, r) => {
      const key = r.role_type;
      if (!acc[key]) acc[key] = [];
      (acc[key] as NonNullable<typeof rolesQuery.data>).push(r);
      return acc;
    },
    {},
  );

  return (
    <Modal
      title={`分配角色 — ${user?.display_name ?? ""}`}
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={mutation.isPending}
      okText="确认分配"
      cancelText="取消"
      width={480}
      destroyOnClose
    >
      {rolesQuery.isLoading ? (
        <Spin />
      ) : (
        Object.entries(grouped).map(([type, roles]) => (
          <div key={type} className={styles.roleGroup}>
            <div className={styles.roleGroupTitle}>
              {ROLE_TYPE_LABELS[type] ?? type}
            </div>
            <Checkbox.Group
              value={selected}
              onChange={(vals) => setSelected(vals as string[])}
            >
              {(roles ?? []).map((r) => (
                <div key={r.role_code}>
                  <Checkbox value={r.role_code}>
                    {r.role_name}
                    <span className={styles.roleCodeLabel}>{r.role_code}</span>
                  </Checkbox>
                </div>
              ))}
            </Checkbox.Group>
          </div>
        ))
      )}
      {mutation.isError && (
        <span className={styles.roleErrorText}>分配失败，请重试</span>
      )}
    </Modal>
  );
}
