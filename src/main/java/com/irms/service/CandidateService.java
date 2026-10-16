package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class CandidateService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> SOURCES = new HashSet<String>(Arrays.asList(
            "Giới thiệu", "Mạng xã hội", "Sự kiện tuyển dụng", "Nguồn khác"));

    private final CandidateDAO candidateDAO;

    public CandidateService(CandidateDAO candidateDAO) {
        this.candidateDAO = candidateDAO;
    }

    public List<Candidate> findRecent() throws SQLException {
        return candidateDAO.findRecent(20);
    }

    public Candidate create(String name, String email, String phone, String position, String source,
                            String cvName, String storageName, String contentType, String clientIp,
                            boolean allowDuplicate)
            throws SQLException, ValidationException, DuplicateCandidateException {
        String normalizedName = required(name, "Họ và tên", 120);
        String normalizedEmail = required(email, "Email", 254).toLowerCase(Locale.ROOT);
        String displayPhone = required(phone, "Số điện thoại", 30);
        String normalizedPhone = normalizePhone(displayPhone);
        String normalizedPosition = required(position, "Vị trí ứng tuyển", 120);
        String normalizedSource = source == null ? "" : source.trim();

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new ValidationException("Email không đúng định dạng.");
        }
        if (normalizedPhone.length() < 8 || normalizedPhone.length() > 15) {
            throw new ValidationException("Số điện thoại phải có từ 8 đến 15 chữ số.");
        }
        if (!SOURCES.contains(normalizedSource)) {
            throw new ValidationException("Nguồn ứng viên không hợp lệ.");
        }

        List<Candidate> duplicates = allowDuplicate
                ? new ArrayList<Candidate>()
                : candidateDAO.findDuplicates(normalizedEmail, normalizedPhone);
        if (!duplicates.isEmpty()) {
            recordHistory("DUPLICATE_BLOCKED", null, normalizedName, normalizedEmail, displayPhone,
                    normalizedSource, cvName != null, "Trùng với " + duplicates.size() + " hồ sơ hiện có.", clientIp);
            throw new DuplicateCandidateException(buildMatches(duplicates, normalizedName, normalizedEmail,
                    normalizedPhone, normalizedPosition));
        }

        Candidate candidate = new Candidate(UUID.randomUUID().toString(), normalizedName,
                normalizedEmail, displayPhone, normalizedPosition,
                normalizedSource, cvName, null);
        try {
            candidateDAO.insert(candidate, normalizedEmail, normalizedPhone, storageName, contentType);
        } catch (SQLIntegrityConstraintViolationException exception) {
            duplicates = candidateDAO.findDuplicates(normalizedEmail, normalizedPhone);
            if (!duplicates.isEmpty()) {
                recordHistory("DUPLICATE_BLOCKED", null, normalizedName, normalizedEmail, displayPhone,
                        normalizedSource, cvName != null, "Trùng khi ghi dữ liệu.", clientIp);
                throw new DuplicateCandidateException(buildMatches(duplicates, normalizedName,
                        normalizedEmail, normalizedPhone, normalizedPosition));
            }
            throw exception;
        }
        recordHistory(allowDuplicate ? "CREATED_DESPITE_DUPLICATE" : "MANUAL_CREATED", candidate.getId(),
                normalizedName, normalizedEmail, displayPhone, normalizedSource, cvName != null,
                allowDuplicate ? "Người dùng xác nhận tạo mới dù có thể trùng." : "Thêm ứng viên thủ công.",
                clientIp);
        return candidate;
    }

    private List<DuplicateMatch> buildMatches(List<Candidate> duplicates, String name, String email,
                                              String normalizedPhone, String position) {
        List<DuplicateMatch> matches = new ArrayList<DuplicateMatch>();
        for (Candidate existing : duplicates) {
            List<String> fields = new ArrayList<String>();
            if (existing.getEmail().equalsIgnoreCase(email)) {
                fields.add("email");
            }
            if (normalizePhone(existing.getPhone()).equals(normalizedPhone)) {
                fields.add("phone");
            }
            matches.add(new DuplicateMatch(existing, fields,
                    existing.getName().equalsIgnoreCase(name),
                    existing.getPosition().equalsIgnoreCase(position)));
        }
        return matches;
    }

    private void recordHistory(String action, String candidateId, String name, String email, String phone,
                               String source, boolean hasCv, String details, String clientIp) {
        try {
            candidateDAO.insertHistory(action, candidateId, name, email, phone, source, hasCv, details, clientIp);
        } catch (SQLException exception) {
            // Lịch sử không được làm hỏng luồng nghiệp vụ chính.
            System.err.println("Failed to record candidate history: " + exception.getMessage());
        }
    }

    private String required(String value, String label, int maxLength) throws ValidationException {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new ValidationException(label + " là bắt buộc.");
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException(label + " không được vượt quá " + maxLength + " ký tự.");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class DuplicateMatch {
        private final Candidate candidate;
        private final List<String> matchedFields;
        private final boolean sameName;
        private final boolean samePosition;

        public DuplicateMatch(Candidate candidate, List<String> matchedFields,
                              boolean sameName, boolean samePosition) {
            this.candidate = candidate;
            this.matchedFields = matchedFields;
            this.sameName = sameName;
            this.samePosition = samePosition;
        }

        public Candidate getCandidate() { return candidate; }
        public List<String> getMatchedFields() { return matchedFields; }
        public boolean isSameName() { return sameName; }
        public boolean isSamePosition() { return samePosition; }
    }

    public static class DuplicateCandidateException extends Exception {
        private final List<DuplicateMatch> matches;

        public DuplicateCandidateException(List<DuplicateMatch> matches) {
            super("Đã tồn tại hồ sơ có cùng email hoặc số điện thoại.");
            this.matches = matches;
        }

        public List<DuplicateMatch> getMatches() {
            return matches;
        }
    }
}
