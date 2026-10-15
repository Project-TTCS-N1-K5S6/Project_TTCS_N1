package com.irms.dao;

import com.irms.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Data Access Object quản lý dữ liệu Ngân hàng câu hỏi phỏng vấn (bảng questions)
 * Thực hiện tìm kiếm, thêm mới, cập nhật, xóa câu hỏi và liên kết tiêu chí năng lực
 */
public class QuestionDAO extends BaseDAO {

    public QuestionDAO() {
        ensureSchema();
    }

    /**
     * Tự động tạo bảng questions nếu chưa tồn tại trong database.
     * Đảm bảo tương thích ngược với các DB cũ chưa chạy migration.
     */
    private void ensureSchema() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS questions (" +
                "  id VARCHAR(36) PRIMARY KEY," +
                "  content TEXT NOT NULL," +
                "  difficulty_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'," +
                "  good_answer_suggestion TEXT NULL," +
                "  criterion_id VARCHAR(36) NULL," +
                "  job_title VARCHAR(255) NULL," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "  INDEX idx_q_criterion (criterion_id)," +
                "  INDEX idx_q_difficulty (difficulty_level)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể tạo/kiểm tra bảng questions: " + e.getMessage());
        } finally {
            closeStatement(conn, stmt);
        }
    }

    /**
     * Chức năng: Tìm kiếm danh sách câu hỏi phỏng vấn theo chức danh công việc và tiêu chí năng lực
     *
     * @param jobTitle    Chức danh vị trí công việc cần lọc
     * @param criterionId ID tiêu chí năng lực cần lọc
     * @return Danh sách câu hỏi phù hợp
     */
    public List<Question> search(String jobTitle, String criterionId) {
        List<Question> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT q.*, c.name as criterion_name FROM questions q " +
            "LEFT JOIN competency_criteria c ON q.criterion_id = c.id WHERE 1=1"
        );
        
        if (jobTitle != null && !jobTitle.trim().isEmpty()) {
            sql.append(" AND q.job_title LIKE ?");
        }
        if (criterionId != null && !criterionId.trim().isEmpty()) {
            sql.append(" AND q.criterion_id = ?");
        }
        
        sql.append(" ORDER BY q.created_at DESC");
        
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            
            int paramIndex = 1;
            if (jobTitle != null && !jobTitle.trim().isEmpty()) {
                ps.setString(paramIndex++, "%" + jobTitle.trim() + "%");
            }
            if (criterionId != null && !criterionId.trim().isEmpty()) {
                ps.setString(paramIndex++, criterionId.trim());
            }
            
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm câu hỏi", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }
    
    /**
     * Chức năng: Lấy toàn bộ danh sách câu hỏi phỏng vấn trong hệ thống
     *
     * @return Danh sách tất cả câu hỏi
     */
    public List<Question> findAll() {
        return search(null, null);
    }

    /**
     * Chức năng: Tìm kiếm thông tin chi tiết một câu hỏi theo mã định danh ID
     *
     * @param id ID của câu hỏi
     * @return Thực thể Question nếu tìm thấy, ngược lại null
     */
    public Question findById(String id) {
        String sql = "SELECT q.*, c.name as criterion_name FROM questions q " +
                     "LEFT JOIN competency_criteria c ON q.criterion_id = c.id WHERE q.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm câu hỏi theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Chức năng: Thêm mới một câu hỏi phỏng vấn vào bảng questions
     *
     * @param q Thực thể Question chứa thông tin cần lưu
     * @return true nếu thêm thành công, ngược lại false
     */
    public boolean insert(Question q) {
        String sql = "INSERT INTO questions (id, content, difficulty_level, good_answer_suggestion, criterion_id, job_title) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, q.getId());
            ps.setString(2, q.getContent());
            ps.setString(3, q.getDifficultyLevel());
            ps.setString(4, q.getGoodAnswerSuggestion());
            ps.setString(5, q.getCriterionId());
            ps.setString(6, q.getJobTitle());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm mới câu hỏi", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Chức năng: Cập nhật nội dung, độ khó, gợi ý câu trả lời và tiêu chí cho câu hỏi hiện có
     *
     * @param q Thực thể Question chứa thông tin cập nhật
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean update(Question q) {
        String sql = "UPDATE questions SET content = ?, difficulty_level = ?, good_answer_suggestion = ?, criterion_id = ?, job_title = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, q.getContent());
            ps.setString(2, q.getDifficultyLevel());
            ps.setString(3, q.getGoodAnswerSuggestion());
            ps.setString(4, q.getCriterionId());
            ps.setString(5, q.getJobTitle());
            ps.setString(6, q.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật câu hỏi", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Chức năng: Xóa một câu hỏi phỏng vấn khỏi cơ sở dữ liệu theo mã ID
     *
     * @param id ID của câu hỏi cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM questions WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa câu hỏi", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
    
    /**
     * Chức năng: Chuyển đổi dữ liệu từ dòng ResultSet sang đối tượng Question
     *
     * @param rs Con trỏ ResultSet đang trỏ tới bản ghi hiện tại
     * @return Đối tượng Question đầy đủ thông tin
     */
    private Question mapResultSetToEntity(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId(rs.getString("id"));
        q.setContent(rs.getString("content"));
        q.setDifficultyLevel(rs.getString("difficulty_level"));
        q.setGoodAnswerSuggestion(rs.getString("good_answer_suggestion"));
        q.setCriterionId(rs.getString("criterion_id"));
        q.setJobTitle(rs.getString("job_title"));
        q.setCreatedAt(rs.getTimestamp("created_at"));
        q.setUpdatedAt(rs.getTimestamp("updated_at"));
        q.setCriterionName(rs.getString("criterion_name"));
        return q;
    }
}
