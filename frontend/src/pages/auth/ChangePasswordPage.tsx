import React, { useState } from 'react';
import { Form, Input, Button, Card, Typography, Alert, message } from 'antd';
import { LockOutlined, KeyOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { api } from '../../services/api';
import { ApiResponse } from '../../types';

const { Title, Text, Paragraph } = Typography;

export const ChangePasswordPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const navigate = useNavigate();

  const onFinish = async (values: any) => {
    setLoading(true);
    setErrorMessage(null);

    try {
      const res = await api.post<ApiResponse>('/auth/change-password', {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });

      if (res.data.success) {
        message.success('Đổi mật khẩu thành công! Các phiên đăng nhập trên thiết bị khác đã được thu hồi.');
        form.resetFields();
        navigate('/');
      }
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || 'Không thể đổi mật khẩu.';
      setErrorMessage(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 560, margin: '20px auto', width: '100%' }}>
      <Card
        title={
          <div style={{ padding: '8px 0' }}>
            <Title level={4} style={{ margin: 0 }}>
              <KeyOutlined style={{ marginRight: 8, color: '#1677ff' }} />
              Đổi Mật Khẩu
            </Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Cập nhật mật khẩu định kỳ hoặc sau khi nhận mật khẩu tạm từ quản trị viên
            </Text>
          </div>
        }
      >
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

        <Paragraph style={{ fontSize: 13, color: '#666', background: '#f6ffed', border: '1px solid #b7eb8f', padding: '10px 14px', borderRadius: 6 }}>
          <strong>Lưu ý bảo mật:</strong> Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm ít nhất 1 chữ cái và 1 chữ số. Khi đổi mật khẩu thành công, mọi phiên đăng nhập khác của bạn sẽ tự động bị thu hồi.
        </Paragraph>

        <Form form={form} layout="vertical" onFinish={onFinish} size="large" style={{ marginTop: 16 }}>
          <Form.Item
            name="currentPassword"
            label="Mật khẩu hiện tại"
            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại.' }]}
          >
            <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="Mật khẩu đang sử dụng" />
          </Form.Item>

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
              { required: true, message: 'Vui lòng xác nhận mật khẩu mới.' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không trùng khớp!'));
                },
              }),
            ]}
          >
            <Input.Password prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} placeholder="Nhập lại mật khẩu mới" />
          </Form.Item>

          <Form.Item style={{ marginTop: 24 }}>
            <Button type="primary" htmlType="submit" loading={loading} block icon={<KeyOutlined />}>
              Cập nhật mật khẩu
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};
