package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.BudgetSubtask;
import com.irms.model.DepartmentHeadcountBudget;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Data Access Object quản lý Định biên & Ngân sách Nhân sự theo phòng ban/năm
 * và danh mục Subtasks theo dõi tiến độ (KN-195 -> KN-201)
 */
public class HeadcountBudgetDAO extends BaseDAO {

    static {
        try {
            ensureSchemaUpToDate();
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger(HeadcountBudgetDAO.class.getName())
                    .log(Level.WARNING, "Lỗi kiểm tra schema định biên & ngân sách: " + t.getMessage());
        }
    }

    private static void ensureSchemaUpToDate() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return;
            DatabaseMetaData md = conn.getMetaData();
            stmt = conn.createStatement();

            // 1. Kiểm tra bảng department_headcount_budgets
            try (ResultSet rs = md.getTables(null, null, "department_headcount_budgets", null)) {
                if (!rs.next()) {
                    String createBudgetSql = "CREATE TABLE department_headcount_budgets (" +
                            "id VARCHAR(36) PRIMARY KEY, " +
                            "department_id VARCHAR(36) NOT NULL, " +
                            "fiscal_year INT NOT NULL, " +
                            "target_headcount INT NOT NULL DEFAULT 0, " +
                            "total_salary_budget DECIMAL(15, 2) NOT NULL DEFAULT 0.00, " +
                            "currency VARCHAR(10) NOT NULL DEFAULT 'VND', " +
                            "description TEXT NULL, " +
                            "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', " +
                            "created_by VARCHAR(36) NULL, " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "updated_by VARCHAR(36) NULL, " +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                            "UNIQUE KEY uk_dept_fiscal_year (department_id, fiscal_year)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                    stmt.executeUpdate(createBudgetSql);
                }
            }

            // 2. Kiểm tra bảng budget_subtasks
            try (ResultSet rs = md.getTables(null, null, "budget_subtasks", null)) {
                if (!rs.next()) {
                    String createSubtaskSql = "CREATE TABLE budget_subtasks (" +
                            "id VARCHAR(36) PRIMARY KEY, " +
                            "code VARCHAR(20) NOT NULL UNIQUE, " +
                            "title VARCHAR(255) NOT NULL, " +
                            "priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', " +
                            "assignee_id VARCHAR(36) NULL, " +
                            "assignee_name VARCHAR(100) NULL, " +
                            "status VARCHAR(20) NOT NULL DEFAULT 'TODO', " +
                            "module_type VARCHAR(10) NOT NULL DEFAULT 'BE', " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
                    stmt.executeUpdate(createSubtaskSql);
                }
            }

            // 3. Kiểm tra các cột override trong recruitment_requisitions
            try (ResultSet rs = md.getColumns(null, null, "recruitment_requisitions", "is_budget_overridden")) {
                if (!rs.next()) {
                    stmt.executeUpdate("ALTER TABLE recruitment_requisitions " +
                            "ADD COLUMN is_budget_overridden BOOLEAN NOT NULL DEFAULT FALSE, " +
                            "ADD COLUMN budget_override_reason TEXT NULL, " +
                            "ADD COLUMN budget_overridden_by VARCHAR(36) NULL, " +
                            "ADD COLUMN budget_overridden_at TIMESTAMP NULL");
                }
            }

