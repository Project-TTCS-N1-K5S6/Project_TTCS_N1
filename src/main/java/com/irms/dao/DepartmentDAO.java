package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.Department;
import com.irms.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thao tác truy vấn và quản lý Phòng ban (bảng departments)
 * Hỗ trợ cấu trúc cây phân cấp nhiều cấp, người phụ trách và kiểm tra ràng buộc tuyển dụng
 */
public class DepartmentDAO extends BaseDAO {

    private static final Logger LOGGER = Logger.getLogger(DepartmentDAO.class.getName());

    static {
        try {
            ensureSchemaUpToDate();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Không thể tự động cập nhật schema phòng ban: " + t.getMessage());
        }
    }

    /**
     * Tự động nâng cấp schema bảng departments nếu thiếu cột/index (Self-healing Schema)
     */
    public static void ensureSchemaUpToDate() {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return;
            DatabaseMetaData md = conn.getMetaData();

            // 1. Kiểm tra và bổ sung cột parent_id
            boolean hasParent = false;
            try (ResultSet rs = md.getColumns(null, null, "departments", "parent_id")) {
                if (rs.next()) hasParent = true;
            }
            if (!hasParent) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE departments ADD COLUMN parent_id VARCHAR(36) NULL AFTER description");
                    LOGGER.info("[DepartmentDAO] Đã thêm cột parent_id vào bảng departments");
                }
            }

            // 2. Kiểm tra và bổ sung cột manager_id
            boolean hasManager = false;
            try (ResultSet rs = md.getColumns(null, null, "departments", "manager_id")) {
                if (rs.next()) hasManager = true;
            }
            if (!hasManager) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE departments ADD COLUMN manager_id VARCHAR(36) NULL AFTER parent_id");
                    LOGGER.info("[DepartmentDAO] Đã thêm cột manager_id vào bảng departments");
                }
            }

            // 3. Kiểm tra và bổ sung cột status
            boolean hasStatus = false;
            try (ResultSet rs = md.getColumns(null, null, "departments", "status")) {
                if (rs.next()) hasStatus = true;
            }
            if (!hasStatus) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE departments ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER manager_id");
                    LOGGER.info("[DepartmentDAO] Đã thêm cột status vào bảng departments");
                }
            }

            // 4. Kiểm tra và bổ sung cột created_by & updated_by
            boolean hasCreatedBy = false;
            try (ResultSet rs = md.getColumns(null, null, "departments", "created_by")) {
                if (rs.next()) hasCreatedBy = true;
            }
            if (!hasCreatedBy) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE departments ADD COLUMN created_by VARCHAR(36) NULL AFTER updated_at");
                    stmt.executeUpdate("ALTER TABLE departments ADD COLUMN updated_by VARCHAR(36) NULL AFTER created_by");
                    LOGGER.info("[DepartmentDAO] Đã thêm cột created_by, updated_by vào bảng departments");
                }
            }

            // 5. Thêm quyền vào permissions & role_permissions nếu chưa có
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES " +
                    "('p-dept-view', 'departments.view', 'Xem cơ cấu phòng ban', 'departments', 'view', 'Xem danh sách và sơ đồ cây cơ cấu phòng ban'), " +
                    "('p-dept-create', 'departments.create', 'Tạo mới phòng ban', 'departments', 'create', 'Thêm mới phòng ban và đơn vị trực thuộc'), " +
                    "('p-dept-update', 'departments.update', 'Chỉnh sửa phòng ban', 'departments', 'update', 'Cập nhật thông tin và cấp bậc phòng ban'), " +
                    "('p-dept-delete', 'departments.delete', 'Xóa phòng ban', 'departments', 'delete', 'Xóa phòng ban khi không có ràng buộc yêu cầu tuyển dụng mở hoặc con'), " +
                    "('p-dept-status', 'departments.status', 'Đổi trạng thái phòng ban', 'departments', 'status', 'Kích hoạt hoặc ngừng áp dụng phòng ban')"
                );

                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-001', id FROM permissions WHERE code LIKE 'departments.%'"
                );
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-002', id FROM permissions WHERE code LIKE 'departments.%'"
                );
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DepartmentDAO] Không thể đảm bảo schema departments: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Lấy toàn bộ danh sách phòng ban kèm thông tin cha, người phụ trách và các chỉ số thống kê
     */
    public List<Department> findAll() {
        return findAll(null, null);
    }

    public List<Department> findAll(String search, String status) {
        List<Department> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT d.*, " +
            "       p.code AS parent_code, p.name AS parent_name, " +
            "       u.full_name AS manager_name, u.email AS manager_email, u.job_title AS manager_job_title, u.avatar_url AS manager_avatar_url, " +
            "       (SELECT COUNT(*) FROM users usr WHERE usr.department_id = d.id) AS user_count, " +
            "       (SELECT COUNT(*) FROM departments ch WHERE ch.parent_id = d.id) AS children_count, " +
            "       (SELECT COUNT(*) FROM recruitment_requisitions req WHERE req.department_id = d.id AND req.status IN ('OPEN', 'PENDING_APPROVAL')) AS open_req_count, " +
            "       (SELECT COUNT(*) FROM recruitment_requisitions req_all WHERE req_all.department_id = d.id) AS total_req_count " +
            "FROM departments d " +
            "LEFT JOIN departments p ON d.parent_id = p.id " +
            "LEFT JOIN users u ON d.manager_id = u.id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (d.name LIKE ? OR d.code LIKE ? OR d.description LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND d.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY d.parent_id IS NULL DESC, d.code ASC, d.name ASC");

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
                list.add(mapResultSetToDepartment(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách phòng ban", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Tìm phòng ban theo ID kèm đầy đủ thông tin chi tiết
     */
    public Department findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = 
            "SELECT d.*, " +
            "       p.code AS parent_code, p.name AS parent_name, " +
            "       u.full_name AS manager_name, u.email AS manager_email, u.job_title AS manager_job_title, u.avatar_url AS manager_avatar_url, " +
            "       (SELECT COUNT(*) FROM users usr WHERE usr.department_id = d.id) AS user_count, " +
            "       (SELECT COUNT(*) FROM departments ch WHERE ch.parent_id = d.id) AS children_count, " +
            "       (SELECT COUNT(*) FROM recruitment_requisitions req WHERE req.department_id = d.id AND req.status IN ('OPEN', 'PENDING_APPROVAL')) AS open_req_count, " +
            "       (SELECT COUNT(*) FROM recruitment_requisitions req_all WHERE req_all.department_id = d.id) AS total_req_count " +
            "FROM departments d " +
            "LEFT JOIN departments p ON d.parent_id = p.id " +
            "LEFT JOIN users u ON d.manager_id = u.id " +
            "WHERE d.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToDepartment(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm phòng ban theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Tìm phòng ban theo Mã (Code)
     */
    public Department findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String sql = "SELECT * FROM departments WHERE code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code.trim().toUpperCase());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToDepartment(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm phòng ban theo mã: " + code, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Kiểm tra mã phòng ban đã tồn tại hay chưa
     */
    public boolean existsByCode(String code, String excludeId) {
        if (code == null || code.trim().isEmpty()) return false;
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM departments WHERE UPPER(code) = ?");
        if (excludeId != null && !excludeId.trim().isEmpty()) {
            sql.append(" AND id != ?");
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setString(1, code.trim().toUpperCase());
            if (excludeId != null && !excludeId.trim().isEmpty()) {
                ps.setString(2, excludeId.trim());
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra trùng mã phòng ban: " + code, e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * Thêm mới phòng ban
     */
    public boolean insert(Department dept) {
        String sql = "INSERT INTO departments (id, code, name, description, parent_id, manager_id, status, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, dept.getId());
            ps.setString(2, dept.getCode().trim().toUpperCase());
            ps.setString(3, dept.getName().trim());
            ps.setString(4, dept.getDescription());
            ps.setString(5, (dept.getParentId() != null && !dept.getParentId().trim().isEmpty()) ? dept.getParentId().trim() : null);
            ps.setString(6, (dept.getManagerId() != null && !dept.getManagerId().trim().isEmpty()) ? dept.getManagerId().trim() : null);
            ps.setString(7, dept.getStatus() != null ? dept.getStatus() : Department.STATUS_ACTIVE);
            ps.setString(8, dept.getCreatedBy());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm mới phòng ban: " + dept.getCode(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật thông tin phòng ban
     */
    public boolean update(Department dept) {
        String sql = "UPDATE departments SET code = ?, name = ?, description = ?, parent_id = ?, manager_id = ?, status = ?, updated_by = ? " +
                     "WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, dept.getCode().trim().toUpperCase());
            ps.setString(2, dept.getName().trim());
            ps.setString(3, dept.getDescription());
            ps.setString(4, (dept.getParentId() != null && !dept.getParentId().trim().isEmpty()) ? dept.getParentId().trim() : null);
            ps.setString(5, (dept.getManagerId() != null && !dept.getManagerId().trim().isEmpty()) ? dept.getManagerId().trim() : null);
            ps.setString(6, dept.getStatus() != null ? dept.getStatus() : Department.STATUS_ACTIVE);
            ps.setString(7, dept.getUpdatedBy());
            ps.setString(8, dept.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật phòng ban: " + dept.getId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật trạng thái phòng ban (Kích hoạt / Ngừng áp dụng)
     */
    public boolean updateStatus(String id, String status, String updatedBy) {
        String sql = "UPDATE departments SET status = ?, updated_by = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, updatedBy);
            ps.setString(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái phòng ban: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Xóa phòng ban
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM departments WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa phòng ban: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Đếm số lượng yêu cầu tuyển dụng đang mở của phòng ban (OPEN, PENDING_APPROVAL)
     */
    public int countOpenRecruitmentRequests(String deptId) {
        if (deptId == null || deptId.trim().isEmpty()) return 0;
        String sql = "SELECT COUNT(*) FROM recruitment_requisitions WHERE department_id = ? AND status IN ('OPEN', 'PENDING_APPROVAL')";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, deptId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm yêu cầu tuyển dụng mở của phòng ban " + deptId + ": " + e.getMessage());
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Đếm tổng số lượng yêu cầu tuyển dụng của phòng ban
     */
    public int countTotalRecruitmentRequests(String deptId) {
        if (deptId == null || deptId.trim().isEmpty()) return 0;
        String sql = "SELECT COUNT(*) FROM recruitment_requisitions WHERE department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, deptId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm tổng số yêu cầu tuyển dụng của phòng ban " + deptId + ": " + e.getMessage());
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Đếm số lượng phòng ban con trực thuộc
     */
    public int countChildren(String deptId) {
        if (deptId == null || deptId.trim().isEmpty()) return 0;
        String sql = "SELECT COUNT(*) FROM departments WHERE parent_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, deptId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm phòng ban con của phòng ban " + deptId, e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Đếm số lượng nhân sự trực thuộc phòng ban
     */
    public int countUsers(String deptId) {
        if (deptId == null || deptId.trim().isEmpty()) return 0;
        String sql = "SELECT COUNT(*) FROM users WHERE department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, deptId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm nhân sự của phòng ban " + deptId, e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Đếm số lượng dải lương chức danh thuộc phòng ban
     */
    public int countSalaryRanges(String deptId) {
        if (deptId == null || deptId.trim().isEmpty()) return 0;
        String sql = "SELECT COUNT(*) FROM salary_ranges WHERE department_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, deptId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm dải lương chức danh của phòng ban: " + e.getMessage());
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Lấy danh sách nhân sự tiềm năng có thể làm người phụ trách (Active Users)
     */
    public List<User> findPotentialManagers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, employee_code, full_name, email, phone, job_title, avatar_url " +
                     "FROM users WHERE status = 'ACTIVE' ORDER BY full_name ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                User u = new User();
                u.setId(rs.getString("id"));
                u.setEmployeeCode(rs.getString("employee_code"));
                u.setFullName(rs.getString("full_name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                u.setJobTitle(rs.getString("job_title"));
                u.setAvatarUrl(rs.getString("avatar_url"));
                list.add(u);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách người phụ trách tiềm năng", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    private Department mapResultSetToDepartment(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setId(rs.getString("id"));
        d.setCode(rs.getString("code"));
        d.setName(rs.getString("name"));
        d.setDescription(rs.getString("description"));

        try { d.setParentId(rs.getString("parent_id")); } catch (SQLException ignored) {}
        try { d.setParentCode(rs.getString("parent_code")); } catch (SQLException ignored) {}
        try { d.setParentName(rs.getString("parent_name")); } catch (SQLException ignored) {}

        try { d.setManagerId(rs.getString("manager_id")); } catch (SQLException ignored) {}
        try { d.setManagerName(rs.getString("manager_name")); } catch (SQLException ignored) {}
        try { d.setManagerEmail(rs.getString("manager_email")); } catch (SQLException ignored) {}
        try { d.setManagerJobTitle(rs.getString("manager_job_title")); } catch (SQLException ignored) {}
        try { d.setManagerAvatarUrl(rs.getString("manager_avatar_url")); } catch (SQLException ignored) {}

        try {
            String st = rs.getString("status");
            d.setStatus(st != null ? st : Department.STATUS_ACTIVE);
        } catch (SQLException ignored) {
            d.setStatus(Department.STATUS_ACTIVE);
        }

        d.setCreatedAt(rs.getTimestamp("created_at"));
        d.setUpdatedAt(rs.getTimestamp("updated_at"));

        try { d.setCreatedBy(rs.getString("created_by")); } catch (SQLException ignored) {}
        try { d.setUpdatedBy(rs.getString("updated_by")); } catch (SQLException ignored) {}

        try { d.setUserCount(rs.getInt("user_count")); } catch (SQLException ignored) {}
        try { d.setChildrenCount(rs.getInt("children_count")); } catch (SQLException ignored) {}
        try { d.setOpenRequisitionCount(rs.getInt("open_req_count")); } catch (SQLException ignored) {}
        try { d.setTotalRequisitionCount(rs.getInt("total_req_count")); } catch (SQLException ignored) {}

        return d;
    }
}
