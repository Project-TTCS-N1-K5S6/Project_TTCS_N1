package com.irms.dao;

import com.irms.model.Candidate;
import com.irms.model.CandidateInternalNote;
import com.irms.model.CandidateTag;
import com.irms.model.CandidateTagCategory;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class CandidateScreeningDAO extends BaseDAO {

    public List<CandidateTagCategory> findTagCatalog() {
        Map<String, CandidateTagCategory> categoriesById = new HashMap<>();
        List<CandidateTagCategory> categories = new ArrayList<>();
        String categorySql = "SELECT id, name FROM candidate_tag_categories WHERE is_active = TRUE ORDER BY name";
        String tagSql = "SELECT t.id, t.category_id, c.name AS category_name, t.name " +
                "FROM candidate_tags t JOIN candidate_tag_categories c ON c.id = t.category_id " +
                "WHERE t.is_active = TRUE AND c.is_active = TRUE ORDER BY c.name, t.name";
        Connection conn = null;
        PreparedStatement categoryStatement = null;
        PreparedStatement tagStatement = null;
        ResultSet categoryResults = null;
        ResultSet tagResults = null;
        try {
            conn = getConnection();
            categoryStatement = conn.prepareStatement(categorySql);
            categoryResults = categoryStatement.executeQuery();
            while (categoryResults.next()) {
                CandidateTagCategory category = new CandidateTagCategory();
                category.setId(categoryResults.getString("id"));
                category.setName(categoryResults.getString("name"));
                categoriesById.put(category.getId(), category);
                categories.add(category);
            }

            tagStatement = conn.prepareStatement(tagSql);
            tagResults = tagStatement.executeQuery();
            while (tagResults.next()) {
                CandidateTag tag = new CandidateTag();
                tag.setId(tagResults.getString("id"));
                tag.setCategoryId(tagResults.getString("category_id"));
                tag.setCategoryName(tagResults.getString("category_name"));
                tag.setName(tagResults.getString("name"));
                CandidateTagCategory category = categoriesById.get(tag.getCategoryId());
                if (category != null) category.getTags().add(tag);
            }
            return categories;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh mục nhãn sàng lọc", e);
            throw new IllegalStateException("Không thể tải danh mục nhãn.", e);
        } finally {
            close(null, tagStatement, tagResults);
            close(conn, categoryStatement, categoryResults);
        }
    }

    public boolean createTagCategory(String id, String name) {
        String sql = "INSERT INTO candidate_tag_categories (id, name) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ps.setString(2, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạo danh mục nhãn sàng lọc", e);
            throw new IllegalStateException("Không thể tạo danh mục nhãn. Vui lòng kiểm tra tên danh mục.", e);
        } finally {
            close(conn, ps);
        }
    }

    public boolean createTag(String id, String categoryId, String name) {
        String sql = "INSERT INTO candidate_tags (id, category_id, name) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            ps.setString(2, categoryId);
            ps.setString(3, name);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạo nhãn sàng lọc", e);
            throw new IllegalStateException("Không thể tạo nhãn. Vui lòng kiểm tra tên nhãn và danh mục.", e);
        } finally {
            close(conn, ps);
        }
    }

    public boolean replaceCandidateTags(String candidateId, List<String> tagIds, String assignedBy) {
        Connection conn = null;
        PreparedStatement verify = null;
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        ResultSet results = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            if (!tagIds.isEmpty()) {
                String verifySql = "SELECT COUNT(*) FROM candidate_tags t " +
                        "JOIN candidate_tag_categories c ON c.id = t.category_id " +
                        "WHERE t.is_active = TRUE AND c.is_active = TRUE AND t.id IN (" +
                        placeholders(tagIds.size()) + ")";
                verify = conn.prepareStatement(verifySql);
                for (int i = 0; i < tagIds.size(); i++) verify.setString(i + 1, tagIds.get(i));
                results = verify.executeQuery();
                if (!results.next() || results.getInt(1) != tagIds.size()) {
                    conn.rollback();
                    return false;
                }
                results.close();
                results = null;
                verify.close();
                verify = null;
            }

            delete = conn.prepareStatement("DELETE FROM candidate_tag_assignments WHERE candidate_id = ?");
            delete.setString(1, candidateId);
            delete.executeUpdate();

            if (!tagIds.isEmpty()) {
                insert = conn.prepareStatement("INSERT INTO candidate_tag_assignments " +
                        "(candidate_id, tag_id, assigned_by) VALUES (?, ?, ?)");
                for (String tagId : tagIds) {
                    insert.setString(1, candidateId);
                    insert.setString(2, tagId);
                    insert.setString(3, assignedBy);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            rollback(conn, e);
            logger.log(Level.SEVERE, "Lỗi cập nhật nhãn hồ sơ ứng viên", e);
            throw new IllegalStateException("Không thể cập nhật nhãn cho hồ sơ ứng viên.", e);
        } finally {
            close(null, verify, results);
            close(null, delete);
            close(conn, insert);
        }
    }

    public boolean saveQuickScreening(String candidateId, String reviewerId, String reviewerName,
                                      Integer rating, String internalNote) {
        if (rating == null && internalNote == null) return false;

        Connection conn = null;
        PreparedStatement ratingStatement = null;
        PreparedStatement noteStatement = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            if (rating != null) {
                ratingStatement = conn.prepareStatement(
                        "INSERT INTO candidate_quick_ratings (candidate_id, reviewer_id, rating) VALUES (?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE rating = VALUES(rating), updated_at = CURRENT_TIMESTAMP");
                ratingStatement.setString(1, candidateId);
                ratingStatement.setString(2, reviewerId);
                ratingStatement.setInt(3, rating);
                ratingStatement.executeUpdate();
            }

            if (internalNote != null) {
                noteStatement = conn.prepareStatement(
                        "INSERT INTO candidate_internal_notes " +
                                "(id, candidate_id, author_id, author_name, content) VALUES (?, ?, ?, ?, ?)");
                noteStatement.setString(1, SecurityUtil.generateUUID());
                noteStatement.setString(2, candidateId);
                noteStatement.setString(3, reviewerId);
                noteStatement.setString(4, reviewerName);
                noteStatement.setString(5, internalNote);
                noteStatement.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            rollback(conn, e);
            logger.log(Level.SEVERE, "Lỗi lưu đánh giá nhanh hồ sơ ứng viên", e);
            throw new IllegalStateException("Không thể lưu đánh giá nhanh cho hồ sơ ứng viên.", e);
        } finally {
            close(null, ratingStatement);
            close(conn, noteStatement);
        }
    }

    public void attachScreeningDetails(List<Candidate> candidates, String reviewerId, boolean includeInternalNotes) {
        if (candidates.isEmpty()) return;
        List<String> candidateIds = new ArrayList<>();
        Map<String, Candidate> candidatesById = new HashMap<>();
        for (Candidate candidate : candidates) {
            candidateIds.add(candidate.getId());
            candidatesById.put(candidate.getId(), candidate);
        }
        String inClause = placeholders(candidateIds.size());

        String ratingsSql = "SELECT candidate_id, AVG(rating) AS average_rating, COUNT(*) AS rating_count, " +
                "MAX(CASE WHEN reviewer_id = ? THEN rating END) AS current_user_rating " +
                "FROM candidate_quick_ratings WHERE candidate_id IN (" + inClause + ") GROUP BY candidate_id";
        String tagsSql = "SELECT a.candidate_id, t.id, t.category_id, c.name AS category_name, t.name " +
                "FROM candidate_tag_assignments a JOIN candidate_tags t ON t.id = a.tag_id " +
                "JOIN candidate_tag_categories c ON c.id = t.category_id " +
                "WHERE a.candidate_id IN (" + inClause + ") ORDER BY c.name, t.name";
        String notesSql = "SELECT candidate_id, content, author_name, created_at " +
                "FROM candidate_internal_notes WHERE candidate_id IN (" + inClause + ") " +
                "ORDER BY created_at DESC";

        Connection conn = null;
        PreparedStatement ratingStatement = null;
        PreparedStatement tagsStatement = null;
        PreparedStatement notesStatement = null;
        ResultSet results = null;
        try {
            conn = getConnection();
            ratingStatement = conn.prepareStatement(ratingsSql);
            ratingStatement.setString(1, reviewerId);
            setIds(ratingStatement, 2, candidateIds);
            results = ratingStatement.executeQuery();
            while (results.next()) {
                Candidate candidate = candidatesById.get(results.getString("candidate_id"));
                int currentRating = results.getInt("current_user_rating");
                candidate.setCurrentUserRating(results.wasNull() ? null : currentRating);
                candidate.setAverageRating(Math.round(results.getDouble("average_rating") * 10.0) / 10.0);
                candidate.setRatingCount(results.getInt("rating_count"));
            }
            results.close();

            tagsStatement = conn.prepareStatement(tagsSql);
            setIds(tagsStatement, 1, candidateIds);
            results = tagsStatement.executeQuery();
            while (results.next()) {
                CandidateTag tag = new CandidateTag();
                tag.setId(results.getString("id"));
                tag.setCategoryId(results.getString("category_id"));
                tag.setCategoryName(results.getString("category_name"));
                tag.setName(results.getString("name"));
                candidatesById.get(results.getString("candidate_id")).getTags().add(tag);
            }
            results.close();

            if (includeInternalNotes) {
                notesStatement = conn.prepareStatement(notesSql);
                setIds(notesStatement, 1, candidateIds);
                results = notesStatement.executeQuery();
                while (results.next()) {
                    CandidateInternalNote note = new CandidateInternalNote();
                    note.setContent(results.getString("content"));
                    note.setAuthorName(results.getString("author_name"));
                    note.setCreatedAt(results.getTimestamp("created_at"));
                    candidatesById.get(results.getString("candidate_id")).getInternalNotes().add(note);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tải dữ liệu sàng lọc nhanh hồ sơ ứng viên", e);
            throw new IllegalStateException("Không thể tải dữ liệu sàng lọc nhanh.", e);
        } finally {
            close(null, ratingStatement, results);
            close(null, tagsStatement);
            close(conn, notesStatement);
        }
    }

    private void rollback(Connection conn, SQLException original) {
        if (conn == null) return;
        try {
            conn.rollback();
        } catch (SQLException rollbackError) {
            original.addSuppressed(rollbackError);
            logger.log(Level.SEVERE, "Không thể hoàn tác giao dịch sàng lọc ứng viên", rollbackError);
        }
    }

    private String placeholders(int count) {
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) placeholders.append(", ");
            placeholders.append('?');
        }
        return placeholders.toString();
    }

    private void setIds(PreparedStatement statement, int firstIndex, List<String> ids) throws SQLException {
        for (int i = 0; i < ids.size(); i++) statement.setString(firstIndex + i, ids.get(i));
    }
}
