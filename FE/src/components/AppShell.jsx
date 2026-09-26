import React, { useState } from 'react';
import {
  Users,
  Briefcase,
  Calendar,
  AlertTriangle,
  Moon,
  Sun,
  ShieldCheck,
  ChevronRight,
  Layers,
  Sparkles,
  FileQuestion,
  ShieldAlert,
  Lock,
  ServerCrash,
  WifiOff
} from 'lucide-react';

export function AppShell({
  activeTab,
  setActiveTab,
  theme,
  setTheme,
  onTriggerSimulatedError,
  children
}) {
  return (
    <div className="app-container" data-theme={theme}>
      {/* Sidebar Navigation */}
      <aside className="app-sidebar">
        <div className="sidebar-logo">
          <div className="logo-badge">HR</div>
          <div className="logo-text">
            <h2>Hệ Thống Tuyển Dụng</h2>
            <p>TTCS Kỳ T9/2026 (K5S6)</p>
          </div>
        </div>

        <div className="nav-menu">
          <div className="nav-menu-title">Quản lý Tuyển dụng</div>

          <div
            className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <Users size={18} />
            <span>Danh sách Ứng viên</span>
          </div>

          <div
            className={`nav-item ${activeTab === 'vacancies' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <Briefcase size={18} />
            <span>Vị trí Tuyển dụng</span>
          </div>

          <div
            className={`nav-item ${activeTab === 'schedule' ? 'active' : ''}`}
            onClick={() => setActiveTab('dashboard')}
          >
            <Calendar size={18} />
            <span>Lịch Phỏng vấn</span>
          </div>

          <div className="nav-menu-title" style={{ marginTop: '1.25rem' }}>
            Kiểm thử Trang Lỗi (KN-17)
          </div>

          <div
            className={`nav-item ${activeTab === 'error-403' ? 'active' : ''}`}
            onClick={() => setActiveTab('error-403')}
          >
            <ShieldAlert size={18} color="var(--error-403-color)" />
            <span>Lỗi 403 (Không đủ quyền)</span>
          </div>

          <div
            className={`nav-item ${activeTab === 'error-404' ? 'active' : ''}`}
            onClick={() => setActiveTab('error-404')}
          >
            <FileQuestion size={18} color="var(--error-404-color)" />
            <span>Lỗi 404 (Không tìm thấy)</span>
          </div>

          <div
            className={`nav-item ${activeTab === 'error-401' ? 'active' : ''}`}
            onClick={() => setActiveTab('error-401')}
          >
            <Lock size={18} color="var(--error-401-color)" />
            <span>Lỗi 401 (Hết phiên login)</span>
          </div>

          <div
            className={`nav-item ${activeTab === 'error-500' ? 'active' : ''}`}
            onClick={() => setActiveTab('error-500')}
          >
            <ServerCrash size={18} color="var(--error-500-color)" />
            <span>Lỗi 500 (Máy chủ hỏng)</span>
          </div>

          <div
            className={`nav-item ${activeTab === 'error-503' ? 'active' : ''}`}
            onClick={() => setActiveTab('error-503')}
          >
            <WifiOff size={18} color="var(--error-503-color)" />
            <span>Lỗi 503 (Bảo trì/Mất mạng)</span>
          </div>
        </div>

        <div style={{ marginTop: 'auto', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            User Story: <strong>KN-17</strong><br />
            Sub-tasks: <strong>KN-72 &rarr; KN-78</strong>
          </div>
        </div>
      </aside>

      {/* Main Container */}
      <main className="app-main">
        {/* Top Header */}
        <header className="app-header">
          <div className="header-title">
            <Layers size={18} color="var(--accent-primary)" />
            <span>Hệ thống Tuyển dụng Nội bộ</span>
            <ChevronRight size={14} color="var(--text-muted)" />
            <span style={{ color: 'var(--text-secondary)' }}>
              {activeTab === 'dashboard' ? 'Tổng quan Dashboard' : `Trang Kiểm thử Trang Lỗi ${activeTab.toUpperCase()}`}
            </span>
          </div>

          <div className="header-actions">
            <button
              className="btn-icon"
              title="Chuyển đổi Chế độ Sáng / Tối"
              onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            >
              {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
            </button>

            <div className="user-profile">
              <div className="user-avatar">HR</div>
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 700 }}>DTC245200439</span>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Nhân sự Nội bộ</span>
              </div>
            </div>
          </div>
        </header>

        {/* Demo Quick Switching Bar */}
        <div className="demo-control-bar">
          <div className="demo-title">
            <Sparkles size={16} />
            <span>Thanh Điều Hướng Kiểm Thử Trực Tiếp KN-17:</span>
          </div>

          <div className="demo-buttons">
            <button
              className={`demo-chip ${activeTab === 'dashboard' ? 'active' : ''}`}
              onClick={() => setActiveTab('dashboard')}
            >
              Trang chủ Tuyển dụng
            </button>
            <button
              className={`demo-chip ${activeTab === 'error-403' ? 'active' : ''}`}
              onClick={() => setActiveTab('error-403')}
            >
              Xem mẫu 403 (Không đủ quyền)
            </button>
            <button
              className={`demo-chip ${activeTab === 'error-404' ? 'active' : ''}`}
              onClick={() => setActiveTab('error-404')}
            >
              Xem mẫu 404 (Trang trống)
            </button>
            <button
              className={`demo-chip ${activeTab === 'error-401' ? 'active' : ''}`}
              onClick={() => setActiveTab('error-401')}
            >
              Xem mẫu 401 (Hết phiên)
            </button>
            <button
              className={`demo-chip ${activeTab === 'error-500' ? 'active' : ''}`}
              onClick={() => setActiveTab('error-500')}
            >
              Xem mẫu 500 (Lỗi máy chủ)
            </button>
            <button
              className={`demo-chip`}
              onClick={() => onTriggerSimulatedError('REACT_CRASH')}
              style={{ borderColor: '#ef4444', color: '#ef4444' }}
            >
              Thử React UI Crash (KN-75 Error Boundary)
            </button>
          </div>
        </div>

        {/* Dynamic Page Content */}
        {children}
      </main>
    </div>
  );
}
