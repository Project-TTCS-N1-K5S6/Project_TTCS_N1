package com.irms.dao;

import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Data Access Object quản lý Tin tuyển dụng & Lịch sử xuất bản (KN-120)
 * Bảng: job_postings, job_post_publications
 */
public class JobPostingDAO extends BaseDAO {

    public JobPostingDAO() {
        initTablesIfNotExists();
    }

    private void initTablesIfNotExists() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();

            // 1. Tạo bảng job_postings
            String sql1 = "CREATE TABLE IF NOT EXISTS job_postings (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "requisition_id VARCHAR(36) NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "platform VARCHAR(100) DEFAULT 'CAREER_PORTAL', " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', " +
                    "current_version INT NOT NULL DEFAULT 1, " +
                    "published_at TIMESTAMP NULL, " +
                    "published_by VARCHAR(36) NULL, " +
                    "unpublished_at TIMESTAMP NULL, " +
                    "unpublished_by VARCHAR(36) NULL, " +
                    "public_url VARCHAR(255) NULL, " +
                    "posted_url VARCHAR(255) NULL, " +
                    "views_count INT NOT NULL DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.executeUpdate(sql1);

            // 2. Tạo bảng job_post_publications lưu lịch sử bản xuất & thời điểm
            String sql2 = "CREATE TABLE IF NOT EXISTS job_post_publications (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "requisition_id VARCHAR(36) NOT NULL, " +
                    "job_posting_id VARCHAR(36) NULL, " +
                    "version_number INT NOT NULL DEFAULT 1, " +
                    "version_label VARCHAR(50) NOT NULL, " +
                    "channel VARCHAR(100) NOT NULL DEFAULT 'CAREER_PORTAL', " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED', " +
                    "published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "published_by VARCHAR(36) NULL, " +
                    "published_by_name VARCHAR(255) NULL, " +
                    "unpublished_at TIMESTAMP NULL, " +
                    "unpublished_by VARCHAR(36) NULL, " +
                    "snapshot_title VARCHAR(255) NOT NULL, " +
                    "snapshot_data LONGTEXT NOT NULL, " +
                    "change_summary TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.executeUpdate(sql2);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể tự động khởi tạo bảng job_postings / job_post_publications: " + e.getMessage());
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private void closeQuietly(AutoCloseable ac) {
        if (ac != null) {
            try { ac.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * Lấy danh sách tất cả yêu cầu tuyển dụng kèm thông tin xuất bản
     */
    public List<JobPosting> findAllPosts(String search, String deptId, String approvalStatus, String publishStatus, int offset, int limit) {
        List<JobPosting> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.id AS req_id, r.code AS req_code, r.title AS req_title, r.position_title, " +
                "r.department_id, d.name AS department_name, r.headcount, r.recruitment_reason, " +
                "r.min_salary, r.max_salary, r.currency, r.deadline, r.job_description, r.job_requirements, " +
                "r.salary_explanation, r.status AS req_status, r.created_at AS req_created_at, " +
                "jp.id AS jp_id, jp.title AS jp_title, jp.platform AS jp_platform, " +
                "COALESCE(jp.status, 'DRAFT') AS jp_status, " +
                "COALESCE(jp.current_version, 0) AS jp_version, " +
                "jp.published_at AS jp_published_at, jp.published_by AS jp_published_by, " +
                "u_pub.full_name AS published_by_name, " +
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count " +
                "FROM recruitment_requisitions r " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN job_postings jp ON r.id = jp.requisition_id " +
                "LEFT JOIN users u_pub ON jp.published_by = u_pub.id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(r.code) LIKE ? OR LOWER(r.title) LIKE ? OR LOWER(r.position_title) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (deptId != null && !deptId.trim().isEmpty()) {
            sql.append("AND r.department_id = ? ");
            params.add(deptId.trim());
        }

        if (approvalStatus != null && !approvalStatus.trim().isEmpty()) {
            if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status IN ('APPROVED', 'OPEN') ");
            } else if ("PENDING".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status = 'PENDING_APPROVAL' ");
            } else if ("DRAFT".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status = 'DRAFT' ");
            } else if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status = 'REJECTED' ");
            } else {
                sql.append("AND r.status = ? ");
                params.add(approvalStatus.trim());
            }
        }

        if (publishStatus != null && !publishStatus.trim().isEmpty()) {
            if ("PUBLISHED".equalsIgnoreCase(publishStatus)) {
                sql.append("AND jp.status = 'PUBLISHED' ");
            } else if ("UNPUBLISHED".equalsIgnoreCase(publishStatus)) {
                sql.append("AND jp.status = 'UNPUBLISHED' ");
            } else if ("DRAFT".equalsIgnoreCase(publishStatus)) {
                sql.append("AND (jp.status IS NULL OR jp.status = 'DRAFT') ");
            }
        }

        sql.append("ORDER BY COALESCE(jp.published_at, r.created_at) DESC");
        if (limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);
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
            while (rs.next()) {
                list.add(mapResultSetToPosting(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách bài đăng", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countAllPosts(String search, String deptId, String approvalStatus, String publishStatus) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM recruitment_requisitions r " +
                "LEFT JOIN job_postings jp ON r.id = jp.requisition_id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(r.code) LIKE ? OR LOWER(r.title) LIKE ? OR LOWER(r.position_title) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (deptId != null && !deptId.trim().isEmpty()) {
            sql.append("AND r.department_id = ? ");
            params.add(deptId.trim());
        }

        if (approvalStatus != null && !approvalStatus.trim().isEmpty()) {
            if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status IN ('APPROVED', 'OPEN') ");
            } else if ("PENDING".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status = 'PENDING_APPROVAL' ");
            } else if ("DRAFT".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status = 'DRAFT' ");
            } else if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
                sql.append("AND r.status = 'REJECTED' ");
            } else {
                sql.append("AND r.status = ? ");
                params.add(approvalStatus.trim());
            }
        }

        if (publishStatus != null && !publishStatus.trim().isEmpty()) {
            if ("PUBLISHED".equalsIgnoreCase(publishStatus)) {
                sql.append("AND jp.status = 'PUBLISHED' ");
            } else if ("UNPUBLISHED".equalsIgnoreCase(publishStatus)) {
                sql.append("AND jp.status = 'UNPUBLISHED' ");
            } else if ("DRAFT".equalsIgnoreCase(publishStatus)) {
                sql.append("AND (jp.status IS NULL OR jp.status = 'DRAFT') ");
            }
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
            logger.log(Level.SEVERE, "Lỗi đếm số lượng bài đăng", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public JobPosting findByRequisitionId(String requisitionId) {
        String sql = "SELECT r.id AS req_id, r.code AS req_code, r.title AS req_title, r.position_title, " +
                "r.department_id, d.name AS department_name, r.headcount, r.recruitment_reason, " +
                "r.min_salary, r.max_salary, r.currency, r.deadline, r.job_description, r.job_requirements, " +
                "r.salary_explanation, r.status AS req_status, r.created_at AS req_created_at, " +
                "jp.id AS jp_id, jp.title AS jp_title, jp.platform AS jp_platform, " +
                "COALESCE(jp.status, 'DRAFT') AS jp_status, " +
                "COALESCE(jp.current_version, 0) AS jp_version, " +
                "jp.published_at AS jp_published_at, jp.published_by AS jp_published_by, " +
                "u_pub.full_name AS published_by_name, " +
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count " +
                "FROM recruitment_requisitions r " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN job_postings jp ON r.id = jp.requisition_id " +
                "LEFT JOIN users u_pub ON jp.published_by = u_pub.id " +
                "WHERE r.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToPosting(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm bài đăng theo requisitionId: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * KN-120 (1) + (3): Xuất bản bài đăng
     * Yêu cầu:
     * - Tin phải duyệt trước khi show công khai (nếu chưa duyệt ném IllegalStateException)
     * - Ghi lại bản xuất và thời điểm
     */
    public JobPostPublication publish(String requisitionId, String channel, String changeSummary,
                                     String actorId, String actorName, String snapshotJson) throws SQLException {
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psMaxVer = null;
        PreparedStatement psPosting = null;
        PreparedStatement psArchivePrev = null;
        PreparedStatement psInsertPub = null;
        ResultSet rsCheck = null;
        ResultSet rsMaxVer = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Kiểm tra trạng thái yêu cầu tuyển dụng: BẮT BUỘC ĐÃ DUYỆT (APPROVED hoặc OPEN)
            String sqlCheck = "SELECT status, title FROM recruitment_requisitions WHERE id = ?";
            psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setString(1, requisitionId);
            rsCheck = psCheck.executeQuery();

            if (!rsCheck.next()) {
                throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng có mã ID: " + requisitionId);
            }

            String reqStatus = rsCheck.getString("status");
            String reqTitle = rsCheck.getString("title");

            if (reqStatus == null || (!"APPROVED".equalsIgnoreCase(reqStatus) && !"OPEN".equalsIgnoreCase(reqStatus))) {
                throw new IllegalStateException("Tin phải duyệt trước khi show công khai! Trạng thái hiện tại: " +
                        (reqStatus != null ? reqStatus : "Chưa xác định") + ". Vui lòng phê duyệt trước khi xuất bản.");
            }

            // 2. Tính số phiên bản xuất bản kế tiếp (Bản xuất #1, #2, #3...)
            String sqlMaxVer = "SELECT COALESCE(MAX(version_number), 0) FROM job_post_publications WHERE requisition_id = ?";
            psMaxVer = conn.prepareStatement(sqlMaxVer);
            psMaxVer.setString(1, requisitionId);
            rsMaxVer = psMaxVer.executeQuery();
            int nextVersion = 1;
            if (rsMaxVer.next()) {
                nextVersion = rsMaxVer.getInt(1) + 1;
            }

            String versionLabel = "Bản xuất #" + nextVersion;

            // 3. Cập nhật hoặc thêm mới bản ghi trong job_postings
            String checkPostingSql = "SELECT id FROM job_postings WHERE requisition_id = ?";
            PreparedStatement psCheckP = conn.prepareStatement(checkPostingSql);
            psCheckP.setString(1, requisitionId);
            ResultSet rsP = psCheckP.executeQuery();
            String postingId;
            boolean existsPosting = false;

            if (rsP.next()) {
                postingId = rsP.getString("id");
                existsPosting = true;
            } else {
                postingId = SecurityUtil.generateUUID();
            }
            rsP.close();
            psCheckP.close();

            if (existsPosting) {
                String sqlUpPost = "UPDATE job_postings SET title = ?, platform = ?, status = 'PUBLISHED', " +
                        "current_version = ?, published_at = CURRENT_TIMESTAMP, published_by = ?, " +
                        "unpublished_at = NULL, unpublished_by = NULL, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ?";
                psPosting = conn.prepareStatement(sqlUpPost);
                psPosting.setString(1, reqTitle);
                psPosting.setString(2, channel != null ? channel : "CAREER_PORTAL");
                psPosting.setInt(3, nextVersion);
                psPosting.setString(4, actorId);
                psPosting.setString(5, postingId);
                psPosting.executeUpdate();
            } else {
                String sqlInPost = "INSERT INTO job_postings (id, requisition_id, title, platform, status, " +
                        "current_version, published_at, published_by, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 'PUBLISHED', ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                psPosting = conn.prepareStatement(sqlInPost);
                psPosting.setString(1, postingId);
                psPosting.setString(2, requisitionId);
                psPosting.setString(3, reqTitle);
                psPosting.setString(4, channel != null ? channel : "CAREER_PORTAL");
                psPosting.setInt(5, nextVersion);
                psPosting.setString(6, actorId);
                psPosting.executeUpdate();
            }

            // 4. Lưu trữ các bản xuất cũ (chuyển status thành ARCHIVED)
            String sqlArchive = "UPDATE job_post_publications SET status = 'ARCHIVED' WHERE requisition_id = ? AND status = 'PUBLISHED'";
            psArchivePrev = conn.prepareStatement(sqlArchive);
            psArchivePrev.setString(1, requisitionId);
            psArchivePrev.executeUpdate();

            // 5. Ghi lại bản xuất và thời điểm xuất bản (job_post_publications)
            String pubId = SecurityUtil.generateUUID();
            String sqlInsertPub = "INSERT INTO job_post_publications " +
                    "(id, requisition_id, job_posting_id, version_number, version_label, channel, status, " +
                    "published_at, published_by, published_by_name, snapshot_title, snapshot_data, change_summary, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'PUBLISHED', CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            psInsertPub = conn.prepareStatement(sqlInsertPub);
            psInsertPub.setString(1, pubId);
            psInsertPub.setString(2, requisitionId);
            psInsertPub.setString(3, postingId);
            psInsertPub.setInt(4, nextVersion);
            psInsertPub.setString(5, versionLabel);
            psInsertPub.setString(6, channel != null ? channel : "CAREER_PORTAL");
            psInsertPub.setString(7, actorId);
            psInsertPub.setString(8, actorName);
            psInsertPub.setString(9, reqTitle);
            psInsertPub.setString(10, snapshotJson);
            psInsertPub.setString(11, changeSummary);
            psInsertPub.executeUpdate();

            conn.commit();

            JobPostPublication pub = new JobPostPublication();
            pub.setId(pubId);
            pub.setRequisitionId(requisitionId);
            pub.setJobPostingId(postingId);
            pub.setVersionNumber(nextVersion);
            pub.setVersionLabel(versionLabel);
            pub.setChannel(channel);
            pub.setStatus("PUBLISHED");
            pub.setPublishedBy(actorId);
            pub.setPublishedByName(actorName);
            pub.setSnapshotTitle(reqTitle);
            pub.setSnapshotData(snapshotJson);
            pub.setChangeSummary(changeSummary);
            return pub;

        } catch (SQLException | RuntimeException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            closeQuietly(rsCheck);
            closeQuietly(rsMaxVer);
            closeQuietly(psCheck);
            closeQuietly(psMaxVer);
            closeQuietly(psPosting);
            closeQuietly(psArchivePrev);
            closeQuietly(psInsertPub);
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Gỡ tin đã xuất bản (Unpublish)
     */
    public boolean unpublish(String requisitionId, String actorId, String actorName, String reason) {
        Connection conn = null;
        PreparedStatement psPost = null;
        PreparedStatement psPub = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String sqlPost = "UPDATE job_postings SET status = 'UNPUBLISHED', unpublished_at = CURRENT_TIMESTAMP, " +
                    "unpublished_by = ?, updated_at = CURRENT_TIMESTAMP WHERE requisition_id = ?";
            psPost = conn.prepareStatement(sqlPost);
            psPost.setString(1, actorId);
            psPost.setString(2, requisitionId);
            int rowsPost = psPost.executeUpdate();

            String sqlPub = "UPDATE job_post_publications SET status = 'UNPUBLISHED', unpublished_at = CURRENT_TIMESTAMP, " +
                    "unpublished_by = ? WHERE requisition_id = ? AND status = 'PUBLISHED'";
            psPub = conn.prepareStatement(sqlPub);
            psPub.setString(1, actorId);
            psPub.setString(2, requisitionId);
            psPub.executeUpdate();

            conn.commit();
            return rowsPost > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            logger.log(Level.SEVERE, "Lỗi gỡ tin đăng", e);
            return false;
        } finally {
            closeQuietly(psPost);
            closeQuietly(psPub);
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Phê duyệt yêu cầu tuyển dụng để đủ điều kiện xuất bản
     */
    public boolean approveRequisition(String requisitionId, String actorId) {
        String sql = "UPDATE recruitment_requisitions SET status = 'OPEN', updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi duyệt yêu cầu tuyển dụng", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Lấy toàn bộ lịch sử bản xuất của một bài đăng ("Ghi lại bản xuất và thời điểm")
     */
    public List<JobPostPublication> getPublicationHistory(String requisitionId) {
        List<JobPostPublication> list = new ArrayList<>();
        String sql = "SELECT * FROM job_post_publications WHERE requisition_id = ? ORDER BY version_number DESC, published_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToPublication(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy lịch sử bản xuất", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy chi tiết một bản xuất theo ID
     */
    public JobPostPublication getPublicationById(String publicationId) {
        String sql = "SELECT * FROM job_post_publications WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, publicationId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToPublication(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy bản xuất theo ID: " + publicationId, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Lấy danh sách tin tuyển dụng công khai cho ứng viên (CHỈ TIN ĐÃ DUYỆT VÀ ĐANG XUẤT BẢN)
     */
    public List<JobPosting> findPublicPublishedPosts(String search, String deptId, int offset, int limit) {
        List<JobPosting> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.id AS req_id, r.code AS req_code, r.title AS req_title, r.position_title, " +
                "r.department_id, d.name AS department_name, r.headcount, r.recruitment_reason, " +
                "r.min_salary, r.max_salary, r.currency, r.deadline, r.job_description, r.job_requirements, " +
                "r.salary_explanation, r.status AS req_status, r.created_at AS req_created_at, " +
                "jp.id AS jp_id, jp.title AS jp_title, jp.platform AS jp_platform, jp.status AS jp_status, " +
                "jp.current_version AS jp_version, jp.published_at AS jp_published_at, jp.published_by AS jp_published_by, " +
                "u_pub.full_name AS published_by_name, " +
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count " +
                "FROM job_postings jp " +
                "JOIN recruitment_requisitions r ON jp.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN users u_pub ON jp.published_by = u_pub.id " +
                "WHERE jp.status = 'PUBLISHED' AND r.status IN ('APPROVED', 'OPEN') "
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(r.code) LIKE ? OR LOWER(r.title) LIKE ? OR LOWER(r.position_title) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (deptId != null && !deptId.trim().isEmpty()) {
            sql.append("AND r.department_id = ? ");
            params.add(deptId.trim());
        }

        sql.append("ORDER BY jp.published_at DESC");
        if (limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);
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
            while (rs.next()) {
                list.add(mapResultSetToPosting(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy tin tuyển dụng công khai", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public void incrementViews(String postingId) {
        String sql = "UPDATE job_postings SET views_count = views_count + 1 WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, postingId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        } finally {
            close(conn, ps);
        }
    }

    private JobPosting mapResultSetToPosting(ResultSet rs) throws SQLException {
        JobPosting jp = new JobPosting();
        jp.setId(rs.getString("jp_id"));
        jp.setRequisitionId(rs.getString("req_id"));
        jp.setTitle(rs.getString("jp_title") != null ? rs.getString("jp_title") : rs.getString("req_title"));
        jp.setPlatform(rs.getString("jp_platform"));
        jp.setStatus(rs.getString("jp_status"));
        jp.setCurrentVersion(rs.getInt("jp_version"));
        jp.setPublishedAt(rs.getTimestamp("jp_published_at"));
        jp.setPublishedBy(rs.getString("jp_published_by"));
        jp.setPublishedByName(rs.getString("published_by_name"));
        jp.setUnpublishedAt(rs.getTimestamp("jp_unpublished_at"));
        jp.setPublicUrl(rs.getString("public_url"));
        jp.setViewsCount(rs.getInt("views_count"));

        RecruitmentRequest req = new RecruitmentRequest();
        req.setId(rs.getString("req_id"));
        req.setCode(rs.getString("req_code"));
        req.setTitle(rs.getString("req_title"));
        req.setPositionTitle(rs.getString("position_title"));
        req.setDepartmentId(rs.getString("department_id"));
        req.setDepartmentName(rs.getString("department_name"));
        req.setHeadcount(rs.getInt("headcount"));
        req.setRecruitmentReason(rs.getString("recruitment_reason"));
        req.setMinSalary(rs.getBigDecimal("min_salary"));
        req.setMaxSalary(rs.getBigDecimal("max_salary"));
        req.setCurrency(rs.getString("currency"));
        req.setDeadline(rs.getDate("deadline"));
        req.setJobDescription(rs.getString("job_description"));
        req.setJobRequirements(rs.getString("job_requirements"));
        req.setSalaryExplanation(rs.getString("salary_explanation"));
        req.setStatus(rs.getString("req_status"));
        req.setCreatedAt(rs.getTimestamp("req_created_at"));

        jp.setRequisition(req);
        return jp;
    }

    private JobPostPublication mapResultSetToPublication(ResultSet rs) throws SQLException {
        JobPostPublication pub = new JobPostPublication();
        pub.setId(rs.getString("id"));
        pub.setRequisitionId(rs.getString("requisition_id"));
        pub.setJobPostingId(rs.getString("job_posting_id"));
        pub.setVersionNumber(rs.getInt("version_number"));
        pub.setVersionLabel(rs.getString("version_label"));
        pub.setChannel(rs.getString("channel"));
        pub.setStatus(rs.getString("status"));
        pub.setPublishedAt(rs.getTimestamp("published_at"));
        pub.setPublishedBy(rs.getString("published_by"));
        pub.setPublishedByName(rs.getString("published_by_name"));
        pub.setUnpublishedAt(rs.getTimestamp("unpublished_at"));
        pub.setUnpublishedBy(rs.getString("unpublished_by"));
        pub.setSnapshotTitle(rs.getString("snapshot_title"));
        pub.setSnapshotData(rs.getString("snapshot_data"));
        pub.setChangeSummary(rs.getString("change_summary"));
        pub.setCreatedAt(rs.getTimestamp("created_at"));
        return pub;
    }

    // =========================================================================
    // Các phương thức phục vụ Quản lý trạng thái & Tự động gỡ tin (KN-109 / Branch 1)
    // =========================================================================

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
                list.add(mapSimpleResultSet(rs));
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
     * Tự động gỡ các tin tuyển dụng đang đăng của yêu cầu tuyển dụng này khi kết thúc yêu cầu
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
        String sql = "INSERT INTO job_postings (id, requisition_id, title, platform, status, public_url, published_at) " +
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
            ps.setString(6, p.getPublicUrl() != null ? p.getPublicUrl() : p.getPostedUrl());
            ps.setTimestamp(7, p.getPublishedAt());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm tin tuyển dụng mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    private JobPosting mapSimpleResultSet(ResultSet rs) throws SQLException {
        JobPosting p = new JobPosting();
        p.setId(rs.getString("id"));
        p.setRequisitionId(rs.getString("requisition_id"));
        p.setTitle(rs.getString("title"));
        try { p.setPlatform(rs.getString("platform")); } catch (SQLException ignored) {}
        try { p.setStatus(rs.getString("status")); } catch (SQLException ignored) {}
        try { 
            String url = rs.getString("public_url");
            if (url == null) url = rs.getString("posted_url");
            p.setPublicUrl(url); 
        } catch (SQLException ignored) {}
        try { p.setPublishedAt(rs.getTimestamp("published_at")); } catch (SQLException ignored) {}
        try { p.setUnpublishedAt(rs.getTimestamp("unpublished_at")); } catch (SQLException ignored) {}
        try { p.setCreatedAt(rs.getTimestamp("created_at")); } catch (SQLException ignored) {}
        try { p.setUpdatedAt(rs.getTimestamp("updated_at")); } catch (SQLException ignored) {}
        return p;
    }

}
