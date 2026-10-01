import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Space, Divider, Tag } from 'antd';
import { UserOutlined, LockOutlined, LoginOutlined } from '@ant-design/icons';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useAuth } from '../../app/context/AuthContext';

const { Title, Text, Paragraph } = Typography;

export const LoginPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as any)?.from?.pathname || '/';

  const onFinish = async (values: any) => {
    setLoading(true);
    setErrorMessage(null);

    try {
      await login(values.email, values.password);
      navigate(from, { replace: true });
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Email hoặc mật khẩu không chính xác.';
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemo = (email: string) => {
    form.setFieldsValue({
      email,
      password: 'Admin@123456',
    });
    setErrorMessage(null);
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #001529 0%, #003a8c 50%, #0958d9 100%)',
        padding: '20px',
      }}
    >
      <Card
        style={{
          width: '100%',
          maxWidth: 440,
          borderRadius: 12,
          boxShadow: '0 20px 40px rgba(0,0,0,0.3)',
          border: 'none',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: 12,
              background: '#1677ff',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#fff',
              fontSize: 26,
              fontWeight: 800,
              marginBottom: 12,
              boxShadow: '0 4px 12px rgba(22, 119, 255, 0.4)',
            }}
          >
            I
          </div>
          <Title level={3} style={{ marginBottom: 4 }}>
            IRMS Đăng Nhập
          </Title>
          <Text type="secondary">Hệ thống Tuyển dụng Nội bộ Doanh nghiệp</Text>
        </div>

        {errorMessage && (
          <Alert
            message={errorMessage}
            type="error"
            showIcon
            closable
            onClose={() => setErrorMessage(null)}
            style={{ marginBottom: 20 }}
          />
        )}

        <Form form={form} layout="vertical" onFinish={onFinish} requiredMark={false} size="large">
          <Form.Item
            name="email"
            label="Email công ty"
            rules={[
              { required: true, message: 'Vui lòng nhập email công ty.' },
              { type: 'email', message: 'Email không đúng định dạng.' },
            ]}
          >
            <Input prefix={<UserOutlined style={{ color: '#bfbfbf' }} />} placeholder="ten@company.local" />
          </Form.Item>

          <Form.Item
            name="password"
            label={
              <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                <span>Mật khẩu</span>
                <Link to="/forgot-password" style={{ fontSize: 13 }}>
                  Quên mật khẩu?
                </Link>
              </div>
            }
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu.' }]}
          >
            <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="••••••••" />
          </Form.Item>

          <Form.Item style={{ marginTop: 24 }}>
            <Button type="primary" htmlType="submit" loading={loading} block icon={<LoginOutlined />}>
              Đăng nhập hệ thống
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: '16px 0', fontSize: 12, color: '#888' }}>Tài khoản mẫu trải nghiệm</Divider>

        <Paragraph style={{ fontSize: 12, color: '#666', marginBottom: 8, textAlign: 'center' }}>
          Nhấp nhanh để điền tài khoản mẫu (Mật khẩu: <code>Admin@123456</code>):
        </Paragraph>

        <Space wrap direction="horizontal" style={{ justifyContent: 'center', width: '100%' }}>
          <Tag
            color="red"
            style={{ cursor: 'pointer', padding: '3px 8px' }}
            onClick={() => handleFillDemo('admin@company.local')}
          >
            Admin
          </Tag>
          <Tag
            color="purple"
            style={{ cursor: 'pointer', padding: '3px 8px' }}
            onClick={() => handleFillDemo('hr.manager@company.local')}
          >
            HR Manager
          </Tag>
          <Tag
            color="blue"
            style={{ cursor: 'pointer', padding: '3px 8px' }}
            onClick={() => handleFillDemo('recruiter@company.local')}
          >
            Recruiter
          </Tag>
          <Tag
            color="cyan"
            style={{ cursor: 'pointer', padding: '3px 8px' }}
            onClick={() => handleFillDemo('hiring.manager@company.local')}
          >
            Hiring Mgr
          </Tag>
          <Tag
            color="green"
            style={{ cursor: 'pointer', padding: '3px 8px' }}
            onClick={() => handleFillDemo('interviewer@company.local')}
          >
            Interviewer
          </Tag>
          <Tag
            color="magenta"
            style={{ cursor: 'pointer', padding: '3px 8px' }}
            onClick={() => handleFillDemo('leader.tech@company.local')}
          >
            Dual Role (HM+Interviewer)
          </Tag>
        </Space>
      </Card>
    </div>
  );
};
