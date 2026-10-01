import React, { useState } from 'react';
import {
  ShieldAlert,
  FileQuestion,
  Lock,
  ServerCrash,
  WifiOff,
  Home,
  ArrowLeft,
  RotateCw,
  HelpCircle,
  Lightbulb,
  ChevronDown,
  ChevronUp,
  Send,
  CheckCircle2,
  X
} from 'lucide-react';

/**
 * Sub-task KN-72: [FE] Lỗi trang giao diện Sử dụng chung
 * Sub-task KN-74: [FE] Gợi ý Hành động Điều hướng & Mẹo Nhỏ
 * Sub-task KN-78: [FE] Hiển thị Cấu trúc phản hồi lỗi chuẩn hóa
 */
export function CommonErrorPage({ errorPayload, onRetry }) {
  const [showDetails, setShowDetails] = useState(false);
  const [showRequestModal, setShowRequestModal] = useState(false);
  const [requestSubmitted, setRequestSubmitted] = useState(false);
  const [requestReason, setRequestReason] = useState('');

  const status = errorPayload?.status || 404;
  const config = getErrorConfig(status);

  const handleGoHome = () => {
    window.location.href = '/';
  };

  const handleGoBack = () => {
    if (window.history.length > 1) {
      window.history.back();
    } else {
      handleGoHome();
    }
  };

  const handleSendRequest = (e) => {
    e.preventDefault();
    setRequestSubmitted(true);
    setTimeout(() => {
      setShowRequestModal(false);
      setRequestSubmitted(false);
      setRequestReason('');
    }, 2000);
  };

  return (
    <div className="error-page-wrapper" style={{ '--error-accent-gradient': config.gradient }}>
      <div className="error-card">
        {/* Badge mã lỗi */}
        <div
          className="error-badge-container"
          style={{ background: config.badgeBg, color: config.color }}
        >
          <config.Icon style={{ width: 16, height: 16 }} />
          <span>HTTP STATUS {status} &bull; {errorPayload?.errorCode || config.code}</span>
        </div>

        {/* Icon minh họa động */}
        <div
          className="error-illustration"
          style={{ background: config.badgeBg, color: config.color }}
        >
          <config.Icon />
        </div>

        {/* Tiêu đề & Miêu tả */}
        <h1 className="error-title">{config.title}</h1>
        <p className="error-description">
          {errorPayload?.message || config.description}
        </p>

        {/* Sub-task KN-74: Khối "Mẹo nhỏ" nổi bật theo yêu cầu User Story KN-17 */}
        <div className="error-hint-box">
          <Lightbulb className="hint-icon" size={22} />
          <div className="hint-content">
            <h4>Mẹo hướng dẫn cho bạn</h4>
            <p>{errorPayload?.hint || config.defaultHint}</p>
          </div>
        </div>

        {/* Sub-task KN-74: Các nút hành động điều hướng */}
        <div className="error-actions-group">
          <button className="btn-primary" onClick={handleGoHome}>
            <Home size={18} />
            Quay lại trang chủ
          </button>

          <button className="btn-secondary" onClick={handleGoBack}>
            <ArrowLeft size={18} />
            Trở về trang trước
          </button>

          {onRetry && (
            <button className="btn-secondary" onClick={onRetry}>
              <RotateCw size={18} />
              Thử lại
            </button>
          )}

          {status === 403 && (
            <button
              className="btn-outline-danger"
              onClick={() => setShowRequestModal(true)}
            >
              <Send size={18} />
              Yêu cầu cấp quyền HR
            </button>
          )}
        </div>

        {/* Sub-task KN-78: Chi tiết kỹ thuật (Technical JSON Payload Viewer) */}
        <div className="technical-details">
          <button
            className="technical-toggle"
            onClick={() => setShowDetails(!showDetails)}
          >
            <HelpCircle size={14} />
            {showDetails ? 'Ẩn chi tiết phản hồi JSON API' : 'Xem cấu trúc phản hồi lỗi kỹ thuật (KN-78)'}
            {showDetails ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
          </button>

          {showDetails && (
            <pre className="code-block">
              {JSON.stringify(errorPayload, null, 2)}
            </pre>
          )}
        </div>
      </div>

      {/* Modal Yêu cầu cấp quyền khi bị lỗi 403 */}
      {showRequestModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Gửi yêu cầu cấp quyền truy cập</h3>
              <button
                className="btn-icon"
                onClick={() => setShowRequestModal(false)}
              >
                <X size={18} />
              </button>
            </div>

            {requestSubmitted ? (
              <div style={{ textAlign: 'center', padding: '2rem 0' }}>
                <CheckCircle2 size={48} color="#059669" style={{ margin: '0 auto 1rem auto' }} />
                <h4 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                  Đã gửi yêu cầu thành công!
                </h4>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                  Hệ thống đã thông báo cho Trưởng phòng HR Admin. Bạn sẽ nhận được phản hồi qua Email nội bộ.
                </p>
              </div>
            ) : (
              <form onSubmit={handleSendRequest}>
                <div className="form-group">
                  <label>Đường dẫn tài nguyên cần truy cập</label>
                  <input
                    type="text"
                    className="form-input"
                    value={errorPayload?.path || window.location.pathname}
                    readOnly
                  />
                </div>

                <div className="form-group">
                  <label>Lý do cần cấp quyền</label>
                  <textarea
                    className="form-textarea"
                    rows={3}
                    placeholder="Ví dụ: Tôi cần xem thông tin định biên vị trí tuyển dụng để duyệt hồ sơ ứng viên..."
                    value={requestReason}
                    onChange={(e) => setRequestReason(e.target.value)}
                    required
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
                  <button
                    type="button"
                    className="btn-secondary"
                    onClick={() => setShowRequestModal(false)}
                  >
                    Hủy bỏ
                  </button>
                  <button type="submit" className="btn-primary">
                    <Send size={16} />
                    Gửi yêu cầu
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function getErrorConfig(status) {
  switch (status) {
    case 403:
      return {
        code: 'FORBIDDEN_RESOURCE',
        title: 'Không đủ quyền truy cập',
        description: 'Tài khoản nội bộ của bạn chưa được cấp quyền truy cập vào danh mục hoặc tài nguyên này.',
        defaultHint: 'Mẹo: Bạn có thể nhấn vào nút "Yêu cầu cấp quyền HR" bên dưới để gửi đề xuất cho Admin hoặc quay lại trang làm việc chính.',
        color: 'var(--error-403-color)',
        badgeBg: 'var(--error-403-bg)',
        gradient: 'linear-gradient(135deg, #f43f5e 0%, #e11d48 100%)',
        Icon: ShieldAlert
      };

    case 401:
      return {
        code: 'UNAUTHORIZED_ACCESS',
        title: 'Phiên đăng nhập hết hạn',
        description: 'Phiên truy cập của bạn đã hết hạn bảo mật. Vui lòng xác thực lại tài khoản nội bộ.',
        defaultHint: 'Mẹo: Nhấn nút bên dưới để đăng nhập lại và quay lại luồng công việc đang dở dang.',
        color: 'var(--error-401-color)',
        badgeBg: 'var(--error-401-bg)',
        gradient: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
        Icon: Lock
      };

    case 500:
      return {
        code: 'INTERNAL_SERVER_ERROR',
        title: 'Lỗi hệ thống máy chủ',
        description: 'Máy chủ tuyển dụng gặp sự cố xử lý dữ liệu đột xuất.',
        defaultHint: 'Mẹo: Sự cố đã được gửi tự động tới IT Support. Hãy thử nhấn "Thử lại" sau vài giây.',
        color: 'var(--error-500-color)',
        badgeBg: 'var(--error-500-bg)',
        gradient: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
        Icon: ServerCrash
      };

    case 503:
      return {
        code: 'SERVICE_UNAVAILABLE',
        title: 'Hệ thống đang bảo trì',
        description: 'Hệ thống tuyển dụng nội bộ đang được nâng cấp định kỳ để phục vụ kỳ TTCS T9/2026 tốt hơn.',
        defaultHint: 'Mẹo: Dịch vụ sẽ mở lại trong ít phút. Bạn có thể quay lại trang chủ hoặc kiểm tra thông báo công ty.',
        color: 'var(--error-503-color)',
        badgeBg: 'var(--error-503-bg)',
        gradient: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
        Icon: WifiOff
      };

    case 404:
    default:
      return {
        code: 'RESOURCE_NOT_FOUND',
        title: 'Không tìm thấy trang hoặc tài nguyên',
        description: 'Địa chỉ bạn vừa truy cập không tồn tại hoặc đã được di chuyển sang vị trí khác.',
        defaultHint: 'Mẹo: Kiểm tra lại liên kết URL hoặc sử dụng danh mục điều hướng bên trái để tìm vị trí làm việc.',
        color: 'var(--error-404-color)',
        badgeBg: 'var(--error-404-bg)',
        gradient: 'linear-gradient(135deg, #38bdf8 0%, #0284c7 100%)',
        Icon: FileQuestion
      };
  }
}
