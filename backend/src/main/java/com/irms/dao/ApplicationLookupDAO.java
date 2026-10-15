package com.irms.dao;

import com.irms.model.ApplicationLookupResult;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * ==============================================================================
 * [KN-243] [KN-245] [KN-246] [KN-247]: DAO tra cứu trạng thái hồ sơ công khai
 * ==============================================================================
 * Chỉ trả về thông tin tổng quát (trạng thái), KHÔNG tiết lộ:
 *  - Nhận xét nội bộ (notes, internal_comments)
 *  - Tên người phỏng vấn
 *  - Mức lương đề nghị
 *  - Lý do từ chối nội bộ chi tiết
 * Xác thực bằng cả candidateId (mã tra cứu) VÀ email để bảo vệ quyền truy cập.
 * ==============================================================================
 */
public class ApplicationLookupDAO extends BaseDAO {

    /**
     * [KN-247] [KN-245]: Tra cứu hồ sơ theo mã tra cứu và email.
     * Xác thực kép: phải đúng cả id VÀ email mới trả kết quả.
     * [KN-246]: Chỉ SELECT các cột được phép public, không JOIN bảng nội bộ.
     *
     * @param candidateId Mã tra cứu (UUID của hồ sơ)
     * @param email       Email ứng viên để xác minh quyền truy cập
     * @return ApplicationLookupResult nếu tìm thấy; null nếu không khớp
     */
    public ApplicationLookupResult findByIdAndEmail(String candidateId, String email) {
        String cleanCode = candidateId != null ? candidateId.trim() : "";
        String cleanEmail = email != null ? email.trim() : "";
        if (cleanCode.isEmpty() || cleanEmail.isEmpty()) {
            return null;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            boolean hasTrackingCode = false;
            try {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet colRs = meta.getColumns(null, null, "candidates", "tracking_code")) {
                    if (colRs.next()) {
                        hasTrackingCode = true;
                    } else {
                        try (ResultSet colUpperRs = meta.getColumns(null, null, "CANDIDATES", "TRACKING_CODE")) {
                            if (colUpperRs.next()) {
                                hasTrackingCode = true;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            String sql;
            if (hasTrackingCode) {
                // [KN-245] [KN-247]: Hỗ trợ tra cứu bằng cả UUID (c.id) và Mã tra cứu thân thiện (c.tracking_code)
                sql = "SELECT c.id, c.tracking_code, c.full_name, c.status, c.created_at, c.updated_at, " +
                      "       r.title AS position_title " +
                      "FROM   candidates c " +
                      "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                      "WHERE  (c.id = ? OR c.tracking_code = ?) AND LOWER(c.email) = LOWER(?) ";
                ps = conn.prepareStatement(sql);
                ps.setString(1, cleanCode);
                ps.setString(2, cleanCode);
                ps.setString(3, cleanEmail);
            } else {
                sql = "SELECT c.id, c.full_name, c.status, c.created_at, c.updated_at, " +
                      "       r.title AS position_title " +
                      "FROM   candidates c " +
                      "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                      "WHERE  c.id = ? AND LOWER(c.email) = LOWER(?) ";
                ps = conn.prepareStatement(sql);
                ps.setString(1, cleanCode);
                ps.setString(2, cleanEmail);
            }

            rs = ps.executeQuery();
            if (rs.next()) {
                ApplicationLookupResult result = new ApplicationLookupResult();
                result.setCandidateId(rs.getString("id"));
                if (hasTrackingCode) {
                    try {
                        result.setTrackingCode(rs.getString("tracking_code"));
                    } catch (SQLException ignored) {}
                }
                result.setFullName(rs.getString("full_name"));
                result.setRawStatus(rs.getString("status"));
                result.setPositionTitle(rs.getString("position_title"));
                result.setSubmittedAt(rs.getTimestamp("created_at"));
                result.setLastUpdatedAt(rs.getTimestamp("updated_at"));
                return result;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "[KN-247] Lỗi tra cứu hồ sơ theo mã và email", e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }
}
