import React, { useState, useEffect, useMemo } from 'react';
import {
  Card,
  Table,
  Button,
  Tag,
  Space,
  Modal,
  Checkbox,
  Row,
  Col,
  Divider,
  Typography,
  message,
  Spin,
  Alert,
  Input,
  Form,
  Badge,
} from 'antd';
import {
  SafetyCertificateOutlined,
  SaveOutlined,
  SettingOutlined,
  PlusOutlined,
  SearchOutlined,
  CheckSquareOutlined,
  CloseSquareOutlined,
  EditOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { api } from '../../services/api';
import { useAuth } from '../../app/context/AuthContext';
import { Role, Permission, ApiResponse } from '../../types';

const { Title, Text } = Typography;

export const RolesPage: React.FC = () => {
  const { hasPermission } = useAuth();

  const [roles, setRoles] = useState<Role[]>([]);
  const [loading, setLoading] = useState(false);

  // Permission Matrix Modal State
  const [matrixModalOpen, setMatrixModalOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [matrixLoading, setMatrixLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);

  const [groupedPermissions, setGroupedPermissions] = useState<Record<string, Permission[]>>({});
  const [allPermissionsList, setAllPermissionsList] = useState<Permission[]>([]);
  const [selectedPermIds, setSelectedPermIds] = useState<string[]>([]);
  const [permSearch, setPermSearch] = useState<string>('');

  // Create Role Modal State
  const [createRoleModalOpen, setCreateRoleModalOpen] = useState(false);
  const [createRoleLoading, setCreateRoleLoading] = useState(false);
  const [createForm] = Form.useForm();

  // Edit Role Modal State
  const [editRoleModalOpen, setEditRoleModalOpen] = useState(false);
  const [editRoleLoading, setEditRoleLoading] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | null>(null);
  const [editForm] = Form.useForm();

  // Fetch Roles
  const fetchRoles = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<Role[]>>('/roles');
      if (res.data.success) {
        setRoles(res.data.data);
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tải danh sách vai trò.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRoles();
  }, []);

  // Open Permission Matrix for a role
  const handleOpenMatrix = async (role: Role) => {
    setSelectedRole(role);
    setPermSearch('');
    setMatrixModalOpen(true);
    setMatrixLoading(true);

    try {
      const [permsRes, rolePermsRes] = await Promise.all([
        api.get<ApiResponse<{ all: Permission[]; byModule: Record<string, Permission[]> }>>('/permissions'),
        api.get<ApiResponse<{ permissionIds: string[] }>>(`/roles/${role.id}/permissions`),
      ]);

      if (permsRes.data.success) {
        setGroupedPermissions(permsRes.data.data.byModule);
        setAllPermissionsList(permsRes.data.data.all || []);
      }
      if (rolePermsRes.data.success) {
        setSelectedPermIds(rolePermsRes.data.data.permissionIds || []);
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tải ma trận phân quyền.');
    } finally {
      setMatrixLoading(false);
    }
  };

  // Toggle permission in matrix (Fixed: pure toggle with no double-fire)
  const handleTogglePerm = (permId: string) => {
    setSelectedPermIds((prev) =>
      prev.includes(permId) ? prev.filter((id) => id !== permId) : [...prev, permId]
    );
  };

  // Select all permissions
  const handleSelectAll = () => {
    const allIds = allPermissionsList.map((p) => p.id);
    setSelectedPermIds(allIds);
  };

  // Deselect all permissions
  const handleDeselectAll = () => {
    setSelectedPermIds([]);
  };

  // Select all permissions for a specific module
  const handleSelectModule = (modulePerms: Permission[]) => {
    const moduleIds = modulePerms.map((p) => p.id);
    setSelectedPermIds((prev) => Array.from(new Set([...prev, ...moduleIds])));
  };

  // Deselect all permissions for a specific module
  const handleDeselectModule = (modulePerms: Permission[]) => {
    const moduleIds = new Set(modulePerms.map((p) => p.id));
    setSelectedPermIds((prev) => prev.filter((id) => !moduleIds.has(id)));
  };

  // Save permissions matrix
  const handleSaveMatrix = async () => {
    if (!selectedRole) return;
    setSaveLoading(true);

    try {
      const res = await api.put<ApiResponse>(`/roles/${selectedRole.id}/permissions`, {
        permissionIds: selectedPermIds,
      });

      if (res.data.success) {
        message.success(`Đã cập nhật phân quyền cho vai trò [${selectedRole.name}] thành công!`);
        setMatrixModalOpen(false);
        fetchRoles();
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể lưu phân quyền.');
    } finally {
      setSaveLoading(false);
    }
  };

  // Open Create Role Modal
  const handleOpenCreateModal = () => {
    createForm.resetFields();
    setCreateRoleModalOpen(true);
  };

  // Submit Create Role
  const handleCreateRoleSubmit = async () => {
    try {
      const values = await createForm.validateFields();
      setCreateRoleLoading(true);

      const res = await api.post<ApiResponse<Role>>('/roles', {
        code: values.code.toUpperCase().trim(),
        name: values.name.trim(),
        description: values.description ? values.description.trim() : null,
      });

      if (res.data.success) {
        message.success(`Tạo vai trò [${values.name}] thành công!`);
        setCreateRoleModalOpen(false);
        fetchRoles();
      }
    } catch (err: any) {
      if (err.errorFields) return; // Validation error
      message.error(err.response?.data?.message || 'Không thể tạo vai trò mới.');
    } finally {
      setCreateRoleLoading(false);
    }
  };

  // Open Edit Role Modal
  const handleOpenEditModal = (role: Role) => {
    setEditingRole(role);
    editForm.setFieldsValue({
      name: role.name,
      description: role.description || '',
    });
    setEditRoleModalOpen(true);
  };

  // Submit Edit Role
  const handleEditRoleSubmit = async () => {
    if (!editingRole) return;
    try {
      const values = await editForm.validateFields();
      setEditRoleLoading(true);

      const res = await api.put<ApiResponse<Role>>(`/roles/${editingRole.id}`, {
        name: values.name.trim(),
        description: values.description ? values.description.trim() : null,
      });

      if (res.data.success) {
        message.success(`Cập nhật vai trò [${values.name}] thành công!`);
        setEditRoleModalOpen(false);
        fetchRoles();
      }
    } catch (err: any) {
      if (err.errorFields) return;
      message.error(err.response?.data?.message || 'Không thể cập nhật thông tin vai trò.');
    } finally {
      setEditRoleLoading(false);
    }
  };

  const moduleNamesMap: Record<string, string> = {
    users: 'Quản lý tài khoản (Users)',
    roles: 'Quản lý vai trò (Roles)',
    permissions: 'Quản lý quyền hạn (Permissions)',
    audit: 'Nhật ký kiểm toán (Audit Logs)',
    departments: 'Phòng ban & Tổ chức',
    requisitions: 'Yêu cầu tuyển dụng (Requisitions)',
    candidates: 'Hồ sơ ứng viên (Candidates)',
    interviews: 'Lịch & Đánh giá phỏng vấn (Interviews)',
    salary: 'Mức lương & Dải lương (Salary)',
  };

  // Filter permissions based on search
  const filteredGroupedPermissions = useMemo(() => {
    if (!permSearch.trim()) return groupedPermissions;
    const query = permSearch.toLowerCase().trim();
    const result: Record<string, Permission[]> = {};

    Object.entries(groupedPermissions).forEach(([moduleKey, perms]) => {
      const matched = perms.filter(
        (p) =>
          p.name.toLowerCase().includes(query) ||
          p.code.toLowerCase().includes(query) ||
          (p.description && p.description.toLowerCase().includes(query))
      );
      if (matched.length > 0) {
        result[moduleKey] = matched;
      }
    });

    return result;
  }, [groupedPermissions, permSearch]);

  const columns: ColumnsType<Role> = [
    {
      title: 'Mã vai trò',
      dataIndex: 'code',
      key: 'code',
      render: (code) => <Tag color="blue" style={{ fontWeight: 600 }}>{code}</Tag>,
    },
    {
      title: 'Tên vai trò',
      dataIndex: 'name',
      key: 'name',
      render: (name) => <Text strong>{name}</Text>,
    },
    {
      title: 'Mô tả nghiệp vụ',
      dataIndex: 'description',
      key: 'description',
      render: (desc) => desc || '-',
    },
    {
      title: 'Số quyền được cấp',
      dataIndex: 'permissions_count',
      key: 'permissions_count',
      render: (count) => (
        <Tag color="cyan">
          <SafetyCertificateOutlined style={{ marginRight: 4 }} />
          {count || 0} quyền
        </Tag>
      ),
    },
    {
      title: 'Số người dùng',
      dataIndex: 'users_count',
      key: 'users_count',
      render: (count) => <Tag color="purple">{count || 0} tài khoản</Tag>,
    },
    {
      title: 'Thao tác',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            ghost
            size="small"
            icon={<SettingOutlined />}
            onClick={() => handleOpenMatrix(record)}
            disabled={!hasPermission('permissions.manage') && !hasPermission('roles.view')}
          >
            Cấu hình quyền hạn
          </Button>
          <Button
            type="default"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleOpenEditModal(record)}
            disabled={!hasPermission('roles.update')}
          >
            Sửa
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
      <Card
        title={
          <div>
            <Title level={4} style={{ margin: 0 }}>
              Quản Trị Vai Trò & Ma Trận Phân Quyền
            </Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Danh sách vai trò người dùng trong hệ thống và cấu hình chi tiết phân quyền cấp chức năng
            </Text>
          </div>
        }
        extra={
          hasPermission('roles.create') && (
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleOpenCreateModal}
            >
              Thêm vai trò mới
            </Button>
          )
        }
      >
        <Table
          columns={columns}
          dataSource={roles}
          rowKey="id"
          loading={loading}
          pagination={false}
        />
      </Card>

      {/* MODAL: PERMISSION MATRIX */}
      <Modal
        title={
          <div style={{ padding: '4px 0' }}>
            <Title level={4} style={{ margin: 0 }}>
              <SafetyCertificateOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              Phân Quyền Vai Trò: [{selectedRole?.name}]
            </Title>
            <div style={{ marginTop: 4 }}>
              <Text type="secondary" style={{ fontSize: 13, marginRight: 16 }}>
                Mã vai trò: <code>{selectedRole?.code}</code>
              </Text>
              <Badge
                count={`${selectedPermIds.length} quyền đã chọn`}
                style={{ backgroundColor: '#52c41a' }}
              />
            </div>
          </div>
        }
        open={matrixModalOpen}
        onCancel={() => setMatrixModalOpen(false)}
        width={850}
        footer={[
          <Button key="cancel" onClick={() => setMatrixModalOpen(false)}>
            Đóng
          </Button>,
          hasPermission('permissions.manage') && (
            <Button
              key="save"
              type="primary"
              icon={<SaveOutlined />}
              loading={saveLoading}
              onClick={handleSaveMatrix}
            >
              Lưu thay đổi phân quyền
            </Button>
          ),
        ]}
      >
        {matrixLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}>
            <Spin size="large" tip="Đang tải danh sách quyền hệ thống..." />
          </div>
        ) : (
          <div>
            {selectedRole?.code === 'ADMIN' && (
              <Alert
                message="Toàn quyền Quản trị (Admin Superuser)"
                description="Vai trò ADMIN sở hữu quyền quản trị toàn bộ hệ thống. Bạn có thể điều chỉnh hoặc bổ sung quyền hạn tùy theo yêu cầu vận hành."
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            {/* Quick Actions & Search */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                flexWrap: 'wrap',
                gap: 12,
                marginBottom: 16,
                padding: '10px 14px',
                background: '#f5f5f5',
                borderRadius: 8,
              }}
            >
              <Space wrap>
                <Button
                  size="small"
                  icon={<CheckSquareOutlined />}
                  onClick={handleSelectAll}
                >
                  Chọn tất cả ({allPermissionsList.length})
                </Button>
                <Button
                  size="small"
                  icon={<CloseSquareOutlined />}
                  onClick={handleDeselectAll}
                >
                  Bỏ chọn tất cả
                </Button>
              </Space>

              <Input
                placeholder="Tìm kiếm quyền theo tên hoặc mã..."
                prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
                value={permSearch}
                onChange={(e) => setPermSearch(e.target.value)}
                allowClear
                style={{ width: 280 }}
              />
            </div>

            {/* Permissions List Grouped by Module */}
            <div style={{ maxHeight: 520, overflowY: 'auto', paddingRight: 8 }}>
              {Object.keys(filteredGroupedPermissions).length === 0 ? (
                <div style={{ textAlign: 'center', padding: 30, color: '#888' }}>
                  Không tìm thấy quyền nào phù hợp với từ khóa &quot;{permSearch}&quot;
                </div>
              ) : (
                Object.entries(filteredGroupedPermissions).map(([moduleKey, perms]) => {
                  const moduleSelectedCount = perms.filter((p) =>
                    selectedPermIds.includes(p.id)
                  ).length;
                  const isAllModuleSelected =
                    perms.length > 0 && moduleSelectedCount === perms.length;

                  return (
                    <div key={moduleKey} style={{ marginBottom: 20 }}>
                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          marginBottom: 8,
                          padding: '4px 8px',
                          borderBottom: '1px solid #e8e8e8',
                        }}
                      >
                        <Space>
                          <Text strong style={{ color: '#1677ff', fontSize: 14 }}>
                            {moduleNamesMap[moduleKey] || moduleKey.toUpperCase()}
                          </Text>
                          <Tag color={moduleSelectedCount > 0 ? 'blue' : 'default'}>
                            {moduleSelectedCount}/{perms.length} đã chọn
                          </Tag>
                        </Space>
                        <Space size="small">
                          {!isAllModuleSelected ? (
                            <Button
                              type="link"
                              size="small"
                              style={{ padding: 0, fontSize: 12 }}
                              onClick={() => handleSelectModule(perms)}
                            >
                              Chọn cả nhóm
                            </Button>
                          ) : (
                            <Button
                              type="link"
                              danger
                              size="small"
                              style={{ padding: 0, fontSize: 12 }}
                              onClick={() => handleDeselectModule(perms)}
                            >
                              Bỏ chọn nhóm
                            </Button>
                          )}
                        </Space>
                      </div>

                      <Row gutter={[10, 10]}>
                        {perms.map((p) => {
                          const checked = selectedPermIds.includes(p.id);
                          return (
                            <Col xs={24} sm={12} key={p.id}>
                              <div
                                style={{
                                  padding: '10px 12px',
                                  background: checked ? '#e6f4ff' : '#fafafa',
                                  border: `1px solid ${checked ? '#91caff' : '#f0f0f0'}`,
                                  borderRadius: 6,
                                  transition: 'all 0.2s',
                                  cursor: 'pointer',
                                  userSelect: 'none',
                                }}
                                onClick={() => handleTogglePerm(p.id)}
                              >
                                <Row wrap={false} align="top">
                                  <Col flex="24px" style={{ paddingTop: 2 }}>
                                    <Checkbox
                                      checked={checked}
                                      style={{ pointerEvents: 'none' }}
                                    />
                                  </Col>
                                  <Col flex="auto" style={{ paddingLeft: 8 }}>
                                    <div
                                      style={{
                                        fontWeight: 600,
                                        fontSize: 13,
                                        color: checked ? '#0958d9' : '#1f1f1f',
                                      }}
                                    >
                                      {p.name}
                                    </div>
                                    <div style={{ fontSize: 11, color: '#888' }}>
                                      <code>{p.code}</code>
                                    </div>
                                    {p.description && (
                                      <div
                                        style={{
                                          fontSize: 11,
                                          color: '#666',
                                          marginTop: 2,
                                        }}
                                      >
                                        {p.description}
                                      </div>
                                    )}
                                  </Col>
                                </Row>
                              </div>
                            </Col>
                          );
                        })}
                      </Row>
                    </div>
                  );
                })
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* MODAL: CREATE ROLE */}
      <Modal
        title={
          <Title level={4} style={{ margin: 0 }}>
            <PlusOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Thêm Vai Trò Mới
          </Title>
        }
        open={createRoleModalOpen}
        onCancel={() => setCreateRoleModalOpen(false)}
        onOk={handleCreateRoleSubmit}
        confirmLoading={createRoleLoading}
        okText="Tạo vai trò"
        cancelText="Hủy"
        width={520}
      >
        <Form form={createForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="code"
            label="Mã vai trò (Code)"
            rules={[
              { required: true, message: 'Vui lòng nhập mã vai trò.' },
              {
                pattern: /^[A-Z0-9_]+$/,
                message: 'Mã vai trò chỉ gồm chữ hoa, số và dấu gạch dưới (VD: TECH_LEAD).',
              },
            ]}
            extra="Quy ước viết hoa không dấu, ví dụ: HR_MANAGER, TECH_LEAD, AUDITOR"
          >
            <Input placeholder="VD: TECH_LEAD" style={{ textTransform: 'uppercase' }} />
          </Form.Item>

          <Form.Item
            name="name"
            label="Tên vai trò hiển thị"
            rules={[{ required: true, message: 'Vui lòng nhập tên vai trò.' }]}
          >
            <Input placeholder="VD: Trưởng nhóm Kỹ thuật" />
          </Form.Item>

          <Form.Item name="description" label="Mô tả quyền hạn / nghiệp vụ">
            <Input.TextArea
              rows={3}
              placeholder="Mô tả phạm vi quyền hạn và trách nhiệm của vai trò..."
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL: EDIT ROLE */}
      <Modal
        title={
          <Title level={4} style={{ margin: 0 }}>
            <EditOutlined style={{ marginRight: 8, color: '#1677ff' }} />
            Chỉnh Sửa Thông Tin Vai Trò
          </Title>
        }
        open={editRoleModalOpen}
        onCancel={() => setEditRoleModalOpen(false)}
        onOk={handleEditRoleSubmit}
        confirmLoading={editRoleLoading}
        okText="Lưu thay đổi"
        cancelText="Hủy"
        width={500}
      >
        <Form form={editForm} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="Mã vai trò">
            <Input value={editingRole?.code} disabled />
          </Form.Item>

          <Form.Item
            name="name"
            label="Tên vai trò hiển thị"
            rules={[{ required: true, message: 'Vui lòng nhập tên vai trò.' }]}
          >
            <Input placeholder="Tên vai trò" />
          </Form.Item>

          <Form.Item name="description" label="Mô tả nghiệp vụ">
            <Input.TextArea rows={3} placeholder="Mô tả trách nhiệm của vai trò..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
