import React from 'react';
import { Result, Button, Space, Card } from 'antd';
import { useNavigate } from 'react-router-dom';
import { ArrowLeftOutlined, HomeOutlined } from '@ant-design/icons';

export const NotFoundPage: React.FC = () => {
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
          status="404"
          title="404 - Không Tìm Thấy Trang"
          subTitle="Đường dẫn bạn yêu cầu không tồn tại hoặc đã bị di chuyển."
          extra={
            <Space size="middle">
              <Button icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)}>
                Quay lại
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
