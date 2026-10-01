import React, { useState } from 'react';
import {
  UserPlus,
  Mail,
  Lock,
  User,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  Eye,
  EyeOff,
  ShieldCheck,
  Sparkles,
  BadgeCheck
} from 'lucide-react';

export function Register({ onBackToLogin }) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [registeredUser, setRegisteredUser] = useState(null);

  // Kiểm tra độ mạnh mật khẩu realtime
  const passwordCriteria = [
    { label: 'Tối thiểu 8 ký tự', valid: password.length >= 8 },
    { label: 'Chứa ít nhất 1 chữ cái', valid: /[a-zA-Z]/.test(password) },
    { label: 'Chứa ít nhất 1 chữ số', valid: /[0-9]/.test(password) },
    { label: 'Mật khẩu xác nhận trùng khớp', valid: confirmPassword.length > 0 && password === confirmPassword }
  ];

  const isPasswordSecure = password.length >= 8 && /[a-zA-Z]/.test(password) && /[0-9]/.test(password);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!fullName.trim() || !email.trim() || !password) {
      setErrorMessage('Vui lòng điền đầy đủ các thông tin bắt buộc.');
      return;
    }

    if (!isPasswordSecure) {
      setErrorMessage('Mật khẩu chưa đáp ứng tiêu chuẩn an toàn (tối thiểu 8 ký tự, gồm cả chữ và số).');
      return;
    }

    if (password !== confirmPassword) {
      setErrorMessage('Mật khẩu xác nhận không trùng khớp.');
      return;
    }

    setIsSubmitting(true);
    try {
      // Gọi qua proxy /api/v1/auth/register hoặc direct
      const response = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          fullName: fullName.trim(),
          email: email.trim().toLowerCase(),
          password,
          confirmPassword
        })
      });

      const data = await response.json();

      if (!response.ok || !data.success) {
        throw new Error(data.message || 'Đăng ký tài khoản không thành công.');
      }

      // Đăng ký thành công - nhận mã nhân viên tự động (ví dụ: PV002)
      setRegisteredUser(data.user);
    } catch (err) {
      setErrorMessage(err.message || 'Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="error-page-wrapper" style={{ padding: '2rem 1rem' }}>
      <div className="error-card" style={{ maxWidth: '520px', padding: '2.5rem 2rem', textAlign: 'left' }}>

        {/* Nút quay lại */}
        <button
          onClick={onBackToLogin}
          type="button"
          style={{
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            cursor: 'pointer',
            marginBottom: '1.25rem',
            fontSize: '0.9rem',
            padding: 0
          }}
        >
          <ArrowLeft size={16} /> Quay lại Đăng nhập / Bảng điều khiển
        </button>

        {registeredUser ? (
          /* Màn hình hiển thị kết quả Đăng ký thành công */
          <div style={{ textAlign: 'center', animation: 'fadeIn 0.3s ease' }}>
            <div
              className="error-illustration"
              style={{
                background: 'var(--error-503-bg)',
                color: 'var(--error-503-color)',
                margin: '0 auto 1.5rem auto'
              }}
            >
              <CheckCircle2 size={44} />
            </div>

            <h2 className="error-title" style={{ color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              Đăng Ký Thành Công!
            </h2>
            <p className="error-description" style={{ fontSize: '0.9rem', marginBottom: '1.5rem' }}>
              Tài khoản của bạn đã được khởi tạo trong hệ thống với mã định danh nhân sự độc nhất.
            </p>

            {/* Khung hiển thị Mã Nhân Viên Tự Sinh (PV00x) */}
            <div
              style={{
                background: 'rgba(99, 102, 241, 0.08)',
                border: '1px solid rgba(99, 102, 241, 0.25)',
                borderRadius: '16px',
                padding: '1.5rem',
                marginBottom: '1.75rem',
                textAlign: 'left'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Sparkles size={16} color="var(--accent-primary)" /> Mã Nhân Sự Cấp Tự Động
                </span>
                <span
                  style={{
                    background: 'var(--accent-primary)',
                    color: '#fff',
                    padding: '3px 10px',
                    borderRadius: '999px',
                    fontSize: '0.75rem',
                    fontWeight: 700
                  }}
                >
                  Tự Tăng
                </span>
              </div>

              <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--accent-primary)', letterSpacing: '1px', marginBottom: '0.5rem' }}>
                {registeredUser.employeeCode}
              </div>

              <div style={{ borderTop: '1px dashed var(--border-color)', paddingTop: '0.75rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                <p style={{ margin: '4px 0' }}><strong>Họ và tên:</strong> {registeredUser.fullName}</p>
                <p style={{ margin: '4px 0' }}><strong>Email đăng nhập:</strong> {registeredUser.email}</p>
                <p style={{ margin: '4px 0' }}><strong>Vai trò ban đầu:</strong> <span style={{ color: 'var(--accent-primary)', fontWeight: 600 }}>Người phỏng vấn (nguoi_phong_van)</span></p>
              </div>
            </div>

            <button
              onClick={onBackToLogin}
              type="button"
              className="btn-primary"
              style={{ width: '100%', justifyContent: 'center' }}
            >
              Tiến hành Đăng nhập <ArrowRight size={18} />
            </button>
          </div>
        ) : (
          /* Form Đăng ký mới */
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '1rem' }}>
              <div
                style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '12px',
                  background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.2) 0%, rgba(168, 85, 247, 0.2) 100%)',
                  color: 'var(--accent-primary)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <UserPlus size={24} />
              </div>
              <div>
                <h2 style={{ fontSize: '1.35rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>
                  Đăng Ký Tài Khoản
                </h2>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', margin: 0 }}>
                  Tạo tài khoản mới trong Hệ thống Nhân sự TTCS
                </p>
              </div>
            </div>

            {/* Thông báo nghiệp vụ về mã nhân viên tự tăng */}
            <div
              style={{
                background: 'rgba(99, 102, 241, 0.05)',
                border: '1px solid rgba(99, 102, 241, 0.15)',
                borderRadius: '10px',
                padding: '10px 14px',
                marginBottom: '1.25rem',
                fontSize: '0.82rem',
                color: 'var(--text-secondary)',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '8px'
              }}
            >
              <BadgeCheck size={18} color="var(--accent-primary)" style={{ flexShrink: 0, marginTop: '1px' }} />
              <span>
                <strong>Mã nhân sự (PV00x):</strong> Hệ thống sẽ tự động gán mã tiếp theo dựa trên số thứ tự lớn nhất hiện tại (không cần tự nhập).
              </span>
            </div>

            {/* Thông báo lỗi nếu có */}
            {errorMessage && (
              <div
                style={{
                  background: 'var(--error-403-bg)',
                  border: '1px solid var(--error-403-color)',
                  borderRadius: '10px',
                  padding: '10px 14px',
                  marginBottom: '1.25rem',
                  fontSize: '0.85rem',
                  color: 'var(--error-403-color)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}
              >
                <AlertCircle size={18} style={{ flexShrink: 0 }} />
                <span>{errorMessage}</span>
              </div>
            )}

            <form onSubmit={handleSubmit}>
              {/* Họ và tên */}
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px', color: 'var(--text-secondary)' }}>
                  Họ và tên <span style={{ color: 'var(--error-403-color)' }}>*</span>
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="text"
                    className="form-input"
                    placeholder="VD: Trần Văn Nam"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                    style={{ paddingLeft: '38px', width: '100%' }}
                  />
                  <User size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                </div>
              </div>

              {/* Email */}
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px', color: 'var(--text-secondary)' }}>
                  Địa chỉ Email <span style={{ color: 'var(--error-403-color)' }}>*</span>
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type="email"
                    className="form-input"
                    placeholder="email@company.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                    style={{ paddingLeft: '38px', width: '100%' }}
                  />
                  <Mail size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                </div>
              </div>

              {/* Mật khẩu */}
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px', color: 'var(--text-secondary)' }}>
                  Mật khẩu <span style={{ color: 'var(--error-403-color)' }}>*</span>
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Tối thiểu 8 ký tự..."
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    style={{ paddingLeft: '38px', paddingRight: '40px', width: '100%' }}
                  />
                  <Lock size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{
                      position: 'absolute',
                      right: '12px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-muted)',
                      cursor: 'pointer',
                      padding: 0
                    }}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {/* Xác nhận Mật khẩu */}
              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '6px', color: 'var(--text-secondary)' }}>
                  Xác nhận Mật khẩu <span style={{ color: 'var(--error-403-color)' }}>*</span>
                </label>
                <div style={{ position: 'relative' }}>
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    className="form-input"
                    placeholder="Nhập lại mật khẩu..."
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    style={{ paddingLeft: '38px', paddingRight: '40px', width: '100%' }}
                  />
                  <ShieldCheck size={18} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    style={{
                      position: 'absolute',
                      right: '12px',
                      top: '50%',
                      transform: 'translateY(-50%)',
                      background: 'none',
                      border: 'none',
                      color: 'var(--text-muted)',
                      cursor: 'pointer',
                      padding: 0
                    }}
                  >
                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
              </div>

              {/* Tiêu chí độ an toàn mật khẩu */}
              <div style={{ marginBottom: '1.5rem', background: 'var(--bg-surface-hover)', borderRadius: '10px', padding: '10px 14px' }}>
                <div style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '6px' }}>
                  Tiêu chuẩn mật khẩu:
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '6px', fontSize: '0.76rem' }}>
                  {passwordCriteria.map((c, i) => (
                    <div
                      key={i}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '5px',
                        color: c.valid ? 'var(--error-503-color)' : 'var(--text-muted)',
                        transition: 'color 0.2s'
                      }}
                    >
                      <CheckCircle2 size={13} color={c.valid ? 'var(--error-503-color)' : 'var(--text-muted)'} />
                      {c.label}
                    </div>
                  ))}
                </div>
              </div>

              {/* Nút gửi */}
              <button
                type="submit"
                className="btn-primary"
                style={{ width: '100%', justifyContent: 'center' }}
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Đang khởi tạo tài khoản...' : 'Hoàn tất Đăng ký'} <ArrowRight size={18} />
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}

export default Register;