            // 4. Khởi tạo quyền hạn Phân hệ Định biên & Ngân sách nếu chưa có
            try {
                stmt.executeUpdate("INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES " +
                        "('p-037', 'budget.view', 'Xem định biên & ngân sách', 'budget', 'view', 'Xem danh sách và báo cáo định biên headcount và ngân sách nhân sự theo phòng ban'), " +
                        "('p-038', 'budget.manage', 'Khai báo định biên & ngân sách', 'budget', 'manage', 'Khai báo và cập nhật chỉ tiêu định biên và ngân sách lương theo phòng ban'), " +
                        "('p-039', 'budget.override', 'Xác nhận ghi đè vượt chỉ tiêu', 'budget', 'override', 'Xác nhận ghi đè khi tạo yêu cầu tuyển dụng vượt chỉ tiêu định biên hoặc ngân sách (Dành cho Trưởng phòng Nhân sự)')");

                // Gán quyền cho ADMIN (role-001) và HR_MANAGER (role-002)
                stmt.executeUpdate("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                        "SELECT 'role-001', id FROM permissions WHERE code IN ('budget.view', 'budget.manage', 'budget.override')");
                stmt.executeUpdate("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                        "SELECT 'role-002', id FROM permissions WHERE code IN ('budget.view', 'budget.manage', 'budget.override')");
                stmt.executeUpdate("INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                        "SELECT 'role-004', id FROM permissions WHERE code IN ('budget.view')");
            } catch (Exception ignored) {}

            // 5. Khởi tạo Subtasks (KN-195 -> KN-201) theo đúng hình ảnh tham chiếu nếu bảng chưa có dữ liệu
            try (ResultSet rsSub = stmt.executeQuery("SELECT COUNT(*) FROM budget_subtasks")) {
                if (rsSub.next() && rsSub.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO budget_subtasks (id, code, title, priority, assignee_id, assignee_name, status, module_type) VALUES " +
                            "('st-195', 'KN-195', '[BE] Tính toán headcount còn lại và kiểm tra vượt chỉ tiêu khi tạo yêu cầu tuyển dụng', 'MEDIUM', NULL, 'U', 'TODO', 'BE'), " +
                            "('st-196', 'KN-196', '[BE] API tra cứu ngân sách còn lại theo phòng ban để phục vụ màn hình requisition', 'MEDIUM', NULL, 'U', 'TODO', 'BE'), " +
                            "('st-197', 'KN-197', '[BE] API khai báo/cập nhật định biên và ngân sách nhân sự theo phòng ban', 'MEDIUM', NULL, 'U', 'TODO', 'BE'), " +
                            "('st-198', 'KN-198', '[BE] Thiết kế mô hình lưu chỉ tiêu headcount và ngân sách lương theo phòng ban/năm', 'MEDIUM', NULL, 'U', 'TODO', 'BE'), " +
                            "('st-199', 'KN-199', '[BE] Cơ chế xác nhận ghi đè khi vượt chỉ tiêu bởi Trưởng phòng Nhân sự', 'MEDIUM', NULL, 'U', 'TODO', 'BE'), " +
                            "('st-200', 'KN-200', '[FE] Màn hình khai báo định biên và ngân sách nhân sự theo phòng ban', 'MEDIUM', NULL, 'U', 'TODO', 'BE'), " +
                            "('st-201', 'KN-201', '[FE] Hiển thị cảnh báo vượt chỉ tiêu và luồng xác nhận ghi đè trên form yêu cầu tuyển dụng', 'MEDIUM', NULL, 'U', 'TODO', 'FE')");
                }
            } catch (Exception ignored) {}

            // 6. Khởi tạo Định biên mẫu năm 2026 nếu bảng chưa có dữ liệu
            try (ResultSet rsB = stmt.executeQuery("SELECT COUNT(*) FROM department_headcount_budgets")) {
                if (rsB.next() && rsB.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO department_headcount_budgets (id, department_id, fiscal_year, target_headcount, total_salary_budget, currency, description, status, created_by) VALUES " +
                            "('dhb-tech-2026', 'dept-003', 2026, 15, 650000000.00, 'VND', 'Định biên Khối Công nghệ Thông tin năm tài chính 2026', 'ACTIVE', 'usr-001'), " +
                            "('dhb-hr-2026', 'dept-002', 2026, 8, 250000000.00, 'VND', 'Định biên Ban Nhân sự & Tuyển dụng năm 2026', 'ACTIVE', 'usr-001'), " +
                            "('dhb-sales-2026', 'dept-004', 2026, 20, 500000000.00, 'VND', 'Định biên Khối Kinh doanh & Thị trường năm 2026', 'ACTIVE', 'usr-001'), " +
                            "('dhb-mkt-2026', 'dept-005', 2026, 6, 180000000.00, 'VND', 'Định biên Phòng Truyền thông & Marketing năm 2026', 'ACTIVE', 'usr-001'), " +
                            "('dhb-bgd-2026', 'dept-001', 2026, 5, 300000000.00, 'VND', 'Định biên Ban Giám đốc năm 2026', 'ACTIVE', 'usr-001')");
                }
            } catch (Exception ignored) {}
        } catch (Throwable e) {
            java.util.logging.Logger.getLogger(HeadcountBudgetDAO.class.getName())
                    .log(Level.INFO, "[HeadcountBudgetDAO] Schema check completed: " + e.getMessage());
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException ignored) {} }
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
    }

    /**
     * Tra cứu danh sách định biên theo bộ lọc năm, phòng ban, từ khóa
     */
    public List<DepartmentHeadcountBudget> getBudgets(Integer year, String departmentId, String keyword) {
        List<DepartmentHeadcountBudget> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT b.*, d.name AS department_name, d.code AS department_code, " +
                "uc.full_name AS created_by_name, uu.full_name AS updated_by_name " +
                "FROM department_headcount_budgets b " +
                "JOIN departments d ON b.department_id = d.id " +
                "LEFT JOIN users uc ON b.created_by = uc.id " +
                "LEFT JOIN users uu ON b.updated_by = uu.id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (year != null && year > 0) {
            sql.append("AND b.fiscal_year = ? ");
            params.add(year);
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            sql.append("AND b.department_id = ? ");
            params.add(departmentId.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (d.name LIKE ? OR d.code LIKE ? OR b.description LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        sql.append("ORDER BY b.fiscal_year DESC, d.name ASC");

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
                DepartmentHeadcountBudget item = mapResultSetToBudget(rs);
                populateRealtimeUsage(conn, item, null);
                list.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách định biên & ngân sách", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy định biên theo ID
     */
    public DepartmentHeadcountBudget getBudgetById(String id) {
        String sql = "SELECT b.*, d.name AS department_name, d.code AS department_code, " +
                "uc.full_name AS created_by_name, uu.full_name AS updated_by_name " +
                "FROM department_headcount_budgets b " +
                "JOIN departments d ON b.department_id = d.id " +
                "LEFT JOIN users uc ON b.created_by = uc.id " +
                "LEFT JOIN users uu ON b.updated_by = uu.id " +
                "WHERE b.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                DepartmentHeadcountBudget item = mapResultSetToBudget(rs);
                populateRealtimeUsage(conn, item, null);
                return item;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm định biên theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Lấy định biên theo phòng ban và năm tài chính
     */
    public DepartmentHeadcountBudget getBudgetByDeptAndYear(String departmentId, int year, String excludeRequisitionId) {
        String sql = "SELECT b.*, d.name AS department_name, d.code AS department_code, " +
                "uc.full_name AS created_by_name, uu.full_name AS updated_by_name " +
                "FROM department_headcount_budgets b " +
                "JOIN departments d ON b.department_id = d.id " +
                "LEFT JOIN users uc ON b.created_by = uc.id " +
                "LEFT JOIN users uu ON b.updated_by = uu.id " +
                "WHERE b.department_id = ? AND b.fiscal_year = ? AND b.status = 'ACTIVE'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, departmentId);
            ps.setInt(2, year);
            rs = ps.executeQuery();
            if (rs.next()) {
                DepartmentHeadcountBudget item = mapResultSetToBudget(rs);
                populateRealtimeUsage(conn, item, excludeRequisitionId);
                return item;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm định biên theo phòng ban " + departmentId + " và năm " + year, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Kiểm tra trùng lặp bản ghi định biên cho cùng phòng ban và năm
     */
    public boolean existsByDeptAndYear(String departmentId, int year, String excludeId) {
        String sql = "SELECT COUNT(*) FROM department_headcount_budgets " +
                     "WHERE department_id = ? AND fiscal_year = ? AND (? IS NULL OR id != ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, departmentId);
            ps.setInt(2, year);
            ps.setString(3, excludeId);
            ps.setString(4, excludeId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra trùng định biên", e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * Thêm mới định biên & ngân sách
     */
    public boolean insert(DepartmentHeadcountBudget budget) {
        String sql = "INSERT INTO department_headcount_budgets " +
                     "(id, department_id, fiscal_year, target_headcount, total_salary_budget, currency, " +
                     "description, status, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, budget.getId());
            ps.setString(2, budget.getDepartmentId());
            ps.setInt(3, budget.getFiscalYear());
            ps.setInt(4, budget.getTargetHeadcount());
            ps.setBigDecimal(5, budget.getTotalSalaryBudget());
            ps.setString(6, budget.getCurrency() != null ? budget.getCurrency() : "VND");
            ps.setString(7, budget.getDescription());
            ps.setString(8, budget.getStatus() != null ? budget.getStatus() : "ACTIVE");
            ps.setString(9, budget.getCreatedBy());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm định biên & ngân sách", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật định biên & ngân sách
     */
    public boolean update(DepartmentHeadcountBudget budget) {
        String sql = "UPDATE department_headcount_budgets SET " +
                     "target_headcount = ?, total_salary_budget = ?, currency = ?, " +
                     "description = ?, status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, budget.getTargetHeadcount());
            ps.setBigDecimal(2, budget.getTotalSalaryBudget());
            ps.setString(3, budget.getCurrency() != null ? budget.getCurrency() : "VND");
            ps.setString(4, budget.getDescription());
            ps.setString(5, budget.getStatus());
            ps.setString(6, budget.getUpdatedBy());
            ps.setString(7, budget.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật định biên & ngân sách: " + budget.getId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Xóa định biên
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM department_headcount_budgets WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa định biên: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Tính toán số liệu thực tế thời gian thực (Headcount và Ngân sách đã dùng / còn lại)
     */
    private void populateRealtimeUsage(Connection conn, DepartmentHeadcountBudget budget, String excludeRequisitionId) {
        if (budget == null || budget.getDepartmentId() == null) return;

        // 1. Số nhân sự chính thức đang làm việc trong phòng ban
        int activeEmployees = 0;
        String countEmpSql = "SELECT COUNT(*) FROM users WHERE department_id = ? AND status = 'ACTIVE'";
        try (PreparedStatement ps = conn.prepareStatement(countEmpSql)) {
            ps.setString(1, budget.getDepartmentId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) activeEmployees = rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi đếm nhân sự phòng ban: " + budget.getDepartmentId(), e);
        }

        // 2. Số headcount đang chiếm dụng bởi các yêu cầu tuyển dụng trong năm (OPEN, PENDING_APPROVAL)
        int occupiedReqHeadcount = 0;
        BigDecimal committedBudget = BigDecimal.ZERO;
        String reqUsageSql = "SELECT COALESCE(SUM(headcount), 0) AS total_headcount, " +
                "COALESCE(SUM(headcount * COALESCE(max_salary, min_salary, 0)), 0) AS committed_budget " +
                "FROM recruitment_requisitions " +
                "WHERE department_id = ? " +
                "AND status IN ('OPEN', 'PENDING_APPROVAL') " +
                "AND (YEAR(created_at) = ? OR YEAR(deadline) = ?) " +
                "AND (? IS NULL OR id != ?)";

        try (PreparedStatement ps = conn.prepareStatement(reqUsageSql)) {
            ps.setString(1, budget.getDepartmentId());
            ps.setInt(2, budget.getFiscalYear());
            ps.setInt(3, budget.getFiscalYear());
            ps.setString(4, excludeRequisitionId);
            ps.setString(5, excludeRequisitionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    occupiedReqHeadcount = rs.getInt("total_headcount");
                    committedBudget = rs.getBigDecimal("committed_budget");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi tính chiếm dụng tuyển dụng phòng ban: " + budget.getDepartmentId(), e);
        }

        int totalUsedHeadcount = activeEmployees + occupiedReqHeadcount;
        int remainingHeadcount = budget.getTargetHeadcount() - totalUsedHeadcount;
        BigDecimal remainingBudget = budget.getTotalSalaryBudget().subtract(committedBudget != null ? committedBudget : BigDecimal.ZERO);

        budget.setUsedHeadcount(totalUsedHeadcount);
        budget.setRemainingHeadcount(remainingHeadcount);
        budget.setCommittedSalaryBudget(committedBudget);
        budget.setRemainingSalaryBudget(remainingBudget);
    }

    /**
     * Tổng hợp KPI toàn công ty cho năm tài chính
     */
    public Map<String, Object> getCompanyKpiSummary(int fiscalYear) {
        Map<String, Object> kpis = new HashMap<>();
        List<DepartmentHeadcountBudget> budgets = getBudgets(fiscalYear, null, null);

        int totalTargetHeadcount = 0;
        int totalUsedHeadcount = 0;
        int totalRemainingHeadcount = 0;
        BigDecimal totalApprovedBudget = BigDecimal.ZERO;
        BigDecimal totalCommittedBudget = BigDecimal.ZERO;
        BigDecimal totalRemainingBudget = BigDecimal.ZERO;

        for (DepartmentHeadcountBudget b : budgets) {
            totalTargetHeadcount += b.getTargetHeadcount();
            totalUsedHeadcount += b.getUsedHeadcount();
            totalRemainingHeadcount += b.getRemainingHeadcount();
            totalApprovedBudget = totalApprovedBudget.add(b.getTotalSalaryBudget());
            totalCommittedBudget = totalCommittedBudget.add(b.getCommittedSalaryBudget());
            totalRemainingBudget = totalRemainingBudget.add(b.getRemainingSalaryBudget());
        }

        kpis.put("fiscalYear", fiscalYear);
        kpis.put("totalDepartmentsWithBudget", budgets.size());
        kpis.put("totalTargetHeadcount", totalTargetHeadcount);
        kpis.put("totalUsedHeadcount", totalUsedHeadcount);
        kpis.put("totalRemainingHeadcount", totalRemainingHeadcount);
        kpis.put("totalApprovedBudget", totalApprovedBudget);
        kpis.put("totalCommittedBudget", totalCommittedBudget);
        kpis.put("totalRemainingBudget", totalRemainingBudget);

        double hcPercent = (totalTargetHeadcount > 0)
                ? ((double) totalUsedHeadcount / totalTargetHeadcount) * 100.0 : 0.0;
        kpis.put("headcountUsagePercent", Math.min(100.0, Math.round(hcPercent * 10.0) / 10.0));

        double bgPercent = (totalApprovedBudget.compareTo(BigDecimal.ZERO) > 0)
                ? (totalCommittedBudget.divide(totalApprovedBudget, 4, BigDecimal.ROUND_HALF_UP).doubleValue() * 100.0) : 0.0;
        kpis.put("budgetUsagePercent", Math.min(100.0, Math.round(bgPercent * 10.0) / 10.0));

        return kpis;
    }

    // =========================================================================
    // SUBTASKS QUẢN LÝ (KN-195 -> KN-201)
    // =========================================================================

    public List<BudgetSubtask> getAllSubtasks() {
        List<BudgetSubtask> list = new ArrayList<>();
        String sql = "SELECT * FROM budget_subtasks ORDER BY code ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToSubtask(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách Subtasks", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public BudgetSubtask getSubtaskById(String id) {
        String sql = "SELECT * FROM budget_subtasks WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToSubtask(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy Subtask theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean updateSubtaskStatus(String id, String status) {
        String sql = "UPDATE budget_subtasks SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái Subtask: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean updateSubtask(BudgetSubtask subtask) {
        String sql = "UPDATE budget_subtasks SET priority = ?, assignee_id = ?, assignee_name = ?, " +
                     "status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, subtask.getPriority());
            ps.setString(2, subtask.getAssigneeId());
            ps.setString(3, subtask.getAssigneeName());
            ps.setString(4, subtask.getStatus());
            ps.setString(5, subtask.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật Subtask: " + subtask.getId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    private DepartmentHeadcountBudget mapResultSetToBudget(ResultSet rs) throws SQLException {
        DepartmentHeadcountBudget b = new DepartmentHeadcountBudget();
        b.setId(rs.getString("id"));
        b.setDepartmentId(rs.getString("department_id"));
        b.setDepartmentName(rs.getString("department_name"));
        b.setDepartmentCode(rs.getString("department_code"));
        b.setFiscalYear(rs.getInt("fiscal_year"));
        b.setTargetHeadcount(rs.getInt("target_headcount"));
        b.setTotalSalaryBudget(rs.getBigDecimal("total_salary_budget"));
        b.setCurrency(rs.getString("currency"));
        b.setDescription(rs.getString("description"));
        b.setStatus(rs.getString("status"));
        b.setCreatedBy(rs.getString("created_by"));
        b.setCreatedByName(rs.getString("created_by_name"));
        b.setCreatedAt(rs.getTimestamp("created_at"));
        b.setUpdatedBy(rs.getString("updated_by"));
        b.setUpdatedByName(rs.getString("updated_by_name"));
        b.setUpdatedAt(rs.getTimestamp("updated_at"));
        return b;
    }

    private BudgetSubtask mapResultSetToSubtask(ResultSet rs) throws SQLException {
        BudgetSubtask s = new BudgetSubtask();
        s.setId(rs.getString("id"));
        s.setCode(rs.getString("code"));
        s.setTitle(rs.getString("title"));
        s.setPriority(rs.getString("priority"));
        s.setAssigneeId(rs.getString("assignee_id"));
        s.setAssigneeName(rs.getString("assignee_name"));
        s.setStatus(rs.getString("status"));
        s.setModuleType(rs.getString("module_type"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setUpdatedAt(rs.getTimestamp("updated_at"));
        return s;
    }
}
