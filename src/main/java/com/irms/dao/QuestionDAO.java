package com.irms.dao;

import com.irms.model.Question;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class QuestionDAO extends BaseDAO {

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
    
    public List<Question> findAll() {
        return search(null, null);
    }

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
