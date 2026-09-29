import React, { useState, useEffect } from 'react';
import {
  Shield,
  ShieldCheck,
  ShieldAlert,
  UserCheck,
  CheckSquare,
  Lock,
  RefreshCw,
  Zap,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Users,
  Edit3,
  Sparkles,
  ChevronRight,
  Info
} from 'lucide-react';
import { apiRequest } from '../services/apiClient';

export function UserRoleManagement() {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(true);

  // Giả lập tài khoản đang đăng nhập hiện tại (Mặc định USR-001 - Admin)
  const [currentRequesterId, setCurrentRequesterId] = useState('USR-001');

  // State cho Modal chỉnh sửa vai trò
  const [editingUser, setEditingUser] = useState(null);
  const [selectedRoleCodes, setSelectedRoleCodes] = useState([]);
  const [saveLoading, setSaveLoading] = useState(false);
  const [modalError, setModalError] = useState(null);

  // State cho Kiểm thử Thao tác Kế tiếp (KN-89)
  const [testResult, setTestResult] = useState(null);
  const [testLoading, setTestLoading] = useState(false);

  // System notification toast
  const [notification, setNotification] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [usersRes, rolesRes] = await Promise.all([
        apiRequest('/api/v1/users'),
        apiRequest('/api/v1/roles')
      ]);

      if (usersRes?.success) setUsers(usersRes.data);
      if (rolesRes?.success) setRoles(rolesRes.data);
    } catch (err) {
      showNotification('error', 'Không thể tải dữ liệu vai trò: ' + (err.errorPayload?.message || err.message));
    } finally {
      setLoading(false);
    }
  };

  const showNotification = (type, message) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 5000);
  };

  const handleOpenEditModal = (user) => {
    setEditingUser(user);
    setSelectedRoleCodes([...user.roles]);
    setModalError(null);
  };

  const handleToggleRole = (roleCode) => {
    // Kiểm tra KN-88: Khóa chức năng Tự thu hồi quyền Admin
    const isSelf = currentRequesterId === editingUser.id;
    const isEditingAdminUser = editingUser.roles.includes('ADMIN');

    if (isSelf && isEditingAdminUser && roleCode === 'ADMIN') {
      setModalError({
        errorCode: 'SELF_ADMIN_REVOCATION_BLOCKED',
        message: 'Bạn không thể tự bỏ vai trò Quản trị (Admin) của chính mình để tránh mất quyền điều hành hệ thống.',
        hint: 'Mẹo: Nhờ một Quản trị viên khác thực hiện thu hồi quyền Admin nếu bạn muốn chuyển giao.'
      });
      return;
    }

    setModalError(null);
    if (selectedRoleCodes.includes(roleCode)) {
      if (selectedRoleCodes.length === 1) {
        setModalError({
          errorCode: 'VALIDATION_FAILED',
          message: 'Người dùng phải giữ ít nhất 1 vai trò trong hệ thống.',
          hint: 'Mẹo: Bạn không thể bỏ hết tất cả các vai trò.'
        });
        return;
      }
      setSelectedRoleCodes(selectedRoleCodes.filter(r => r !== roleCode));
    } else {
      setSelectedRoleCodes([...selectedRoleCodes, roleCode]);
    }
  };

  const handleSaveRoles = async () => {
    if (!editingUser) return;
    setSaveLoading(true);
    setModalError(null);

    try {
      const res = await apiRequest(`/api/v1/users/${editingUser.id}/roles`, {
        method: 'PUT',
        headers: {
          'x-user-id': currentRequesterId
        },
        body: JSON.stringify({ roles: selectedRoleCodes })
      });

      if (res?.success) {
        showNotification('success', `Đã cập nhật vai trò cho ${editingUser.name}. Quyền mới có hiệu lực ngay lập tức!`);
        // Cập nhật state danh sách users
        setUsers(users.map(u => u.id === editingUser.id ? res.data : u));
        setEditingUser(null);
      }
    } catch (err) {
      const payload = err.errorPayload || {};
      setModalError({
        errorCode: payload.errorCode || 'ERROR',
        message: payload.message || err.message,
        hint: payload.hint || 'Vui lòng kiểm tra lại thao tác.'
      });
    } finally {
      setSaveLoading(false);
    }
  };

  // Sub-task KN-89: Kiểm thử Thao tác Kế tiếp Tức thì
  const handleTestOperation = async (requiredRole, operationName) => {
    setTestLoading(true);
    setTestResult(null);

    try {
      const res = await apiRequest(`/api/v1/users/check-access?role=${requiredRole}`, {
        headers: {
          'x-user-id': currentRequesterId
        }
      });

      setTestResult({
        success: true,
        operationName,
        requiredRole,
        data: res.data,
        message: res.message
      });
    } catch (err) {
      const payload = err.errorPayload || {};
      setTestResult({
        success: false,
        operationName,
        requiredRole,
        status: payload.status || 403,
        errorCode: payload.errorCode || 'FORBIDDEN',
        message: payload.message || err.message,
        hint: payload.hint
      });
    } finally {
      setTestLoading(false);
    }
  };

  const getRoleBadgeStyle = (code) => {
    switch (code) {
      case 'ADMIN': return { bg: 'rgba(239, 68, 68, 0.12)', color: '#ef4444', border: 'rgba(239, 68, 68, 0.3)', icon: ShieldCheck };
      case 'HIRING_MANAGER': return { bg: 'rgba(79, 70, 229, 0.12)', color: '#6366f1', border: 'rgba(79, 70, 229, 0.3)', icon: UserCheck };
      case 'INTERVIEWER': return { bg: 'rgba(16, 185, 129, 0.12)', color: '#10b981', border: 'rgba(16, 185, 129, 0.3)', icon: CheckSquare };
      case 'RECRUITER': return { bg: 'rgba(245, 158, 11, 0.12)', color: '#f59e0b', border: 'rgba(245, 158, 11, 0.3)', icon: Users };
      default: return { bg: 'rgba(107, 114, 128, 0.12)', color: '#9ca3af', border: 'rgba(107, 114, 128, 0.3)', icon: Shield };
    }
  };

  const currentRequesterUser = users.find(u => u.id === currentRequesterId);

  return (
    <div style={{ padding: '1.5rem', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Toast Notification */}
      {notification && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          zIndex: 1000,
          padding: '1rem 1.5rem',
          borderRadius: 'var(--radius-md)',
          backgroundColor: notification.type === 'success' ? '#10b981' : '#ef4444',
          color: '#ffffff',
          boxShadow: '0 10px 25px rgba(0,0,0,0.2)',
          display: 'flex',
          alignItems: 'center',
          gap: '0.75rem',
          fontWeight: 600,
          animation: 'slideIn 0.3s ease-out'
        }}>
          {notification.type === 'success' ? <CheckCircle2 size={20} /> : <AlertTriangle size={20} />}
          <span>{notification.message}</span>
        </div>
      )}

      {/* Header Banner cho User Story KN-19 */}
      <div style={{
        backgroundColor: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '1.75rem',
        marginBottom: '2rem',
        boxShadow: 'var(--card-shadow)',
        background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.05) 0%, rgba(168, 85, 247, 0.05) 100%)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              <span style={{
                backgroundColor: 'var(--accent-primary)',
                color: '#fff',
                fontSize: '0.75rem',
                fontWeight: 700,
                padding: '0.2rem 0.6rem',
                borderRadius: 'var(--radius-full)'
              }}>
                TICKET KN-19
              </span>
              <span style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>Quản trị Phân quyền Hệ thống</span>
            </div>
            <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
              Gán và Thu hồi Vai trò Người dùng
            </h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', maxWidth: '850px' }}>
              Xử lý trường hợp một cán bộ có thể đảm nhận đồng thời nhiều vai trò (ví dụ: vừa là <strong>Hiring Manager</strong> vừa là <strong>Người phỏng vấn</strong>). Quy tắc bảo mật khóa tự thu hồi Admin và áp dụng quyền ngay ở thao tác tiếp theo.
            </p>
          </div>

          <button
            onClick={fetchData}
            className="btn-icon"
            style={{ padding: '0.6rem 1rem', display: 'flex', alignItems: 'center', gap: '0.5rem', borderRadius: 'var(--radius-md)' }}
          >
            <RefreshCw size={16} className={loading ? 'spin' : ''} />
            <span>Làm mới Dữ liệu</span>
          </button>
        </div>

        {/* Requirements Badges List */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem', marginTop: '1.25rem' }}>
          <div style={{ backgroundColor: 'var(--bg-app)', padding: '0.85rem 1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
            <Sparkles size={18} color="#6366f1" style={{ marginTop: '2px', flexShrink: 0 }} />
            <div>
              <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)', display: 'block' }}>KN-87 & KN-90: Giữ Nhiều Vai trò</strong>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Một tài khoản có thể được gán đồng thời nhiều quyền hạn.</span>
            </div>
          </div>

          <div style={{ backgroundColor: 'var(--bg-app)', padding: '0.85rem 1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
            <Lock size={18} color="#ef4444" style={{ marginTop: '2px', flexShrink: 0 }} />
            <div>
              <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)', display: 'block' }}>KN-88 & KN-91: Chặn Tự Thu hồi Admin</strong>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Khóa giao diện & Logic BE không cho tự gỡ quyền Admin của chính mình.</span>
            </div>
          </div>

          <div style={{ backgroundColor: 'var(--bg-app)', padding: '0.85rem 1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'flex-start', gap: '0.75rem' }}>
            <Zap size={18} color="#10b981" style={{ marginTop: '2px', flexShrink: 0 }} />
            <div>
              <strong style={{ fontSize: '0.85rem', color: 'var(--text-primary)', display: 'block' }}>KN-89 & KN-92: Hiệu lực Tức thì</strong>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Cập nhật vai trò có tác dụng ngay lập tức ở yêu cầu kế tiếp.</span>
            </div>
          </div>
        </div>
      </div>

      {/* Controller Chuyển đổi Tài khoản Giả lập đang Đăng nhập */}
      <div style={{
        backgroundColor: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-md)',
        padding: '1.25rem',
        marginBottom: '1.75rem',
        display: 'flex',
        alignItems: 'center',
        justify: 'space-between',
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{
            width: '40px',
            height: '40px',
            borderRadius: '50%',
            backgroundColor: 'rgba(99, 102, 241, 0.15)',
            color: 'var(--accent-primary)',
            display: 'flex',
            alignItems: 'center',
            justify: 'center',
            fontWeight: 800
          }}>
            {currentRequesterUser?.name.substring(0, 2).toUpperCase() || 'US'}
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
              Tài khoản đang đăng nhập (Requester):
            </div>
            <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--text-primary)' }}>
              {currentRequesterUser?.name} ({currentRequesterUser?.email})
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <label htmlFor="requester-select" style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', fontWeight: 600 }}>
            Đổi nhân tài khoản:
          </label>
          <select
            id="requester-select"
            value={currentRequesterId}
            onChange={(e) => {
              setCurrentRequesterId(e.target.value);
              setTestResult(null);
            }}
            style={{
              padding: '0.5rem 1rem',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border-color)',
              backgroundColor: 'var(--bg-app)',
              color: 'var(--text-primary)',
              fontWeight: 600,
              cursor: 'pointer'
            }}
          >
            {users.map(u => (
              <option key={u.id} value={u.id}>
                {u.name} [{u.roles.join(', ')}]
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Main Table Content - Subtask KN-87 */}
      <div style={{
        backgroundColor: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--card-shadow)',
        overflow: 'hidden',
        marginBottom: '2rem'
      }}>
        <div style={{
          padding: '1.25rem 1.5rem',
          borderBottom: '1px solid var(--border-color)',
          display: 'flex',
          alignItems: 'center',
          justify: 'space-between'
        }}>
          <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Users size={20} color="var(--accent-primary)" />
            <span>Danh sách Người dùng & Vai trò Hiện tại (Subtask KN-87 & KN-90)</span>
          </h2>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            Tổng số: <strong>{users.length}</strong> cán bộ
          </span>
        </div>

        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            <RefreshCw className="spin" size={32} style={{ marginBottom: '0.5rem' }} />
            <p>Đang tải danh sách phân quyền người dùng...</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ backgroundColor: 'var(--bg-app)', borderBottom: '1px solid var(--border-color)' }}>
                  <th style={{ padding: '1rem 1.5rem', fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Họ và Tên / Email</th>
                  <th style={{ padding: '1rem 1.5rem', fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Phòng ban</th>
                  <th style={{ padding: '1rem 1.5rem', fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>Vai trò được Gán (Roles)</th>
                  <th style={{ padding: '1rem 1.5rem', fontSize: '0.8rem', color: 'var(--text-muted)', textTransform: 'uppercase', textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => {
                  const isCurrentLoggedUser = user.id === currentRequesterId;
                  const hasMultipleRoles = user.roles.length > 1;

                  return (
                    <tr
                      key={user.id}
                      style={{
                        borderBottom: '1px solid var(--border-color)',
                        backgroundColor: isCurrentLoggedUser ? 'rgba(99, 102, 241, 0.04)' : 'transparent',
                        transition: 'background-color 0.2s'
                      }}
                    >
                      <td style={{ padding: '1.25rem 1.5rem' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                          <div style={{
                            width: '38px',
                            height: '38px',
                            borderRadius: '50%',
                            backgroundColor: 'var(--bg-app)',
                            border: '1px solid var(--border-color)',
                            display: 'flex',
                            alignItems: 'center',
                            justify: 'center',
                            fontWeight: 700,
                            color: 'var(--text-primary)'
                          }}>
                            {user.name.charAt(0)}
                          </div>
                          <div>
                            <div style={{ fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                              <span>{user.name}</span>
                              {isCurrentLoggedUser && (
                                <span style={{
                                  backgroundColor: 'var(--accent-primary)',
                                  color: '#fff',
                                  fontSize: '0.65rem',
                                  padding: '0.1rem 0.4rem',
                                  borderRadius: 'var(--radius-full)'
                                }}>
                                  BẠN
                                </span>
                              )}
                            </div>
                            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{user.email}</div>
                          </div>
                        </div>
                      </td>

                      <td style={{ padding: '1.25rem 1.5rem', color: 'var(--text-secondary)', fontWeight: 500 }}>
                        {user.department}
                      </td>

                      <td style={{ padding: '1.25rem 1.5rem' }}>
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem', alignItems: 'center' }}>
                          {user.roles.map(rCode => {
                            const badge = getRoleBadgeStyle(rCode);
                            const IconComponent = badge.icon;
                            const roleObj = roles.find(r => r.code === rCode);

                            return (
                              <span
                                key={rCode}
                                title={roleObj?.description || rCode}
                                style={{
                                  backgroundColor: badge.bg,
                                  color: badge.color,
                                  border: `1px solid ${badge.border}`,
                                  fontSize: '0.75rem',
                                  fontWeight: 700,
                                  padding: '0.25rem 0.6rem',
                                  borderRadius: 'var(--radius-md)',
                                  display: 'inline-flex',
                                  alignItems: 'center',
                                  gap: '0.3rem'
                                }}
                              >
                                <IconComponent size={13} />
                                <span>{roleObj?.name || rCode}</span>
                              </span>
                            );
                          })}

                          {hasMultipleRoles && (
                            <span style={{
                              fontSize: '0.7rem',
                              color: 'var(--accent-primary)',
                              backgroundColor: 'rgba(99, 102, 241, 0.1)',
                              padding: '0.2rem 0.5rem',
                              borderRadius: 'var(--radius-full)',
                              fontWeight: 700,
                              marginLeft: '0.2rem'
                            }}>
                              ⭐ Đa vai trò ({user.roles.length})
                            </span>
                          )}
                        </div>
                      </td>

                      <td style={{ padding: '1.25rem 1.5rem', textAlign: 'right' }}>
                        <button
                          onClick={() => handleOpenEditModal(user)}
                          style={{
                            padding: '0.45rem 0.9rem',
                            borderRadius: 'var(--radius-sm)',
                            border: '1px solid var(--accent-primary)',
                            backgroundColor: 'transparent',
                            color: 'var(--accent-primary)',
                            fontWeight: 600,
                            fontSize: '0.85rem',
                            cursor: 'pointer',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.4rem',
                            transition: 'all 0.2s'
                          }}
                        >
                          <Edit3 size={14} />
                          <span>Gán / Thu hồi Vai trò</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Subtask KN-89 & KN-92: Console Kiểm thử Thao tác Kế tiếp */}
      <div style={{
        backgroundColor: 'var(--bg-surface)',
        border: '1px solid var(--border-color)',
        borderRadius: 'var(--radius-lg)',
        padding: '1.5rem',
        boxShadow: 'var(--card-shadow)'
      }}>
        <div style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '0.5rem' }}>
          <div>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--text-primary)', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Zap size={20} color="#10b981" />
              <span>Kiểm thử Áp dụng Quyền Tức thì ở Thao tác Kế tiếp (KN-89 & KN-92)</span>
            </h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Nhấn thử các hành động bên dưới với tư cách tài khoản <strong>{currentRequesterUser?.name}</strong>. Backend sẽ phản hồi tức thời dựa trên vai trò hiện tại!
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1.25rem' }}>
          <button
            onClick={() => handleTestOperation('HIRING_MANAGER', 'Duyệt Định biên Tuyển dụng')}
            disabled={testLoading}
            className="demo-chip"
            style={{ padding: '0.6rem 1.1rem', borderRadius: 'var(--radius-md)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <UserCheck size={16} color="#6366f1" />
            <span>Thực hiện Thao tác Hiring Manager</span>
          </button>

          <button
            onClick={() => handleTestOperation('INTERVIEWER', 'Nhập Đánh giá Phỏng vấn')}
            disabled={testLoading}
            className="demo-chip"
            style={{ padding: '0.6rem 1.1rem', borderRadius: 'var(--radius-md)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <CheckSquare size={16} color="#10b981" />
            <span>Thực hiện Thao tác Người phỏng vấn</span>
          </button>

          <button
            onClick={() => handleTestOperation('ADMIN', 'Xem Báo cáo Quỹ lương')}
            disabled={testLoading}
            className="demo-chip"
            style={{ padding: '0.6rem 1.1rem', borderRadius: 'var(--radius-md)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <ShieldCheck size={16} color="#ef4444" />
            <span>Thực hiện Thao tác Quản trị viên (Admin)</span>
          </button>
        </div>

        {/* Dynamic Response Result Display */}
        {testResult && (
          <div style={{
            padding: '1.25rem',
            borderRadius: 'var(--radius-md)',
            border: `1px solid ${testResult.success ? '#10b981' : '#ef4444'}`,
            backgroundColor: testResult.success ? 'rgba(16, 185, 129, 0.08)' : 'rgba(239, 68, 68, 0.08)',
            animation: 'fadeIn 0.25s ease-in'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                {testResult.success ? <CheckCircle2 color="#10b981" size={20} /> : <XCircle color="#ef4444" size={20} />}
                <strong style={{ color: testResult.success ? '#10b981' : '#ef4444', fontSize: '1rem' }}>
                  {testResult.success ? 'KẾT QUẢ: CHO PHÉP THỰC HIỆN (200 OK)' : `KẾT QUẢ: TỪ CHỐI TRUY CẬP (${testResult.status} ${testResult.errorCode})`}
                </strong>
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Thời gian thực thi: {new Date().toLocaleTimeString()}
              </span>
            </div>

            <p style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: '0.9rem', marginBottom: '0.4rem' }}>
              {testResult.message}
            </p>

            {testResult.hint && (
              <div style={{ fontSize: '0.825rem', color: 'var(--text-secondary)', fontStyle: 'italic', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Info size={14} color="var(--accent-primary)" />
                <span>{testResult.hint}</span>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Modal Chỉnh sửa Vai trò - Subtasks KN-87 & KN-88 */}
      {editingUser && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.6)',
          backdropFilter: 'blur(4px)',
          zIndex: 999,
          display: 'flex',
          alignItems: 'center',
          justify: 'center',
          padding: '1rem'
        }}>
          <div style={{
            backgroundColor: 'var(--bg-surface)',
            border: '1px solid var(--border-color)',
            borderRadius: 'var(--radius-lg)',
            boxShadow: 'var(--glass-shadow)',
            width: '100%',
            maxWidth: '560px',
            overflow: 'hidden',
            animation: 'scaleUp 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
          }}>
            {/* Modal Header */}
            <div style={{
              padding: '1.25rem 1.5rem',
              borderBottom: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              justify: 'space-between'
            }}>
              <div>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--text-primary)' }}>
                  Gán nhiều Vai trò cho {editingUser.name}
                </h3>
                <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                  Email: {editingUser.email}
                </span>
              </div>
              <button
                onClick={() => setEditingUser(null)}
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-muted)' }}
              >
                ✕
              </button>
            </div>

            {/* Modal Body */}
            <div style={{ padding: '1.5rem', maxHeight: '70vh', overflowY: 'auto' }}>
              {/* Alert nếu bị chặn Tự thu hồi Admin (KN-88) */}
              {editingUser.id === currentRequesterId && editingUser.roles.includes('ADMIN') && (
                <div style={{
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.3)',
                  marginBottom: '1.25rem',
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '0.75rem'
                }}>
                  <Lock size={18} color="#ef4444" style={{ marginTop: '2px', flexShrink: 0 }} />
                  <div>
                    <strong style={{ fontSize: '0.85rem', color: '#ef4444', display: 'block' }}>
                      KN-88: Khóa chức năng Tự thu hồi quyền Admin
                    </strong>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                      Bạn đang chỉnh sửa vai trò của chính mình. Vai trò <strong>Quản trị hệ thống (ADMIN)</strong> đã bị khóa cố định để ngăn chặn việc tự tước quyền quản trị.
                    </span>
                  </div>
                </div>
              )}

              {/* Error Message banner inside modal if API validation fails */}
              {modalError && (
                <div style={{
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'rgba(239, 68, 68, 0.12)',
                  border: '1px solid #ef4444',
                  marginBottom: '1.25rem'
                }}>
                  <div style={{ color: '#ef4444', fontWeight: 700, fontSize: '0.875rem', marginBottom: '0.2rem' }}>
                    {modalError.errorCode}: {modalError.message}
                  </div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                    {modalError.hint}
                  </div>
                </div>
              )}

              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1rem', fontWeight: 600 }}>
                Tích chọn các vai trò muốn gán cho người dùng này (Có thể chọn đồng thời nhiều vai trò):
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {roles.map((role) => {
                  const isChecked = selectedRoleCodes.includes(role.code);
                  const isSelfAdminLocked = (editingUser.id === currentRequesterId) && (role.code === 'ADMIN') && editingUser.roles.includes('ADMIN');
                  const badge = getRoleBadgeStyle(role.code);

                  return (
                    <label
                      key={role.code}
                      style={{
                        display: 'flex',
                        alignItems: 'flex-start',
                        gap: '0.85rem',
                        padding: '1rem',
                        borderRadius: 'var(--radius-md)',
                        border: `1.5px solid ${isChecked ? 'var(--accent-primary)' : 'var(--border-color)'}`,
                        backgroundColor: isChecked ? 'rgba(99, 102, 241, 0.05)' : 'var(--bg-app)',
                        cursor: isSelfAdminLocked ? 'not-allowed' : 'pointer',
                        opacity: isSelfAdminLocked ? 0.75 : 1,
                        transition: 'all 0.2s'
                      }}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        disabled={isSelfAdminLocked}
                        onChange={() => handleToggleRole(role.code)}
                        style={{
                          width: '18px',
                          height: '18px',
                          marginTop: '3px',
                          accentColor: 'var(--accent-primary)',
                          cursor: isSelfAdminLocked ? 'not-allowed' : 'pointer'
                        }}
                      />

                      <div style={{ flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.2rem' }}>
                          <strong style={{ color: 'var(--text-primary)', fontSize: '0.95rem' }}>
                            {role.name}
                          </strong>
                          <span style={{
                            fontSize: '0.7rem',
                            fontWeight: 700,
                            padding: '0.15rem 0.4rem',
                            borderRadius: 'var(--radius-sm)',
                            backgroundColor: badge.bg,
                            color: badge.color
                          }}>
                            {role.code}
                          </span>

                          {isSelfAdminLocked && (
                            <span style={{
                              fontSize: '0.7rem',
                              color: '#ef4444',
                              backgroundColor: 'rgba(239, 68, 68, 0.15)',
                              padding: '0.15rem 0.5rem',
                              borderRadius: 'var(--radius-full)',
                              fontWeight: 700,
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.2rem'
                            }}>
                              <Lock size={11} /> Khóa Tự Thu Hồi
                            </span>
                          )}
                        </div>
                        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                          {role.description}
                        </p>
                      </div>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Modal Footer */}
            <div style={{
              padding: '1.25rem 1.5rem',
              borderTop: '1px solid var(--border-color)',
              display: 'flex',
              alignItems: 'center',
              justify: 'flex-end',
              gap: '0.75rem',
              backgroundColor: 'var(--bg-app)'
            }}>
              <button
                onClick={() => setEditingUser(null)}
                style={{
                  padding: '0.55rem 1.2rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                  backgroundColor: 'transparent',
                  color: 'var(--text-secondary)',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                Hủy bỏ
              </button>

              <button
                onClick={handleSaveRoles}
                disabled={saveLoading}
                style={{
                  padding: '0.55rem 1.4rem',
                  borderRadius: 'var(--radius-md)',
                  border: 'none',
                  background: 'var(--accent-gradient)',
                  color: '#ffffff',
                  fontWeight: 700,
                  cursor: 'pointer',
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  boxShadow: '0 4px 12px rgba(79, 70, 229, 0.3)'
                }}
              >
                {saveLoading ? <RefreshCw size={16} className="spin" /> : <ShieldCheck size={16} />}
                <span>Lưu Vai trò Mới</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
