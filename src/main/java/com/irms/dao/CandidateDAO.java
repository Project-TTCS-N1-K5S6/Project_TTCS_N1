package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.Candidate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CandidateDAO {
    private static final String CANDIDATE_COLUMNS =
            "id, full_name, email, phone, position_applied, source_name AS source,"
                    + " cv_original_name, created_at";

    public List<Candidate> findRecent(int limit) throws SQLException {
        String sql = "SELECT " + CANDIDATE_COLUMNS
                + " FROM candidates ORDER BY created_at DESC LIMIT ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Candidate> candidates = new ArrayList<Candidate>();
                while (resultSet.next()) {
                    candidates.add(mapCandidate(resultSet));
                }
                return candidates;
            }
        }
    }

    public List<Candidate> findDuplicates(String normalizedEmail, String normalizedPhone) throws SQLException {
        String sql = "SELECT " + CANDIDATE_COLUMNS
                + " FROM candidates WHERE email_normalized = ? OR phone_normalized = ?"
                + " ORDER BY created_at DESC";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedEmail);
            statement.setString(2, normalizedPhone);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Candidate> candidates = new ArrayList<Candidate>();
                while (resultSet.next()) {
                    candidates.add(mapCandidate(resultSet));
                }
                return candidates;
            }
        }
    }

    public CandidateFile findCvByCandidateId(String id) throws SQLException {
        String sql = "SELECT cv_storage_name FROM candidates WHERE id = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getString("cv_storage_name") == null) {
                    return null;
                }
                return new CandidateFile(resultSet.getString("cv_storage_name"));
            }
        }
    }

    public void insert(Candidate candidate, String normalizedEmail, String normalizedPhone,
                       String storageName, String contentType) throws SQLException {
        String sql = "INSERT INTO candidates (id, full_name, email, email_normalized, phone,"
                + " phone_normalized, position_applied, source_name, cv_original_name,"
                + " cv_storage_name, cv_content_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, candidate.getId());
            statement.setString(2, candidate.getName());
            statement.setString(3, candidate.getEmail());
            statement.setString(4, normalizedEmail);
            statement.setString(5, candidate.getPhone());
            statement.setString(6, normalizedPhone);
            statement.setString(7, candidate.getPosition());
            statement.setString(8, candidate.getSource());
            statement.setString(9, candidate.getCvName());
            statement.setString(10, storageName);
            statement.setString(11, contentType);
            statement.executeUpdate();
        }
    }

    public void insertHistory(String action, String candidateId, String name, String email,
                              String phone, String source, boolean hasCv, String details,
                              String clientIp) throws SQLException {
        String sql = "INSERT INTO candidate_history (action, candidate_id, full_name, email, phone,"
                + " source_name, has_cv, details, client_ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, action);
            statement.setString(2, candidateId);
            statement.setString(3, name);
            statement.setString(4, email);
            statement.setString(5, phone);
            statement.setString(6, source);
            statement.setBoolean(7, hasCv);
            statement.setString(8, details);
            statement.setString(9, clientIp);
            statement.executeUpdate();
        }
    }

    private Candidate mapCandidate(ResultSet resultSet) throws SQLException {
        return new Candidate(
                resultSet.getString("id"),
                resultSet.getString("full_name"),
                resultSet.getString("email"),
                resultSet.getString("phone"),
                resultSet.getString("position_applied"),
                resultSet.getString("source"),
                resultSet.getString("cv_original_name"),
                resultSet.getTimestamp("created_at"));
    }

    public static class CandidateFile {
        private final String storageName;

        public CandidateFile(String storageName) {
            this.storageName = storageName;
        }

        public String getStorageName() { return storageName; }
    }
}
