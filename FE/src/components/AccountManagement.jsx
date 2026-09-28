import React, { useEffect, useState } from 'react';
import { Search, UserPlus, Pencil, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { apiRequest } from '../services/apiClient';

const ROLES = ['HR', 'INTERVIEWER', 'ADMIN'];
const STATUSES = ['Chờ kích hoạt', 'Đang hoạt động', 'Đã khóa'];
const ROLE_LABELS = { HR: 'Nhân sự', INTERVIEWER: 'Người phỏng vấn', ADMIN: 'Quản trị viên' };
const emptyForm = { name: '', email: '', department: '', role: 'INTERVIEWER', status: 'Đang hoạt động' };

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
      setResult(await apiRequest(`/api/v1/auth/accounts?${params}`));
    } catch (err) {
      if (err.errorPayload?.status === 403) setError(err.message);
      else if (err.errorPayload) onTriggerError(err.errorPayload);
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, [page, query, roleFilter, statusFilter]);
  const totalPages = Math.max(1, Math.ceil(result.total / result.pageSize));
  const beginCreate = () => { setEditingId(null); setForm({ ...emptyForm }); setNotice(''); setError(''); };
  const beginEdit = (user) => { setEditingId(user.id); setForm({ name: user.name, email: user.email, department: user.department || '', role: ROLES.includes(user.role) ? user.role : 'HR', status: user.status }); setNotice(''); setError(''); };

  async function save(event) {
    event.preventDefault(); setError(''); setNotice('');
    try {
      const response = await apiRequest(editingId ? `/api/v1/auth/accounts/${editingId}` : '/api/v1/auth/accounts', {
        method: editingId ? 'PATCH' : 'POST', body: JSON.stringify(form)
      });
      setNotice(response.message); setForm(null); await load();
    } catch (err) {
      if (err.errorPayload?.status === 409 || err.errorPayload?.status === 400 || err.errorPayload?.status === 503 || err.errorPayload?.status === 502) setError(err.message);
      else if (err.errorPayload) onTriggerError(err.errorPayload);
    }
  }

  return <section className="account-page">
    <div className="account-heading"><div><p className="account-eyebrow">QUẢN TRỊ HỆ THỐNG</p><h1>Quản lý tài khoản</h1><p>Tạo, cập nhật và cấp quyền cho thành viên hội đồng phỏng vấn.</p></div>
      <button className="account-primary" onClick={beginCreate}><UserPlus size={17}/> Tạo tài khoản</button></div>
    {notice && <div className="account-notice">{notice}</div>}
    <div className="account-filters"><label className="account-search"><Search size={17}/><input value={query} onChange={e => { setQuery(e.target.value); setPage(1); }} placeholder="Tìm tên, email hoặc phòng ban" /></label>
      <select value={roleFilter} onChange={e => { setRoleFilter(e.target.value); setPage(1); }}><option value="">Tất cả vai trò</option>{ROLES.map(role => <option key={role} value={role}>{ROLE_LABELS[role]}</option>)}</select>
      <select value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(1); }}><option value="">Tất cả trạng thái</option>{STATUSES.map(status => <option key={status}>{status}</option>)}</select>
    </div>
    {form && <form className="account-form" onSubmit={save}><div className="account-form-head"><h2>{editingId ? 'Sửa tài khoản' : 'Tạo tài khoản mới'}</h2><button type="button" className="account-close" onClick={() => setForm(null)} aria-label="Đóng"><X size={18}/></button></div>
      <div className="account-form-grid"><label>Họ và tên<input required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}/></label><label>Email<input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}/></label><label>Phòng ban<input required value={form.department} onChange={e => setForm({ ...form, department: e.target.value })}/></label><label>Vai trò<select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>{ROLES.map(role => <option key={role} value={role}>{ROLE_LABELS[role]}</option>)}</select></label>
        {editingId && <label>Trạng thái<select value={form.status} onChange={e => setForm({ ...form, status: e.target.value })}>{STATUSES.map(status => <option key={status}>{status}</option>)}</select></label>}</div>
      {!editingId && <p className="account-form-hint">Hệ thống tạo mật khẩu tạm thời và gửi đến email đã nhập.</p>}
      {error && <p className="account-error">{error}</p>}<div className="account-form-actions"><button type="button" className="account-secondary" onClick={() => setForm(null)}>Hủy</button><button className="account-primary" type="submit">{editingId ? 'Lưu thay đổi' : 'Tạo và gửi email'}</button></div>
    </form>}
    <div className="account-table-wrap"><table className="account-table"><thead><tr><th>Họ và tên</th><th>Email</th><th>Phòng ban</th><th>Vai trò</th><th>Trạng thái</th><th></th></tr></thead><tbody>
      {loading ? <tr><td colSpan="6" className="account-empty">Đang tải tài khoản…</td></tr> : result.data.length ? result.data.map(user => <tr key={user.id}><td className="account-name">{user.name}</td><td>{user.email}</td><td>{user.department || '—'}</td><td>{ROLE_LABELS[user.role] || user.role}</td><td><span className={`account-status ${user.status === 'Đang hoạt động' ? 'active' : user.status === 'Đã khóa' ? 'locked' : 'pending'}`}>{user.status}</span></td><td><button className="account-edit" onClick={() => beginEdit(user)} aria-label={`Sửa ${user.name}`}><Pencil size={16}/></button></td></tr>) : <tr><td colSpan="6" className="account-empty">Không tìm thấy tài khoản phù hợp.</td></tr>}
    </tbody></table></div>
    <div className="account-pagination"><span>{result.total ? `${(page - 1) * 20 + 1}–${Math.min(page * 20, result.total)} trong ${result.total} tài khoản` : '0 tài khoản'}</span><div><button disabled={page <= 1} onClick={() => setPage(page - 1)} aria-label="Trang trước"><ChevronLeft size={18}/></button><span>Trang {page} / {totalPages}</span><button disabled={page >= totalPages} onClick={() => setPage(page + 1)} aria-label="Trang sau"><ChevronRight size={18}/></button></div></div>
  </section>;
}
