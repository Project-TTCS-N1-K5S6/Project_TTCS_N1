package com.irms.dao;

import com.irms.model.Candidate;
import com.irms.model.SlaConfig;
import com.irms.model.SlaReminderLog;
import com.irms.util.SecurityUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * ==============================================================================
 * DAO XỬ LÝ CẤU HÌNH NGƯỠNG SLA, QUÉT QUÁ HẠN & NHẬT KÝ NHẮC VIỆC (KN-364, 367, 368)
 * ==============================================================================
 */
public class SlaDAO extends BaseDAO {

    private static boolean isTablesInitialized = false;

    public SlaDAO() {
        initTables();
    }

    /**
     * Tự động khởi tạo và kiểm tra schema nếu bảng hoặc cột chưa tồn tại
     */
    public synchronized void initTables() {
        if (isTablesInitialized) return;
        isTablesInitialized = true;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // 1. Bảng sla_configurations
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sla_configurations (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "stage_code VARCHAR(50) NOT NULL UNIQUE, " +
                    "stage_name VARCHAR(100) NOT NULL, " +
                    "sla_days INT NOT NULL DEFAULT 3, " +
                    "warning_threshold_days INT NOT NULL DEFAULT 1, " +
                    "action_required VARCHAR(255) NULL, " +
                    "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "updated_by VARCHAR(36) NULL" +
                    ")");

            // 2. Bảng in_app_notifications
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS in_app_notifications (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "content TEXT NOT NULL, " +
                    "type VARCHAR(50) NOT NULL DEFAULT 'SLA_OVERDUE', " +
                    "reference_id VARCHAR(36) NULL, " +
                    "is_read BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // 3. Bảng sla_reminder_logs
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS sla_reminder_logs (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "candidate_id VARCHAR(36) NOT NULL, " +
                    "candidate_name VARCHAR(255) NOT NULL, " +
                    "requisition_id VARCHAR(36) NULL, " +
                    "requisition_title VARCHAR(255) NULL, " +
                    "stage_code VARCHAR(50) NOT NULL, " +
                    "sla_days_threshold INT NOT NULL, " +
                    "days_in_stage INT NOT NULL, " +
                    "recruiter_id VARCHAR(36) NULL, " +
                    "recruiter_name VARCHAR(255) NULL, " +
                    "recruiter_email VARCHAR(255) NULL, " +
                    "channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP_AND_EMAIL', " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'SENT', " +
                    "message TEXT NULL, " +
                    "sent_date DATE NOT NULL, " +
                    "sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            // 4. Bổ sung các cột cần thiết cho bảng candidates nếu chưa có
            safeAddCandidateColumn(conn, "is_sla_overdue", "BOOLEAN NOT NULL DEFAULT FALSE");
            safeAddCandidateColumn(conn, "sla_overdue_detected_at", "TIMESTAMP NULL");
            safeAddCandidateColumn(conn, "stage_entered_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            // 5. Nạp cấu hình mặc định ban đầu nếu bảng rỗng
            initDefaultSlaConfigs(conn);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "[SlaDAO] Ghi chú khởi tạo bảng SLA: " + e.getMessage());
        } finally {
            close(conn, stmt);
        }
    }

    private void safeAddCandidateColumn(Connection conn, String colName, String colType) {
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("ALTER TABLE candidates ADD COLUMN " + colName + " " + colType);
        } catch (SQLException ignored) {
            // Cột đã tồn tại
        }
    }

