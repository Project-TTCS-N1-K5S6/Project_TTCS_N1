package com.irms.dao;

import com.irms.model.CandidateInteraction;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CandidateInteractionDAO extends BaseDAO {

    public List<CandidateInteraction> findByCandidate(String candidateId) {
        List<CandidateInteraction> events = new ArrayList<>();
        String sql =
                "SELECT event_type, title, description, actor_name, occurred_at FROM (" +
                " SELECT 'APPLICATION' AS event_type, 'Nộp hồ sơ' AS title, " +
                "        CONCAT('Ứng tuyển vị trí ', COALESCE(r.title, 'chưa xác định')) AS description, " +
                "        NULL AS actor_name, c.created_at AS occurred_at " +
                " FROM candidates c LEFT JOIN recruitment_requisitions r ON r.id = c.requisition_id " +
                " WHERE c.id = ? " +
                " UNION ALL " +
                " SELECT 'STAGE_CHANGED', 'Chuyển giai đoạn', a.description, u.full_name, a.created_at " +
                " FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id " +
                " WHERE a.entity_type = 'CANDIDATE' AND a.entity_id = ? " +
                "   AND a.action IN ('UPDATE_CANDIDATE_STATUS', 'CANDIDATE_WITHDRAWN') " +
                "   AND NOT EXISTS (SELECT 1 FROM candidate_interaction_events i " +
                "       WHERE i.candidate_id = a.entity_id AND i.event_type = 'STAGE_CHANGED' " +
                "         AND i.description = a.description) " +
                " UNION ALL " +
                " SELECT i.event_type, i.title, i.description, u.full_name, i.occurred_at " +
                " FROM candidate_interaction_events i LEFT JOIN users u ON u.id = i.actor_user_id " +
                " WHERE i.candidate_id = ? " +
                ") interactions ORDER BY occurred_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, candidateId);
            ps.setString(2, candidateId);
            ps.setString(3, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CandidateInteraction event = new CandidateInteraction();
                    event.setType(rs.getString("event_type"));
                    event.setTitle(rs.getString("title"));
                    event.setDescription(rs.getString("description"));
                    event.setActorName(rs.getString("actor_name"));
                    event.setOccurredAt(rs.getTimestamp("occurred_at"));
                    events.add(event);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể tải nhật ký tương tác ứng viên " + candidateId, e);
            throw new IllegalStateException("Không thể tải nhật ký tương tác ứng viên.", e);
        }
        return events;
    }

    public void record(String candidateId, String eventType, String description, String actorUserId,
                       Timestamp occurredAt) {
        String title;
        if ("INTERVIEW_SCHEDULED".equals(eventType)) {
            title = "Lịch phỏng vấn";
        } else if ("STAGE_CHANGED".equals(eventType)) {
            title = "Chuyển giai đoạn";
        } else {
            title = "Đánh giá";
        }
        String sql = "INSERT INTO candidate_interaction_events " +
                "(id, candidate_id, event_type, title, description, actor_user_id, occurred_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, candidateId);
            ps.setString(3, eventType);
            ps.setString(4, title);
            ps.setString(5, description);
            ps.setString(6, actorUserId);
            ps.setTimestamp(7, occurredAt);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Không thể lưu tương tác ứng viên.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể ghi nhật ký tương tác ứng viên " + candidateId, e);
            throw new IllegalStateException("Không thể ghi nhật ký tương tác ứng viên.", e);
        }
    }

    public void recordEmailSent(String candidateId, String subject) {
        String sql = "INSERT INTO candidate_interaction_events " +
                "(id, candidate_id, event_type, title, description, occurred_at) " +
                "VALUES (?, ?, 'EMAIL_SENT', 'Email đã gửi', ?, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, candidateId);
            ps.setString(3, subject);
            if (ps.executeUpdate() != 1) {
                throw new IllegalStateException("Không thể lưu nhật ký email đã gửi.");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể ghi email đã gửi vào nhật ký ứng viên " + candidateId, e);
            throw new IllegalStateException("Không thể ghi email đã gửi vào nhật ký ứng viên.", e);
        }
    }
}
