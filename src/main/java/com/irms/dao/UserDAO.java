package com.irms.dao;

import com.irms.model.Role;
import com.irms.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn dữ liệu Người dùng (bảng users)
 */
public class UserDAO extends BaseDAO {

    public User findByEmail(String email) {
        String sql = "SELECT u.*, d.name AS department_name FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.email = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserRolesAndPermissions(conn, user);
                return user;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm người dùng theo email: " + email, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public User findByEmployeeCode(String employeeCode) {
        if (employeeCode == null || employeeCode.trim().isEmpty()) return null;
        String sql = "SELECT u.*, d.name AS department_name FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.employee_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, employeeCode.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserRolesAndPermissions(conn, user);
                return user;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm người dùng theo mã nhân viên: " + employeeCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public java.util.Set<String> getAllEmployeeCodes() {
        java.util.Set<String> set = new java.util.HashSet<>();
        String sql = "SELECT employee_code FROM users WHERE employee_code IS NOT NULL";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                String code = rs.getString(1);
                if (code != null && !code.trim().isEmpty()) {
                    set.add(code.trim().toUpperCase());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách mã nhân viên", e);
        } finally {
            close(conn, ps, rs);
        }
        return set;
    }

    public java.util.Set<String> getAllEmails() {
        java.util.Set<String> set = new java.util.HashSet<>();
        String sql = "SELECT email FROM users WHERE email IS NOT NULL";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                String email = rs.getString(1);
                if (email != null && !email.trim().isEmpty()) {
                    set.add(email.trim().toLowerCase());
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách email", e);
        } finally {
            close(conn, ps, rs);
        }
        return set;
    }

