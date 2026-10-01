import React, { useState } from 'react';
import {
  Layout,
  Menu,
  Button,
  Avatar,
  Dropdown,
  Tag,
  Space,
  Drawer,
  Modal,
  Descriptions,
  Tooltip,
} from 'antd';
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
  TeamOutlined,
  SafetyCertificateOutlined,
  HistoryOutlined,
  UserOutlined,
  KeyOutlined,
  LogoutOutlined,
  InfoCircleOutlined,
  MailOutlined,
} from '@ant-design/icons';
import { useNavigate, useLocation, Outlet } from 'react-router-dom';
import { useAuth } from '../../app/context/AuthContext';
import { EmailOutboxDrawer } from '../email/EmailOutboxDrawer';

const { Header, Sider, Content, Footer } = Layout;

export const AppLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const [profileModalOpen, setProfileModalOpen] = useState(false);
  const [emailOutboxOpen, setEmailOutboxOpen] = useState(false);

  const { user, roles, logout, hasPermission } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Filter menu items by permission
  const menuItems = [
    {
      key: '/',
      icon: <DashboardOutlined />,
      label: 'Tổng quan (Dashboard)',
    },
    ...(hasPermission('users.view')
      ? [
          {
            key: '/admin/users',
            icon: <TeamOutlined />,
            label: 'Quản lý người dùng',
          },
        ]
      : []),
    ...(hasPermission('roles.view')
      ? [
          {
            key: '/admin/roles',
            icon: <SafetyCertificateOutlined />,
            label: 'Vai trò & Phân quyền',
          },
        ]
      : []),
    ...(hasPermission('audit.view')
      ? [
          {
            key: '/admin/audit-logs',
            icon: <HistoryOutlined />,
            label: 'Nhật ký hệ thống',
          },
        ]
      : []),
  ];

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key);
    setMobileDrawerOpen(false);
  };

  const roleNameMap: Record<string, { label: string; color: string }> = {
    ADMIN: { label: 'Quản trị hệ thống', color: 'red' },
    HR_MANAGER: { label: 'Trưởng phòng Nhân sự', color: 'purple' },
    RECRUITER: { label: 'Chuyên viên tuyển dụng', color: 'blue' },
    HIRING_MANAGER: { label: 'Trưởng bộ phận', color: 'cyan' },
    INTERVIEWER: { label: 'Người phỏng vấn', color: 'green' },
    APPROVER: { label: 'Người phê duyệt', color: 'gold' },
    CANDIDATE: { label: 'Ứng viên', color: 'default' },
  };

  const primaryRole = roles[0] || 'USER';
  const roleInfo = roleNameMap[primaryRole] || { label: primaryRole, color: 'blue' };

  const userDropdownItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: 'Hồ sơ cá nhân',
      onClick: () => setProfileModalOpen(true),
    },
    {
      key: 'change-password',
      icon: <KeyOutlined />,
      label: 'Đổi mật khẩu',
      onClick: () => navigate('/change-password'),
    },
    {
      type: 'divider' as const,
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      danger: true,
      label: 'Đăng xuất',
      onClick: () => logout(),
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* Desktop Sider */}
      <Sider
        trigger={null}
        collapsible
        collapsed={collapsed}
        breakpoint="lg"
        collapsedWidth={80}
        onBreakpoint={(broken) => {
          if (broken) setCollapsed(true);
        }}
        className="desktop-sider"
        style={{
          overflow: 'auto',
          height: '100vh',
          position: 'sticky',
          top: 0,
          left: 0,
          background: '#001529',
          boxShadow: '2px 0 8px rgba(0,21,41,0.35)',
          zIndex: 10,
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start',
            padding: collapsed ? '0' : '0 20px',
            color: '#fff',
            fontWeight: 700,
            fontSize: collapsed ? '18px' : '17px',
            letterSpacing: '0.5px',
            borderBottom: '1px solid rgba(255,255,255,0.1)',
            cursor: 'pointer',
          }}
          onClick={() => navigate('/')}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 6,
              background: '#1677ff',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              fontWeight: 800,
              marginRight: collapsed ? 0 : 10,
            }}
          >
            I
          </div>
          {!collapsed && <span>IRMS RECRUIT</span>}
        </div>

        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ marginTop: 12 }}
        />
      </Sider>

      {/* Mobile Drawer Navigation */}
      <Drawer
        title="Menu Chức Năng"
        placement="left"
        onClose={() => setMobileDrawerOpen(false)}
        open={mobileDrawerOpen}
        bodyStyle={{ padding: 0, background: '#001529' }}
        width={260}
      >
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Drawer>

      <Layout style={{ minWidth: 0 }}>
        {/* Header */}
        <Header
          style={{
            padding: '0 20px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,21,41,0.08)',
            position: 'sticky',
            top: 0,
            zIndex: 9,
          }}
        >
          <Space>
            {/* Toggle Button for Desktop */}
            <Button
              type="text"
              icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={() => setCollapsed(!collapsed)}
              className="desktop-toggle-btn"
              style={{ fontSize: 16 }}
            />
            {/* Toggle Button for Mobile */}
            <Button
              type="text"
              icon={<MenuUnfoldOutlined />}
              onClick={() => setMobileDrawerOpen(true)}
              className="mobile-toggle-btn"
              style={{ fontSize: 16 }}
            />
            <span style={{ fontWeight: 600, fontSize: 15, color: '#1f1f1f' }} className="header-title">
              Hệ thống Tuyển dụng Nội bộ (IRMS)
            </span>
          </Space>

          {/* User profile and Email Outbox on the right */}
          <Space direction="horizontal" size="middle">
            <Tooltip title="Hộp thư hệ thống (Email Outbox / Xem mật khẩu tạm & link đặt lại)">
              <Button
                type="text"
                icon={<MailOutlined style={{ fontSize: 17, color: '#1677ff' }} />}
                onClick={() => setEmailOutboxOpen(true)}
              />
            </Tooltip>

            <Dropdown menu={{ items: userDropdownItems }} trigger={['click']}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  cursor: 'pointer',
                  padding: '4px 10px',
                  borderRadius: 6,
                  transition: 'background 0.2s',
                }}
                className="user-dropdown-btn"
              >
                <Avatar
                  style={{
                    backgroundColor: '#1677ff',
                    verticalAlign: 'middle',
                    marginRight: 8,
                  }}
                  size="small"
                >
                  {user?.fullName?.charAt(0).toUpperCase() || 'U'}
                </Avatar>
                <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
                  <span style={{ fontWeight: 600, fontSize: 13, color: '#333' }}>
                    {user?.fullName || 'Người dùng'}
                  </span>
                  <Tag color={roleInfo.color} style={{ fontSize: 10, margin: 0, padding: '0 4px', lineHeight: '16px' }}>
                    {roleInfo.label}
                  </Tag>
                </div>
              </div>
            </Dropdown>
          </Space>
        </Header>

        {/* Content Area */}
        <Content
          style={{
            margin: '20px',
            minHeight: 280,
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <Outlet />
        </Content>

        {/* Footer */}
        <Footer style={{ textAlign: 'center', color: '#888', padding: '16px 20px', fontSize: 12 }}>
          IRMS © 2026 - Internal Recruitment Management System. All rights reserved.
        </Footer>
      </Layout>

      {/* Profile Detail Modal */}
      <Modal
        title={
          <Space>
            <InfoCircleOutlined style={{ color: '#1677ff' }} />
            <span>Thông Tin Tài Khoản Nội Bộ</span>
          </Space>
        }
        open={profileModalOpen}
        onCancel={() => setProfileModalOpen(false)}
        footer={[
          <Button key="close" type="primary" onClick={() => setProfileModalOpen(false)}>
            Đóng
          </Button>,
        ]}
      >
        <Descriptions column={1} bordered size="small" style={{ marginTop: 16 }}>
          <Descriptions.Item label="Mã nhân viên">{user?.employeeCode || 'Chưa cập nhật'}</Descriptions.Item>
          <Descriptions.Item label="Họ và tên">{user?.fullName}</Descriptions.Item>
          <Descriptions.Item label="Email công ty">{user?.email}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{user?.phone || 'Chưa cập nhật'}</Descriptions.Item>
          <Descriptions.Item label="Chức danh">{user?.jobTitle || 'Chưa cập nhật'}</Descriptions.Item>
          <Descriptions.Item label="Phòng ban">{user?.department?.name || 'Chưa phân bổ'}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <Tag color={user?.status === 'ACTIVE' ? 'green' : 'red'}>
              {user?.status === 'ACTIVE' ? 'Hoạt động' : user?.status === 'LOCKED' ? 'Bị khóa' : 'Không hoạt động'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Vai trò gán">
            <Space wrap>
              {user?.roles?.map((r) => (
                <Tag key={r.code} color={roleNameMap[r.code]?.color || 'blue'}>
                  {roleNameMap[r.code]?.label || r.name}
                </Tag>
              ))}
            </Space>
          </Descriptions.Item>
        </Descriptions>
      </Modal>

      {/* Email Outbox Drawer */}
      <EmailOutboxDrawer
        open={emailOutboxOpen}
        onClose={() => setEmailOutboxOpen(false)}
      />
    </Layout>
  );
};
