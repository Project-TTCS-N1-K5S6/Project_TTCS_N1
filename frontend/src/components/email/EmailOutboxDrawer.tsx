import React, { useState, useEffect } from 'react';
import {
  Drawer,
  List,
  Tag,
  Button,
  Typography,
  Space,
  Empty,
  Spin,
  Tooltip,
  message,
} from 'antd';
import {
  MailOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  KeyOutlined,
  CopyOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { api } from '../../services/api';
import { ApiResponse } from '../../types';
import { useNavigate } from 'react-router-dom';

const { Text, Paragraph } = Typography;

interface EmailOutboxItem {
  id: string;
  recipient: string;
  subject: string;
  template: string;
  payload: any;
  status: string;
  last_error?: string;
  created_at: string;
  sent_at: string;
}

interface EmailOutboxDrawerProps {
  open: boolean;
  onClose: () => void;
}

export const EmailOutboxDrawer: React.FC<EmailOutboxDrawerProps> = ({ open, onClose }) => {
  const [emails, setEmails] = useState<EmailOutboxItem[]>([]);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const fetchEmails = async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<EmailOutboxItem[]>>('/email-outbox');
      if (res.data.success) {
        setEmails(res.data.data || []);
      }
    } catch {
      // Quiet fail if not allowed or dev endpoint unreachable
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open) {
      fetchEmails();
    }
  }, [open]);

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    message.success(`Đã sao chép ${label}!`);
  };

  const handleNavigateReset = (resetUrl: string) => {
    try {
      const url = new URL(resetUrl);
      navigate(`/reset-password${url.search}`);
      onClose();
    } catch {
      window.open(resetUrl, '_blank');
    }
  };

  return (
    <Drawer
      title={
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
          <Space>
            <MailOutlined style={{ color: '#1677ff' }} />
            <span>Hộp Thư Hệ Thống (Email Outbox)</span>
          </Space>
          <Button
            type="text"
            size="small"
            icon={<ReloadOutlined spin={loading} />}
            onClick={fetchEmails}
          >
            Làm mới
          </Button>
        </div>
      }
      placement="right"
      width={520}
      open={open}
      onClose={onClose}
    >
      <Paragraph type="secondary" style={{ fontSize: 13, marginBottom: 16 }}>
        Ghi nhận danh sách thư điện tử đã được hệ thống tạo và gửi (mật khẩu tạm thời, liên kết đặt lại mật khẩu).
      </Paragraph>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin tip="Đang tải hộp thư..." />
        </div>
      ) : emails.length === 0 ? (
        <Empty description="Chưa có email nào trong hộp thư gửi đi." />
      ) : (
        <List
          itemLayout="vertical"
          dataSource={emails}
          renderItem={(item) => {
            const isReset = item.template === 'PASSWORD_RESET';
            const isActivation = item.template === 'ACCOUNT_ACTIVATION';

            return (
              <List.Item
                key={item.id}
                style={{
                  background: '#fafafa',
                  border: '1px solid #f0f0f0',
                  borderRadius: 8,
                  padding: 14,
                  marginBottom: 12,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                  <div>
                    <Text strong style={{ fontSize: 14, color: '#1f1f1f' }}>
                      {item.subject}
                    </Text>
                    <div style={{ fontSize: 12, color: '#888', marginTop: 2 }}>
                      Gửi tới: <Text strong style={{ color: '#1677ff' }}>{item.recipient}</Text>
                    </div>
                  </div>
                  <Tag color={item.status === 'SENT' ? 'success' : 'warning'}>
                    <CheckCircleOutlined style={{ marginRight: 4 }} />
                    {item.status === 'SENT' ? 'Đã gửi' : item.status}
                  </Tag>
                </div>

                <div style={{ fontSize: 11, color: '#999', marginBottom: 10 }}>
                  Thời gian: {new Date(item.sent_at || item.created_at).toLocaleString('vi-VN')}
                </div>

                {/* Specific Template Details */}
                {isReset && item.payload?.resetUrl && (
                  <div
                    style={{
                      background: '#fff1f0',
                      border: '1px solid #ffccc7',
                      borderRadius: 6,
                      padding: 10,
                      marginTop: 8,
                    }}
                  >
                    <div style={{ fontSize: 12, color: '#cf1322', fontWeight: 600, marginBottom: 6 }}>
                      Yêu cầu Đặt lại Mật khẩu
                    </div>
                    <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
                      <Button
                        type="primary"
                        danger
                        size="small"
                        icon={<KeyOutlined />}
                        onClick={() => handleNavigateReset(item.payload.resetUrl)}
                      >
                        Mở liên kết đổi mật khẩu
                      </Button>
                      <Tooltip title="Sao chép liên kết đặt lại">
                        <Button
                          size="small"
                          icon={<CopyOutlined />}
                          onClick={() => handleCopy(item.payload.resetUrl, 'liên kết đặt lại mật khẩu')}
                        >
                          Sao chép URL
                        </Button>
                      </Tooltip>
                    </Space>
                  </div>
                )}

                {isActivation && (
                  <div
                    style={{
                      background: '#e6f7ff',
                      border: '1px solid #91d5ff',
                      borderRadius: 6,
                      padding: 10,
                      marginTop: 8,
                    }}
                  >
                    <div style={{ fontSize: 12, color: '#0050b3', fontWeight: 600, marginBottom: 4 }}>
                      <UserOutlined style={{ marginRight: 6 }} />
                      Thông tin tài khoản mới khởi tạo:
                    </div>
                    <div style={{ fontSize: 12, color: '#333' }}>
                      Họ tên: <strong>{item.payload?.fullName}</strong>
                    </div>
                    {item.payload?.temporaryPassword && (
                      <div style={{ marginTop: 4, display: 'flex', alignItems: 'center', gap: 8 }}>
                        <span style={{ fontSize: 12, color: '#333' }}>Mật khẩu tạm:</span>
                        <code style={{ background: '#fff', padding: '2px 6px', border: '1px solid #d9d9d9', fontWeight: 700, color: '#d4380d' }}>
                          {item.payload.temporaryPassword}
                        </code>
                        <Tooltip title="Sao chép mật khẩu">
                          <Button
                            type="text"
                            size="small"
                            icon={<CopyOutlined />}
                            onClick={() => handleCopy(item.payload.temporaryPassword, 'mật khẩu tạm')}
                          />
                        </Tooltip>
                      </div>
                    )}
                  </div>
                )}
              </List.Item>
            );
          }}
        />
      )}
    </Drawer>
  );
};
