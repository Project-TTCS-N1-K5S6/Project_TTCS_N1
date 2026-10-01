import React, { useState, useEffect, useCallback } from 'react';
import { Card, Table, Tag, Typography, Select, Button, Space, message } from 'antd';
import { ReloadOutlined, HistoryOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { api } from '../../services/api';
import { ApiResponse, PaginatedResult } from '../../types';

const { Title, Text } = Typography;
const { Option } = Select;

interface AuditLogItem {
  id: string;
  action: string;
  entity_type: string;
  entity_id: string;
  description: string;
  ip_address: string;
  user_agent: string;
  created_at: string;
  user_full_name?: string;
  user_email?: string;
}

export const AuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [actionFilter, setActionFilter] = useState<string | undefined>(undefined);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<ApiResponse<PaginatedResult<AuditLogItem>>>('/audit-logs', {
        params: {
          page,
          pageSize,
          action: actionFilter || undefined,
        },
      });

      if (res.data.success) {
        setLogs(res.data.data.items);
        setTotal(res.data.data.total);
      }
    } catch (err: any) {
      message.error(err.response?.data?.message || 'Không thể tải nhật ký hệ thống.');
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, actionFilter]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const actionColors: Record<string, string> = {
    LOGIN_SUCCESS: 'green',
    LOGIN_FAILED: 'orange',
    LOGIN_BLOCKED: 'red',
    LOGIN_BLOCKED_TEMP: 'volcano',
    LOGOUT: 'default',
    USER_CREATED: 'blue',
    USER_UPDATED: 'cyan',
    USER_LOCKED: 'red',
    USER_UNLOCKED: 'green',
    ROLE_ASSIGNED: 'purple',
    ROLE_REVOKED: 'magenta',
    ROLE_PERMISSION_UPDATED: 'geekblue',
    PASSWORD_CHANGED: 'gold',
    PASSWORD_RESET_REQUESTED: 'warning',
    PASSWORD_RESET_COMPLETED: 'success',
  };

  const columns: ColumnsType<AuditLogItem> = [
    {
      title: 'Thời gian',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 170,
      render: (date) => new Date(date).toLocaleString('vi-VN'),
    },
    {
      title: 'Người thực hiện',
      key: 'user',
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 600 }}>{record.user_full_name || 'Hệ thống / Khách'}</div>
          {record.user_email && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              {record.user_email}
            </Text>
          )}
        </div>
      ),
    },
    {
      title: 'Hành động',
      dataIndex: 'action',
      key: 'action',
      width: 190,
      render: (action) => <Tag color={actionColors[action] || 'blue'}>{action}</Tag>,
    },
    {
      title: 'Mô tả chi tiết',
      dataIndex: 'description',
      key: 'description',
      render: (text) => text || '-',
    },
    {
      title: 'IP Address',
      dataIndex: 'ip_address',
      key: 'ip_address',
      width: 130,
      render: (ip) => <code style={{ fontSize: 11 }}>{ip || '-'}</code>,
    },
  ];

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', width: '100%' }}>
      <Card
        title={
          <div>
            <Title level={4} style={{ margin: 0 }}>
              <HistoryOutlined style={{ marginRight: 8, color: '#fa8c16' }} />
              Nhật Ký Hệ Thống (Audit Logs)
            </Title>
            <Text type="secondary" style={{ fontSize: 13 }}>
              Ghi nhận các hoạt động bảo mật, xác thực tài khoản và thao tác quản trị theo thời gian thực
            </Text>
          </div>
        }
      >
        <div style={{ marginBottom: 16, display: 'flex', gap: 12, alignItems: 'center' }}>
          <Select
            placeholder="Lọc theo hành động"
            style={{ width: 260 }}
            value={actionFilter}
            onChange={(val) => {
              setActionFilter(val);
              setPage(1);
            }}
            allowClear
          >
            <Option value="LOGIN_SUCCESS">LOGIN_SUCCESS (Đăng nhập thành công)</Option>
            <Option value="LOGIN_FAILED">LOGIN_FAILED (Đăng nhập thất bại)</Option>
            <Option value="ACCOUNT_TEMP_LOCKED">ACCOUNT_TEMP_LOCKED (Tạm khóa 15p)</Option>
            <Option value="USER_CREATED">USER_CREATED (Tạo người dùng)</Option>
            <Option value="USER_LOCKED">USER_LOCKED (Khóa tài khoản)</Option>
            <Option value="USER_UNLOCKED">USER_UNLOCKED (Mở khóa)</Option>
            <Option value="ROLE_ASSIGNED">ROLE_ASSIGNED (Gán vai trò)</Option>
            <Option value="ROLE_REVOKED">ROLE_REVOKED (Thu hồi vai trò)</Option>
            <Option value="ROLE_PERMISSION_UPDATED">ROLE_PERMISSION_UPDATED (Cập nhật ma trận quyền)</Option>
            <Option value="PASSWORD_CHANGED">PASSWORD_CHANGED (Đổi mật khẩu)</Option>
          </Select>

          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              setActionFilter(undefined);
              setPage(1);
              fetchLogs();
            }}
          >
            Làm mới
          </Button>
        </div>

        <Table
          columns={columns}
          dataSource={logs}
          rowKey="id"
          loading={loading}
          pagination={{
            current: page,
            pageSize,
            total,
            defaultPageSize: 20,
            showSizeChanger: true,
            onChange: (p, ps) => {
              setPage(p);
              setPageSize(ps);
            },
            showTotal: (tot) => `Tổng cộng ${tot} bản ghi nhật ký`,
          }}
          scroll={{ x: 800 }}
        />
      </Card>
    </div>
  );
};
