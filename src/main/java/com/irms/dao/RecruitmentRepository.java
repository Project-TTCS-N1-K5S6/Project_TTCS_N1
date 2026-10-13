package com.irms.dao;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RecruitmentRepository {
    private final DataSource dataSource;

    public RecruitmentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Job> findAllJobs() throws SQLException {
        String sql = "SELECT id, title, status, description, requirements " +
            "FROM recruitment_jobs ORDER BY title";
        List<Job> jobs = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                jobs.add(new Job(
                    result.getString("id"),
                    result.getString("title"),
                    result.getString("status"),
                    result.getString("description"),
                    result.getString("requirements")
                ));
            }
        }
        return jobs;
    }

    public List<RecruitmentCopy> findAllCopies() throws SQLException {
        String sql = "SELECT id, source_job_id, source_title, title, description, " +
            "requirements, status, created_at FROM recruitment_copies " +
            "ORDER BY created_at DESC, id DESC";
        List<RecruitmentCopy> copies = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                copies.add(mapCopy(result));
            }
        }
        return copies;
    }

    public RecruitmentCopy createCopy(String sourceJobId, String title, String description,
                                      String requirements) throws SQLException {
        String id = UUID.randomUUID().toString();
        String insertSql = "INSERT INTO recruitment_copies " +
            "(id, source_job_id, source_title, title, description, requirements, status) " +
            "SELECT ?, id, title, ?, ?, ?, 'Nháp' FROM recruitment_jobs WHERE id = ?";
        String selectSql = "SELECT id, source_job_id, source_title, title, description, " +
            "requirements, status, created_at FROM recruitment_copies WHERE id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setString(1, id);
            insert.setString(2, title);
            insert.setString(3, description);
            insert.setString(4, requirements);
            insert.setString(5, sourceJobId);

            if (insert.executeUpdate() == 0) {
                return null;
            }

            try (PreparedStatement select = connection.prepareStatement(selectSql)) {
                select.setString(1, id);
                try (ResultSet result = select.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("Created recruitment copy could not be read.");
                    }
                    return mapCopy(result);
                }
            }
        }
    }

    private RecruitmentCopy mapCopy(ResultSet result) throws SQLException {
        Timestamp createdAt = result.getTimestamp("created_at");
        Instant createdAtInstant = createdAt == null ? null : createdAt.toInstant();
        return new RecruitmentCopy(
            result.getString("id"),
            result.getString("source_job_id"),
            result.getString("source_title"),
            result.getString("title"),
            result.getString("description"),
            result.getString("requirements"),
            result.getString("status"),
            createdAtInstant == null ? null : createdAtInstant.toString()
        );
    }

    public static class Job {
        public final String id;
        public final String title;
        public final String status;
        public final String description;
        public final String requirements;

        public Job(String id, String title, String status, String description, String requirements) {
            this.id = id;
            this.title = title;
            this.status = status;
            this.description = description;
            this.requirements = requirements;
        }
    }

    public static class RecruitmentCopy {
        public final String id;
        public final String sourceJobId;
        public final String sourceTitle;
        public final String title;
        public final String description;
        public final String requirements;
        public final String status;
        public final String createdAt;

        public RecruitmentCopy(String id, String sourceJobId, String sourceTitle, String title,
                               String description, String requirements, String status, String createdAt) {
            this.id = id;
            this.sourceJobId = sourceJobId;
            this.sourceTitle = sourceTitle;
            this.title = title;
            this.description = description;
            this.requirements = requirements;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
}
