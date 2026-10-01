import React from 'react';
import { Card, Row, Col, Typography, Tag, Space, Button, Alert, Statistic } from 'antd';
import {
  TeamOutlined,
  SafetyCertificateOutlined,
  HistoryOutlined,
  KeyOutlined,
  CheckCircleOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../app/context/AuthContext';

const { Title, Text, Paragraph } = Typography;

export const DashboardPage: React.FC = () => {
  const { user, roles, hasPermission } = useAuth();
  const navigate = useNavigate();

  const roleLabels: Record<string, { name: string; color: string }> = {
    ADMIN: { name: 'Quản trị hệ thống', color: 'red' },
    HR_MANAGER: { name: 'Trưởng phòng Nhân sự', color: 'purple' },
    RECRUITER: { name: 'Chuyên viên tuyển dụng', color: 'blue' },
    HIRING_MANAGER: { name: 'Trưởng bộ phận tuyển dụng', color: 'cyan' },
    INTERVIEWER: { name: 'Người phỏng vấn', color: 'green' },
    APPROVER: { name: 'Người phê duyệt', color: 'gold' },
    CANDIDATE: { name: 'Ứng viên', color: 'default' },
  };

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
      {/* Welcome Banner */}
      <Card
        style={{
          marginBottom: 24,
          background: 'linear-gradient(135deg, #1677ff 0%, #0958d9 100%)',
          color: '#fff',
          borderRadius: 12,
          border: 'none',
        }}
      >
        <Row align="middle" justify="space-between">
          <Col xs={24} md={16}>
            <Title level={2} style={{ color: '#fff', marginBottom: 8 }}>
              Xin chào, {user?.fullName}!
            </Title>
            <Paragraph style={{ color: 'rgba(255, 255, 255, 0.9)', fontSize: 15, marginBottom: 16 }}>
              Chào mừng bạn đến với Hệ thống Quản lý Tuyển dụng Nội bộ (IRMS). Bạn đang đăng nhập với quyền hạn được phân bổ theo ma trận vai trò nội bộ.
            </Paragraph>
            <Space wrap>
              {roles.map((r) => (
                <Tag
                  key={r}
                  color={roleLabels[r]?.color || 'blue'}
                  style={{ fontSize: 13, padding: '4px 12px', borderRadius: 16, border: 'none', fontWeight: 600 }}
                >
                  {roleLabels[r]?.name || r}
                </Tag>
              ))}
              {user?.department && (
                <Tag style={{ fontSize: 13, padding: '4px 12px', borderRadius: 16, background: 'rgba(255,255,255,0.2)', color: '#fff', border: 'none' }}>
                  Phòng ban: {user.department.name}
                </Tag>
              )}
            </Space>
          </Col>
          <Col xs={24} md={8} style={{ textAlign: 'right', marginTop: 16 }}>
            <Button
              type="default"
              size="large"
              icon={<KeyOutlined />}
              onClick={() => navigate('/change-password')}
              style={{ background: '#fff', fontWeight: 600, color: '#1677ff' }}
            >
              Đổi mật khẩu
            </Button>
          </Col>
        </Row>
      </Card>

      {/* System Overview Notice */}
      <Alert
        message="Hệ thống Tuyển dụng Nội bộ (IRMS) — Phiên bản Doanh nghiệp"
        description="Hệ thống tích hợp Quản trị Tài khoản, Phân quyền bảo mật đa tầng (RBAC), Quản trị Người dùng và Kiểm soát truy cập phân tầng. Menu điều hướng được tự động tối ưu theo vai trò và quyền hạn được cấp."
        type="info"
        showIcon
        icon={<CheckCircleOutlined />}
        style={{ marginBottom: 24, borderRadius: 8 }}
      />

      {/* Quick Statistics Overview */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false}>
            <Statistic
              title="Trạng thái tài khoản"
              value={user?.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm khóa'}
              valueStyle={{ color: user?.status === 'ACTIVE' ? '#3f8600' : '#cf1322', fontSize: 20 }}
              prefix={<UserOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Mã NV: {user?.employeeCode || 'Chưa gán'}
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false}>
            <Statistic
              title="Số vai trò được gán"
              value={roles.length}
              valueStyle={{ color: '#1677ff' }}
              prefix={<SafetyCertificateOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Cơ chế gán đa vai trò linh hoạt
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false}>
            <Statistic
              title="Quyền hạn truy cập"
              value={roles.includes('ADMIN') ? 'Toàn quyền (Admin)' : `${user?.permissions?.length || 0} quyền`}
              valueStyle={{ color: '#722ed1', fontSize: 20 }}
              prefix={<CheckCircleOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Phân quyền cấp độ chức năng
            </Text>
          </Card>
        </Col>

        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false}>
            <Statistic
              title="Phiên làm việc"
              value="An toàn (JWT + Refresh)"
              valueStyle={{ color: '#fa8c16', fontSize: 18 }}
              prefix={<HistoryOutlined />}
            />
            <Text type="secondary" style={{ fontSize: 12 }}>
              Tự động gia hạn khi hoạt động
            </Text>
          </Card>
        </Col>
      </Row>

      {/* Accessible Features & Quick Navigation */}
      <Card title="Chức Năng Được Phép Sử Dụng" style={{ borderRadius: 8 }}>
        <Row gutter={[16, 16]}>
          {hasPermission('users.view') && (
            <Col xs={24} sm={12} md={8}>
              <Card
                hoverable
                onClick={() => navigate('/admin/users')}
                style={{ height: '100%', border: '1px solid #d9d9d9', borderRadius: 8 }}
              >
                <Space direction="vertical" size="small">
                  <TeamOutlined style={{ fontSize: 32, color: '#1677ff' }} />
                  <Title level={5} style={{ margin: 0 }}>
                    Quản lý người dùng
                  </Title>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    Tạo tài khoản nội bộ, cấp mật khẩu tạm, khóa/mở khóa tài khoản và phân quyền vai trò.
                  </Text>
                </Space>
              </Card>
            </Col>
          )}

          {hasPermission('roles.view') && (
            <Col xs={24} sm={12} md={8}>
              <Card
                hoverable
                onClick={() => navigate('/admin/roles')}
                style={{ height: '100%', border: '1px solid #d9d9d9', borderRadius: 8 }}
              >
                <Space direction="vertical" size="small">
                  <SafetyCertificateOutlined style={{ fontSize: 32, color: '#722ed1' }} />
                  <Title level={5} style={{ margin: 0 }}>
                    Vai trò & Phân quyền
                  </Title>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    Xem ma trận phân quyền và cấu hình chi tiết quyền hạn cho từng vai trò nghiệp vụ.
                  </Text>
                </Space>
              </Card>
            </Col>
          )}

          {hasPermission('audit.view') && (
            <Col xs={24} sm={12} md={8}>
              <Card
                hoverable
                onClick={() => navigate('/admin/audit-logs')}
                style={{ height: '100%', border: '1px solid #d9d9d9', borderRadius: 8 }}
              >
                <Space direction="vertical" size="small">
                  <HistoryOutlined style={{ fontSize: 32, color: '#fa8c16' }} />
                  <Title level={5} style={{ margin: 0 }}>
                    Nhật ký hệ thống
                  </Title>
                  <Text type="secondary" style={{ fontSize: 13 }}>
                    Theo dõi lịch sử đăng nhập, thay đổi mật khẩu, thao tác khóa và gán quyền tài khoản.
                  </Text>
                </Space>
              </Card>
            </Col>
          )}

          <Col xs={24} sm={12} md={8}>
            <Card
              hoverable
              onClick={() => navigate('/change-password')}
              style={{ height: '100%', border: '1px solid #d9d9d9', borderRadius: 8 }}
            >
              <Space direction="vertical" size="small">
                <KeyOutlined style={{ fontSize: 32, color: '#52c41a' }} />
                <Title level={5} style={{ margin: 0 }}>
                  Đổi mật khẩu tài khoản
                </Title>
                <Text type="secondary" style={{ fontSize: 13 }}>
                  Chủ động đổi mật khẩu định kỳ để bảo vệ tài khoản cá nhân.
                </Text>
              </Space>
            </Card>
          </Col>
        </Row>
      </Card>
    </div>
  );
};
