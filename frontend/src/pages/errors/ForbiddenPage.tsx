import React from 'react';
import { Result, Button, Space, Card } from 'antd';
import { useNavigate } from 'react-router-dom';
import { ArrowLeftOutlined, HomeOutlined } from '@ant-design/icons';

export const ForbiddenPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div
      style={{
        minHeight: '75vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '20px',
      }}
    >
      <Card style={{ maxWidth: 520, width: '100%', borderRadius: 12, textAlign: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
        <Result
          status="403"
          title="403 - Quyền Truy Cập Bị Từ Chối"
          subTitle="Bạn không có quyền truy cập chức năng này. Vui lòng liên hệ với Quản trị hệ thống nếu bạn cần cấp thêm quyền hạn nghiệp vụ."
          extra={
            <Space size="middle">
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
                Quay lại trang trước
              </Button>
              <Button type="primary" icon={<HomeOutlined />} onClick={() => navigate('/')}>
                Về trang chủ
              </Button>
            </Space>
          }
        />
      </Card>
    </div>
  );
};
