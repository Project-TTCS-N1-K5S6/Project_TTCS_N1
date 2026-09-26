const bcrypt = require('bcryptjs');

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

class UserModel {
  static findByEmail(email) {
    return users.find(u => u.email.toLowerCase() === email.toLowerCase());
  }

  static findById(id) {
    return users.find(u => u.id === id);
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
    if (user.tokenVersion !== tokenVersion) {
      return false;
    }

    return true;
  }
}

module.exports = UserModel;
