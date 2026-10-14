package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.ApprovalMatrix;
import com.irms.model.ApprovalMatrixStep;
import com.irms.model.RequisitionApprovalStep;
import com.irms.util.SecurityUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object quản lý dữ liệu Ma trận phê duyệt và Lưu vết phê duyệt tuyển dụng
 */
public class ApprovalMatrixDAO {

    private static final Logger LOGGER = Logger.getLogger(ApprovalMatrixDAO.class.getName());

    static {
        try {
            ensureSchemaUpToDate();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Không thể tự động khởi tạo schema Ma trận phê duyệt: " + t.getMessage());
        }
    }

    public static void ensureSchemaUpToDate() {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return;
            DatabaseMetaData md = conn.getMetaData();

            // 1. Kiểm tra & tạo bảng approval_matrices
            boolean hasApmTable = false;
            try (ResultSet rs = md.getTables(null, null, "approval_matrices", null)) {
                if (rs.next()) hasApmTable = true;
            }

            if (!hasApmTable) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS approval_matrices (" +
                        "    id VARCHAR(36) PRIMARY KEY," +
                        "    code VARCHAR(50) NOT NULL UNIQUE," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    description TEXT NULL," +
                        "    department_id VARCHAR(36) NULL," +
                        "    salary_condition VARCHAR(50) NOT NULL DEFAULT 'RANGE'," +
                        "    min_salary DECIMAL(15, 2) NULL," +
                        "    max_salary DECIMAL(15, 2) NULL," +
                        "    threshold_amount DECIMAL(15, 2) NULL," +
                        "    priority INT NOT NULL DEFAULT 0," +
                        "    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "    created_by VARCHAR(36) NULL," +
                        "    updated_by VARCHAR(36) NULL," +
                        "    INDEX idx_apm_dept (department_id)," +
                        "    INDEX idx_apm_status (status)," +
                        "    INDEX idx_apm_code (code)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
                    );
                    LOGGER.info("[ApprovalMatrixDAO] Đã tạo bảng approval_matrices");
                }
            }

            // 2. Kiểm tra & tạo bảng approval_matrix_steps
            boolean hasStepsTable = false;
            try (ResultSet rs = md.getTables(null, null, "approval_matrix_steps", null)) {
                if (rs.next()) hasStepsTable = true;
            }

            if (!hasStepsTable) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS approval_matrix_steps (" +
                        "    id VARCHAR(36) PRIMARY KEY," +
                        "    matrix_id VARCHAR(36) NOT NULL," +
                        "    step_order INT NOT NULL," +
                        "    step_name VARCHAR(255) NOT NULL," +
                        "    approver_type VARCHAR(50) NOT NULL," +
                        "    approver_role_id VARCHAR(36) NULL," +
                        "    approver_user_id VARCHAR(36) NULL," +
                        "    is_escalation_step BOOLEAN NOT NULL DEFAULT FALSE," +
                        "    escalation_condition VARCHAR(255) NULL," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    INDEX idx_apms_matrix (matrix_id)," +
                        "    INDEX idx_apms_order (matrix_id, step_order)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
                    );
                    LOGGER.info("[ApprovalMatrixDAO] Đã tạo bảng approval_matrix_steps");
                }
            }

            // 3. Kiểm tra & tạo bảng requisition_approval_steps
            boolean hasReqStepsTable = false;
            try (ResultSet rs = md.getTables(null, null, "requisition_approval_steps", null)) {
                if (rs.next()) hasReqStepsTable = true;
            }

            if (!hasReqStepsTable) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS requisition_approval_steps (" +
                        "    id VARCHAR(36) PRIMARY KEY," +
                        "    requisition_id VARCHAR(36) NOT NULL," +
                        "    matrix_id VARCHAR(36) NULL," +
                        "    step_order INT NOT NULL," +
                        "    step_name VARCHAR(255) NOT NULL," +
                        "    approver_type VARCHAR(50) NOT NULL," +
                        "    approver_role_id VARCHAR(36) NULL," +
                        "    approver_user_id VARCHAR(36) NULL," +
                        "    assigned_approver_name VARCHAR(255) NOT NULL," +
                        "    status VARCHAR(30) NOT NULL DEFAULT 'PENDING'," +
                        "    comment TEXT NULL," +
                        "    acted_by VARCHAR(36) NULL," +
                        "    acted_at TIMESTAMP NULL," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    INDEX idx_ras_req (requisition_id)," +
                        "    INDEX idx_ras_status (requisition_id, status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
                    );
                    LOGGER.info("[ApprovalMatrixDAO] Đã tạo bảng requisition_approval_steps");
                }
            }

            // 4. Bổ sung quyền phân hệ vào permissions & role_permissions
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES " +
                    "('p-apm-view', 'approval_matrix.view', 'Xem ma trận phê duyệt', 'approval_matrix', 'view', 'Xem danh sách và chi tiết các quy tắc ma trận phê duyệt'), " +
                    "('p-apm-manage', 'approval_matrix.manage', 'Quản lý ma trận phê duyệt', 'approval_matrix', 'manage', 'Thêm, sửa, đổi trạng thái và cấu hình các cấp phê duyệt')"
                );

                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-001', id FROM permissions WHERE code LIKE 'approval_matrix.%'"
                );
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-002', id FROM permissions WHERE code LIKE 'approval_matrix.%'"
                );
            }

            // 5. Kiểm tra và thêm dữ liệu seed nếu bảng còn rỗng
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM approval_matrices")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    // Seed ma trận mẫu
                    stmt.executeUpdate(
                        "INSERT INTO approval_matrices (id, code, name, description, department_id, salary_condition, min_salary, max_salary, threshold_amount, priority, status) VALUES " +
                        "('apm-001', 'APM-TECH', 'Ma trận phê duyệt Khối Công nghệ Thông tin', 'Áp dụng cho mọi vị trí công nghệ: Cấp 1 Trưởng bộ phận, tự động bổ sung Cấp 2 Giám đốc Khối khi lương > 35M', 'dept-003', 'RANGE', 0.00, 70000000.00, 35000000.00, 10, 'ACTIVE'), " +
                        "('apm-002', 'APM-SALES', 'Ma trận phê duyệt Khối Kinh doanh & Thị trường', 'Áp dụng cho bộ phận kinh doanh: Cấp 1 Trưởng phòng kinh doanh, tự động bổ sung Cấp 2 Ban Giám đốc khi lương > 30M', 'dept-004', 'RANGE', 0.00, 60000000.00, 30000000.00, 10, 'ACTIVE'), " +
                        "('apm-003', 'APM-GENERAL', 'Ma trận phê duyệt tiêu chuẩn toàn Công ty', 'Quy tắc phê duyệt chung: Cấp 1 Trưởng phòng, Cấp 2 Trưởng phòng Nhân sự, tự động bổ sung Cấp 3 Tổng Giám đốc khi lương > 25M', NULL, 'RANGE', 0.00, 50000000.00, 25000000.00, 1, 'ACTIVE')"
                    );

                    stmt.executeUpdate(
                        "INSERT INTO approval_matrix_steps (id, matrix_id, step_order, step_name, approver_type, approver_role_id, approver_user_id, is_escalation_step, escalation_condition) VALUES " +
                        "('step-001-1', 'apm-001', 1, 'Trưởng bộ phận Kỹ thuật', 'DEPT_MANAGER', NULL, NULL, FALSE, NULL), " +
                        "('step-001-2', 'apm-001', 2, 'Giám đốc Khối CNTT', 'ROLE', 'role-001', NULL, TRUE, 'Tự động bổ sung khi mức lương đề xuất vượt hạn mức 35,000,000 VNĐ'), " +
                        "('step-002-1', 'apm-002', 1, 'Trưởng phòng Kinh doanh', 'DEPT_MANAGER', NULL, NULL, FALSE, NULL), " +
                        "('step-002-2', 'apm-002', 2, 'Ban Giám đốc Điều hành', 'ROLE', 'role-001', NULL, TRUE, 'Tự động bổ sung khi mức lương đề xuất vượt hạn mức 30,000,000 VNĐ'), " +
                        "('step-003-1', 'apm-003', 1, 'Trưởng phòng ban của yêu cầu', 'DEPT_MANAGER', NULL, NULL, FALSE, NULL), " +
                        "('step-003-2', 'apm-003', 2, 'Trưởng phòng Nhân sự', 'ROLE', 'role-002', NULL, FALSE, NULL), " +
                        "('step-003-3', 'apm-003', 3, 'Ban Tổng Giám đốc', 'ROLE', 'role-001', NULL, TRUE, 'Tự động bổ sung khi mức lương đề xuất vượt hạn mức 25,000,000 VNĐ')"
                    );
                    LOGGER.info("[ApprovalMatrixDAO] Đã nạp dữ liệu mẫu ban đầu cho Ma trận phê duyệt");
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[ApprovalMatrixDAO] Lỗi ensureSchema: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<ApprovalMatrix> findAll(String search, String departmentId, String status, int page, int pageSize) {
        List<ApprovalMatrix> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT m.*, d.name AS department_name, d.code AS department_code, " +
            "       u1.full_name AS created_by_name, u2.full_name AS updated_by_name " +
            "FROM approval_matrices m " +
            "LEFT JOIN departments d ON m.department_id = d.id " +
            "LEFT JOIN users u1 ON m.created_by = u1.id " +
            "LEFT JOIN users u2 ON m.updated_by = u2.id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (m.code LIKE ? OR m.name LIKE ? OR m.description LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            if ("GLOBAL".equalsIgnoreCase(departmentId) || "null".equalsIgnoreCase(departmentId)) {
                sql.append("AND m.department_id IS NULL ");
            } else {
                sql.append("AND m.department_id = ? ");
                params.add(departmentId.trim());
            }
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND m.status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY m.priority DESC, m.created_at DESC ");

        if (page > 0 && pageSize > 0) {
            sql.append("LIMIT ? OFFSET ? ");
            params.add(pageSize);
            params.add((page - 1) * pageSize);
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ApprovalMatrix m = mapMatrix(rs);
                    m.setSteps(getStepsByMatrixId(conn, m.getId()));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi findAll ApprovalMatrix: " + e.getMessage(), e);
        }
        return list;
    }

    public int countAll(String search, String departmentId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM approval_matrices m WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (m.code LIKE ? OR m.name LIKE ? OR m.description LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            if ("GLOBAL".equalsIgnoreCase(departmentId) || "null".equalsIgnoreCase(departmentId)) {
                sql.append("AND m.department_id IS NULL ");
            } else {
                sql.append("AND m.department_id = ? ");
                params.add(departmentId.trim());
            }
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND m.status = ? ");
            params.add(status.trim());
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi countAll ApprovalMatrix: " + e.getMessage(), e);
        }
        return 0;
    }

    public ApprovalMatrix findById(String id) {
        String sql = "SELECT m.*, d.name AS department_name, d.code AS department_code, " +
                     "       u1.full_name AS created_by_name, u2.full_name AS updated_by_name " +
                     "FROM approval_matrices m " +
                     "LEFT JOIN departments d ON m.department_id = d.id " +
                     "LEFT JOIN users u1 ON m.created_by = u1.id " +
                     "LEFT JOIN users u2 ON m.updated_by = u2.id " +
                     "WHERE m.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ApprovalMatrix m = mapMatrix(rs);
                    m.setSteps(getStepsByMatrixId(conn, m.getId()));
                    return m;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi findById ApprovalMatrix: " + e.getMessage(), e);
        }
        return null;
    }

    public ApprovalMatrix findByCode(String code) {
        String sql = "SELECT m.*, d.name AS department_name, d.code AS department_code, " +
                     "       u1.full_name AS created_by_name, u2.full_name AS updated_by_name " +
                     "FROM approval_matrices m " +
                     "LEFT JOIN departments d ON m.department_id = d.id " +
                     "LEFT JOIN users u1 ON m.created_by = u1.id " +
                     "LEFT JOIN users u2 ON m.updated_by = u2.id " +
                     "WHERE UPPER(m.code) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ApprovalMatrix m = mapMatrix(rs);
                    m.setSteps(getStepsByMatrixId(conn, m.getId()));
                    return m;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi findByCode ApprovalMatrix: " + e.getMessage(), e);
        }
        return null;
    }

    public List<ApprovalMatrix> findActiveByDepartment(String departmentId) {
        List<ApprovalMatrix> list = new ArrayList<>();
        String sql = "SELECT m.*, d.name AS department_name, d.code AS department_code, " +
                     "       u1.full_name AS created_by_name, u2.full_name AS updated_by_name " +
                     "FROM approval_matrices m " +
                     "LEFT JOIN departments d ON m.department_id = d.id " +
                     "LEFT JOIN users u1 ON m.created_by = u1.id " +
                     "LEFT JOIN users u2 ON m.updated_by = u2.id " +
                     "WHERE m.status = 'ACTIVE' AND (m.department_id = ? OR m.department_id IS NULL) " +
                     "ORDER BY CASE WHEN m.department_id IS NOT NULL THEN 1 ELSE 2 END, m.priority DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ApprovalMatrix m = mapMatrix(rs);
                    m.setSteps(getStepsByMatrixId(conn, m.getId()));
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi findActiveByDepartment: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean insert(ApprovalMatrix matrix) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO approval_matrices (id, code, name, description, department_id, " +
                         "salary_condition, min_salary, max_salary, threshold_amount, priority, status, created_by, updated_by) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, matrix.getId());
                ps.setString(2, matrix.getCode());
                ps.setString(3, matrix.getName());
                ps.setString(4, matrix.getDescription());
                ps.setString(5, matrix.getDepartmentId());
                ps.setString(6, matrix.getSalaryCondition());
                ps.setBigDecimal(7, matrix.getMinSalary());
                ps.setBigDecimal(8, matrix.getMaxSalary());
                ps.setBigDecimal(9, matrix.getThresholdAmount());
                ps.setInt(10, matrix.getPriority());
                ps.setString(11, matrix.getStatus() != null ? matrix.getStatus() : "ACTIVE");
                ps.setString(12, matrix.getCreatedBy());
                ps.setString(13, matrix.getUpdatedBy());
                ps.executeUpdate();
            }

            // Insert steps
            insertSteps(conn, matrix.getId(), matrix.getSteps());

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            LOGGER.log(Level.SEVERE, "Lỗi insert ApprovalMatrix: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean update(ApprovalMatrix matrix) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String sql = "UPDATE approval_matrices SET code = ?, name = ?, description = ?, department_id = ?, " +
                         "salary_condition = ?, min_salary = ?, max_salary = ?, threshold_amount = ?, priority = ?, " +
                         "status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, matrix.getCode());
                ps.setString(2, matrix.getName());
                ps.setString(3, matrix.getDescription());
                ps.setString(4, matrix.getDepartmentId());
                ps.setString(5, matrix.getSalaryCondition());
                ps.setBigDecimal(6, matrix.getMinSalary());
                ps.setBigDecimal(7, matrix.getMaxSalary());
                ps.setBigDecimal(8, matrix.getThresholdAmount());
                ps.setInt(9, matrix.getPriority());
                ps.setString(10, matrix.getStatus());
                ps.setString(11, matrix.getUpdatedBy());
                ps.setString(12, matrix.getId());
                ps.executeUpdate();
            }

            // Delete old steps and re-insert
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM approval_matrix_steps WHERE matrix_id = ?")) {
                ps.setString(1, matrix.getId());
                ps.executeUpdate();
            }

            insertSteps(conn, matrix.getId(), matrix.getSteps());

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            LOGGER.log(Level.SEVERE, "Lỗi update ApprovalMatrix: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean updateStatus(String id, String status, String updatedBy) {
        String sql = "UPDATE approval_matrices SET status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, updatedBy);
            ps.setString(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi updateStatus ApprovalMatrix: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM approval_matrices WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi delete ApprovalMatrix: " + e.getMessage(), e);
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // QUẢN LÝ SNAPSHOT CHUỖI PHÊ DUYỆT CỦA TỪNG YÊU CẦU TUYỂN DỤNG
    // ------------------------------------------------------------------------

    public boolean saveRequisitionSteps(List<RequisitionApprovalStep> steps) {
        if (steps == null || steps.isEmpty()) return true;
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String sql = "INSERT INTO requisition_approval_steps (id, requisition_id, matrix_id, step_order, " +
                         "step_name, approver_type, approver_role_id, approver_user_id, assigned_approver_name, " +
                         "status, comment, acted_by, acted_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (RequisitionApprovalStep s : steps) {
                    if (s.getId() == null || s.getId().trim().isEmpty()) {
                        s.setId(SecurityUtil.generateUUID());
                    }
                    ps.setString(1, s.getId());
                    ps.setString(2, s.getRequisitionId());
                    ps.setString(3, s.getMatrixId());
                    ps.setInt(4, s.getStepOrder());
                    ps.setString(5, s.getStepName());
                    ps.setString(6, s.getApproverType());
                    ps.setString(7, s.getApproverRoleId());
                    ps.setString(8, s.getApproverUserId());
                    ps.setString(9, s.getAssignedApproverName());
                    ps.setString(10, s.getStatus() != null ? s.getStatus() : "PENDING");
                    ps.setString(11, s.getComment());
                    ps.setString(12, s.getActedBy());
                    ps.setTimestamp(13, s.getActedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            LOGGER.log(Level.SEVERE, "Lỗi saveRequisitionSteps: " + e.getMessage(), e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<RequisitionApprovalStep> getRequisitionSteps(String requisitionId) {
        List<RequisitionApprovalStep> list = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name AS acted_by_name " +
                     "FROM requisition_approval_steps s " +
                     "LEFT JOIN users u ON s.acted_by = u.id " +
                     "WHERE s.requisition_id = ? " +
                     "ORDER BY s.step_order ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requisitionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RequisitionApprovalStep s = new RequisitionApprovalStep();
                    s.setId(rs.getString("id"));
                    s.setRequisitionId(rs.getString("requisition_id"));
                    s.setMatrixId(rs.getString("matrix_id"));
                    s.setStepOrder(rs.getInt("step_order"));
                    s.setStepName(rs.getString("step_name"));
                    s.setApproverType(rs.getString("approver_type"));
                    s.setApproverRoleId(rs.getString("approver_role_id"));
                    s.setApproverUserId(rs.getString("approver_user_id"));
                    s.setAssignedApproverName(rs.getString("assigned_approver_name"));
                    s.setStatus(rs.getString("status"));
                    s.setComment(rs.getString("comment"));
                    s.setActedBy(rs.getString("acted_by"));
                    s.setActedByName(rs.getString("acted_by_name"));
                    s.setActedAt(rs.getTimestamp("acted_at"));
                    s.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(s);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi getRequisitionSteps: " + e.getMessage(), e);
        }
        return list;
    }

    public boolean updateRequisitionStepStatus(String stepId, String status, String comment, String actorId) {
        String sql = "UPDATE requisition_approval_steps SET status = ?, comment = ?, acted_by = ?, " +
                     "acted_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, comment);
            ps.setString(3, actorId);
            ps.setString(4, stepId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi updateRequisitionStepStatus: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteRequisitionSteps(String requisitionId) {
        String sql = "DELETE FROM requisition_approval_steps WHERE requisition_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, requisitionId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi deleteRequisitionSteps: " + e.getMessage(), e);
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // HELPER METHODS
    // ------------------------------------------------------------------------

    private void insertSteps(Connection conn, String matrixId, List<ApprovalMatrixStep> steps) throws SQLException {
        if (steps == null || steps.isEmpty()) return;
        String sql = "INSERT INTO approval_matrix_steps (id, matrix_id, step_order, step_name, approver_type, " +
                     "approver_role_id, approver_user_id, is_escalation_step, escalation_condition) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ApprovalMatrixStep s : steps) {
                if (s.getId() == null || s.getId().trim().isEmpty()) {
                    s.setId(SecurityUtil.generateUUID());
                }
                ps.setString(1, s.getId());
                ps.setString(2, matrixId);
                ps.setInt(3, s.getStepOrder());
                ps.setString(4, s.getStepName());
                ps.setString(5, s.getApproverType());
                ps.setString(6, s.getApproverRoleId());
                ps.setString(7, s.getApproverUserId());
                ps.setBoolean(8, s.isEscalationStep());
                ps.setString(9, s.getEscalationCondition());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<ApprovalMatrixStep> getStepsByMatrixId(Connection conn, String matrixId) throws SQLException {
        List<ApprovalMatrixStep> steps = new ArrayList<>();
        String sql = "SELECT s.*, r.name AS approver_role_name, u.full_name AS approver_user_name " +
                     "FROM approval_matrix_steps s " +
                     "LEFT JOIN roles r ON s.approver_role_id = r.id " +
                     "LEFT JOIN users u ON s.approver_user_id = u.id " +
                     "WHERE s.matrix_id = ? " +
                     "ORDER BY s.step_order ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matrixId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ApprovalMatrixStep s = new ApprovalMatrixStep();
                    s.setId(rs.getString("id"));
                    s.setMatrixId(rs.getString("matrix_id"));
                    s.setStepOrder(rs.getInt("step_order"));
                    s.setStepName(rs.getString("step_name"));
                    s.setApproverType(rs.getString("approver_type"));
                    s.setApproverRoleId(rs.getString("approver_role_id"));
                    s.setApproverRoleName(rs.getString("approver_role_name"));
                    s.setApproverUserId(rs.getString("approver_user_id"));
                    s.setApproverUserName(rs.getString("approver_user_name"));
                    s.setEscalationStep(rs.getBoolean("is_escalation_step"));
                    s.setEscalationCondition(rs.getString("escalation_condition"));
                    s.setCreatedAt(rs.getTimestamp("created_at"));
                    steps.add(s);
                }
            }
        }
        return steps;
    }

    private ApprovalMatrix mapMatrix(ResultSet rs) throws SQLException {
        ApprovalMatrix m = new ApprovalMatrix();
        m.setId(rs.getString("id"));
        m.setCode(rs.getString("code"));
        m.setName(rs.getString("name"));
        m.setDescription(rs.getString("description"));
        m.setDepartmentId(rs.getString("department_id"));
        m.setDepartmentName(rs.getString("department_name"));
        m.setDepartmentCode(rs.getString("department_code"));
        m.setSalaryCondition(rs.getString("salary_condition"));
        m.setMinSalary(rs.getBigDecimal("min_salary"));
        m.setMaxSalary(rs.getBigDecimal("max_salary"));
        m.setThresholdAmount(rs.getBigDecimal("threshold_amount"));
        m.setPriority(rs.getInt("priority"));
        m.setStatus(rs.getString("status"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        m.setUpdatedAt(rs.getTimestamp("updated_at"));
        m.setCreatedBy(rs.getString("created_by"));
        m.setCreatedByName(rs.getString("created_by_name"));
        m.setUpdatedBy(rs.getString("updated_by"));
        m.setUpdatedByName(rs.getString("updated_by_name"));
        return m;
    }
}
