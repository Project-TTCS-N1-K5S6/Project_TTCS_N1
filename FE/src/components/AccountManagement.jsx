import React, { useEffect, useState } from 'react';
import { Search, UserPlus, Pencil, Trash2, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { apiRequest } from '../services/apiClient';

const ROLES = ['nhan_su', 'nguoi_phong_van', 'quan_tri'];
const STATUSES = ['pending', 'active', 'locked'];
const ROLE_LABELS = { nhan_su: 'Nhân sự', nguoi_phong_van: 'Người phỏng vấn', quan_tri: 'Quản trị viên' };
const STATUS_LABELS = { pending: 'Chờ kích hoạt', active: 'Đang hoạt động', locked: 'Đã khóa' };
const emptyForm = { fullName: '', email: '', department: '', role: 'nguoi_phong_van', status: 'active' };

export function AccountManagement({ onTriggerError }) {
  const [query, setQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(1);
  const [result, setResult] = useState({ data: [], total: 0, pageSize: 20 });
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  async function load() {
    setLoading(true);
    try {
      const params = new URLSearchParams({ page: String(page) });
      if (query.trim()) params.set('query', query.trim());
      if (roleFilter) params.set('role', roleFilter);
      if (statusFilter) params.set('status', statusFilter);
      const response = await apiRequest(`/api/v1/auth/accounts?${params}`);
      setResult(response);
    } catch (err) {
      if (err.errorPayload?.status === 401) window.location.assign('/login.html');
      else setError(err.message || 'Không thể tải danh sách nhân viên.');
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, [page, query, roleFilter, statusFilter]);
  const totalPages = Math.max(1, Math.ceil(result.total / result.pageSize));
  const beginCreate = () => { setEditingId(null); setForm({ ...emptyForm }); setNotice(''); setError(''); };
  const beginEdit = (user) => { setEditingId(user.id); setForm({ fullName: user.name, email: user.email, department: user.department || '', role: ROLES.includes(user.role) ? user.role : 'nguoi_phong_van', status: user.status }); setNotice(''); setError(''); };

  async function save(event) {
    event.preventDefault(); setError(''); setNotice('');
    try {
      const response = await apiRequest(editingId ? `/api/v1/auth/accounts/${editingId}` : '/api/v1/auth/accounts', {
        method: editingId ? 'PATCH' : 'POST', body: JSON.stringify(form)
      });
      setNotice(response.message); setForm(null); await load();
    } catch (err) {
      if (err.errorPayload?.status === 401) window.location.assign('/login.html');
      else setError(err.message || 'Không thể lưu thông tin nhân viên.');
    }
  }

  async function remove(user) {
    if (!window.confirm(`Xóa ${user.name} khỏi danh sách nhân viên? Tài khoản sẽ bị khóa và các phiên đăng nhập bị thu hồi.`)) return;
    setError(''); setNotice('');
    try {
      const response = await apiRequest(`/api/v1/auth/accounts/${user.id}`, { method: 'DELETE' });
      setNotice(response.message);
      if (result.data.length === 1 && page > 1) setPage(page - 1);
      else await load();
    } catch (err) {
      if (err.errorPayload?.status === 401) window.location.assign('/login.html');
      else setError(err.message || 'Không thể xóa nhân viên.');
    }
  }

  return <section className="account-page">
    <div className="account-heading"><div><p className="account-eyebrow">QUẢN TRỊ HỆ THỐNG</p><h1>Danh sách nhân viên</h1><p>Tìm kiếm, tạo, sửa và xóa tài khoản nhân viên nội bộ.</p></div>
      <button className="account-primary" onClick={beginCreate}><UserPlus size={17}/> Tạo tài khoản</button></div>
    {notice && <div className="account-notice">{notice}</div>}
    <div className="account-filters"><label className="account-search"><Search size={17}/><input value={query} onChange={e => { setQuery(e.target.value); setPage(1); }} placeholder="Tìm tên, email hoặc phòng ban" /></label>
      <select value={roleFilter} onChange={e => { setRoleFilter(e.target.value); setPage(1); }}><option value="">Tất cả vai trò</option>{ROLES.map(role => <option key={role} value={role}>{ROLE_LABELS[role]}</option>)}</select>
      <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(1); }}><option value="">Tất cả trạng thái</option>{STATUSES.map(status => <option key={status} value={status}>{STATUS_LABELS[status]}</option>)}</select>
    </div>
    {error && !form && <p className="account-error" role="alert">{error}</p>}
    {form && <form className="account-form" onSubmit={save}><div className="account-form-head"><h2>{editingId ? 'Sửa nhân viên' : 'Tạo nhân viên mới'}</h2><button type="button" className="account-close" onClick={() => setForm(null)} aria-label="Đóng"><X size={18}/></button></div>
      <div className="account-form-grid"><label>Họ và tên<input required minLength="2" value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })}/></label><label>Email<input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}/></label><label>Phòng ban<input required value={form.department} onChange={e => setForm({ ...form, department: e.target.value })}/></label><label>Vai trò<select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>{ROLES.map(role => <option key={role} value={role}>{ROLE_LABELS[role]}</option>)}</select></label>
        {editingId && <label>Trạng thái<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>{STATUSES.map(status => <option key={status} value={status}>{STATUS_LABELS[status]}</option>)}</select></label>}</div>
      {!editingId && <p className="account-form-hint">Hệ thống tạo mật khẩu tạm thời và gửi đến email đã nhập.</p>}
      {error && <p className="account-error">{error}</p>}<div className="account-form-actions"><button type="button" className="account-secondary" onClick={() => setForm(null)}>Hủy</button><button className="account-primary" type="submit">{editingId ? 'Lưu thay đổi' : 'Tạo và gửi email'}</button></div>
    </form>}
    <div className="account-table-wrap"><table className="account-table"><thead><tr><th>Họ và tên</th><th>Email</th><th>Phòng ban</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody>
      {loading ? <tr><td colSpan="6" className="account-empty">Đang tải danh sách nhân viên…</td></tr> : result.data.length ? result.data.map(user => <tr key={user.id}><td className="account-name">{user.name}</td><td>{user.email}</td><td>{user.department || '—'}</td><td>{ROLE_LABELS[user.role] || user.role}</td><td><span className={`account-status ${user.status === 'active' ? 'active' : user.status === 'locked' ? 'locked' : 'pending'}`}>{STATUS_LABELS[user.status] || user.status}</span></td><td><div className="account-row-actions"><button className="account-edit" onClick={() => beginEdit(user)} aria-label={`Sửa ${user.name}`} title="Sửa"><Pencil size={16}/></button><button className="account-edit account-delete" onClick={() => remove(user)} aria-label={`Xóa ${user.name}`} title="Xóa"><Trash2 size={16}/></button></div></td></tr>) : <tr><td colSpan="6" className="account-empty">Không tìm thấy nhân viên phù hợp.</td></tr>}
    </tbody></table></div>
    <div className="account-pagination"><span>{result.total ? `${(page - 1) * 20 + 1}–${Math.min(page * 20, result.total)} trong ${result.total} nhân viên` : '0 nhân viên'}</span><div><button disabled={page <= 1} onClick={() => setPage(page - 1)} aria-label="Trang trước"><ChevronLeft size={18}/></button><span>Trang {page} / {totalPages}</span><button disabled={page >= totalPages} onClick={() => setPage(page + 1)} aria-label="Trang sau"><ChevronRight size={18}/></button></div></div>
  </section>;
}
