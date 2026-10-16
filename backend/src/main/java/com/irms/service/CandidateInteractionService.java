package com.irms.service;

import com.irms.dao.CandidateInteractionDAO;
import com.irms.model.CandidateInteraction;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CandidateInteractionService {
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private final CandidateInteractionDAO interactionDAO = new CandidateInteractionDAO();

    public List<CandidateInteraction> getTimeline(String candidateId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        return interactionDAO.findByCandidate(candidateId.trim());
    }

    public void record(String candidateId, String type, String details, String scheduledAt, String actorUserId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        if (!"INTERVIEW_SCHEDULED".equals(type) && !"EVALUATION_RECORDED".equals(type)) {
            throw new IllegalArgumentException("Loại tương tác không hợp lệ.");
        }
        if (details == null || details.trim().isEmpty() || details.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung tương tác (tối đa 2000 ký tự).");
        }

        String description = details.trim();
        Timestamp occurredAt = new Timestamp(System.currentTimeMillis());
        if ("INTERVIEW_SCHEDULED".equals(type)) {
            if (scheduledAt == null || scheduledAt.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập thời gian phỏng vấn.");
            }
            try {
                LocalDateTime interviewTime = LocalDateTime.parse(scheduledAt.trim());
                if (!interviewTime.isAfter(LocalDateTime.now())) {
                    throw new IllegalArgumentException("Thời gian phỏng vấn phải ở tương lai.");
                }
                description = "Thời gian: " + interviewTime.toString().replace('T', ' ') + "\n" + description;
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Thời gian phỏng vấn không hợp lệ.", e);
            }
        }
        interactionDAO.record(candidateId.trim(), type, description, actorUserId, occurredAt);
    }

    public void recordStageChange(String candidateId, String previousStatus, String newStatus, String actorUserId,
                                  String candidateName) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        String description = "Chuyển giai đoạn từ " + previousStatus + " sang " + newStatus
                + " cho ứng viên " + candidateName;
        interactionDAO.record(candidateId.trim(), "STAGE_CHANGED", description, actorUserId,
                new Timestamp(System.currentTimeMillis()));
    }

    public void recordEmailSent(String candidateId, String subject) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        interactionDAO.recordEmailSent(candidateId.trim(), subject);
    }
}
