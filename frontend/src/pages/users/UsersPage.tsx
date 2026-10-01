import React, { useState, useEffect, useCallback } from 'react';
import {
  Table,
  Card,
  Input,
  Select,
  Button,
  Space,
  Tag,
  Modal,
  Form,
  message,
  Typography,
  Popconfirm,
  Drawer,
  Descriptions,
  Alert,
  Tooltip,
} from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  LockOutlined,
  UnlockOutlined,
  EditOutlined,
  EyeOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
  CopyOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { api } from '../../services/api';
import { useAuth } from '../../app/context/AuthContext';
import { User, Role, Department, ApiResponse, PaginatedResult } from '../../types';

const { Title, Text, Paragraph } = Typography;
const { Option } = Select;

export const UsersPage: React.FC = () => {
  const { user: currentUser, hasPermission } = useAuth();

  // Table state
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Filters
  const [search, setSearch] = useState('');
  const [departmentId, setDepartmentId] = useState<string | undefined>(undefined);
  const [roleFilter, setRoleFilter] = useState<string | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<string | undefined>(undefined);

  // Metadata
  const [roles, setRoles] = useState<Role[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);

  // Modals & Drawers
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [viewDrawerOpen, setViewDrawerOpen] = useState(false);
  const [rolesModalOpen, setRolesModalOpen] = useState(false);
  const [lockModalOpen, setLockModalOpen] = useState(false);
  const [handoverWarningModalOpen, setHandoverWarningModalOpen] = useState(false);

  // Selected user
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [userRolesList, setUserRolesList] = useState<any[]>([]);
  const [handoverWarningData, setHandoverWarningData] = useState<{
    count: number;
    message: string;
    reason: string;
  } | null>(null);

  // Created user temp password modal
  const [createdTempPasswordData, setCreatedTempPasswordData] = useState<{
    email: string;
    tempPassword: string;
  } | null>(null);

  // Forms
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [lockForm] = Form.useForm();
  const [assignRoleForm] = Form.useForm();

  // Load roles & departments
  useEffect(() => {
    const fetchMeta = async () => {
      try {
        const [rRes, dRes] = await Promise.all([
          api.get<ApiResponse<Role[]>>('/roles'),
          api.get<ApiResponse<Department[]>>('/departments'),
        ]);
        if (rRes.data.success) setRoles(rRes.data.data);
        if (dRes.data.success) setDepartments(dRes.data.data);
      } catch (err) {
        console.warn('Failed to fetch roles/departments meta:', err);
      }
    };
    fetchMeta();
  }, []);

  // Fetch Users
  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<PaginatedResult<User>>>('/users', {
        params: {
          page,
          pageSize,
          search: search || undefined,
          departmentId: departmentId || undefined,
          role: roleFilter || undefined,
          status: statusFilter || undefined,
        },
      });

      if (res.data.success) {
        setUsers(res.data.data.items);
        setTotal(res.data.data.total);
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tải danh sách người dùng.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, search, departmentId, roleFilter, statusFilter]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  // Open Edit User
  const handleOpenEdit = (record: User) => {
    setSelectedUser(record);
    editForm.setFieldsValue({
      fullName: record.fullName,
      phone: record.phone,
      jobTitle: record.jobTitle,
      departmentId: record.department?.id,
      status: record.status,
    });
    setEditModalOpen(true);
  };

  // Open View User
  const handleOpenView = async (record: User) => {
    setSelectedUser(record);
    setViewDrawerOpen(true);
    try {
      const res = await api.get<ApiResponse<User>>(`/users/${record.id}`);
      if (res.data.success) {
        setSelectedUser(res.data.data);
      }
    } catch (err) {
      console.warn('Failed to load user details:', err);
    }
  };

  // Open Manage Roles
  const handleOpenRoles = async (record: User) => {
    setSelectedUser(record);
    setRolesModalOpen(true);
    assignRoleForm.resetFields();
    try {
      const res = await api.get<ApiResponse<any[]>>(`/users/${record.id}/roles`);
      if (res.data.success) {
        setUserRolesList(res.data.data);
      }
    } catch (err) {
      console.warn('Failed to fetch user roles:', err);
    }
  };

  // Open Lock Modal
  const handleOpenLock = (record: User) => {
    setSelectedUser(record);
    lockForm.resetFields();
    setLockModalOpen(true);
  };

  // Submit Create User
  const handleCreateUser = async (values: any) => {
    try {
      const res = await api.post<ApiResponse<{ user: User; temporaryPassword: string }>>('/users', values);
      if (res.data.success) {
        message.success('Đã tạo tài khoản thành công!');
        setCreateModalOpen(false);
        createForm.resetFields();
        fetchUsers();

        // Show temp password modal
        if (res.data.data.temporaryPassword) {
          setCreatedTempPasswordData({
            email: values.email,
            tempPassword: res.data.data.temporaryPassword,
          });
        }
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tạo tài khoản.');
    }
  };

  // Submit Edit User
  const handleEditUser = async (values: any) => {
    if (!selectedUser) return;
    try {
      const res = await api.put<ApiResponse<User>>(`/users/${selectedUser.id}`, values);
      if (res.data.success) {
        message.success('Cập nhật tài khoản thành công!');
        setEditModalOpen(false);
        fetchUsers();
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể cập nhật tài khoản.');
    }
  };

  // Submit Lock User (with Handover Check)
  const handleLockUser = async (values: { reason: string }, force = false) => {
    if (!selectedUser) return;
    try {
      const res = await api.post<
        ApiResponse<{
          requiresHandoverWarning?: boolean;
          activeRequisitionCount?: number;
          message?: string;
        }>
      >(`/users/${selectedUser.id}/lock`, {
        reason: values.reason,
        force,
      });

      if (res.data.success) {
        if (res.data.data?.requiresHandoverWarning && !force) {
          // Trigger Handover Warning Dialog
          setLockModalOpen(false);
          setHandoverWarningData({
            count: res.data.data.activeRequisitionCount || 1,
            message: res.data.data.message || 'Người dùng đang phụ trách vị trí tuyển dụng.',
            reason: values.reason,
          });
          setHandoverWarningModalOpen(true);
        } else {
          message.success('Đã khóa tài khoản thành công và thu hồi tất cả phiên đăng nhập.');
          setLockModalOpen(false);
          setHandoverWarningModalOpen(false);
          fetchUsers();
        }
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể khóa tài khoản.');
    }
  };

  // Submit Unlock User
  const handleUnlockUser = async (record: User) => {
    try {
      const res = await api.post<ApiResponse>(`/users/${record.id}/unlock`);
      if (res.data.success) {
        message.success('Đã mở khóa tài khoản thành công.');
        fetchUsers();
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể mở khóa tài khoản.');
    }
  };

  // Assign Role to User
  const handleAssignRole = async (values: { roleId: string }) => {
    if (!selectedUser) return;
    try {
      const res = await api.post<ApiResponse>(`/users/${selectedUser.id}/roles`, {
        roleId: values.roleId,
      });
      if (res.data.success) {
        message.success('Gán vai trò thành công.');
        assignRoleForm.resetFields();
        // Refresh roles
        const updatedRoles = await api.get<ApiResponse<any[]>>(`/users/${selectedUser.id}/roles`);
        if (updatedRoles.data.success) setUserRolesList(updatedRoles.data.data);
        fetchUsers();
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể gán vai trò.');
    }
  };

  // Revoke Role from User
  const handleRevokeRole = async (roleId: string) => {
    if (!selectedUser) return;
    try {
      const res = await api.delete<ApiResponse>(`/users/${selectedUser.id}/roles/${roleId}`);
      if (res.data.success) {
        message.success('Thu hồi vai trò thành công.');
        const updatedRoles = await api.get<ApiResponse<any[]>>(`/users/${selectedUser.id}/roles`);
        if (updatedRoles.data.success) setUserRolesList(updatedRoles.data.data);
        fetchUsers();
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể thu hồi vai trò.');
    }
  };

  const roleNameMap: Record<string, { label: string; color: string }> = {
    ADMIN: { label: 'Admin', color: 'red' },
    HR_MANAGER: { label: 'HR Manager', color: 'purple' },
    RECRUITER: { label: 'Recruiter', color: 'blue' },
    HIRING_MANAGER: { label: 'Hiring Mgr', color: 'cyan' },
    INTERVIEWER: { label: 'Interviewer', color: 'green' },
    APPROVER: { label: 'Approver', color: 'gold' },
    CANDIDATE: { label: 'Candidate', color: 'default' },
  };

  // Table Columns
  const columns: ColumnsType<User> = [
    {
      title: 'Mã NV',
      dataIndex: 'employeeCode',
      key: 'employeeCode',
      width: 100,
      render: (text) => <Text strong>{text || '-'}</Text>,
    },
    {
      title: 'Họ và tên',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (text, record) => (
        <div>
          <div style={{ fontWeight: 600, color: '#1f1f1f' }}>{text}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.email}
          </Text>
        </div>
      ),
    },
    {
      title: 'Phòng ban',
      dataIndex: 'department',
      key: 'department',
      render: (dept) => (dept ? <Tag color="blue">{dept.name}</Tag> : <Text type="secondary">-</Text>),
    },
    {
      title: 'Chức danh',
      dataIndex: 'jobTitle',
      key: 'jobTitle',
      render: (text) => text || '-',
    },
    {
      title: 'Vai trò',
      dataIndex: 'roles',
      key: 'roles',
      render: (userRoles: Array<{ code: string; name: string }>) => (
        <Space wrap direction="horizontal" size={[0, 4]}>
          {userRoles?.map((r) => (
            <Tag key={r.code} color={roleNameMap[r.code]?.color || 'blue'}>
              {roleNameMap[r.code]?.label || r.name}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Trạng thái',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string, record) => {
        if (status === 'ACTIVE') return <Tag color="success">HOẠT ĐỘNG</Tag>;
        if (status === 'LOCKED') {
          return (
            <Tooltip title={`Lý do: ${record.lockReason || 'Không có'}`}>
              <Tag color="error">BỊ KHÓA</Tag>
            </Tooltip>
          );
        }
        return <Tag color="default">VÔ HIỆU</Tag>;
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 170,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Tooltip title="Xem chi tiết">
            <Button
              type="text"
              icon={<EyeOutlined />}
              onClick={() => handleOpenView(record)}
            />
          </Tooltip>

          {hasPermission('users.update') && (
            <Tooltip title="Chỉnh sửa thông tin">
              <Button
                type="text"
                icon={<EditOutlined style={{ color: '#1677ff' }} />}
                onClick={() => handleOpenEdit(record)}
              />
            </Tooltip>
          )}

          {hasPermission('roles.assign') && (
            <Tooltip title="Quản lý vai trò tài khoản">
              <Button
                type="text"
                icon={<SafetyCertificateOutlined style={{ color: '#722ed1' }} />}
                onClick={() => handleOpenRoles(record)}
              />
            </Tooltip>
          )}

          {record.status === 'LOCKED' ? (
            hasPermission('users.unlock') && (
              <Tooltip title="Mở khóa tài khoản">
                <Popconfirm
                  title="Mở khóa tài khoản?"
                  description={`Xác nhận cho phép ${record.fullName} đăng nhập lại vào hệ thống.`}
                  onConfirm={() => handleUnlockUser(record)}
                  okText="Mở khóa"
                  cancelText="Hủy"
                >
                  <Button type="text" icon={<UnlockOutlined style={{ color: '#52c41a' }} />} />
                </Popconfirm>
              </Tooltip>
            )
          ) : (
            hasPermission('users.lock') && (
              <Tooltip title="Khóa tài khoản">
                <Button
                  type="text"
                  danger
                  icon={<LockOutlined />}
                  onClick={() => handleOpenLock(record)}
                />
              </Tooltip>
            )
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
            <div>
              <Title level={4} style={{ margin: 0 }}>
                Quản Trị Người Dùng & Phân Quyền
              </Title>
              <Text type="secondary" style={{ fontSize: 13 }}>
                Danh sách tài khoản nội bộ IRMS, phân quyền vai trò và kiểm soát trạng thái đăng nhập
              </Text>
            </div>
            {hasPermission('users.create') && (
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => {
                  createForm.resetFields();
                  setCreateModalOpen(true);
                }}
              >
                Tạo tài khoản mới
              </Button>
            )}
          </div>
        }
      >
        {/* Filters */}
        <div style={{ marginBottom: 20, display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
          <Input
            placeholder="Tìm theo tên, email, SĐT, mã NV..."
            prefix={<SearchOutlined style={{ color: '#bfbfbf' }} />}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{ width: 280 }}
            allowClear
          />

          <Select
            placeholder="Phòng ban"
            style={{ width: 180 }}
            value={departmentId}
            onChange={(val) => setDepartmentId(val)}
            allowClear
          >
            {departments.map((d) => (
              <Option key={d.id} value={d.id}>
                {d.name}
              </Option>
            ))}
          </Select>

          <Select
            placeholder="Vai trò"
            style={{ width: 170 }}
            value={roleFilter}
            onChange={(val) => setRoleFilter(val)}
            allowClear
          >
            {roles.map((r) => (
              <Option key={r.code} value={r.code}>
                {r.name}
              </Option>
            ))}
          </Select>

          <Select
            placeholder="Trạng thái"
            style={{ width: 140 }}
            value={statusFilter}
            onChange={(val) => setStatusFilter(val)}
            allowClear
          >
            <Option value="ACTIVE">Hoạt động</Option>
            <Option value="LOCKED">Bị khóa</Option>
            <Option value="INACTIVE">Không hoạt động</Option>
          </Select>

          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              setSearch('');
              setDepartmentId(undefined);
              setRoleFilter(undefined);
              setStatusFilter(undefined);
              setPage(1);
              fetchUsers();
            }}
          >
            Làm mới
          </Button>
        </div>

        {/* Users Table */}
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page,
            pageSize,
            total,
            defaultPageSize: 20,
            showSizeChanger: true,
            pageSizeOptions: ['10', '20', '50'],
            onChange: (p, ps) => {
              setPage(p);
              setPageSize(ps);
            },
            showTotal: (tot) => `Tổng cộng ${tot} tài khoản`,
          }}
          scroll={{ x: 900 }}
        />
      </Card>

      {/* MODAL 1: CREATE USER */}
      <Modal
        title="Tạo Tài Khoản Nội Bộ Mới"
        open={createModalOpen}
        onCancel={() => setCreateModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Alert
          message="Cấp mật khẩu tự động"
          description="Hệ thống sẽ sinh mật khẩu tạm ngẫu nhiên an toàn và tự động xếp lịch gửi email kích hoạt tới địa chỉ email công ty của người dùng."
          type="info"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Form form={createForm} layout="vertical" onFinish={handleCreateUser}>
          <Form.Item
            name="fullName"
            label="Họ và tên"
            rules={[{ required: true, message: 'Vui lòng nhập họ và tên.' }]}
          >
            <Input placeholder="Ví dụ: Nguyễn Văn An" />
          </Form.Item>

          <Form.Item
            name="email"
            label="Email công ty"
            rules={[
              { required: true, message: 'Vui lòng nhập email.' },
              { type: 'email', message: 'Email không đúng định dạng.' },
            ]}
          >
            <Input placeholder="an.nv@company.local" />
          </Form.Item>

          <Form.Item name="phone" label="Số điện thoại">
            <Input placeholder="0901234567" />
          </Form.Item>

          <Form.Item name="jobTitle" label="Chức danh">
            <Input placeholder="Chuyên viên tuyển dụng / Kỹ sư phần mềm" />
          </Form.Item>

          <Form.Item name="departmentId" label="Phòng ban">
            <Select placeholder="Chọn phòng ban">
              {departments.map((d) => (
                <Option key={d.id} value={d.id}>
                  {d.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item
            name="roleIds"
            label="Vai trò nghiệp vụ"
            rules={[{ required: true, message: 'Vui lòng chọn ít nhất một vai trò.' }]}
          >
            <Select mode="multiple" placeholder="Chọn một hoặc nhiều vai trò">
              {roles.map((r) => (
                <Option key={r.id} value={r.id}>
                  {r.name} ({r.code})
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setCreateModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit">
                Tạo tài khoản & Cấp quyền
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL 2: EDIT USER */}
      <Modal
        title={`Chỉnh Sửa Tài Khoản: ${selectedUser?.fullName}`}
        open={editModalOpen}
        onCancel={() => setEditModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Form form={editForm} layout="vertical" onFinish={handleEditUser}>
          <Form.Item
            name="fullName"
            label="Họ và tên"
            rules={[{ required: true, message: 'Vui lòng nhập họ và tên.' }]}
          >
            <Input />
          </Form.Item>

          <Form.Item name="phone" label="Số điện thoại">
            <Input />
          </Form.Item>

          <Form.Item name="jobTitle" label="Chức danh">
            <Input />
          </Form.Item>

          <Form.Item name="departmentId" label="Phòng ban">
            <Select placeholder="Chọn phòng ban" allowClear>
              {departments.map((d) => (
                <Option key={d.id} value={d.id}>
                  {d.name}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="status" label="Trạng thái tài khoản">
            <Select>
              <Option value="ACTIVE">Hoạt động (ACTIVE)</Option>
              <Option value="LOCKED">Khóa tài khoản (LOCKED)</Option>
              <Option value="INACTIVE">Ngừng hoạt động (INACTIVE)</Option>
            </Select>
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setEditModalOpen(false)}>Hủy</Button>
              <Button type="primary" htmlType="submit">
                Lưu thay đổi
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL 3: LOCK USER (REASON MANDATORY) */}
      <Modal
        title={
          <Space>
            <LockOutlined style={{ color: '#ff4d4f' }} />
            <span>Khóa Tài Khoản: {selectedUser?.fullName}</span>
          </Space>
        }
        open={lockModalOpen}
        onCancel={() => setLockModalOpen(false)}
        footer={null}
        destroyOnClose
      >
        <Paragraph style={{ color: '#666', fontSize: 13 }}>
          Khi bị khóa, tài khoản sẽ <strong>bị thu hồi tất cả các phiên làm việc ngay lập tức</strong> và không thể đăng nhập vào hệ thống.
        </Paragraph>
        <Form form={lockForm} layout="vertical" onFinish={(val) => handleLockUser(val, false)}>
          <Form.Item
            name="reason"
            label="Lý do khóa tài khoản (Bắt buộc)"
            rules={[{ required: true, min: 3, message: 'Vui lòng nhập lý do khóa (tối thiểu 3 ký tự).' }]}
          >
            <Input.TextArea rows={3} placeholder="Ví dụ: Nhân sự đã nghỉ việc từ ngày 01/10/2026..." />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setLockModalOpen(false)}>Hủy</Button>
              <Button type="primary" danger htmlType="submit">
                Xác nhận khóa tài khoản
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL 4: HANDOVER WARNING MODAL */}
      <Modal
        title={
          <Space>
            <WarningOutlined style={{ color: '#faad14', fontSize: 20 }} />
            <span style={{ color: '#d46b08' }}>Cảnh Báo Bàn Giao Tuyển Dụng</span>
          </Space>
        }
        open={handoverWarningModalOpen}
        onCancel={() => setHandoverWarningModalOpen(false)}
        footer={[
          <Button key="cancel" onClick={() => setHandoverWarningModalOpen(false)}>
            Hủy thao tác
          </Button>,
          <Button
            key="confirm"
            type="primary"
            danger
            onClick={() => {
              if (handoverWarningData) {
                handleLockUser({ reason: handoverWarningData.reason }, true);
              }
            }}
          >
            Vẫn tiếp tục khóa tài khoản
          </Button>,
        ]}
      >
        <Alert
          message="Phát hiện vị trí tuyển dụng đang phụ trách!"
          description={handoverWarningData?.message}
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Paragraph style={{ fontSize: 13, color: '#333' }}>
          Người dùng này hiện đang được phân công phụ trách <strong>{handoverWarningData?.count}</strong> vị trí tuyển dụng đang ở trạng thái <code>OPEN</code>.
        </Paragraph>
        <Paragraph style={{ fontSize: 13, color: '#666' }}>
          Bạn nên kiểm tra và bàn giao lại các yêu cầu tuyển dụng cho nhân sự khác trước khi khóa. Nếu tiếp tục khóa, hệ thống sẽ bảo toàn dữ liệu hồ sơ và chỉ thu hồi quyền truy cập của tài khoản này.
        </Paragraph>
      </Modal>

      {/* MODAL 5: MANAGE ROLES */}
      <Modal
        title={`Quản Lý Vai Trò: ${selectedUser?.fullName}`}
        open={rolesModalOpen}
        onCancel={() => setRolesModalOpen(false)}
        footer={[
          <Button key="close" type="primary" onClick={() => setRolesModalOpen(false)}>
            Xong
          </Button>,
        ]}
        width={560}
      >
        <Paragraph style={{ fontSize: 13, color: '#666' }}>
          Một người dùng có thể được gán nhiều vai trò khác nhau (Ví dụ: Vừa là Hiring Manager vừa là Người phỏng vấn).
        </Paragraph>

        <div style={{ marginBottom: 16 }}>
          <Text strong>Các vai trò hiện tại:</Text>
          <div style={{ marginTop: 8 }}>
            {userRolesList.length === 0 ? (
              <Text type="secondary">Chưa có vai trò nào được gán.</Text>
            ) : (
              <Space wrap>
                {userRolesList.map((r) => {
                  const isSelfAdmin = selectedUser?.id === currentUser?.id && r.code === 'ADMIN';
                  return (
                    <Tag
                      key={r.id}
                      color={roleNameMap[r.code]?.color || 'blue'}
                      closable={!isSelfAdmin}
                      onClose={(e) => {
                        e.preventDefault();
                        if (isSelfAdmin) {
                          message.error('Bạn không thể tự thu hồi quyền quản trị của chính mình.');
                          return;
                        }
                        handleRevokeRole(r.id);
                      }}
                      style={{ padding: '4px 8px', fontSize: 13 }}
                    >
                      {r.name} {isSelfAdmin && '(Chính bạn - Không thể tự thu hồi)'}
                    </Tag>
                  );
                })}
              </Space>
            )}
          </div>
        </div>

        <Form form={assignRoleForm} layout="inline" onFinish={handleAssignRole} style={{ marginTop: 20 }}>
          <Form.Item
            name="roleId"
            rules={[{ required: true, message: 'Chọn vai trò cần thêm.' }]}
            style={{ minWidth: 260 }}
          >
            <Select placeholder="Chọn vai trò để gán thêm">
              {roles
                .filter((r) => !userRolesList.some((ur) => ur.id === r.id))
                .map((r) => (
                  <Option key={r.id} value={r.id}>
                    {r.name} ({r.code})
                  </Option>
                ))}
            </Select>
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              Gán thêm vai trò
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      {/* MODAL 6: TEMPORARY PASSWORD CREATED NOTICE */}
      <Modal
        title="Tài Khoản Đã Khởi Tạo Thành Công"
        open={!!createdTempPasswordData}
        onCancel={() => setCreatedTempPasswordData(null)}
        footer={[
          <Button key="close" type="primary" onClick={() => setCreatedTempPasswordData(null)}>
            Đã lưu thông tin
          </Button>,
        ]}
      >
        <Alert
          message="Email kích hoạt đã được gửi"
          description={`Email: ${createdTempPasswordData?.email}`}
          type="success"
          showIcon
          style={{ marginBottom: 16 }}
        />
        <Paragraph>
          Mật khẩu tạm đã được sinh ngẫu nhiên cho người dùng:
        </Paragraph>
        <div
          style={{
            background: '#f5f5f5',
            padding: '12px 16px',
            borderRadius: 6,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontFamily: 'monospace',
            fontSize: 16,
            fontWeight: 700,
            color: '#cf1322',
            marginBottom: 16,
          }}
        >
          <span>{createdTempPasswordData?.tempPassword}</span>
          <Button
            size="small"
            icon={<CopyOutlined />}
            onClick={() => {
              if (createdTempPasswordData?.tempPassword) {
                navigator.clipboard.writeText(createdTempPasswordData.tempPassword);
                message.success('Đã sao chép mật khẩu tạm vào clipboard!');
              }
            }}
          >
            Sao chép
          </Button>
        </div>
        <Paragraph style={{ fontSize: 12, color: '#888' }}>
          Người dùng sẽ được yêu cầu đổi mật khẩu trong lần đăng nhập đầu tiên.
        </Paragraph>
      </Modal>

      {/* DRAWER: VIEW USER DETAIL */}
      <Drawer
        title="Chi Tiết Hồ Sơ Người Dùng"
        open={viewDrawerOpen}
        onClose={() => setViewDrawerOpen(false)}
        width={480}
      >
        {selectedUser && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Mã nhân viên">{selectedUser.employeeCode || '-'}</Descriptions.Item>
            <Descriptions.Item label="Họ và tên">{selectedUser.fullName}</Descriptions.Item>
            <Descriptions.Item label="Email công ty">{selectedUser.email}</Descriptions.Item>
            <Descriptions.Item label="Số điện thoại">{selectedUser.phone || '-'}</Descriptions.Item>
            <Descriptions.Item label="Chức danh">{selectedUser.jobTitle || '-'}</Descriptions.Item>
            <Descriptions.Item label="Phòng ban">{selectedUser.department?.name || '-'}</Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              {selectedUser.status === 'ACTIVE' ? (
                <Tag color="success">Hoạt động</Tag>
              ) : selectedUser.status === 'LOCKED' ? (
                <Tag color="error">Bị khóa</Tag>
              ) : (
                <Tag color="default">Vô hiệu</Tag>
              )}
            </Descriptions.Item>
            {selectedUser.lockReason && (
              <Descriptions.Item label="Lý do khóa">{selectedUser.lockReason}</Descriptions.Item>
            )}
            <Descriptions.Item label="Vị trí phụ trách (OPEN)">
              <Tag color={selectedUser.activeRequisitionCount ? 'orange' : 'default'}>
                {selectedUser.activeRequisitionCount || 0} vị trí
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Vai trò gán">
              <Space wrap>
                {selectedUser.roles?.map((r) => (
                  <Tag key={r.code} color={roleNameMap[r.code]?.color || 'blue'}>
                    {roleNameMap[r.code]?.label || r.name}
                  </Tag>
                ))}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Lần đăng nhập cuối">
              {selectedUser.lastLoginAt ? new Date(selectedUser.lastLoginAt).toLocaleString('vi-VN') : 'Chưa đăng nhập'}
            </Descriptions.Item>
            <Descriptions.Item label="Ngày khởi tạo">
              {new Date(selectedUser.createdAt).toLocaleString('vi-VN')}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>
    </div>
  );
};
