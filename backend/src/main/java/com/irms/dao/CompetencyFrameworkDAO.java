package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.CompetencyFramework;
import com.irms.model.CompetencyFrameworkCriterion;
import com.irms.model.PositionCompetencyFramework;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Thao tác dữ liệu bảng Khung năng lực & quan hệ Tiêu chí, Chức danh
 * (competency_frameworks, competency_framework_criteria, position_competency_frameworks)
 */
public class CompetencyFrameworkDAO extends BaseDAO {

    static {
        try {
            ensureSchemaUpToDate();
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger(CompetencyFrameworkDAO.class.getName())
                    .log(Level.WARNING, "Không thể tự động cập nhật schema Khung năng lực: " + t.getMessage());
        }
    }

    public static void ensureSchemaUpToDate() {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return;
            DatabaseMetaData md = conn.getMetaData();

            // 1. Kiểm tra & tạo bảng competency_frameworks
            boolean hasCfTable = false;
            try (ResultSet rs = md.getTables(null, null, "competency_frameworks", null)) {
                if (rs.next()) hasCfTable = true;
            }

            if (!hasCfTable) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS competency_frameworks (" +
                        "    id VARCHAR(36) PRIMARY KEY," +
                        "    code VARCHAR(50) NOT NULL UNIQUE," +
                        "    name VARCHAR(255) NOT NULL," +
                        "    description TEXT," +
                        "    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "    created_by VARCHAR(36) NULL," +
                        "    updated_by VARCHAR(36) NULL," +
                        "    INDEX idx_cf_code (code)," +
                        "    INDEX idx_cf_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
                    );
                }
            }

            // 2. Kiểm tra & tạo bảng competency_framework_criteria
            boolean hasCfcTable = false;
            try (ResultSet rs = md.getTables(null, null, "competency_framework_criteria", null)) {
                if (rs.next()) hasCfcTable = true;
            }

            if (!hasCfcTable) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS competency_framework_criteria (" +
                        "    id VARCHAR(36) PRIMARY KEY," +
                        "    framework_id VARCHAR(36) NOT NULL," +
                        "    criterion_id VARCHAR(36) NOT NULL," +
                        "    weight DECIMAL(5, 2) NOT NULL," +
                        "    display_order INT NOT NULL DEFAULT 1," +
                        "    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    INDEX idx_cfc_framework (framework_id)," +
                        "    INDEX idx_cfc_criterion (criterion_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
                    );
                }
            }

            // 3. Kiểm tra & tạo bảng position_competency_frameworks
            boolean hasPcfTable = false;
            try (ResultSet rs = md.getTables(null, null, "position_competency_frameworks", null)) {
                if (rs.next()) hasPcfTable = true;
            }

            if (!hasPcfTable) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS position_competency_frameworks (" +
                        "    id VARCHAR(36) PRIMARY KEY," +
                        "    position_id VARCHAR(36) NOT NULL UNIQUE," +
                        "    framework_id VARCHAR(36) NOT NULL," +
                        "    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "    assigned_by VARCHAR(36) NULL," +
                        "    INDEX idx_pcf_framework (framework_id)," +
                        "    INDEX idx_pcf_position (position_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;"
                    );
                }
            }

            // 4. Bổ sung các quyền hạn mới vào bảng permissions nếu chưa có
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES " +
                    "('p-031', 'competencies.view', 'Xem khung năng lực', 'competencies', 'view', 'Tra cứu danh sách, chi tiết và chức danh sử dụng khung năng lực')," +
                    "('p-032', 'competencies.create', 'Tạo khung năng lực', 'competencies', 'create', 'Tạo mới bộ khung năng lực đánh giá nhân sự')," +
                    "('p-033', 'competencies.update', 'Chỉnh sửa khung năng lực', 'competencies', 'update', 'Cập nhật nội dung tiêu chí và cơ cấu trọng số khung năng lực')," +
                    "('p-034', 'competencies.status', 'Kích hoạt / Ngừng áp dụng', 'competencies', 'status', 'Kích hoạt hoặc ngừng áp dụng khung năng lực')," +
                    "('p-035', 'competencies.assign', 'Gán khung cho chức danh', 'competencies', 'assign', 'Thiết lập khung năng lực áp dụng cho từng chức danh vị trí');"
                );

                // Gán quyền cho ADMIN và HR_MANAGER
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-001', id FROM permissions WHERE module = 'competencies';"
                );
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-002', id FROM permissions WHERE module = 'competencies';"
                );
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-003', id FROM permissions WHERE code = 'competencies.view';"
                );
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-004', id FROM permissions WHERE code = 'competencies.view';"
                );
                stmt.executeUpdate(
                    "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT 'role-005', id FROM permissions WHERE code = 'competencies.view';"
                );
            }

            // 5. Kiểm tra nếu chưa có khung mẫu nào thì seed mẫu
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM competency_frameworks")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate(
                        "INSERT INTO competency_frameworks (id, code, name, description, status, created_by) VALUES " +
                        "('cf-001', 'KNL-DEV-SR', 'Khung năng lực Kỹ sư Backend Senior', 'Bộ tiêu chí đánh giá kỹ năng chuyên môn và phối hợp dành cho lập trình viên Backend cấp cao', 'ACTIVE', 'usr-001'), " +
                        "('cf-002', 'KNL-SALES-B2B', 'Khung năng lực Chuyên viên Kinh doanh B2B', 'Bộ tiêu chí đánh giá năng lực đàm phán, giao tiếp và kỹ năng giải quyết vấn đề khách hàng', 'ACTIVE', 'usr-001'), " +
                        "('cf-003', 'KNL-DEV-JR', 'Khung năng lực Kỹ sư Java Backend Junior', 'Bộ tiêu chí dành cho các lập trình viên Java mới ra trường hoặc dưới 1 năm kinh nghiệm', 'DRAFT', 'usr-002');"
                    );

                    stmt.executeUpdate(
                        "INSERT INTO competency_framework_criteria (id, framework_id, criterion_id, weight, display_order) VALUES " +
                        "('cfc-001', 'cf-001', 'crit-001', 30.00, 1), " +
                        "('cfc-002', 'cf-001', 'crit-002', 25.00, 2), " +
                        "('cfc-003', 'cf-001', 'crit-004', 25.00, 3), " +
                        "('cfc-004', 'cf-001', 'crit-003', 20.00, 4), " +
                        "('cfc-005', 'cf-002', 'crit-007', 35.00, 1), " +
                        "('cfc-006', 'cf-002', 'crit-005', 25.00, 2), " +
                        "('cfc-007', 'cf-002', 'crit-004', 20.00, 3), " +
                        "('cfc-008', 'cf-002', 'crit-003', 20.00, 4), " +
                        "('cfc-009', 'cf-003', 'crit-001', 40.00, 1), " +
                        "('cfc-010', 'cf-003', 'crit-002', 30.00, 2), " +
                        "('cfc-011', 'cf-003', 'crit-008', 30.00, 3);"
                    );

                    // Gán mẫu cho sal-002 và sal-005
                    stmt.executeUpdate(
                        "INSERT IGNORE INTO position_competency_frameworks (id, position_id, framework_id, assigned_by) VALUES " +
                        "('pcf-001', 'sal-002', 'cf-001', 'usr-001'), " +
                        "('pcf-002', 'sal-005', 'cf-002', 'usr-001');"
                    );
                }
            }

        } catch (Throwable e) {
            java.util.logging.Logger.getLogger(CompetencyFrameworkDAO.class.getName())
                    .log(Level.INFO, "[CompetencyFrameworkDAO] Schema sẵn sàng: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Tra cứu danh sách khung năng lực với bộ lọc tìm kiếm và trạng thái
     */
    public List<CompetencyFramework> findAll(String search, String status) {
        List<CompetencyFramework> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT cf.*, " +
            "       u1.full_name AS created_by_name, " +
            "       u2.full_name AS updated_by_name, " +
            "       COALESCE(crit_stats.crit_count, 0) AS criteria_count, " +
            "       COALESCE(crit_stats.total_weight, 0) AS total_weight, " +
            "       COALESCE(pos_stats.pos_count, 0) AS assigned_positions_count " +
            "FROM competency_frameworks cf " +
            "LEFT JOIN users u1 ON cf.created_by = u1.id " +
            "LEFT JOIN users u2 ON cf.updated_by = u2.id " +
            "LEFT JOIN (" +
            "    SELECT framework_id, COUNT(1) AS crit_count, SUM(weight) AS total_weight " +
            "    FROM competency_framework_criteria GROUP BY framework_id" +
            ") crit_stats ON cf.id = crit_stats.framework_id " +
            "LEFT JOIN (" +
            "    SELECT framework_id, COUNT(1) AS pos_count " +
            "    FROM position_competency_frameworks GROUP BY framework_id" +
            ") pos_stats ON cf.id = pos_stats.framework_id " +
            "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(cf.code) LIKE ? OR LOWER(cf.name) LIKE ? OR LOWER(cf.description) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND cf.status = ? ");
            params.add(status.trim().toUpperCase());
        }

        sql.append("ORDER BY cf.created_at DESC");

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
                CompetencyFramework cf = new CompetencyFramework();
                cf.setId(rs.getString("id"));
                cf.setCode(rs.getString("code"));
                cf.setName(rs.getString("name"));
                cf.setDescription(rs.getString("description"));
                cf.setStatus(rs.getString("status"));
                cf.setCreatedAt(rs.getTimestamp("created_at"));
                cf.setUpdatedAt(rs.getTimestamp("updated_at"));
                cf.setCreatedBy(rs.getString("created_by"));
                cf.setCreatedByName(rs.getString("created_by_name"));
                cf.setUpdatedBy(rs.getString("updated_by"));
                cf.setUpdatedByName(rs.getString("updated_by_name"));
                cf.setCriteriaCount(rs.getInt("criteria_count"));
                cf.setTotalWeight(rs.getBigDecimal("total_weight"));
                cf.setAssignedPositionsCount(rs.getInt("assigned_positions_count"));

                list.add(cf);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách khung năng lực", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Tra cứu chi tiết khung năng lực theo ID, nạp đầy đủ danh sách tiêu chí và chức danh sử dụng
     */
    public CompetencyFramework findById(String id) {
        String sql = 
            "SELECT cf.*, " +
            "       u1.full_name AS created_by_name, " +
            "       u2.full_name AS updated_by_name " +
            "FROM competency_frameworks cf " +
            "LEFT JOIN users u1 ON cf.created_by = u1.id " +
            "LEFT JOIN users u2 ON cf.updated_by = u2.id " +
            "WHERE cf.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                CompetencyFramework cf = new CompetencyFramework();
                cf.setId(rs.getString("id"));
                cf.setCode(rs.getString("code"));
                cf.setName(rs.getString("name"));
                cf.setDescription(rs.getString("description"));
                cf.setStatus(rs.getString("status"));
                cf.setCreatedAt(rs.getTimestamp("created_at"));
                cf.setUpdatedAt(rs.getTimestamp("updated_at"));
                cf.setCreatedBy(rs.getString("created_by"));
                cf.setCreatedByName(rs.getString("created_by_name"));
                cf.setUpdatedBy(rs.getString("updated_by"));
                cf.setUpdatedByName(rs.getString("updated_by_name"));

                // Nạp danh sách tiêu chí
                cf.setCriteria(findCriteriaByFrameworkId(id));

                // Nạp danh sách chức danh đang áp dụng
                cf.setAssignedPositions(findAssignedPositionsByFrameworkId(id));

                return cf;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm khung năng lực theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Tra cứu khung năng lực theo Code
     */
    public CompetencyFramework findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String sql = "SELECT id FROM competency_frameworks WHERE LOWER(code) = LOWER(?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return findById(rs.getString("id"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm khung năng lực theo code: " + code, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Kiểm tra trùng mã khung năng lực
     */
    public boolean existsByCode(String code, String excludeId) {
        if (code == null || code.trim().isEmpty()) return false;
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM competency_frameworks WHERE LOWER(code) = ?");
        if (excludeId != null && !excludeId.trim().isEmpty()) {
            sql.append(" AND id != ?");
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setString(1, code.trim().toLowerCase());
            if (excludeId != null && !excludeId.trim().isEmpty()) {
                ps.setString(2, excludeId.trim());
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra trùng mã khung năng lực", e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * Lấy danh sách tiêu chí của 1 khung năng lực kèm thông tin chi tiết tiêu chí
     */
    public List<CompetencyFrameworkCriterion> findCriteriaByFrameworkId(String frameworkId) {
        List<CompetencyFrameworkCriterion> list = new ArrayList<>();
        String sql = 
            "SELECT cfc.*, " +
            "       c.code AS criterion_code, " +
            "       c.name AS criterion_name, " +
            "       c.description AS criterion_description, " +
            "       c.evaluation_guideline " +
            "FROM competency_framework_criteria cfc " +
            "JOIN competency_criteria c ON cfc.criterion_id = c.id " +
            "WHERE cfc.framework_id = ? " +
            "ORDER BY cfc.display_order ASC, cfc.created_at ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, frameworkId);
            rs = ps.executeQuery();
            while (rs.next()) {
                CompetencyFrameworkCriterion item = new CompetencyFrameworkCriterion();
                item.setId(rs.getString("id"));
                item.setFrameworkId(rs.getString("framework_id"));
                item.setCriterionId(rs.getString("criterion_id"));
                item.setCriterionCode(rs.getString("criterion_code"));
                item.setCriterionName(rs.getString("criterion_name"));
                item.setCriterionDescription(rs.getString("criterion_description"));
                item.setEvaluationGuideline(rs.getString("evaluation_guideline"));
                item.setWeight(rs.getBigDecimal("weight"));
                item.setDisplayOrder(rs.getInt("display_order"));
                item.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy tiêu chí theo frameworkId: " + frameworkId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy danh sách các chức danh đang áp dụng khung năng lực này
     */
    public List<PositionCompetencyFramework> findAssignedPositionsByFrameworkId(String frameworkId) {
        List<PositionCompetencyFramework> list = new ArrayList<>();
        String sql = 
            "SELECT pcf.*, " +
            "       sr.position_code, sr.position_title, sr.level, sr.department_id, " +
            "       d.name AS department_name, " +
            "       u.full_name AS assigned_by_name " +
            "FROM position_competency_frameworks pcf " +
            "JOIN salary_ranges sr ON pcf.position_id = sr.id " +
            "LEFT JOIN departments d ON sr.department_id = d.id " +
            "LEFT JOIN users u ON pcf.assigned_by = u.id " +
            "WHERE pcf.framework_id = ? " +
            "ORDER BY sr.position_title ASC, sr.position_code ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, frameworkId);
            rs = ps.executeQuery();
            while (rs.next()) {
                PositionCompetencyFramework item = new PositionCompetencyFramework();
                item.setId(rs.getString("id"));
                item.setPositionId(rs.getString("position_id"));
                item.setFrameworkId(rs.getString("framework_id"));
                item.setPositionCode(rs.getString("position_code"));
                item.setPositionTitle(rs.getString("position_title"));
                item.setLevel(rs.getString("level"));
                item.setDepartmentId(rs.getString("department_id"));
                item.setDepartmentName(rs.getString("department_name"));
                item.setAssignedAt(rs.getTimestamp("assigned_at"));
                item.setAssignedBy(rs.getString("assigned_by"));
                item.setAssignedByName(rs.getString("assigned_by_name"));
                list.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy chức danh theo frameworkId: " + frameworkId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy toàn bộ danh sách chức danh từ bảng salary_ranges cùng thông tin khung năng lực đang gắn (nếu có)
     */
    public List<PositionCompetencyFramework> findAllPositionsWithFrameworkInfo() {
        List<PositionCompetencyFramework> list = new ArrayList<>();
        String sql = 
            "SELECT sr.id AS position_id, sr.position_code, sr.position_title, sr.level, sr.department_id, " +
            "       d.name AS department_name, " +
            "       pcf.id AS pcf_id, pcf.framework_id, pcf.assigned_at, " +
            "       cf.code AS framework_code, cf.name AS framework_name " +
            "FROM salary_ranges sr " +
            "LEFT JOIN departments d ON sr.department_id = d.id " +
            "LEFT JOIN position_competency_frameworks pcf ON sr.id = pcf.position_id " +
            "LEFT JOIN competency_frameworks cf ON pcf.framework_id = cf.id " +
            "ORDER BY sr.position_title ASC, sr.position_code ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                PositionCompetencyFramework item = new PositionCompetencyFramework();
                item.setId(rs.getString("pcf_id"));
                item.setPositionId(rs.getString("position_id"));
                item.setPositionCode(rs.getString("position_code"));
                item.setPositionTitle(rs.getString("position_title"));
                item.setLevel(rs.getString("level"));
                item.setDepartmentId(rs.getString("department_id"));
                item.setDepartmentName(rs.getString("department_name"));
                item.setFrameworkId(rs.getString("framework_id"));
                item.setFrameworkCode(rs.getString("framework_code"));
                item.setFrameworkName(rs.getString("framework_name"));
                item.setAssignedAt(rs.getTimestamp("assigned_at"));
                list.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách chức danh và khung năng lực", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Thêm mới Khung năng lực kèm danh sách tiêu chí trong 1 Transaction an toàn
     */
    public boolean insert(CompetencyFramework framework, List<CompetencyFrameworkCriterion> criteria) {
        Connection conn = null;
        PreparedStatement psFramework = null;
        PreparedStatement psCriteria = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String sqlFramework = 
                "INSERT INTO competency_frameworks (id, code, name, description, status, created_by, updated_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
            psFramework = conn.prepareStatement(sqlFramework);
            psFramework.setString(1, framework.getId());
            psFramework.setString(2, framework.getCode());
            psFramework.setString(3, framework.getName());
            psFramework.setString(4, framework.getDescription());
            psFramework.setString(5, framework.getStatus());
            psFramework.setString(6, framework.getCreatedBy());
            psFramework.setString(7, framework.getUpdatedBy());
            psFramework.executeUpdate();

            if (criteria != null && !criteria.isEmpty()) {
                String sqlCriteria = 
                    "INSERT INTO competency_framework_criteria (id, framework_id, criterion_id, weight, display_order) " +
                    "VALUES (?, ?, ?, ?, ?)";
                psCriteria = conn.prepareStatement(sqlCriteria);

                int order = 1;
                for (CompetencyFrameworkCriterion c : criteria) {
                    psCriteria.setString(1, (c.getId() != null && !c.getId().isEmpty()) ? c.getId() : UUID.randomUUID().toString());
                    psCriteria.setString(2, framework.getId());
                    psCriteria.setString(3, c.getCriterionId());
                    psCriteria.setBigDecimal(4, c.getWeight());
                    psCriteria.setInt(5, c.getDisplayOrder() > 0 ? c.getDisplayOrder() : order++);
                    psCriteria.addBatch();
                }
                psCriteria.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            logger.log(Level.SEVERE, "Lỗi thêm mới khung năng lực", e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            close(null, psCriteria, null);
            close(conn, psFramework, null);
        }
    }

    /**
     * Cập nhật Khung năng lực và làm mới danh sách tiêu chí trong 1 Transaction
     */
    public boolean update(CompetencyFramework framework, List<CompetencyFrameworkCriterion> criteria) {
        Connection conn = null;
        PreparedStatement psFramework = null;
        PreparedStatement psDeleteCriteria = null;
        PreparedStatement psInsertCriteria = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String sqlFramework = 
                "UPDATE competency_frameworks SET code = ?, name = ?, description = ?, status = ?, updated_by = ? " +
                "WHERE id = ?";
            psFramework = conn.prepareStatement(sqlFramework);
            psFramework.setString(1, framework.getCode());
            psFramework.setString(2, framework.getName());
            psFramework.setString(3, framework.getDescription());
            psFramework.setString(4, framework.getStatus());
            psFramework.setString(5, framework.getUpdatedBy());
            psFramework.setString(6, framework.getId());
            psFramework.executeUpdate();

            // Xóa danh sách tiêu chí cũ của khung
            String sqlDel = "DELETE FROM competency_framework_criteria WHERE framework_id = ?";
            psDeleteCriteria = conn.prepareStatement(sqlDel);
            psDeleteCriteria.setString(1, framework.getId());
            psDeleteCriteria.executeUpdate();

            // Chèn danh sách tiêu chí mới
            if (criteria != null && !criteria.isEmpty()) {
                String sqlIns = 
                    "INSERT INTO competency_framework_criteria (id, framework_id, criterion_id, weight, display_order) " +
                    "VALUES (?, ?, ?, ?, ?)";
                psInsertCriteria = conn.prepareStatement(sqlIns);

                int order = 1;
                for (CompetencyFrameworkCriterion c : criteria) {
                    psInsertCriteria.setString(1, (c.getId() != null && !c.getId().isEmpty()) ? c.getId() : UUID.randomUUID().toString());
                    psInsertCriteria.setString(2, framework.getId());
                    psInsertCriteria.setString(3, c.getCriterionId());
                    psInsertCriteria.setBigDecimal(4, c.getWeight());
                    psInsertCriteria.setInt(5, c.getDisplayOrder() > 0 ? c.getDisplayOrder() : order++);
                    psInsertCriteria.addBatch();
                }
                psInsertCriteria.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            logger.log(Level.SEVERE, "Lỗi cập nhật khung năng lực: " + framework.getId(), e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            close(null, psInsertCriteria, null);
            close(null, psDeleteCriteria, null);
            close(conn, psFramework, null);
        }
    }

    /**
     * Cập nhật trạng thái khung năng lực (DRAFT, ACTIVE, INACTIVE)
     */
    public boolean updateStatus(String frameworkId, String newStatus, String updatedBy) {
        String sql = "UPDATE competency_frameworks SET status = ?, updated_by = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setString(2, updatedBy);
            ps.setString(3, frameworkId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái khung năng lực: " + frameworkId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Gán danh sách chức danh sử dụng Khung năng lực trong Transaction
     * Một chức danh chỉ có 1 khung năng lực áp dụng tại 1 thời điểm.
     */
    public boolean assignPositionsToFramework(String frameworkId, List<String> positionIds, String assignedBy) {
        Connection conn = null;
        PreparedStatement psDelCurrent = null;
        PreparedStatement psDelExisting = null;
        PreparedStatement psInsert = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Xóa các chức danh hiện tại đang gán cho khung này
            String sqlDelCurrent = "DELETE FROM position_competency_frameworks WHERE framework_id = ?";
            psDelCurrent = conn.prepareStatement(sqlDelCurrent);
            psDelCurrent.setString(1, frameworkId);
            psDelCurrent.executeUpdate();

            // 2. Với các chức danh được chọn mới, nếu đang gắn với khung khác thì xóa để thay thế
            if (positionIds != null && !positionIds.isEmpty()) {
                StringBuilder sqlDelExisting = new StringBuilder("DELETE FROM position_competency_frameworks WHERE position_id IN (");
                for (int i = 0; i < positionIds.size(); i++) {
                    sqlDelExisting.append(i > 0 ? ",?" : "?");
                }
                sqlDelExisting.append(")");
                psDelExisting = conn.prepareStatement(sqlDelExisting.toString());
                for (int i = 0; i < positionIds.size(); i++) {
                    psDelExisting.setString(i + 1, positionIds.get(i));
                }
                psDelExisting.executeUpdate();

                // 3. Chèn liên kết mới
                String sqlInsert = 
                    "INSERT INTO position_competency_frameworks (id, position_id, framework_id, assigned_by) " +
                    "VALUES (?, ?, ?, ?)";
                psInsert = conn.prepareStatement(sqlInsert);
                for (String posId : positionIds) {
                    if (posId == null || posId.trim().isEmpty()) continue;
                    psInsert.setString(1, UUID.randomUUID().toString());
                    psInsert.setString(2, posId.trim());
                    psInsert.setString(3, frameworkId);
                    psInsert.setString(4, assignedBy);
                    psInsert.addBatch();
                }
                psInsert.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            logger.log(Level.SEVERE, "Lỗi gán chức danh cho khung năng lực: " + frameworkId, e);
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            close(null, psInsert, null);
            close(null, psDelExisting, null);
            close(conn, psDelCurrent, null);
        }
    }

    /**
     * Xóa khung năng lực (chỉ khi không có chức danh nào đang áp dụng)
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM competency_frameworks WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa khung năng lực: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}
