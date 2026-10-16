package com.irms.dao;

import com.google.gson.Gson;
import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                    "job_description LONGTEXT NULL, " +
                    "work_location_id VARCHAR(36) NULL, " +
                    "work_location VARCHAR(255) NULL, " +
                    "work_mode_id VARCHAR(36) NULL, " +
                    "work_mode VARCHAR(100) NULL, " +
                    "application_deadline DATE NULL, " +
                    "is_salary_visible BOOLEAN NOT NULL DEFAULT TRUE, " +
                    "min_salary DECIMAL(15, 2) NULL, " +
                    "max_salary DECIMAL(15, 2) NULL, " +
                    "currency VARCHAR(10) NOT NULL DEFAULT 'VND', " +
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

            // 3. Đảm bảo các cột mới phục vụ Tạo nhanh bài đăng (KN-204, KN-205, KN-206) tồn tại nếu bảng đã có sẵn
            ensureJobPostingColumns(conn);

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể tự động khởi tạo bảng job_postings / job_post_publications: " + e.getMessage());
        } finally {
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private void ensureJobPostingColumns(Connection conn) {
        Statement stmt = null;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            stmt = conn.createStatement();
            addColumnIfNotExists(meta, stmt, "job_postings", "current_version", "INT NOT NULL DEFAULT 1");
            addColumnIfNotExists(meta, stmt, "job_postings", "published_by", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "unpublished_by", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "public_url", "VARCHAR(255) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "views_count", "INT NOT NULL DEFAULT 0");
            addColumnIfNotExists(meta, stmt, "job_postings", "job_description", "LONGTEXT NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "description", "LONGTEXT NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "department_id", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "work_location_id", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "location_id", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "work_location", "VARCHAR(255) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "work_mode_id", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "work_mode", "VARCHAR(100) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "work_type", "VARCHAR(100) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "application_deadline", "DATE NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "expired_at", "TIMESTAMP NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "is_salary_visible", "BOOLEAN NOT NULL DEFAULT TRUE");
            addColumnIfNotExists(meta, stmt, "job_postings", "min_salary", "DECIMAL(15, 2) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "max_salary", "DECIMAL(15, 2) NULL");
            addColumnIfNotExists(meta, stmt, "job_postings", "currency", "VARCHAR(10) NOT NULL DEFAULT 'VND'");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể kiểm tra/cập nhật cột cho job_postings: " + e.getMessage());
        } finally {
            closeQuietly(stmt);
        }
    }

    private void addColumnIfNotExists(DatabaseMetaData meta, Statement stmt, String tableName, String columnName, String columnDef) {
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                try (ResultSet rsUpper = meta.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                    if (!rsUpper.next()) {
                        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef;
                        stmt.executeUpdate(sql);
                    }
                }
            }
        } catch (Exception ignored) {
            try {
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
            } catch (SQLException ignored2) {}
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
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count, " +
                "jp.job_description AS jp_job_description, jp.work_location_id AS jp_work_location_id, " +
                "jp.work_location AS jp_work_location, jp.work_mode_id AS jp_work_mode_id, " +
                "jp.work_mode AS jp_work_mode, jp.application_deadline AS jp_deadline, " +
                "jp.is_salary_visible AS jp_salary_visible, jp.min_salary AS jp_min_salary, " +
                "jp.max_salary AS jp_max_salary, jp.currency AS jp_currency " +
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
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count, " +
                "jp.job_description AS jp_job_description, jp.work_location_id AS jp_work_location_id, " +
                "jp.work_location AS jp_work_location, jp.work_mode_id AS jp_work_mode_id, " +
                "jp.work_mode AS jp_work_mode, jp.application_deadline AS jp_deadline, " +
                "jp.is_salary_visible AS jp_salary_visible, jp.min_salary AS jp_min_salary, " +
                "jp.max_salary AS jp_max_salary, jp.currency AS jp_currency " +
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

    public JobPosting findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        JobPosting byReq = findByRequisitionId(id);
        if (byReq != null && byReq.getId() != null) {
            return byReq;
        }

        String sql = "SELECT r.id AS req_id, r.code AS req_code, r.title AS req_title, r.position_title, " +
                "r.department_id, d.name AS department_name, r.headcount, r.recruitment_reason, " +
                "r.min_salary, r.max_salary, r.currency, r.deadline, r.job_description, r.job_requirements, " +
                "r.salary_explanation, r.status AS req_status, r.created_at AS req_created_at, " +
                "jp.id AS jp_id, jp.title AS jp_title, jp.platform AS jp_platform, " +
                "COALESCE(jp.status, 'DRAFT') AS jp_status, " +
                "COALESCE(jp.current_version, 0) AS jp_version, " +
                "jp.published_at AS jp_published_at, jp.published_by AS jp_published_by, " +
                "u_pub.full_name AS published_by_name, " +
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count, " +
                "jp.job_description AS jp_job_description, jp.work_location_id AS jp_work_location_id, " +
                "jp.work_location AS jp_work_location, jp.work_mode_id AS jp_work_mode_id, " +
                "jp.work_mode AS jp_work_mode, jp.application_deadline AS jp_deadline, " +
                "jp.is_salary_visible AS jp_salary_visible, jp.min_salary AS jp_min_salary, " +
                "jp.max_salary AS jp_max_salary, jp.currency AS jp_currency " +
                "FROM job_postings jp " +
                "LEFT JOIN recruitment_requisitions r ON jp.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN users u_pub ON jp.published_by = u_pub.id " +
                "WHERE jp.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToPosting(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm bài đăng theo id: " + id, e);
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
                    "unpublished_by = ?, updated_at = CURRENT_TIMESTAMP WHERE requisition_id = ? AND status = 'PUBLISHED'";
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
     * KN-219, KN-222, KN-223: Tìm kiếm và lọc danh sách việc làm công khai
     * - Công khai không cần đăng nhập
     * - Chỉ trả về tin đã xuất bản (status = 'PUBLISHED') và còn hạn (expired_at >= NOW())
     * - Tìm kiếm từ khóa (q / keyword) case-insensitive trên title và description
     * - Lọc theo phòng ban (department_id), địa điểm (location_id), hình thức làm việc (work_type)
     * - Sắp xếp mặc định mới nhất lên đầu (COALESCE(published_at, created_at) DESC)
     * - Phân trang theo offset và limit
     */
    public List<JobPosting> findPublicJobs(String keyword, String departmentId, String locationId, String workType, String sortBy, int offset, int limit) {
        List<JobPosting> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.id AS req_id, r.code AS req_code, r.title AS req_title, r.position_title, " +
                "COALESCE(jp.department_id, r.department_id) AS department_id, " +
                "COALESCE(jp.department_id, r.department_id) AS jp_department_id, " +
                "d.name AS department_name, r.headcount, r.recruitment_reason, " +
                "COALESCE(jp.min_salary, r.min_salary) AS min_salary, " +
                "COALESCE(jp.max_salary, r.max_salary) AS max_salary, " +
                "COALESCE(jp.currency, r.currency, 'VND') AS currency, " +
                "COALESCE(jp.application_deadline, r.deadline) AS deadline, " +
                "COALESCE(jp.description, jp.job_description, r.job_description) AS job_description, " +
                "r.job_requirements, r.salary_explanation, r.status AS req_status, r.created_at AS req_created_at, " +
                "jp.id AS jp_id, jp.title AS jp_title, jp.platform AS jp_platform, jp.status AS jp_status, " +
                "jp.current_version AS jp_version, jp.published_at AS jp_published_at, jp.published_by AS jp_published_by, " +
                "u_pub.full_name AS published_by_name, " +
                "jp.unpublished_at AS jp_unpublished_at, jp.public_url, jp.views_count, " +
                "COALESCE(jp.description, jp.job_description, r.job_description) AS jp_description, " +
                "COALESCE(jp.job_description, jp.description, r.job_description) AS jp_job_description, " +
                "COALESCE(jp.location_id, jp.work_location_id, r.work_location_id) AS jp_location_id, " +
                "COALESCE(jp.work_location_id, jp.location_id, r.work_location_id) AS jp_work_location_id, " +
                "COALESCE(jp.work_location, loc_sc.value, 'Toàn quốc') AS jp_work_location, " +
                "COALESCE(jp.work_mode_id, r.work_mode_id) AS jp_work_mode_id, " +
                "COALESCE(jp.work_type, jp.work_mode, mode_sc.value, 'Toàn thời gian') AS jp_work_type, " +
                "COALESCE(jp.work_mode, jp.work_type, mode_sc.value, 'Toàn thời gian') AS jp_work_mode, " +
                "COALESCE(jp.application_deadline, r.deadline) AS jp_deadline, " +
                "COALESCE(jp.expired_at, TIMESTAMP(CONCAT(COALESCE(jp.application_deadline, r.deadline), ' 23:59:59'))) AS jp_expired_at, " +
                "jp.is_salary_visible AS jp_salary_visible, " +
                "COALESCE(jp.min_salary, r.min_salary) AS jp_min_salary, " +
                "COALESCE(jp.max_salary, r.max_salary) AS jp_max_salary, " +
                "COALESCE(jp.currency, r.currency, 'VND') AS jp_currency " +
                "FROM job_postings jp " +
                "LEFT JOIN recruitment_requisitions r ON jp.requisition_id = r.id " +
                "LEFT JOIN departments d ON COALESCE(jp.department_id, r.department_id) = d.id " +
                "LEFT JOIN shared_catalogs loc_sc ON COALESCE(jp.location_id, jp.work_location_id, r.work_location_id) = loc_sc.id " +
                "LEFT JOIN shared_catalogs mode_sc ON COALESCE(jp.work_mode_id, r.work_mode_id) = mode_sc.id " +
                "LEFT JOIN users u_pub ON jp.published_by = u_pub.id " +
                "WHERE LOWER(jp.status) = 'published' " +
                "AND ( " +
                "    (jp.expired_at IS NOT NULL AND jp.expired_at >= CURRENT_TIMESTAMP) " +
                "    OR (jp.expired_at IS NULL AND jp.application_deadline IS NOT NULL AND jp.application_deadline >= CURRENT_DATE()) " +
                "    OR (jp.expired_at IS NULL AND jp.application_deadline IS NULL AND r.deadline IS NOT NULL AND r.deadline >= CURRENT_DATE()) " +
                "    OR (jp.expired_at IS NULL AND jp.application_deadline IS NULL AND r.deadline IS NULL) " +
                ") " +
                "AND (r.status IS NULL OR r.status IN ('APPROVED', 'OPEN')) "
        );

        List<Object> params = new ArrayList<>();

        // KN-222: Tìm kiếm từ khóa case-insensitive trên tiêu đề và mô tả
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(jp.title) LIKE ? OR LOWER(COALESCE(jp.description, jp.job_description, r.job_description, '')) LIKE ? OR (r.title IS NOT NULL AND LOWER(r.title) LIKE ?)) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        // KN-223: Lọc theo phòng ban (department_id)
        if (departmentId != null && !departmentId.trim().isEmpty() && !"ALL".equalsIgnoreCase(departmentId)) {
            sql.append("AND (jp.department_id = ? OR r.department_id = ?) ");
            params.add(departmentId.trim());
            params.add(departmentId.trim());
        }

        // KN-223: Lọc theo địa điểm (location_id)
        if (locationId != null && !locationId.trim().isEmpty() && !"ALL".equalsIgnoreCase(locationId)) {
            sql.append("AND (jp.location_id = ? OR jp.work_location_id = ? OR r.work_location_id = ? OR LOWER(COALESCE(jp.work_location, loc_sc.value, '')) LIKE ?) ");
            params.add(locationId.trim());
            params.add(locationId.trim());
            params.add(locationId.trim());
            params.add("%" + locationId.trim().toLowerCase() + "%");
        }

        // KN-223: Lọc theo hình thức làm việc (work_type)
        if (workType != null && !workType.trim().isEmpty() && !"ALL".equalsIgnoreCase(workType)) {
            sql.append("AND (jp.work_type = ? OR jp.work_mode = ? OR jp.work_mode_id = ? OR r.work_mode_id = ? OR LOWER(COALESCE(jp.work_type, jp.work_mode, mode_sc.value, '')) LIKE ?) ");
            params.add(workType.trim());
            params.add(workType.trim());
            params.add(workType.trim());
            params.add(workType.trim());
            params.add("%" + workType.trim().toLowerCase() + "%");
        }

        // Sắp xếp
        if ("deadline".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY COALESCE(jp.expired_at, jp.application_deadline, r.deadline) ASC, jp.id DESC");
        } else if ("title_asc".equalsIgnoreCase(sortBy)) {
            sql.append(" ORDER BY jp.title ASC, jp.id DESC");
        } else {
            // Mặc định mới nhất lên đầu (KN-219)
            sql.append(" ORDER BY COALESCE(jp.published_at, jp.created_at) DESC, jp.id DESC");
        }

        // Phân trang
        if (limit > 0 && limit < Integer.MAX_VALUE) {
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
            logger.log(Level.SEVERE, "Lỗi tìm kiếm danh sách việc làm công khai", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countPublicJobs(String keyword, String departmentId, String locationId, String workType) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM job_postings jp " +
                "LEFT JOIN recruitment_requisitions r ON jp.requisition_id = r.id " +
                "LEFT JOIN shared_catalogs loc_sc ON COALESCE(jp.location_id, jp.work_location_id, r.work_location_id) = loc_sc.id " +
                "LEFT JOIN shared_catalogs mode_sc ON COALESCE(jp.work_mode_id, r.work_mode_id) = mode_sc.id " +
                "WHERE LOWER(jp.status) = 'published' " +
                "AND ( " +
                "    (jp.expired_at IS NOT NULL AND jp.expired_at >= CURRENT_TIMESTAMP) " +
                "    OR (jp.expired_at IS NULL AND jp.application_deadline IS NOT NULL AND jp.application_deadline >= CURRENT_DATE()) " +
                "    OR (jp.expired_at IS NULL AND jp.application_deadline IS NULL AND r.deadline IS NOT NULL AND r.deadline >= CURRENT_DATE()) " +
                "    OR (jp.expired_at IS NULL AND jp.application_deadline IS NULL AND r.deadline IS NULL) " +
                ") " +
                "AND (r.status IS NULL OR r.status IN ('APPROVED', 'OPEN')) "
        );

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(jp.title) LIKE ? OR LOWER(COALESCE(jp.description, jp.job_description, r.job_description, '')) LIKE ? OR (r.title IS NOT NULL AND LOWER(r.title) LIKE ?)) ");
            String term = "%" + keyword.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (departmentId != null && !departmentId.trim().isEmpty() && !"ALL".equalsIgnoreCase(departmentId)) {
            sql.append("AND (jp.department_id = ? OR r.department_id = ?) ");
            params.add(departmentId.trim());
            params.add(departmentId.trim());
        }

        if (locationId != null && !locationId.trim().isEmpty() && !"ALL".equalsIgnoreCase(locationId)) {
            sql.append("AND (jp.location_id = ? OR jp.work_location_id = ? OR r.work_location_id = ? OR LOWER(COALESCE(jp.work_location, loc_sc.value, '')) LIKE ?) ");
            params.add(locationId.trim());
            params.add(locationId.trim());
            params.add(locationId.trim());
            params.add("%" + locationId.trim().toLowerCase() + "%");
        }

        if (workType != null && !workType.trim().isEmpty() && !"ALL".equalsIgnoreCase(workType)) {
            sql.append("AND (jp.work_type = ? OR jp.work_mode = ? OR jp.work_mode_id = ? OR r.work_mode_id = ? OR LOWER(COALESCE(jp.work_type, jp.work_mode, mode_sc.value, '')) LIKE ?) ");
            params.add(workType.trim());
            params.add(workType.trim());
            params.add(workType.trim());
            params.add(workType.trim());
            params.add("%" + workType.trim().toLowerCase() + "%");
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
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số lượng việc làm công khai", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Lấy danh sách tin tuyển dụng công khai cho ứng viên (Tương thích ngược)
     */
    public List<JobPosting> findPublicPublishedPosts(String search, String deptId, int offset, int limit) {
        return findPublicJobs(search, deptId, null, null, "newest", offset, limit);
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

    /**
     * [KN-265]: Tự động gỡ các tin tuyển dụng công khai đã quá hạn nhận hồ sơ.
     * Cập nhật trạng thái của cả job_postings và job_post_publications sang UNPUBLISHED trong một Transaction.
     */
    public int unpublishExpiredPosts() throws SQLException {
        String expiredPostingIds = "SELECT expired_posts.id FROM (" +
                "SELECT jp.id FROM job_postings jp " +
                "LEFT JOIN recruitment_requisitions r ON jp.requisition_id = r.id " +
                "WHERE jp.status = 'PUBLISHED' AND (" +
                "(jp.expired_at IS NOT NULL AND jp.expired_at < CURRENT_TIMESTAMP) OR " +
                "(jp.expired_at IS NULL AND COALESCE(jp.application_deadline, r.deadline) < CURRENT_DATE)" +
                ")" +
                ") expired_posts";
        String sqlPublications = "UPDATE job_post_publications SET status = 'UNPUBLISHED', " +
                "unpublished_at = CURRENT_TIMESTAMP, unpublished_by = NULL " +
                "WHERE status = 'PUBLISHED' AND job_posting_id IN (" + expiredPostingIds + ")";
        String sqlPostings = "UPDATE job_postings SET status = 'UNPUBLISHED', " +
                "unpublished_at = CURRENT_TIMESTAMP, unpublished_by = NULL, updated_at = CURRENT_TIMESTAMP " +
                "WHERE status = 'PUBLISHED' AND id IN (" + expiredPostingIds + ")";

        Connection conn = null;
        PreparedStatement psPublications = null;
        PreparedStatement psPostings = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psPublications = conn.prepareStatement(sqlPublications);
            psPublications.executeUpdate();

            psPostings = conn.prepareStatement(sqlPostings);
            int unpublishedCount = psPostings.executeUpdate();

            conn.commit();
            return unpublishedCount;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            logger.log(Level.SEVERE, "Lỗi tự động gỡ các tin tuyển dụng đã quá hạn", e);
            throw e;
        } finally {
            closeQuietly(psPublications);
            closeQuietly(psPostings);
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
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

        try { jp.setJobDescription(rs.getString("jp_job_description")); } catch (SQLException ignored) {}
        try { jp.setDescription(rs.getString("jp_description")); } catch (SQLException ignored) {}
        try { jp.setDepartmentId(rs.getString("jp_department_id")); } catch (SQLException ignored) {}
        try { jp.setWorkLocationId(rs.getString("jp_work_location_id")); } catch (SQLException ignored) {}
        try { jp.setLocationId(rs.getString("jp_location_id")); } catch (SQLException ignored) {}
        try { jp.setWorkLocation(rs.getString("jp_work_location")); } catch (SQLException ignored) {}
        try { jp.setWorkModeId(rs.getString("jp_work_mode_id")); } catch (SQLException ignored) {}
        try { jp.setWorkMode(rs.getString("jp_work_mode")); } catch (SQLException ignored) {}
        try { jp.setWorkType(rs.getString("jp_work_type")); } catch (SQLException ignored) {}
        try { jp.setApplicationDeadline(rs.getDate("jp_deadline")); } catch (SQLException ignored) {}
        try { jp.setExpiredAt(rs.getTimestamp("jp_expired_at")); } catch (SQLException ignored) {}
        try { jp.setSalaryVisible(rs.getBoolean("jp_salary_visible")); } catch (SQLException ignored) {}
        try { jp.setMinSalary(rs.getBigDecimal("jp_min_salary")); } catch (SQLException ignored) {}
        try { jp.setMaxSalary(rs.getBigDecimal("jp_max_salary")); } catch (SQLException ignored) {}
        try { jp.setCurrency(rs.getString("jp_currency")); } catch (SQLException ignored) {}

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
        String sql = "INSERT INTO job_postings (id, requisition_id, title, platform, status, public_url, published_at, " +
                     "job_description, work_location_id, work_location, work_mode_id, work_mode, " +
                     "application_deadline, is_salary_visible, min_salary, max_salary, currency) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            ps.setString(8, p.getJobDescription());
            ps.setString(9, p.getWorkLocationId());
            ps.setString(10, p.getWorkLocation());
            ps.setString(11, p.getWorkModeId());
            ps.setString(12, p.getWorkMode());
            ps.setDate(13, p.getApplicationDeadline());
            ps.setBoolean(14, p.isSalaryVisible());
            ps.setBigDecimal(15, p.getMinSalary());
            ps.setBigDecimal(16, p.getMaxSalary());
            ps.setString(17, p.getCurrency() != null ? p.getCurrency() : "VND");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm tin tuyển dụng mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * KN-207, KN-208: Tạo nhanh bài đăng từ yêu cầu tuyển dụng đã duyệt
     * Kiểm tra trạng thái Đã duyệt, phòng tránh trùng lặp, ghi bản xuất nếu xuất bản ngay
     */
    public JobPosting createQuickPosting(JobPosting posting, String actorId, String actorName) throws SQLException {
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psInsert = null;
        ResultSet rsCheck = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 1. Kiểm tra trạng thái yêu cầu tuyển dụng nguồn (KN-208: BẮT BUỘC ĐÃ DUYỆT - APPROVED hoặc OPEN)
            String sqlCheck = "SELECT status, title, code FROM recruitment_requisitions WHERE id = ?";
            psCheck = conn.prepareStatement(sqlCheck);
            psCheck.setString(1, posting.getRequisitionId());
            rsCheck = psCheck.executeQuery();
            if (!rsCheck.next()) {
                throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng nguồn có ID: " + posting.getRequisitionId());
            }
            String reqStatus = rsCheck.getString("status");
            if (reqStatus == null || (!"APPROVED".equalsIgnoreCase(reqStatus.trim()) && !"OPEN".equalsIgnoreCase(reqStatus.trim()))) {
                throw new IllegalStateException("Chỉ những yêu cầu tuyển dụng ở trạng thái Đã duyệt mới được phép tạo bài đăng! Trạng thái hiện tại: " 
                        + (reqStatus != null ? reqStatus : "Chưa xác định"));
            }

            // 2. Phòng tránh tạo bài đăng trùng lặp do gửi yêu cầu liên tiếp (Idempotency)
            String dupSql = "SELECT id FROM job_postings WHERE requisition_id = ? AND title = ? AND created_at >= NOW() - INTERVAL 5 SECOND";
            try (PreparedStatement psDup = conn.prepareStatement(dupSql)) {
                psDup.setString(1, posting.getRequisitionId());
                psDup.setString(2, posting.getTitle());
                try (ResultSet rsDup = psDup.executeQuery()) {
                    if (rsDup.next()) {
                        posting.setId(rsDup.getString("id"));
                        conn.commit();
                        return posting;
                    }
                }
            } catch (Exception ignored) {}

            // 3. Chuẩn bị định danh duy nhất (UUID)
            if (posting.getId() == null || posting.getId().trim().isEmpty()) {
                posting.setId(SecurityUtil.generateUUID());
            }

            // 4. Lưu bài đăng vào cơ sở dữ liệu
            String insertSql = "INSERT INTO job_postings (" +
                    "id, requisition_id, title, platform, status, current_version, " +
                    "job_description, work_location_id, work_location, work_mode_id, work_mode, " +
                    "application_deadline, is_salary_visible, min_salary, max_salary, currency, " +
                    "published_at, published_by, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            psInsert = conn.prepareStatement(insertSql);
            psInsert.setString(1, posting.getId());
            psInsert.setString(2, posting.getRequisitionId());
            psInsert.setString(3, posting.getTitle());
            psInsert.setString(4, posting.getPlatform() != null ? posting.getPlatform() : "CAREER_PORTAL");
            psInsert.setString(5, posting.getStatus() != null ? posting.getStatus() : "PUBLISHED");
            psInsert.setInt(6, posting.getCurrentVersion() > 0 ? posting.getCurrentVersion() : 1);
            psInsert.setString(7, posting.getJobDescription());
            psInsert.setString(8, posting.getWorkLocationId());
            psInsert.setString(9, posting.getWorkLocation());
            psInsert.setString(10, posting.getWorkModeId());
            psInsert.setString(11, posting.getWorkMode());
            psInsert.setDate(12, posting.getApplicationDeadline());
            psInsert.setBoolean(13, posting.isSalaryVisible());
            psInsert.setBigDecimal(14, posting.getMinSalary());
            psInsert.setBigDecimal(15, posting.getMaxSalary());
            psInsert.setString(16, posting.getCurrency() != null ? posting.getCurrency() : "VND");

            if ("PUBLISHED".equalsIgnoreCase(posting.getStatus())) {
                psInsert.setTimestamp(17, new java.sql.Timestamp(System.currentTimeMillis()));
                psInsert.setString(18, actorId);
            } else {
                psInsert.setNull(17, java.sql.Types.TIMESTAMP);
                psInsert.setString(18, actorId);
            }

            psInsert.executeUpdate();

            // 5. Nếu xuất bản ngay, ghi nhận bản xuất và thời điểm (KN-120 compliance)
            if ("PUBLISHED".equalsIgnoreCase(posting.getStatus())) {
                String pubId = SecurityUtil.generateUUID();
                String pubSql = "INSERT INTO job_post_publications " +
                        "(id, requisition_id, job_posting_id, version_number, version_label, channel, status, " +
                        "published_at, published_by, published_by_name, snapshot_title, snapshot_data, change_summary, created_at) " +
                        "VALUES (?, ?, ?, 1, 'Bản xuất #1', ?, 'PUBLISHED', CURRENT_TIMESTAMP, ?, ?, ?, ?, 'Tạo nhanh bài đăng tuyển dụng', CURRENT_TIMESTAMP)";
                try (PreparedStatement psPub = conn.prepareStatement(pubSql)) {
                    psPub.setString(1, pubId);
                    psPub.setString(2, posting.getRequisitionId());
                    psPub.setString(3, posting.getId());
                    psPub.setString(4, posting.getPlatform());
                    psPub.setString(5, actorId);
                    psPub.setString(6, actorName != null ? actorName : "Hệ thống");
                    psPub.setString(7, posting.getTitle());

                    Map<String, Object> snap = new HashMap<>();
                    snap.put("id", posting.getId());
                    snap.put("title", posting.getTitle());
                    snap.put("jobDescription", posting.getJobDescription());
                    snap.put("workLocation", posting.getWorkLocation());
                    snap.put("workMode", posting.getWorkMode());
                    snap.put("deadline", posting.getApplicationDeadline() != null ? posting.getApplicationDeadline().toString() : null);
                    snap.put("isSalaryVisible", posting.isSalaryVisible());
                    snap.put("salaryRange", posting.getFormattedSalaryRange());
                    psPub.setString(8, new Gson().toJson(snap));

                    psPub.executeUpdate();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Không thể tự động ghi lịch sử bản xuất: " + e.getMessage());
                }
            }

            conn.commit();
            return posting;
        } catch (SQLException | RuntimeException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            closeQuietly(rsCheck);
            closeQuietly(psCheck);
            closeQuietly(psInsert);
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Lấy danh sách các yêu cầu tuyển dụng đã duyệt (APPROVED hoặc OPEN) để phục vụ tạo nhanh bài đăng
     */
    public List<RecruitmentRequest> findApprovedRequisitionsForPosting() {
        List<RecruitmentRequest> list = new ArrayList<>();
        String sql = "SELECT r.id, r.code, r.title, r.position_title, r.department_id, d.name AS department_name, " +
                "r.headcount, r.recruitment_reason, r.min_salary, r.max_salary, r.currency, r.deadline, " +
                "r.job_description, r.job_requirements, r.work_location_id, r.work_mode_id, r.status, r.created_at " +
                "FROM recruitment_requisitions r " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "WHERE r.status IN ('APPROVED', 'OPEN') " +
                "ORDER BY r.created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                RecruitmentRequest req = new RecruitmentRequest();
                req.setId(rs.getString("id"));
                req.setCode(rs.getString("code"));
                req.setTitle(rs.getString("title"));
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
                req.setWorkLocationId(rs.getString("work_location_id"));
                req.setWorkModeId(rs.getString("work_mode_id"));
                req.setStatus(rs.getString("status"));
                list.add(req);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách yêu cầu đã duyệt cho bài đăng", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
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

        try { p.setJobDescription(rs.getString("job_description")); } catch (SQLException ignored) {}
        try { p.setWorkLocationId(rs.getString("work_location_id")); } catch (SQLException ignored) {}
        try { p.setWorkLocation(rs.getString("work_location")); } catch (SQLException ignored) {}
        try { p.setWorkModeId(rs.getString("work_mode_id")); } catch (SQLException ignored) {}
        try { p.setWorkMode(rs.getString("work_mode")); } catch (SQLException ignored) {}
        try { p.setApplicationDeadline(rs.getDate("application_deadline")); } catch (SQLException ignored) {}
        try { p.setSalaryVisible(rs.getBoolean("is_salary_visible")); } catch (SQLException ignored) {}
        try { p.setMinSalary(rs.getBigDecimal("min_salary")); } catch (SQLException ignored) {}
        try { p.setMaxSalary(rs.getBigDecimal("max_salary")); } catch (SQLException ignored) {}
        try { p.setCurrency(rs.getString("currency")); } catch (SQLException ignored) {}
        return p;
    }

}
