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
  WifiOff,
  UserCheck
} from 'lucide-react';

export function AppShell({
  activeTab,
  setActiveTab,
  theme,
  setTheme,
  onTriggerSimulatedError,
  children
}) {
  // --- KN-65: LOGIC ĐIỀU HƯỚNG ĐỘNG BẰNG PHÂN QUYỀN (ROLE-BASED DYNAMIC NAVIGATION) ---
  
  // 1. Quản lý Role người dùng hiện tại (Mặc định demo là 'HR', có thể switch đổi vai trò)
  const [currentRole, setCurrentRole] = useState('HR'); 

  // 2. Định nghĩa danh sách các Menu kèm theo Quyền được truy cập (roles)
  const menuSections = [
    {
      title: 'Quản lý Tuyển dụng',
      items: [
        { id: 'dashboard', label: 'Danh sách Ứng viên', icon: Users, roles: ['HR', 'ADMIN', 'INTERVIEWER'] },
        { id: 'vacancies', label: 'Vị trí Tuyển dụng', icon: Briefcase, roles: ['HR', 'ADMIN'] },
        { id: 'schedule', label: 'Lịch Phỏng vấn', icon: Calendar, roles: ['HR', 'ADMIN', 'INTERVIEWER'] },
      ]
    },
    {
      title: 'Quản lý Tài khoản',
      items: [
        { id: 'forgot-password', label: 'Quên mật khẩu (Demo)', icon: ShieldCheck, roles: ['HR', 'ADMIN', 'INTERVIEWER', 'CANDIDATE'] },
      ]
    },
    {
      title: 'Kiểm thử Trang Lỗi (KN-17)',
      items: [
        { id: 'error-403', label: 'Lỗi 403 (Không đủ quyền)', icon: ShieldAlert, color: 'var(--error-403-color)', roles: ['HR', 'ADMIN'] },
        { id: 'error-404', label: 'Lỗi 404 (Không tìm thấy)', icon: FileQuestion, color: 'var(--error-404-color)', roles: ['HR', 'ADMIN'] },
        { id: 'error-401', label: 'Lỗi 401 (Hết phiên login)', icon: Lock, color: 'var(--error-401-color)', roles: ['HR', 'ADMIN'] },
        { id: 'error-500', label: 'Lỗi 500 (Máy chủ hỏng)', icon: ServerCrash, color: 'var(--error-500-color)', roles: ['HR', 'ADMIN'] },
        { id: 'error-503', label: 'Lỗi 503 (Bảo trì/Mất mạng)', icon: WifiOff, color: 'var(--error-503-color)', roles: ['HR', 'ADMIN'] },
      ]
    }
  ];

  return (
    <div className="app-container" data-theme={theme}>
      {/* Sidebar Navigation */}
      <aside className="app-sidebar">
        <div className="sidebar-logo">
          <div className="logo-badge">{currentRole}</div>
          <div className="logo-text">
            <h2>Hệ Thống Tuyển Dụng</h2>
            <p>TTCS Kỳ T9/2026 (K5S6)</p>
          </div>
        </div>

        {/* KN-65: BỘ LỌC ĐIỀU HƯỚNG ĐỘNG (DYNAMIC MENU RENDER) */}
        <div className="nav-menu">
          {menuSections.map((section, idx) => {
            // Lọc ra các item thuộc quyền của role hiện tại
            const visibleItems = section.items.filter(item => item.roles.includes(currentRole));

            // Nếu không có item nào thỏa mãn quyền, không hiển thị tiêu đề nhóm này
            if (visibleItems.length === 0) return null;

            return (
              <React.Fragment key={idx}>
                <div className="nav-menu-title" style={{ marginTop: idx > 0 ? '1.25rem' : '0' }}>
                  {section.title}
                </div>
                {visibleItems.map(item => {
                  const IconComponent = item.icon;
                  return (
                    <div
                      key={item.id}
                      className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
                      onClick={() => setActiveTab(item.id)}
                    >
                      <IconComponent size={18} color={item.color || 'currentColor'} />
                      <span>{item.label}</span>
                    </div>
                  );
                })}
              </React.Fragment>
            );
          })}
        </div>

        <div style={{ marginTop: 'auto', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            User Story: <strong>KN-65</strong><br />
            Chức năng: <strong>Điều hướng Động (Dynamic Nav)</strong>
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
              {activeTab === 'dashboard' ? 'Tổng quan Dashboard' : `Trang Kiểm thử ${activeTab.toUpperCase()}`}
            </span>
          </div>

          <div className="header-actions">
            {/* THÊM BỘ CHUYỂN ĐỔI ROLE ĐỂ ĐEM ĐI DEMO / TESTING THUẬN TIỆN */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginRight: '1rem', background: 'var(--bg-card)', padding: '0.25rem 0.5rem', borderRadius: '6px', border: '1px solid var(--border-color)' }}>
              <UserCheck size={16} color="var(--accent-primary)" />
              <span style={{ fontSize: '0.75rem', fontWeight: 600 }}>Role:</span>
              <select 
                value={currentRole} 
                onChange={(e) => setCurrentRole(e.target.value)}
                style={{ background: 'transparent', color: 'inherit', border: 'none', fontWeight: 'bold', cursor: 'pointer', outline: 'none' }}
              >
                <option value="HR">HR (Nhân sự)</option>
                <option value="ADMIN">ADMIN (Quản trị)</option>
                <option value="INTERVIEWER">INTERVIEWER (Người phỏng vấn)</option>
                <option value="CANDIDATE">CANDIDATE (Ứng viên)</option>
              </select>
            </div>

            <button
              className="btn-icon"
              title="Chuyển đổi Chế độ Sáng / Tối"
              onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
            >
              {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
            </button>

            <div className="user-profile">
              <div className="user-avatar">{currentRole}</div>
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <span style={{ fontSize: '0.8rem', fontWeight: 700 }}>DTC245200439</span>
                <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Quyền: {currentRole}</span>
              </div>
            </div>
          </div>
        </header>

        {/* Demo Quick Switching Bar */}
        <div className="demo-control-bar">
          <div className="demo-title">
            <Sparkles size={16} />
            <span>Thanh Điều Hướng Kiểm Thử Trực Tiếp KN-65 (Role: {currentRole}):</span>
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