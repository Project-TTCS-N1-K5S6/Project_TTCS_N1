package com.irms.dao;

import com.irms.model.ScreeningCriteria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Tiêu chí Sàng lọc & Chấm điểm hồ sơ (bảng job_screening_criteria)
 */
public class ScreeningCriteriaDAO extends BaseDAO {

    public ScreeningCriteriaDAO() {
        ensureTableExists();
    }

    /**
     * Lấy tiêu chí sàng lọc theo ID vị trí tuyển dụng
     */
    public ScreeningCriteria findByRequisitionId(String requisitionId) {
        if (requisitionId == null || requisitionId.trim().isEmpty()) return null;
        String sql = "SELECT sc.*, r.title AS requisition_title " +
                     "FROM job_screening_criteria sc " +
                     "LEFT JOIN recruitment_requisitions r ON sc.requisition_id = r.id " +
                     "WHERE sc.requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapCriteria(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy tiêu chí sàng lọc theo vị trí: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Lấy tiêu chí sàng lọc theo ID bản ghi
     */
    public ScreeningCriteria findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT sc.*, r.title AS requisition_title " +
                     "FROM job_screening_criteria sc " +
                     "LEFT JOIN recruitment_requisitions r ON sc.requisition_id = r.id " +
                     "WHERE sc.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapCriteria(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy tiêu chí sàng lọc theo id: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Lấy toàn bộ danh sách tiêu chí sàng lọc đã cấu hình
     */
    public List<ScreeningCriteria> findAll() {
        List<ScreeningCriteria> list = new ArrayList<>();
        String sql = "SELECT sc.*, r.title AS requisition_title " +
                     "FROM job_screening_criteria sc " +
                     "LEFT JOIN recruitment_requisitions r ON sc.requisition_id = r.id " +
                     "ORDER BY sc.updated_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapCriteria(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách tiêu chí sàng lọc", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Tạo mới tiêu chí sàng lọc cho một vị trí
     */
    public boolean create(ScreeningCriteria sc) {
        if (sc == null) return false;
        String sql = "INSERT INTO job_screening_criteria " +
                     "(id, requisition_id, min_experience_years, required_degree, must_have_skills, " +
                     "nice_to_have_skills, weight_experience, weight_skills, weight_education, " +
                     "created_by, created_by_name, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, sc.getRequisitionId());
            ps.setInt(3, sc.getMinExperienceYears());
            ps.setString(4, sc.getRequiredDegree());
            ps.setString(5, sc.getMustHaveSkills());
            ps.setString(6, sc.getNiceToHaveSkills());
            ps.setInt(7, sc.getWeightExperience());
            ps.setInt(8, sc.getWeightSkills());
            ps.setInt(9, sc.getWeightEducation());
            ps.setString(10, sc.getCreatedBy());
            ps.setString(11, sc.getCreatedByName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạo tiêu chí sàng lọc", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật tiêu chí sàng lọc theo vị trí. Nếu chưa có thì tạo mới (upsert).
     */
    public boolean saveOrUpdate(ScreeningCriteria sc) {
        if (sc == null) return false;
        ScreeningCriteria existing = findByRequisitionId(sc.getRequisitionId());
        if (existing != null) {
            return update(sc);
        } else {
            return create(sc);
        }
    }

    /**
     * Cập nhật tiêu chí sàng lọc đã có
     */
    public boolean update(ScreeningCriteria sc) {
        if (sc == null) return false;
        String sql = "UPDATE job_screening_criteria SET " +
                     "min_experience_years = ?, required_degree = ?, must_have_skills = ?, " +
                     "nice_to_have_skills = ?, weight_experience = ?, weight_skills = ?, " +
                     "weight_education = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, sc.getMinExperienceYears());
            ps.setString(2, sc.getRequiredDegree());
            ps.setString(3, sc.getMustHaveSkills());
            ps.setString(4, sc.getNiceToHaveSkills());
            ps.setInt(5, sc.getWeightExperience());
            ps.setInt(6, sc.getWeightSkills());
            ps.setInt(7, sc.getWeightEducation());
            ps.setString(8, sc.getRequisitionId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật tiêu chí sàng lọc vị trí: " + sc.getRequisitionId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Xoá tiêu chí sàng lọc theo vị trí
     */
    public boolean deleteByRequisitionId(String requisitionId) {
        if (requisitionId == null || requisitionId.trim().isEmpty()) return false;
        String sql = "DELETE FROM job_screening_criteria WHERE requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xoá tiêu chí sàng lọc vị trí: " + requisitionId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Lưu kết quả chấm điểm vào bảng candidates
     */
    public boolean saveCandidateScore(String candidateId, int matchScore, String mandatoryFlag,
                                       String scoreBreakdown, String scoredBy) {
        String sql = "UPDATE candidates SET match_score = ?, mandatory_flag = ?, " +
                     "score_breakdown = ?, scored_at = CURRENT_TIMESTAMP, scored_by = ?, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, matchScore);
            ps.setString(2, mandatoryFlag);
            ps.setString(3, scoreBreakdown);
            ps.setString(4, scoredBy);
            ps.setString(5, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lưu điểm chấm cho ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Đặt lại điểm (reset) toàn bộ ứng viên của một vị trí để chấm lại
     */
    public int resetScoresForRequisition(String requisitionId) {
        String sql = "UPDATE candidates SET match_score = NULL, mandatory_flag = NULL, " +
                     "score_breakdown = NULL, scored_at = NULL, scored_by = NULL, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi reset điểm chấm vị trí: " + requisitionId, e);
            return 0;
        } finally {
            close(conn, ps);
        }
    }

    // ========== Private helpers ==========

    private ScreeningCriteria mapCriteria(ResultSet rs) throws SQLException {
        ScreeningCriteria sc = new ScreeningCriteria();
        sc.setId(rs.getString("id"));
        sc.setRequisitionId(rs.getString("requisition_id"));
        try { sc.setRequisitionTitle(rs.getString("requisition_title")); } catch (SQLException ignored) {}
        sc.setMinExperienceYears(rs.getInt("min_experience_years"));
        sc.setRequiredDegree(rs.getString("required_degree"));
        sc.setMustHaveSkills(rs.getString("must_have_skills"));
        sc.setNiceToHaveSkills(rs.getString("nice_to_have_skills"));
        sc.setWeightExperience(rs.getInt("weight_experience"));
        sc.setWeightSkills(rs.getInt("weight_skills"));
        sc.setWeightEducation(rs.getInt("weight_education"));
        sc.setCreatedBy(rs.getString("created_by"));
        try { sc.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}
        sc.setCreatedAt(rs.getTimestamp("created_at"));
        sc.setUpdatedAt(rs.getTimestamp("updated_at"));
        return sc;
    }

    /**
     * Đảm bảo bảng job_screening_criteria và các cột scoring trong candidates tồn tại
     */
    private void ensureTableExists() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            // Tạo bảng tiêu chí nếu chưa có
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS job_screening_criteria (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "requisition_id VARCHAR(36) NOT NULL, " +
                "min_experience_years INT NOT NULL DEFAULT 0, " +
                "required_degree VARCHAR(100) NULL, " +
                "must_have_skills TEXT NULL, " +
                "nice_to_have_skills TEXT NULL, " +
                "weight_experience INT NOT NULL DEFAULT 40, " +
                "weight_skills INT NOT NULL DEFAULT 40, " +
                "weight_education INT NOT NULL DEFAULT 20, " +
                "created_by VARCHAR(36) NULL, " +
                "created_by_name VARCHAR(255) NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "UNIQUE KEY uk_jsc_requisition (requisition_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
            // Thêm các cột scoring vào candidates nếu chưa có
            DatabaseMetaData meta = conn.getMetaData();
            addCandidateColumnIfNotExists(meta, stmt, "match_score", "INT DEFAULT NULL");
            addCandidateColumnIfNotExists(meta, stmt, "mandatory_flag", "VARCHAR(20) DEFAULT NULL");
            addCandidateColumnIfNotExists(meta, stmt, "score_breakdown", "MEDIUMTEXT DEFAULT NULL");
            addCandidateColumnIfNotExists(meta, stmt, "scored_at", "TIMESTAMP NULL");
            addCandidateColumnIfNotExists(meta, stmt, "scored_by", "VARCHAR(36) NULL");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể khởi tạo bảng/cột chấm điểm: " + e.getMessage());
        } finally {
            closeStatement(conn, stmt);
        }
    }

    private void addCandidateColumnIfNotExists(DatabaseMetaData meta, Statement stmt, String columnName, String columnDef) {
        try (ResultSet rs = meta.getColumns(null, null, "candidates", columnName)) {
            if (!rs.next()) {
                stmt.executeUpdate("ALTER TABLE candidates ADD COLUMN " + columnName + " " + columnDef);
            }
        } catch (Exception ignored) {
            try {
                stmt.executeUpdate("ALTER TABLE candidates ADD COLUMN " + columnName + " " + columnDef);
            } catch (SQLException ignored2) {}
        }
    }
}