    private void initDefaultSlaConfigs(Connection conn) {
        String countSql = "SELECT COUNT(*) FROM sla_configurations";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(countSql)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertSql = "INSERT INTO sla_configurations (id, stage_code, stage_name, sla_days, warning_threshold_days, action_required, is_active) VALUES " +
                        "('sla-001', 'APPLIED', 'Mới ứng tuyển', 3, 1, 'Sàng lọc hồ sơ ứng viên ban đầu và phản hồi/chuyển vòng', TRUE), " +
                        "('sla-002', 'SCREENING', 'Sàng lọc CV', 5, 2, 'Đánh giá chi tiết kinh nghiệm, kỹ năng và hẹn lịch phỏng vấn', TRUE), " +
                        "('sla-003', 'INTERVIEWING', 'Phỏng vấn', 7, 2, 'Hoàn thành buổi phỏng vấn chuyên môn và nhập kết quả đánh giá', TRUE), " +
                        "('sla-004', 'OFFER', 'Đề nghị tuyển dụng (Offer)', 4, 1, 'Thương lượng đãi ngộ và xác nhận phản hồi nhận việc của ứng viên', TRUE)";
                st.executeUpdate(insertSql);
                logger.info("[SlaDAO] Đã nạp thành công 4 cấu hình ngưỡng SLA giai đoạn mặc định.");
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "[SlaDAO] Lỗi nạp cấu hình SLA mặc định", e);
        }
    }

    /**
     * [KN-364]: Lấy danh sách tất cả các cấu hình ngưỡng SLA
     */
    public List<SlaConfig> findAllConfigs() {
        List<SlaConfig> list = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name AS updated_by_name FROM sla_configurations s " +
                     "LEFT JOIN users u ON s.updated_by = u.id " +
                     "ORDER BY CASE s.stage_code " +
                     "  WHEN 'APPLIED' THEN 1 " +
                     "  WHEN 'SCREENING' THEN 2 " +
                     "  WHEN 'INTERVIEWING' THEN 3 " +
                     "  WHEN 'OFFER' THEN 4 " +
                     "  ELSE 5 END ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                SlaConfig cfg = new SlaConfig();
                cfg.setId(rs.getString("id"));
                cfg.setStageCode(rs.getString("stage_code"));
                cfg.setStageName(rs.getString("stage_name"));
                cfg.setSlaDays(rs.getInt("sla_days"));
                cfg.setWarningThresholdDays(rs.getInt("warning_threshold_days"));
                cfg.setActionRequired(rs.getString("action_required"));
                cfg.setActive(rs.getBoolean("is_active"));
                cfg.setCreatedAt(rs.getTimestamp("created_at"));
                cfg.setUpdatedAt(rs.getTimestamp("updated_at"));
                cfg.setUpdatedBy(rs.getString("updated_by"));
                cfg.setUpdatedByName(rs.getString("updated_by_name"));
                list.add(cfg);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi truy vấn cấu hình SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * [KN-364]: Lấy cấu hình ngưỡng SLA của một giai đoạn cụ thể
     */
    public SlaConfig findConfigByStage(String stageCode) {
        String sql = "SELECT * FROM sla_configurations WHERE stage_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, stageCode);
            rs = ps.executeQuery();
            if (rs.next()) {
                SlaConfig cfg = new SlaConfig();
                cfg.setId(rs.getString("id"));
                cfg.setStageCode(rs.getString("stage_code"));
                cfg.setStageName(rs.getString("stage_name"));
                cfg.setSlaDays(rs.getInt("sla_days"));
                cfg.setWarningThresholdDays(rs.getInt("warning_threshold_days"));
                cfg.setActionRequired(rs.getString("action_required"));
                cfg.setActive(rs.getBoolean("is_active"));
                cfg.setCreatedAt(rs.getTimestamp("created_at"));
                cfg.setUpdatedAt(rs.getTimestamp("updated_at"));
                cfg.setUpdatedBy(rs.getString("updated_by"));
                return cfg;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm cấu hình SLA theo stageCode: " + stageCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [KN-364]: Cập nhật cấu hình ngưỡng SLA
     */
    public boolean updateConfig(SlaConfig config) {
        String sql = "UPDATE sla_configurations SET stage_name = ?, sla_days = ?, " +
                     "warning_threshold_days = ?, action_required = ?, is_active = ?, " +
                     "updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, config.getStageName());
            ps.setInt(2, config.getSlaDays());
            ps.setInt(3, config.getWarningThresholdDays());
            ps.setString(4, config.getActionRequired());
            ps.setBoolean(5, config.isActive());
            ps.setString(6, config.getUpdatedBy());
            ps.setString(7, config.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật cấu hình SLA: " + config.getId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-367]: Kiểm tra xem hồ sơ đã được gửi nhắc việc trong cùng ngày hôm nay hay chưa.
     * Ngăn chặn gửi trùng lặp cho cùng hồ sơ và giai đoạn trong cùng một ngày.
     */
    public boolean hasReminderBeenSentToday(String candidateId, String stageCode, Date sentDate) {
        String sql = "SELECT COUNT(*) FROM sla_reminder_logs " +
                     "WHERE candidate_id = ? AND stage_code = ? AND sent_date = ? AND status = 'SENT'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, candidateId);
            ps.setString(2, stageCode);
            ps.setDate(3, sentDate != null ? sentDate : new Date(System.currentTimeMillis()));
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra trùng lặp nhắc việc SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * [KN-368]: Ghi log lịch sử cảnh báo SLA và lần gửi nhắc việc
     */
    public boolean insertReminderLog(SlaReminderLog log) {
        String sql = "INSERT INTO sla_reminder_logs (id, candidate_id, candidate_name, requisition_id, " +
                     "requisition_title, stage_code, sla_days_threshold, days_in_stage, recruiter_id, " +
                     "recruiter_name, recruiter_email, channel, status, message, sent_date, sent_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, log.getId() != null ? log.getId() : SecurityUtil.generateUUID());
            ps.setString(2, log.getCandidateId());
            ps.setString(3, log.getCandidateName());
            ps.setString(4, log.getRequisitionId());
            ps.setString(5, log.getRequisitionTitle());
            ps.setString(6, log.getStageCode());
            ps.setInt(7, log.getSlaDaysThreshold());
            ps.setInt(8, log.getDaysInStage());
            ps.setString(9, log.getRecruiterId());
            ps.setString(10, log.getRecruiterName());
            ps.setString(11, log.getRecruiterEmail());
            ps.setString(12, log.getChannel() != null ? log.getChannel() : "IN_APP_AND_EMAIL");
            ps.setString(13, log.getStatus() != null ? log.getStatus() : "SENT");
            ps.setString(14, log.getMessage());
            ps.setDate(15, log.getSentDate() != null ? log.getSentDate() : new Date(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi ghi nhật ký nhắc việc SLA", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-368]: Lấy danh sách lịch sử nhắc việc SLA có phân trang
     */
    public List<SlaReminderLog> findReminderLogs(String search, Date date, int offset, int limit) {
        List<SlaReminderLog> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT l.*, cfg.stage_name FROM sla_reminder_logs l " +
                "LEFT JOIN sla_configurations cfg ON l.stage_code = cfg.stage_code WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (l.candidate_name LIKE ? OR l.recruiter_name LIKE ? OR l.requisition_title LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (date != null) {
            sql.append("AND l.sent_date = ? ");
            params.add(date);
        }

        sql.append("ORDER BY l.sent_at DESC LIMIT ? OFFSET ?");
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
                SlaReminderLog item = new SlaReminderLog();
                item.setId(rs.getString("id"));
                item.setCandidateId(rs.getString("candidate_id"));
                item.setCandidateName(rs.getString("candidate_name"));
                item.setRequisitionId(rs.getString("requisition_id"));
                item.setRequisitionTitle(rs.getString("requisition_title"));
                item.setStageCode(rs.getString("stage_code"));
                item.setStageName(rs.getString("stage_name") != null ? rs.getString("stage_name") : rs.getString("stage_code"));
                item.setSlaDaysThreshold(rs.getInt("sla_days_threshold"));
                item.setDaysInStage(rs.getInt("days_in_stage"));
                item.setRecruiterId(rs.getString("recruiter_id"));
                item.setRecruiterName(rs.getString("recruiter_name"));
                item.setRecruiterEmail(rs.getString("recruiter_email"));
                item.setChannel(rs.getString("channel"));
                item.setStatus(rs.getString("status"));
                item.setMessage(rs.getString("message"));
                item.setSentDate(rs.getDate("sent_date"));
                item.setSentAt(rs.getTimestamp("sent_at"));
                list.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi truy vấn lịch sử nhắc việc SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countReminderLogs(String search, Date date) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM sla_reminder_logs l WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (l.candidate_name LIKE ? OR l.recruiter_name LIKE ? OR l.requisition_title LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (date != null) {
            sql.append("AND l.sent_date = ? ");
            params.add(date);
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
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số lượng log nhắc việc SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public int countRemindersSentToday() {
        String sql = "SELECT COUNT(*) FROM sla_reminder_logs WHERE sent_date = CURRENT_DATE AND status = 'SENT'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số nhắc việc đã gửi hôm nay", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public int countOverdueCandidates() {
        String sql = "SELECT COUNT(*) FROM candidates WHERE is_sla_overdue = TRUE AND status NOT IN ('HIRED', 'REJECTED')";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số hồ sơ quá hạn SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * [KN-362]: Truy vấn tất cả ứng viên đang trong các giai đoạn chưa kết thúc để quét SLA
     */
    public List<Candidate> findActiveCandidatesForScan() {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT c.*, r.title AS requisition_title, r.recruiter_id, " +
                     "u_rec.full_name AS recruiter_name, u_rec.email AS recruiter_email, " +
                     "DATEDIFF(CURRENT_TIMESTAMP, COALESCE(c.stage_entered_at, c.created_at)) AS days_in_stage " +
                     "FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN users u_rec ON r.recruiter_id = u_rec.id " +
                     "WHERE c.status NOT IN ('HIRED', 'REJECTED')";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                Candidate c = new Candidate();
                c.setId(rs.getString("id"));
                c.setFullName(rs.getString("full_name"));
                c.setEmail(rs.getString("email"));
                c.setPhone(rs.getString("phone"));
                c.setStatus(rs.getString("status"));
                c.setRequisitionId(rs.getString("requisition_id"));
                c.setRequisitionTitle(rs.getString("requisition_title"));
                c.setRecruiterId(rs.getString("recruiter_id"));
                c.setRecruiterName(rs.getString("recruiter_name"));
                c.setRecruiterEmail(rs.getString("recruiter_email"));
                c.setStageEnteredAt(rs.getTimestamp("stage_entered_at"));
                c.setSlaOverdue(rs.getBoolean("is_sla_overdue"));
                c.setSlaOverdueDetectedAt(rs.getTimestamp("sla_overdue_detected_at"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setDaysInCurrentStage(rs.getInt("days_in_stage"));
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách ứng viên phục vụ quét định kỳ SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }
}
