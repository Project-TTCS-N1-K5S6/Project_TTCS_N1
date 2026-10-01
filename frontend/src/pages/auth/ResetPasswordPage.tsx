import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, Result } from 'antd';
import { LockOutlined, CheckCircleOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import { useSearchParams, Link } from 'react-router-dom';
import { api } from '../../services/api';
import { ApiResponse } from '../../types';

const { Title, Text } = Typography;

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const onFinish = async (values: any) => {
    if (!token) {
      setErrorMessage('Mã xác thực token không tồn tại trên đường dẫn.');
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      const res = await api.post<ApiResponse>('/auth/reset-password', {
        token,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });

      if (res.data.success) {
        setSuccess(true);
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Không thể đặt lại mật khẩu.';
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
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
        <Card style={{ width: '100%', maxWidth: 440, borderRadius: 12, textAlign: 'center' }}>
          <Result
            status="warning"
            title="Thiếu mã xác thực"
            subTitle="Đường dẫn không chứa token đặt lại mật khẩu hợp lệ."
            extra={
              <Link to="/login">
                <Button type="primary">Về trang đăng nhập</Button>
              </Link>
            }
          />
        </Card>
      </div>
    );
  }

  if (success) {
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
        <Card style={{ width: '100%', maxWidth: 440, borderRadius: 12, textAlign: 'center' }}>
          <Result
            status="success"
            title="Đặt lại mật khẩu thành công!"
            subTitle="Mật khẩu của bạn đã được cập nhật an toàn. Mọi phiên đăng nhập khác đã được thu hồi."
            extra={
              <Link to="/login">
                <Button type="primary" size="large">
                  Đăng nhập ngay
                </Button>
              </Link>
            }
          />
        </Card>
      </div>
    );
  }

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
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ marginBottom: 4 }}>
            Đặt Lại Mật Khẩu
          </Title>
          <Text type="secondary">Nhập mật khẩu mới bảo mật cho tài khoản của bạn</Text>
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

        <Form layout="vertical" onFinish={onFinish} size="large">
          <Form.Item
            name="newPassword"
            label="Mật khẩu mới"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu mới.' },
              { min: 8, message: 'Mật khẩu phải có tối thiểu 8 ký tự.' },
              { pattern: /[a-zA-Z]/, message: 'Mật khẩu phải chứa ít nhất 1 chữ cái.' },
              { pattern: /[0-9]/, message: 'Mật khẩu phải chứa ít nhất 1 chữ số.' },
            ]}
          >
            <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="Tối thiểu 8 ký tự" />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            label="Xác nhận mật khẩu mới"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp!'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="Nhập lại mật khẩu" />
          </Form.Item>

          <Form.Item style={{ marginTop: 24 }}>
            <Button type="primary" htmlType="submit" loading={loading} block icon={<CheckCircleOutlined />}>
              Xác nhận đổi mật khẩu
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center', marginTop: 16 }}>
            <Link to="/login" style={{ color: '#666', fontSize: 13 }}>
              <ArrowLeftOutlined style={{ marginRight: 6 }} /> Quay lại trang đăng nhập
            </Link>
          </div>
        </Form>
      </Card>
    </div>
  );
};
