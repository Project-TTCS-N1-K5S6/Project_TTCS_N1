import React, { useState } from 'react';
import { Mail, ArrowRight, ArrowLeft } from 'lucide-react';

export function ForgotPassword({ onBackToLogin }) {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [message, setMessage] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email) return;

    setIsSubmitting(true);
    try {
      const response = await fetch('http://localhost:5000/api/v1/auth/forgot-password', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email })
      });
      const data = await response.json();
      
      // Xử lý thông báo phản hồi an toàn (KN-44)
      setIsSuccess(true);
      setMessage(data.message || 'Nếu email tồn tại, một liên kết đã được gửi.');
    } catch (error) {
      setIsSuccess(true);
      setMessage('Đã xảy ra lỗi. Tuy nhiên, nếu email của bạn tồn tại, một liên kết khôi phục đã được gửi.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="error-page-wrapper">
      <div className="error-card" style={{ maxWidth: '450px', padding: '2.5rem 2rem' }}>
        <button 
          onClick={onBackToLogin}
          style={{ background: 'none', border: 'none', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', marginBottom: '1.5rem', fontSize: '0.9rem' }}
        >
          <ArrowLeft size={16} /> Quay lại
        </button>
        
        <div className="error-illustration" style={{ background: 'var(--error-500-bg)', color: 'var(--error-500-color)' }}>
          <Mail size={40} />
        </div>
        
        <h2 className="error-title">Quên mật khẩu?</h2>
        <p className="error-description" style={{ fontSize: '0.9rem', marginBottom: '2rem' }}>
          Đừng lo lắng, hãy nhập email bạn đã sử dụng để đăng ký. Chúng tôi sẽ gửi cho bạn một liên kết để thiết lập lại mật khẩu (có hiệu lực trong 30 phút).
        </p>

        {isSuccess ? (
          <div className="error-hint-box" style={{ background: 'var(--error-503-bg)', borderColor: 'var(--error-503-color)', borderLeftColor: 'var(--error-503-color)' }}>
            <div className="hint-content">
              <h4 style={{ color: 'var(--error-503-color)' }}>Đã gửi yêu cầu</h4>
              <p>{message}</p>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} style={{ textAlign: 'left' }}>
            <div className="form-group">
              <label>Địa chỉ Email</label>
              <input 
                type="email" 
                className="form-input" 
                placeholder="Nhập email của bạn..." 
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            
            <button 
              type="submit" 
              className="btn-primary" 
              style={{ width: '100%', justifyContent: 'center', marginTop: '0.5rem' }}
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Đang xử lý...' : 'Gửi liên kết khôi phục'} <ArrowRight size={18} />
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
