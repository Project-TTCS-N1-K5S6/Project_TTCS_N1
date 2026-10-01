import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Space } from 'antd';
import { MailOutlined, ArrowLeftOutlined, CheckCircleOutlined, KeyOutlined } from '@ant-design/icons';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { ApiResponse } from '../../types';

const { Title, Text, Paragraph } = Typography;

export const ForgotPasswordPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const [responseMessage, setResponseMessage] = useState<string>('');
  const [devResetUrl, setDevResetUrl] = useState<string | null>(null);

  const onFinish = async (values: { email: string }) => {
    setLoading(true);

    try {
      const res = await api.post<ApiResponse<{ devResetUrl?: string }>>('/auth/forgot-password', {
        email: values.email,
      });

      setResponseMessage(
        res.data.message || 'Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.'
      );

      if (res.data.data?.devResetUrl) {
        setDevResetUrl(res.data.data.devResetUrl);
      }

      setSubmitted(true);
    } catch {
      // Always display generic message even on error for security
      setResponseMessage('Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.');
      setSubmitted(true);
    } finally {
      setLoading(false);
    }
  };

  const handleGoToReset = () => {
    if (!devResetUrl) return;
    try {
      const url = new URL(devResetUrl);
      const search = url.search;
      navigate(`/reset-password${search}`);
    } catch {
      window.location.href = devResetUrl;
    }
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
          maxWidth: 460,
          borderRadius: 12,
          boxShadow: '0 20px 40px rgba(0,0,0,0.3)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ marginBottom: 4 }}>
            Quên Mật Khẩu
          </Title>
          <Text type="secondary">Nhập email công ty để nhận liên kết đặt lại mật khẩu</Text>
        </div>

        {submitted ? (
          <div>
            <Alert
              message="Đã tiếp nhận yêu cầu"
              description={responseMessage}
              type="success"
              showIcon
              icon={<CheckCircleOutlined />}
              style={{ marginBottom: 20 }}
            />
            <Paragraph style={{ fontSize: 13, color: '#666' }}>
              Vui lòng kiểm tra hộp thư email của bạn để tiếp tục quy trình đặt lại mật khẩu. Liên kết có hiệu lực trong 30 phút.
            </Paragraph>

            {devResetUrl && (
              <div
                style={{
                  background: '#e6f7ff',
                  border: '1px solid #91d5ff',
                  borderRadius: 8,
                  padding: 16,
                  marginBottom: 20,
                  textAlign: 'center',
                }}
              >
                <div style={{ color: '#0050b3', fontWeight: 600, marginBottom: 8 }}>
                  Liên kết đặt lại mật khẩu trực tiếp
                </div>
                <Button
                  type="primary"
                  icon={<KeyOutlined />}
                  onClick={handleGoToReset}
                  block
                  style={{ backgroundColor: '#1890ff', height: 40 }}
                >
                  Tiến hành Đặt lại Mật khẩu ngay
                </Button>
              </div>
            )}

            <Space direction="vertical" style={{ width: '100%' }}>
              <Link to="/login" style={{ width: '100%' }}>
                <Button type="default" block icon={<ArrowLeftOutlined />}>
                  Quay lại đăng nhập
                </Button>
              </Link>
            </Space>
          </div>
        ) : (
          <Form layout="vertical" onFinish={onFinish} size="large">
            <Form.Item
              name="email"
              label="Email công ty"
              rules={[
                { required: true, message: 'Vui lòng nhập email.' },
                { type: 'email', message: 'Email không đúng định dạng.' },
              ]}
            >
              <Input prefix={<MailOutlined style={{ color: '#bfbfbf' }} />} placeholder="ten@irms.local" />
            </Form.Item>

            <Form.Item style={{ marginTop: 24 }}>
              <Button type="primary" htmlType="submit" loading={loading} block>
                Gửi hướng dẫn đặt lại mật khẩu
              </Button>
            </Form.Item>

            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Link to="/login" style={{ color: '#666', fontSize: 13 }}>
                <ArrowLeftOutlined style={{ marginRight: 6 }} /> Quay lại trang đăng nhập
              </Link>
            </div>
          </Form>
        )}
      </Card>
    </div>
  );
};
