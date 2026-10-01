import React, { useState, useEffect } from 'react';
import { apiRequest } from '../services/apiClient';
import {
  Users,
  Briefcase,
  Calendar,
  AlertCircle,
  ShieldAlert,
  Search,
  CheckCircle2,
  Lock,
  RefreshCw,
  Clock,
  Sparkles,
  Database
} from 'lucide-react';

export function RecruitmentDashboard({ onTriggerError }) {
  const [candidates, setCandidates] = useState([]);
  const [vacancies, setVacancies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [role, setRole] = useState(localStorage.getItem('userRole') || 'EMPLOYEE');

  useEffect(() => {
    loadData();
  }, [role]);

  const loadData = async () => {
    setLoading(true);
    try {
      const candidateRes = await apiRequest('/api/v1/recruitment/candidates');
      setCandidates(candidateRes.data || []);

      const vacancyRes = await apiRequest('/api/v1/recruitment/vacancies');
      setVacancies(vacancyRes.data || []);
    } catch (err) {
      if (err.errorPayload) {
        onTriggerError(err.errorPayload);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleFetchCandidateDetail = async (id) => {
    try {
      await apiRequest(`/api/v1/recruitment/candidates/${id}`);
      alert(`Lấy thông tin ứng viên ${id} thành công!`);
    } catch (err) {
      if (err.errorPayload) {
        onTriggerError(err.errorPayload);
      }
    }
  };

  const handleFetchSalaryReports = async () => {
    try {
      const data = await apiRequest('/api/v1/recruitment/admin/salary-reports', {
        headers: { 'x-user-role': role }
      });
      alert(`Báo cáo ngân sách: ${data.data.totalBudget} - Trạng thái: ${data.data.approvalStatus}`);
    } catch (err) {
      if (err.errorPayload) {
        onTriggerError(err.errorPayload);
      }
    }
  };

  const handleVerifySession = async () => {
    try {
      await apiRequest('/api/v1/auth/verify-session');
    } catch (err) {
      if (err.errorPayload) {
        onTriggerError(err.errorPayload);
      }
    }
  };

  const handleTriggerServerError = async () => {
    try {
      await apiRequest('/api/v1/system/crash-test');
    } catch (err) {
      if (err.errorPayload) {
        onTriggerError(err.errorPayload);
      }
    }
  };

  const handleRoleToggle = (newRole) => {
    setRole(newRole);
    localStorage.setItem('userRole', newRole);
  };

  const filteredCandidates = candidates.filter(c =>
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.position.toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.id.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div style={{ padding: '2rem' }}>
      {/* Banner cài đặt quyền hạn thử nghiệm */}
      <div style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)',
        padding: '1rem 1.5rem',
        marginBottom: '2rem',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        gap: '1rem',
        flexWrap: 'wrap'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <Sparkles color="var(--accent-primary)" size={20} />
          <div>
            <h4 style={{ fontSize: '0.9rem', fontWeight: 700 }}>Chế độ Phân quyền Hiện tại (User Story KN-17 Demo)</h4>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
              Đang giả lập vai trò: <strong>{role === 'HR_ADMIN' ? 'HR Admin (Đủ quyền)' : 'Nhân viên HR Nội bộ (Hạn chế quyền báo cáo lương)'}</strong>
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            className={`demo-chip ${role === 'EMPLOYEE' ? 'active' : ''}`}
            onClick={() => handleRoleToggle('EMPLOYEE')}
          >
            Nhân viên HR (EMPLOYEE)
          </button>
          <button
            className={`demo-chip ${role === 'HR_ADMIN' ? 'active' : ''}`}
            onClick={() => handleRoleToggle('HR_ADMIN')}
          >
            Quản trị viên (HR_ADMIN)
          </button>
        </div>
      </div>

      {/* Thống kê KPIs */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
        gap: '1.25rem',
        marginBottom: '2rem'
      }}>
        <div style={{
          background: 'var(--bg-surface)',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem'
        }}>
          <div style={{ width: 44, height: 44, borderRadius: 'var(--radius-md)', background: 'rgba(99, 102, 241, 0.1)', color: '#6366f1', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Users size={22} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Tổng Hồ Sơ Ứng Viên</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, fontFamily: 'var(--font-heading)' }}>128 UV</div>
          </div>
        </div>

        <div style={{
          background: 'var(--bg-surface)',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem'
        }}>
          <div style={{ width: 44, height: 44, borderRadius: 'var(--radius-md)', background: 'rgba(16, 185, 129, 0.1)', color: '#10b981', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Briefcase size={22} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Vị Trí Đang Tuyển (Open)</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, fontFamily: 'var(--font-heading)' }}>04 Vị trí</div>
          </div>
        </div>

        <div style={{
          background: 'var(--bg-surface)',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem'
        }}>
          <div style={{ width: 44, height: 44, borderRadius: 'var(--radius-md)', background: 'rgba(245, 158, 11, 0.1)', color: '#f59e0b', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Calendar size={22} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Lịch Phỏng Vấn Tuần Này</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, fontFamily: 'var(--font-heading)' }}>12 Buổi</div>
          </div>
        </div>

        <div style={{
          background: 'var(--bg-surface)',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          gap: '1rem'
        }}>
          <div style={{ width: 44, height: 44, borderRadius: 'var(--radius-md)', background: 'rgba(239, 68, 68, 0.1)', color: '#ef4444', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <CheckCircle2 size={22} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Tỷ Lệ Chốt Offer</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 800, fontFamily: 'var(--font-heading)' }}>85%</div>
          </div>
        </div>
      </div>

      {/* Hành động Thử nghiệm Bắt Lỗi Gọi API */}
      <div style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)',
        padding: '1.25rem 1.5rem',
        marginBottom: '2rem'
      }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Database size={18} color="var(--accent-primary)" />
          Thử nghiệm gọi API Backend & Kích hoạt Bắt lỗi (KN-75)
        </h3>
        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
          Thực hiện cuộc gọi API thực tế tới Backend Node.js để kiểm tra luồng chuẩn hóa trạng thái HTTP (KN-76), tập tin ngoại lệ (KN-77) và JSON phản hồi có chứa Mẹo hướng dẫn (KN-74 & KN-78):
        </p>

        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
          <button className="btn-secondary" onClick={handleFetchSalaryReports}>
            <ShieldAlert size={16} color="#e11d48" />
            Truy cập Báo cáo Lương (Thử 403 Forbidden)
          </button>

          <button className="btn-secondary" onClick={() => handleFetchCandidateDetail('UV-9999-NOTFOUND')}>
            <AlertCircle size={16} color="#0284c7" />
            Tìm UV-9999 không tồn tại (Thử 404 Not Found)
          </button>

          <button className="btn-secondary" onClick={handleVerifySession}>
            <Lock size={16} color="#d97706" />
            Kiểm tra Token hết hạn (Thử 401 Unauthorized)
          </button>

          <button className="btn-secondary" onClick={handleTriggerServerError}>
            <RefreshCw size={16} color="#7c3aed" />
            Tải dữ liệu DB bị lỗi (Thử 500 Server Error)
          </button>
        </div>
      </div>

      {/* Danh sách Ứng viên */}
      <div style={{
        background: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)',
        padding: '1.5rem',
        boxShadow: 'var(--card-shadow)'
      }}>
        <div style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          marginBottom: '1.25rem',
          flexWrap: 'wrap',
          gap: '1rem'
        }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Danh Sách Ứng Viên Tiếp Nhận</h3>
            <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Quản lý và cập nhật tiến độ sàng lọc theo thời gian thực</p>
          </div>

          <div style={{ position: 'relative', width: 280 }}>
            <Search size={16} style={{ position: 'absolute', left: 12, top: 10, color: 'var(--text-muted)' }} />
            <input
              type="text"
              placeholder="Tìm kiếm ứng viên..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="form-input"
              style={{ paddingLeft: '2.2rem' }}
            />
          </div>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '3rem 0', color: 'var(--text-muted)' }}>
            Đang tải dữ liệu ứng viên từ máy chủ...
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.9rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-color)', color: 'var(--text-muted)' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Mã UV</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Họ & Tên</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Vị trí Ứng tuyển</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Phòng ban</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Trạng thái</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {filteredCandidates.map(c => (
                  <tr key={c.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                    <td style={{ padding: '1rem', fontWeight: 700, color: 'var(--accent-primary)' }}>{c.id}</td>
                    <td style={{ padding: '1rem', fontWeight: 600 }}>{c.name}</td>
                    <td style={{ padding: '1rem' }}>{c.position}</td>
                    <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{c.department}</td>
                    <td style={{ padding: '1rem' }}>
                      <span style={{
                        padding: '0.25rem 0.65rem',
                        borderRadius: 'var(--radius-full)',
                        fontSize: '0.78rem',
                        fontWeight: 700,
                        background: 'rgba(99, 102, 241, 0.1)',
                        color: 'var(--accent-primary)'
                      }}>
                        {c.status}
                      </span>
                    </td>
                    <td style={{ padding: '1rem', textAlign: 'right' }}>
                      <button
                        className="btn-secondary"
                        style={{ padding: '0.35rem 0.75rem', fontSize: '0.8rem' }}
                        onClick={() => handleFetchCandidateDetail(c.id)}
                      >
                        Xem chi tiết
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
