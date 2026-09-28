const bcrypt = require('bcryptjs');
const fs = require('fs');
const path = require('path');

// Mock User Database with initial seed data
const users = [
  {
    id: 'usr_001',
    name: 'Hoàng Tiến Anh',
    email: 'hoang.ta@company.com',
    // Password: 'TempPassword123' hashed
    password: bcrypt.hashSync('TempPassword123', 10),
    role: 'Nhân sự nội bộ',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    tokenVersion: 1,
    activeSessions: []
  },
  {
    id: 'usr_002',
    name: 'Sìn Văn Cương',
    email: 'cuong.sv@company.com',
    // Password: 'TempPassword123' hashed
    password: bcrypt.hashSync('TempPassword123', 10),
    role: 'Quản trị viên',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
    tokenVersion: 1,
    activeSessions: []
  }
];

const managedUsersPath = path.join(__dirname, '..', 'data', 'managed-users.json');
try {
  const savedUsers = JSON.parse(fs.readFileSync(managedUsersPath, 'utf8'));
  if (Array.isArray(savedUsers)) users.push(...savedUsers.filter(saved => !users.some(user => user.id === saved.id)));
} catch (error) {
  if (error.code !== 'ENOENT') console.error('Could not load managed users:', error.message);
}

function persistManagedUsers() {
  const seededIds = new Set(['usr_001', 'usr_002']);
  const managedUsers = users.filter(user => !seededIds.has(user.id)).map(user => ({
    ...user,
    activeSessions: []
  }));
  fs.mkdirSync(path.dirname(managedUsersPath), { recursive: true });
  fs.writeFileSync(managedUsersPath, JSON.stringify(managedUsers, null, 2), { mode: 0o600 });
}

class UserModel {
  static findByEmail(email) {
    return users.find(u => u.email.toLowerCase() === email.toLowerCase());
  }

  static findById(id) {
    return users.find(u => u.id === id);
  }

  static list({ query = '', role = '', status = '', page = 1, pageSize = 20 } = {}) {
    const normalized = query.trim().toLowerCase();
    const filtered = users.filter(user => {
      const matchesQuery = !normalized || [user.name, user.email, user.department || '']
        .some(value => value.toLowerCase().includes(normalized));
      const accountRole = user.role === 'Quản trị viên' ? 'ADMIN' : user.role === 'Nhân sự nội bộ' ? 'HR' : user.role;
      return matchesQuery && (!role || accountRole === role) && (!status || (user.status || 'Đang hoạt động') === status);
    });
    const start = (page - 1) * pageSize;
    return { total: filtered.length, data: filtered.slice(start, start + pageSize).map(this.toPublic) };
  }

  static toPublic(user) {
    const { password, ...safeUser } = user;
    const accountRole = user.role === 'Quản trị viên' ? 'ADMIN' : user.role === 'Nhân sự nội bộ' ? 'HR' : user.role;
    return { ...safeUser, role: accountRole, department: user.department || '', status: user.status || 'Đang hoạt động' };
  }

  static create({ name, email, department, role, password }) {
    if (this.findByEmail(email)) return null;
    const user = {
      id: 'usr_' + Date.now() + '_' + Math.random().toString(36).slice(2, 7),
      name: name.trim(), email: email.trim().toLowerCase(), department: department.trim(), role,
      status: 'Đang hoạt động', password, avatar: '', tokenVersion: 1, activeSessions: [],
      createdAt: new Date().toISOString()
    };
    users.push(user);
    persistManagedUsers();
    return this.toPublic(user);
  }

  static update(id, { name, email, department, role, status }) {
    const user = this.findById(id);
    if (!user) return null;
    const duplicate = this.findByEmail(email);
    if (duplicate && duplicate.id !== id) return false;
    user.name = name.trim(); user.email = email.trim().toLowerCase();
    user.department = department.trim(); user.role = role; user.status = status;
    persistManagedUsers();
    return this.toPublic(user);
  }

  static updatePassword(userId, newHashedPassword, revokeOthers = true, currentSessionId = null) {
    const user = this.findById(userId);
    if (!user) return null;

    user.password = newHashedPassword;

    if (revokeOthers) {
      // Increment token version to invalidate all previous JWT tokens
      user.tokenVersion += 1;
      
      // If currentSessionId provided, keep only current session
      if (currentSessionId) {
        user.activeSessions = user.activeSessions.filter(s => s.sessionId === currentSessionId);
      } else {
        user.activeSessions = [];
      }
    }

    return user;
  }

  static addSession(userId, sessionData) {
    const user = this.findById(userId);
    if (!user) return;
    user.activeSessions.push(sessionData);
  }

  static isTokenValid(userId, tokenVersion, sessionId = null) {
    const user = this.findById(userId);
    if (!user) return false;
    
    // Check if tokenVersion matches current user tokenVersion
    if (user.tokenVersion !== tokenVersion || (user.status && user.status !== 'Đang hoạt động')) {
      return false;
    }

    return true;
  }
}

module.exports = UserModel;
