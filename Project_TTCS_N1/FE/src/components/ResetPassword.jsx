import React, { useState, useEffect } from 'react';
import { Lock, ArrowRight, ShieldCheck, AlertCircle } from 'lucide-react';

export function ResetPassword({ token, email, onBackToLogin }) {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [status, setStatus] = useState('idle'); // idle, submitting, success, error
  const [message, setMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setStatus('error');
      setMessage('Mật khẩu xác nhận không khớp.');
      return;
    }

    if (password.length < 6) {
      setStatus('error');
      setMessage('Mật khẩu phải có ít nhất 6 ký tự.');
      return;
    }

    setStatus('submitting');
    try {
      const response = await fetch('http://localhost:5000/api/v1/auth/reset-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, token, newPassword: password })
      });
      const data = await response.json();

      if (!response.ok || !data.success) {
        setStatus('error');
        // KN-46: Xử lý lỗi Token trạng thái / Hết hạn
        setMessage(data.message || data.error?.message || 'Đã xảy ra lỗi khi đặt lại mật khẩu.');
      } else {
        setStatus('success');
        setMessage(data.message || 'Mật khẩu của bạn đã được đặt lại thành công.');
      }
    } catch (error) {
      setStatus('error');
      setMessage('Không thể kết nối đến máy chủ. Vui lòng thử lại sau.');
    }
  };

  return (
    <div className="error-page-wrapper">
      <div className="error-card" style={{ maxWidth: '450px', padding: '2.5rem 2rem' }}>
        {status === 'success' ? (
          <>
            <div className="error-illustration" style={{ background: 'var(--error-503-bg)', color: 'var(--error-503-color)' }}>
              <ShieldCheck size={40} />
            </div>
            <h2 className="error-title">Thành công!</h2>
            <p className="error-description" style={{ fontSize: '0.9rem', marginBottom: '2rem' }}>
              {message}
            </p>
            <button onClick={onBackToLogin} className="btn-primary" style={{ width: '100%', justifyContent: 'center' }}>
              Quay lại đăng nhập
            </button>
          </>
        ) : (
          <>
            <div className="error-illustration" style={{ background: 'var(--error-404-bg)', color: 'var(--error-404-color)' }}>
              <Lock size={40} />
            </div>
            <h2 className="error-title">Tạo mật khẩu mới</h2>
            <p className="error-description" style={{ fontSize: '0.9rem', marginBottom: '1.5rem' }}>
              Vui lòng nhập mật khẩu mới cho tài khoản <strong style={{color: 'var(--text-primary)'}}>{email}</strong>.
            </p>

            {status === 'error' && (
              <div className="error-hint-box" style={{ background: 'var(--error-403-bg)', borderColor: 'var(--error-403-color)', borderLeftColor: 'var(--error-403-color)', marginBottom: '1.5rem' }}>
                <AlertCircle className="hint-icon" size={18} style={{ color: 'var(--error-403-color)' }} />
                <div className="hint-content">
                  <h4 style={{ color: 'var(--error-403-color)' }}>Lỗi</h4>
                  <p>{message}</p>
                </div>
              </div>
            )}

            <form onSubmit={handleSubmit} style={{ textAlign: 'left' }}>
              <div className="form-group">
                <label>Mật khẩu mới</label>
                <input 
                  type="password" 
                  className="form-input" 
                  placeholder="Nhập mật khẩu mới..." 
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
              </div>
              <div className="form-group" style={{ marginBottom: '2rem' }}>
                <label>Xác nhận mật khẩu mới</label>
                <input 
                  type="password" 
                  className="form-input" 
                  placeholder="Nhập lại mật khẩu..." 
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>
              
              <button 
                type="submit" 
                className="btn-primary" 
                style={{ width: '100%', justifyContent: 'center' }}
                disabled={status === 'submitting'}
              >
                {status === 'submitting' ? 'Đang xử lý...' : 'Lưu mật khẩu mới'} <ArrowRight size={18} />
              </button>
              
              {status === 'error' && (
                <div style={{ textAlign: 'center', marginTop: '1rem' }}>
                  <button 
                    type="button"
                    onClick={onBackToLogin}
                    style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', fontSize: '0.9rem', textDecoration: 'underline' }}
                  >
                    Yêu cầu link mới
                  </button>
                </div>
              )}
            </form>
          </>
        )}
      </div>
    </div>
  );
}
