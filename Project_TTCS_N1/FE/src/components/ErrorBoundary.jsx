import React from 'react';
import { CommonErrorPage } from './CommonErrorPage';

/**
 * Sub-task KN-75: [FE] Bộ Bắt lỗi Tập trung & Điều hướng Client
 * React Error Boundary để bắt toàn bộ crash giao diện Client-side mà không làm trắng trang
 */
export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('[React Error Boundary Caught]:', error, errorInfo);
    this.setState({ errorInfo });
  }

  render() {
    if (this.state.hasError) {
      const errorPayload = {
        status: 500,
        errorCode: 'CLIENT_RENDER_CRASH',
        message: 'Đã xảy ra lỗi giao diện hiển thị trên trình duyệt.',
        hint: 'Mẹo: Nhấn nút "Tải lại trang" bên dưới để khôi phục giao diện tuyển dụng.',
        path: window.location.pathname,
        timestamp: new Date().toISOString(),
        details: [this.state.error?.toString(), this.state.errorInfo?.componentStack]
      };

      return (
        <CommonErrorPage
          errorPayload={errorPayload}
          onRetry={() => {
            this.setState({ hasError: false, error: null });
            window.location.reload();
          }}
        />
      );
    }

    return this.props.children;
  }
}