    public User findById(String id) {
        String sql = "SELECT u.*, d.name AS department_name FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                User user = mapResultSetToUser(rs);
                loadUserRolesAndPermissions(conn, user);
                return user;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm người dùng theo id: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public List<User> findAll(String search, String deptId, String status, int offset, int limit) {
        return findAll(search, deptId, status, null, offset, limit);
    }

    /**
     * [US 8]: Tìm kiếm và lọc người dùng:
     * - Tìm theo tên, email, mã nhân viên
     * - Lọc theo phòng ban
     * - Lọc theo vai trò (roleId)
     * - Lọc theo trạng thái
     */
    public List<User> findAll(String search, String deptId, String status, String roleId, int offset, int limit) {
        List<User> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT u.*, d.name AS department_name FROM users u " +
                "LEFT JOIN departments d ON u.department_id = d.id ");

        if (roleId != null && !roleId.trim().isEmpty()) {
            sql.append("INNER JOIN user_roles ur_filter ON u.id = ur_filter.user_id AND ur_filter.role_id = ? ");
        }
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (roleId != null && !roleId.trim().isEmpty()) {
            params.add(roleId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (u.full_name LIKE ? OR u.email LIKE ? OR u.employee_code LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (deptId != null && !deptId.trim().isEmpty()) {
            sql.append("AND u.department_id = ? ");
            params.add(deptId.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND u.status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY u.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                User u = mapResultSetToUser(rs);
                loadUserRolesAndPermissions(conn, u);
                list.add(u);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách người dùng", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countAll(String search, String deptId, String status) {
        return countAll(search, deptId, status, null);
    }

    /**
     * [US 8]: Đếm tổng số người dùng có lọc theo vai trò
     */
    public int countAll(String search, String deptId, String status, String roleId) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT u.id) FROM users u ");
        if (roleId != null && !roleId.trim().isEmpty()) {
            sql.append("INNER JOIN user_roles ur_filter ON u.id = ur_filter.user_id AND ur_filter.role_id = ? ");
        }
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (roleId != null && !roleId.trim().isEmpty()) {
            params.add(roleId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (u.full_name LIKE ? OR u.email LIKE ? OR u.employee_code LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (deptId != null && !deptId.trim().isEmpty()) {
            sql.append("AND u.department_id = ? ");
            params.add(deptId.trim());
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND u.status = ? ");
            params.add(status.trim());
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số lượng người dùng", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public boolean insert(User user, String roleId) {
        List<String> roleIds = new ArrayList<>();
        if (roleId != null && !roleId.trim().isEmpty()) {
            roleIds.add(roleId.trim());
        }
        return insert(user, roleIds);
    }

    /**
     * [US 8 & US 9]: Thêm mới người dùng hỗ trợ gán nhiều vai trò cùng lúc
     */
    public boolean insert(User user, List<String> roleIds) {
        String sqlUser = "INSERT INTO users (id, employee_code, full_name, email, phone, job_title, " +
                "department_id, password_hash, status, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlRole = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psUser = null;
        PreparedStatement psRole = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psUser = conn.prepareStatement(sqlUser);
            psUser.setString(1, user.getId());
            psUser.setString(2, user.getEmployeeCode());
            psUser.setString(3, user.getFullName());
            psUser.setString(4, user.getEmail());
            psUser.setString(5, user.getPhone());
            psUser.setString(6, user.getJobTitle());
            psUser.setString(7, user.getDepartmentId());
            psUser.setString(8, user.getPasswordHash());
            psUser.setString(9, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            psUser.setBoolean(10, user.isMustChangePassword());
            psUser.executeUpdate();

            if (roleIds != null && !roleIds.isEmpty()) {
                psRole = conn.prepareStatement(sqlRole);
                for (String rId : roleIds) {
                    if (rId != null && !rId.trim().isEmpty()) {
                        psRole.setString(1, user.getId());
                        psRole.setString(2, rId.trim());
                        psRole.addBatch();
                    }
                }
                psRole.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.log(Level.SEVERE, "Rollback failed", ex); }
            }
            logger.log(Level.SEVERE, "Lỗi thêm mới người dùng", e);
            return false;
        } finally {
            close(null, psRole);
            close(conn, psUser);
        }
    }

    public boolean update(User user, String roleId) {
        List<String> roleIds = new ArrayList<>();
        if (roleId != null && !roleId.trim().isEmpty()) {
            roleIds.add(roleId.trim());
        }
        return update(user, roleIds);
    }

    /**
     * [US 9]: Cập nhật thông tin tài khoản và gán nhiều vai trò cùng lúc
     * - Một người dùng có thể giữ nhiều vai trò cùng lúc (Ví dụ: Trưởng bộ phận kiêm Người phỏng vấn).
     * - Cập nhật tức thì vào bảng user_roles.
     */
    public boolean update(User user, List<String> roleIds) {
        String sqlUser = "UPDATE users SET full_name = ?, phone = ?, job_title = ?, department_id = ?, status = ? WHERE id = ?";
        String sqlDelRole = "DELETE FROM user_roles WHERE user_id = ?";
        String sqlAddRole = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psUser = null;
        PreparedStatement psDelRole = null;
        PreparedStatement psAddRole = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psUser = conn.prepareStatement(sqlUser);
            psUser.setString(1, user.getFullName());
            psUser.setString(2, user.getPhone());
            psUser.setString(3, user.getJobTitle());
            psUser.setString(4, user.getDepartmentId());
            psUser.setString(5, user.getStatus());
            psUser.setString(6, user.getId());
            psUser.executeUpdate();

            if (roleIds != null) {
                psDelRole = conn.prepareStatement(sqlDelRole);
                psDelRole.setString(1, user.getId());
                psDelRole.executeUpdate();

                if (!roleIds.isEmpty()) {
                    psAddRole = conn.prepareStatement(sqlAddRole);
                    for (String rId : roleIds) {
                        if (rId != null && !rId.trim().isEmpty()) {
                            psAddRole.setString(1, user.getId());
                            psAddRole.setString(2, rId.trim());
                            psAddRole.addBatch();
                        }
                    }
                    psAddRole.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.log(Level.SEVERE, "Rollback failed", ex); }
            }
            logger.log(Level.SEVERE, "Lỗi cập nhật người dùng", e);
            return false;
        } finally {
            close(null, psDelRole);
            close(null, psAddRole);
            close(conn, psUser);
        }
    }

    /**
     * [US 2, US 4, US 9, US 10]: Lấy trạng thái phiên và quyền mới nhất của người dùng
     * - Dùng trong AuthFilter để kiểm tra:
     *   + Tài khoản có bị khóa không (status == 'LOCKED') -> Thu hồi phiên ngay lập tức (US 10).
     *   + Phiên có bị hủy do đổi mật khẩu không (session_version khác nhau) (US 4).
     *   + Cập nhật vai trò có hiệu lực ngay ở thao tác kế tiếp (US 9).
     */
    public User findSessionStateById(String userId) {
        String sql = "SELECT id, status, session_version, must_change_password FROM users WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                User u = new User();
                u.setId(rs.getString("id"));
                u.setStatus(rs.getString("status"));
                u.setSessionVersion(rs.getInt("session_version"));
                u.setMustChangePassword(rs.getBoolean("must_change_password"));
                loadUserRolesAndPermissions(conn, u);
                return u;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra session state cho userId: " + userId, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [US 10]: Đếm số lượng vị trí tuyển dụng (Requisitions) đang mở mà nhân sự này phụ trách
     * - Dùng để cảnh báo cần bàn giao trước khi khóa tài khoản.
     */
    public int countActiveRequisitionsByRecruiter(String userId) {
        String sql = "SELECT COUNT(*) FROM recruitment_requisitions WHERE recruiter_id = ? AND status = 'OPEN'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số vị trí tuyển dụng phụ trách", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * [US 4]: Cập nhật mật khẩu và tăng session_version
     * - session_version = session_version + 1 nhằm mục đích thu hồi các phiên đăng nhập cũ trên thiết bị khác.
     */
    public boolean updatePassword(String userId, String passwordHash, boolean mustChangePassword) {
        String sql = "UPDATE users SET password_hash = ?, must_change_password = ?, session_version = session_version + 1 WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, passwordHash);
            ps.setBoolean(2, mustChangePassword);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật mật khẩu cho userId: " + userId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [US 10]: Khóa tài khoản nhân sự
     * - Cập nhật status = 'LOCKED', ghi nhận thời gian, lý do và admin thực hiện.
     * - Tăng session_version để chuẩn bị cho việc thu hồi phiên đang mở.
     */
    public boolean lockUser(String userId, String reason, String lockedBy) {
        String sql = "UPDATE users SET status = 'LOCKED', locked_at = NOW(), lock_reason = ?, locked_by = ?, " +
                     "session_version = session_version + 1 WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, reason);
            ps.setString(2, lockedBy);
            ps.setString(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khóa tài khoản userId: " + userId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean unlockUser(String userId) {
        String sql = "UPDATE users SET status = 'ACTIVE', failed_login_attempts = 0, locked_until = NULL, " +
                     "locked_at = NULL, lock_reason = NULL, locked_by = NULL WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi mở khóa tài khoản userId: " + userId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public void incrementFailedAttempts(String email, int maxAttempts, int lockoutMinutes) {
        String sql = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, " +
                     "locked_until = CASE WHEN failed_login_attempts + 1 >= ? THEN DATE_ADD(NOW(), INTERVAL ? MINUTE) ELSE locked_until END, " +
                     "status = CASE WHEN failed_login_attempts + 1 >= ? THEN 'LOCKED' ELSE status END " +
                     "WHERE email = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, maxAttempts);
            ps.setInt(2, lockoutMinutes);
            ps.setInt(3, maxAttempts);
            ps.setString(4, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tăng failed_login_attempts cho email: " + email, e);
        } finally {
            close(conn, ps);
        }
    }

    public void resetFailedAttempts(String email) {
        String sql = "UPDATE users SET failed_login_attempts = 0, locked_until = NULL, last_login_at = NOW() WHERE email = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi reset failed_login_attempts", e);
        } finally {
            close(conn, ps);
        }
    }

    private void loadUserRolesAndPermissions(Connection conn, User user) throws SQLException {
        // Load roles
        String sqlRoles = "SELECT r.* FROM roles r " +
                          "INNER JOIN user_roles ur ON r.id = ur.role_id " +
                          "WHERE ur.user_id = ?";
        try (PreparedStatement psRoles = conn.prepareStatement(sqlRoles)) {
            psRoles.setString(1, user.getId());
            try (ResultSet rsRoles = psRoles.executeQuery()) {
                List<Role> roles = new ArrayList<>();
                while (rsRoles.next()) {
                    roles.add(new Role(
                            rsRoles.getString("id"),
                            rsRoles.getString("code"),
                            rsRoles.getString("name"),
                            rsRoles.getString("description"),
                            rsRoles.getBoolean("is_system_role")
                    ));
                }
                user.setRoles(roles);
            }
        }

        // Load distinct permissions
        String sqlPerms = "SELECT DISTINCT p.code FROM permissions p " +
                          "INNER JOIN role_permissions rp ON p.id = rp.permission_id " +
                          "INNER JOIN user_roles ur ON rp.role_id = ur.role_id " +
                          "WHERE ur.user_id = ?";
        try (PreparedStatement psPerms = conn.prepareStatement(sqlPerms)) {
            psPerms.setString(1, user.getId());
            try (ResultSet rsPerms = psPerms.executeQuery()) {
                List<String> perms = new ArrayList<>();
                while (rsPerms.next()) {
                    perms.add(rsPerms.getString("code"));
                }
                user.setPermissions(perms);
            }
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setEmployeeCode(rs.getString("employee_code"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setJobTitle(rs.getString("job_title"));
        u.setDepartmentId(rs.getString("department_id"));
        u.setDepartmentName(rs.getString("department_name"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setStatus(rs.getString("status"));
        u.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        u.setLockedUntil(rs.getTimestamp("locked_until"));
        u.setLockedAt(rs.getTimestamp("locked_at"));
        u.setLockReason(rs.getString("lock_reason"));
        u.setLockedBy(rs.getString("locked_by"));
        u.setMustChangePassword(rs.getBoolean("must_change_password"));
        u.setSessionVersion(rs.getInt("session_version"));
        u.setLastLoginAt(rs.getTimestamp("last_login_at"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }
}
