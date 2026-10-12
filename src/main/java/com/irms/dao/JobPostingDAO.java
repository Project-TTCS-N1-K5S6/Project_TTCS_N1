package com.irms.dao;

import com.irms.model.JobPosting;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Tin tuyển dụng (bảng job_postings)
 */
public class JobPostingDAO extends BaseDAO {

    public JobPostingDAO() {
        initTableIfNotExists();
    }

    private void initTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS job_postings (" +
                     "id VARCHAR(36) PRIMARY KEY, " +
                     "requisition_id VARCHAR(36) NOT NULL, " +
                     "title VARCHAR(255) NOT NULL, " +
                     "platform VARCHAR(100) DEFAULT 'INTERNAL', " +
                     "status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED', " +
                     "posted_url VARCHAR(255) NULL, " +
                     "published_at TIMESTAMP NULL, " +
                     "unpublished_at TIMESTAMP NULL, " +
                     "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                     "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                     ")";
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể tự động khởi tạo bảng job_postings: " + e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ignored) {}
        }
    }

    public List<JobPosting> findByRequisitionId(String requisitionId) {
        List<JobPosting> list = new ArrayList<>();
        String sql = "SELECT * FROM job_postings WHERE requisition_id = ? ORDER BY created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy tin tuyển dụng theo requisition_id: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<JobPosting> findPublishedByRequisitionId(String requisitionId) {
        List<JobPosting> list = new ArrayList<>();
        String sql = "SELECT * FROM job_postings WHERE requisition_id = ? AND status = 'PUBLISHED' ORDER BY created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy tin tuyển dụng đang đăng theo requisition_id: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countPublishedByRequisitionId(String requisitionId) {
        String sql = "SELECT COUNT(*) FROM job_postings WHERE requisition_id = ? AND status = 'PUBLISHED'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số tin đang đăng của requisition_id: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Tự động gỡ các tin tuyển dụng đang đăng của yêu cầu tuyển dụng này
     * Chuyển trạng thái từ PUBLISHED sang UNPUBLISHED và cập nhật unpublished_at
     */
    public int unpublishByRequisitionId(String requisitionId) {
        String sql = "UPDATE job_postings SET status = 'UNPUBLISHED', unpublished_at = CURRENT_TIMESTAMP WHERE requisition_id = ? AND status = 'PUBLISHED'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tự động gỡ tin tuyển dụng của requisition_id: " + requisitionId, e);
            return 0;
        } finally {
            close(conn, ps);
        }
    }

    public boolean insert(JobPosting p) {
        String sql = "INSERT INTO job_postings (id, requisition_id, title, platform, status, posted_url, published_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, p.getId());
            ps.setString(2, p.getRequisitionId());
            ps.setString(3, p.getTitle());
            ps.setString(4, p.getPlatform());
            ps.setString(5, p.getStatus());
            ps.setString(6, p.getPostedUrl());
            ps.setTimestamp(7, p.getPublishedAt());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm tin tuyển dụng mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    private JobPosting mapResultSet(ResultSet rs) throws SQLException {
        JobPosting p = new JobPosting();
        p.setId(rs.getString("id"));
        p.setRequisitionId(rs.getString("requisition_id"));
        p.setTitle(rs.getString("title"));
        try { p.setPlatform(rs.getString("platform")); } catch (SQLException ignored) {}
        try { p.setStatus(rs.getString("status")); } catch (SQLException ignored) {}
        try { p.setPostedUrl(rs.getString("posted_url")); } catch (SQLException ignored) {}
        try { p.setPublishedAt(rs.getTimestamp("published_at")); } catch (SQLException ignored) {}
        try { p.setUnpublishedAt(rs.getTimestamp("unpublished_at")); } catch (SQLException ignored) {}
        try { p.setCreatedAt(rs.getTimestamp("created_at")); } catch (SQLException ignored) {}
        try { p.setUpdatedAt(rs.getTimestamp("updated_at")); } catch (SQLException ignored) {}
        return p;
    }
}
