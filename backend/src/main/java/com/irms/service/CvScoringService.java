package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.ScreeningCriteriaDAO;
import com.irms.model.Candidate;
import com.irms.model.CvScoreResult;
import com.irms.model.ScreeningCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * DỊCH VỤ CHẤM ĐIỂM & XẾP HẠNG HỒ SƠ ỨNG VIÊN TỰ ĐỘNG (CvScoringService)
 * ==============================================================================
 * Quy tắc đánh giá theo thang điểm 100:
 *
 *  1. KINH NGHIỆM LÀM VIỆC (Tối đa 40 điểm - weightExperience):
 *     - Đủ/vượt số năm yêu cầu: tỉ lệ tuyến tính (đủ = full marks, thiếu = tỉ lệ)
 *     - Tối đa không vượt quá weightExperience
 *
 *  2. KỸ NĂNG CHUYÊN MÔN (Tối đa 40 điểm - weightSkills):
 *     - Must-have skills: chiếm 70% của phần kỹ năng
 *     - Nice-to-have skills: chiếm 30% của phần kỹ năng (cộng điểm)
 *
 *  3. BẰNG CẤP & CHỨNG CHỈ (Tối đa 20 điểm - weightEducation):
 *     - Đúng bằng cấp yêu cầu: full marks
 *     - Bằng cao hơn: full marks
 *     - Bằng thấp hơn hoặc không khớp: 0
 *
 * Cờ bắt buộc (Mandatory Flag):
 *   - PASS: đáp ứng TẤT CẢ tiêu chí bắt buộc (kinh nghiệm + bằng cấp + must-have skills)
 *   - FAIL: thiếu BẤT KỲ tiêu chí bắt buộc nào
 *
 * LƯU Ý: Điểm chỉ để gợi ý xếp hạng, KHÔNG tự động loại hồ sơ.
 * ==============================================================================
 */
public class CvScoringService {

    private static final Logger logger = Logger.getLogger(CvScoringService.class.getName());

    private final ScreeningCriteriaDAO criteriaDAO = new ScreeningCriteriaDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();

    // Bậc bằng cấp để so sánh (thứ tự tăng dần)
    private static final String[] DEGREE_HIERARCHY = {
        "trung cấp", "cao đẳng", "cử nhân", "đại học", "kỹ sư",
        "thạc sĩ", "tiến sĩ", "phó giáo sư", "giáo sư"
    };

    /**
     * Chấm điểm một ứng viên dựa trên tiêu chí sàng lọc của vị trí tuyển dụng.
     * Đầu vào: thông tin CV được lấy từ notes/coverLetter/tags của ứng viên (thô).
     *
     * @param candidate     Hồ sơ ứng viên
     * @param criteria      Tiêu chí sàng lọc của vị trí
     * @param cvText        Văn bản CV thô (notes, cover letter, tags gộp lại)
     * @return              Kết quả chấm điểm chi tiết
     */
    public CvScoreResult scoreCandidate(Candidate candidate, ScreeningCriteria criteria, String cvText) {
        CvScoreResult result = new CvScoreResult();
        result.setCandidateId(candidate.getId());
        result.setCandidateName(candidate.getFullName());
        result.setRequisitionTitle(criteria.getRequisitionTitle());

        String cvLower = (cvText != null ? cvText : "").toLowerCase(Locale.ROOT);

        // =====================================================
        // 1. CHẤM ĐIỂM KINH NGHIỆM (max weightExperience = 40)
        // =====================================================
        int expScore = 0;
        boolean expPassed = false;
        String expDetail;

        int minYears = criteria.getMinExperienceYears();
        if (minYears <= 0) {
            // Không yêu cầu kinh nghiệm cụ thể → cho đủ điểm
            expScore = criteria.getWeightExperience();
            expPassed = true;
            expDetail = "Không yêu cầu kinh nghiệm cụ thể → " + expScore + "/" + criteria.getWeightExperience() + " điểm";
        } else {
            // Phân tích số năm kinh nghiệm từ văn bản CV
            int detectedYears = extractExperienceYears(cvLower);
            expPassed = detectedYears >= minYears;
            if (expPassed) {
                expScore = criteria.getWeightExperience();
                expDetail = "Phát hiện ≥ " + detectedYears + " năm kinh nghiệm (yêu cầu: " + minYears + " năm) → " + expScore + "/" + criteria.getWeightExperience() + " điểm";
            } else {
                // Tỉ lệ: (số năm có / số năm yêu cầu) * điểm tối đa
                double ratio = (minYears > 0 && detectedYears > 0) ? (double) detectedYears / minYears : 0;
                expScore = (int) Math.round(ratio * criteria.getWeightExperience());
                expScore = Math.min(expScore, criteria.getWeightExperience() - 1);
                expScore = Math.max(expScore, 0);
                expDetail = "Phát hiện ~" + detectedYears + " năm (yêu cầu: " + minYears + " năm) → "
                        + expScore + "/" + criteria.getWeightExperience() + " điểm (chưa đủ)";
            }
        }
        result.setExperienceScore(expScore);
        result.setExperiencePassed(expPassed);
        result.setExperienceDetail(expDetail);

        // =====================================================
        // 2. CHẤM ĐIỂM KỸ NĂNG (max weightSkills = 40)
        // =====================================================
        int skillScore = 0;
        boolean mustSkillsPassed = false;
        StringBuilder skillDetailBuilder = new StringBuilder();

        List<String> mustHaveList = criteria.getMustHaveSkillList();
        List<String> niceToHaveList = criteria.getNiceToHaveSkillList();

        // Must-have chiếm 70% phần kỹ năng, Nice-to-have chiếm 30%
        int mustHaveWeight = (int) Math.round(criteria.getWeightSkills() * 0.70);
        int niceWeight = criteria.getWeightSkills() - mustHaveWeight;

        // Chấm must-have skills
        int mustHaveMatched = 0;
        List<String> missingMustHave = new ArrayList<>();
        if (!mustHaveList.isEmpty()) {
            for (String skill : mustHaveList) {
                if (cvLower.contains(skill.toLowerCase(Locale.ROOT))) {
                    mustHaveMatched++;
                } else {
                    missingMustHave.add(skill);
                }
            }
            double mustRatio = (double) mustHaveMatched / mustHaveList.size();
            skillScore += (int) Math.round(mustRatio * mustHaveWeight);
            mustSkillsPassed = mustHaveMatched == mustHaveList.size();
            skillDetailBuilder.append("Kỹ năng bắt buộc: ").append(mustHaveMatched).append("/").append(mustHaveList.size())
                    .append(" đáp ứng");
            if (!missingMustHave.isEmpty()) {
                skillDetailBuilder.append(" (Thiếu: ").append(String.join(", ", missingMustHave)).append(")");
            }
        } else {
            // Không có must-have → mặc định đạt, cho điểm tối đa phần này
            skillScore += mustHaveWeight;
            mustSkillsPassed = true;
            skillDetailBuilder.append("Không yêu cầu kỹ năng bắt buộc cụ thể");
        }

        // Chấm nice-to-have skills (cộng điểm)
        int niceMatched = 0;
        if (!niceToHaveList.isEmpty()) {
            for (String skill : niceToHaveList) {
                if (cvLower.contains(skill.toLowerCase(Locale.ROOT))) {
                    niceMatched++;
                }
            }
            double niceRatio = (double) niceMatched / niceToHaveList.size();
            skillScore += (int) Math.round(niceRatio * niceWeight);
            skillDetailBuilder.append("; Kỹ năng ưu tiên: ").append(niceMatched).append("/").append(niceToHaveList.size()).append(" đáp ứng");
        }

        skillScore = Math.min(skillScore, criteria.getWeightSkills());
        result.setSkillScore(skillScore);
        result.setMustSkillsPassed(mustSkillsPassed);
        result.setSkillDetail(skillDetailBuilder.toString() + " → " + skillScore + "/" + criteria.getWeightSkills() + " điểm");

        // =====================================================
        // 3. CHẤM ĐIỂM BẰNG CẤP (max weightEducation = 20)
        // =====================================================
        int eduScore = 0;
        boolean degreePassed = false;
        String degreeDetail;

        String requiredDegree = criteria.getRequiredDegree();
        if (requiredDegree == null || requiredDegree.trim().isEmpty()) {
            // Không yêu cầu bằng cấp cụ thể
            eduScore = criteria.getWeightEducation();
            degreePassed = true;
            degreeDetail = "Không yêu cầu bằng cấp cụ thể → " + eduScore + "/" + criteria.getWeightEducation() + " điểm";
        } else {
            String reqDegreeLower = requiredDegree.toLowerCase(Locale.ROOT).trim();
            // Kiểm tra bằng cấp trong CV
            degreePassed = cvLower.contains(reqDegreeLower) || isDegreeHigherOrEqual(cvLower, reqDegreeLower);
            if (degreePassed) {
                eduScore = criteria.getWeightEducation();
                degreeDetail = "Đáp ứng yêu cầu bằng cấp \"" + requiredDegree + "\" → " + eduScore + "/" + criteria.getWeightEducation() + " điểm";
            } else {
                eduScore = 0;
                degreeDetail = "Chưa xác nhận bằng cấp \"" + requiredDegree + "\" trong hồ sơ → 0/" + criteria.getWeightEducation() + " điểm";
            }
        }
        result.setEducationScore(eduScore);
        result.setDegreePassed(degreePassed);
        result.setDegreeDetail(degreeDetail);

        // =====================================================
        // 4. TỔNG ĐIỂM & CỜ BẮT BUỘC
        // =====================================================
        int total = expScore + skillScore + eduScore;
        total = Math.min(100, Math.max(0, total));
        result.setTotalScore(total);

        // Cờ bắt buộc: PASS chỉ khi ĐẦY ĐỦ 3 tiêu chí bắt buộc
        boolean allMandatoryPassed = expPassed && degreePassed && mustSkillsPassed;
        result.setMandatoryFlag(allMandatoryPassed ? "PASS" : "FAIL");

        // =====================================================
        // 5. KHUYẾN NGHỊ SẮP XẾP
        // =====================================================
        String recommendation;
        if (allMandatoryPassed && total >= 75) {
            recommendation = "HIGH";
        } else if (total >= 50) {
            recommendation = "MEDIUM";
        } else {
            recommendation = "REVIEW";
        }
        result.setRecommendation(recommendation);

        return result;
    }

    /**
     * Chấm điểm hàng loạt toàn bộ ứng viên của một vị trí tuyển dụng và lưu vào DB.
     *
     * @param requisitionId  ID vị trí tuyển dụng
     * @param scoredByLabel  Nhãn người thực hiện (VD: "SYSTEM" hoặc userId)
     * @return               Số hồ sơ đã được chấm điểm
     */
    public int scoreAllCandidatesForRequisition(String requisitionId, String scoredByLabel) {
        ScreeningCriteria criteria = criteriaDAO.findByRequisitionId(requisitionId);
        if (criteria == null) {
            logger.warning("Không tìm thấy tiêu chí sàng lọc cho vị trí: " + requisitionId);
            return 0;
        }

        List<Candidate> candidates = candidateDAO.findByRequisitionId(requisitionId);
        int count = 0;
        for (Candidate candidate : candidates) {
            // Tổng hợp văn bản CV từ các trường có sẵn
            String cvText = buildCvText(candidate);
            CvScoreResult result = scoreCandidate(candidate, criteria, cvText);

            // Tạo JSON breakdown
            String breakdown = buildBreakdownJson(result);

            boolean saved = criteriaDAO.saveCandidateScore(
                    candidate.getId(),
                    result.getTotalScore(),
                    result.getMandatoryFlag(),
                    breakdown,
                    scoredByLabel != null ? scoredByLabel : "SYSTEM"
            );
            if (saved) count++;
        }
        return count;
    }

    /**
     * Chấm điểm một ứng viên đơn lẻ và lưu vào DB.
     *
     * @param candidateId    ID ứng viên
     * @param scoredByLabel  Nhãn người chấm
     * @return               Kết quả chấm điểm, null nếu không tìm thấy tiêu chí
     */
    public CvScoreResult scoreOneCandidateAndSave(String candidateId, String scoredByLabel) {
        Candidate candidate = candidateDAO.findById(candidateId);
        if (candidate == null || candidate.getRequisitionId() == null) return null;

        ScreeningCriteria criteria = criteriaDAO.findByRequisitionId(candidate.getRequisitionId());
        if (criteria == null) return null;

        String cvText = buildCvText(candidate);
        CvScoreResult result = scoreCandidate(candidate, criteria, cvText);

        String breakdown = buildBreakdownJson(result);
        criteriaDAO.saveCandidateScore(
                candidateId,
                result.getTotalScore(),
                result.getMandatoryFlag(),
                breakdown,
                scoredByLabel != null ? scoredByLabel : "SYSTEM"
        );
        return result;
    }

    // ========== Private helpers ==========

    /**
     * Ghép nối các trường văn bản của hồ sơ ứng viên thành một chuỗi CV thô để phân tích
     */
    private String buildCvText(Candidate c) {
        StringBuilder sb = new StringBuilder();
        if (c.getNotes() != null) sb.append(c.getNotes()).append(" ");
        if (c.getCoverLetter() != null) sb.append(c.getCoverLetter()).append(" ");
        if (c.getTags() != null) sb.append(c.getTags()).append(" ");
        if (c.getExtractedName() != null) sb.append(c.getExtractedName()).append(" ");
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    /**
     * Trích xuất số năm kinh nghiệm từ văn bản CV thô.
     * Tìm pattern: "X năm", "X year(s)", "X+ năm", "X yrs"
     */
    private int extractExperienceYears(String cvLower) {
        // Pattern: số + "năm" hoặc "year"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "(\\d+)[\\s+]*(?:năm|year|yr|years|yrs|năm kinh nghiệm|years of experience)"
        );
        java.util.regex.Matcher m = p.matcher(cvLower);
        int maxYears = 0;
        while (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y > maxYears && y < 50) maxYears = y; // Bỏ qua giá trị không hợp lý
            } catch (NumberFormatException ignored) {}
        }
        return maxYears;
    }

    /**
     * Kiểm tra xem CV có đề cập bằng cấp cao hơn hoặc bằng bằng cấp yêu cầu không
     */
    private boolean isDegreeHigherOrEqual(String cvLower, String requiredDegreeLower) {
        int reqLevel = getDegreeLevel(requiredDegreeLower);
        for (String degree : DEGREE_HIERARCHY) {
            if (cvLower.contains(degree) && getDegreeLevel(degree) >= reqLevel) {
                return true;
            }
        }
        return false;
    }

    private int getDegreeLevel(String degree) {
        String d = degree.toLowerCase(Locale.ROOT).trim();
        for (int i = 0; i < DEGREE_HIERARCHY.length; i++) {
            if (d.contains(DEGREE_HIERARCHY[i])) return i;
        }
        return -1;
    }

    /**
     * Tạo JSON breakdown điểm để lưu DB
     */
    private String buildBreakdownJson(CvScoreResult r) {
        return String.format(
            "{\"total\":%d,\"experience\":%d,\"skill\":%d,\"education\":%d," +
            "\"expPassed\":%b,\"degreePassed\":%b,\"skillsPassed\":%b," +
            "\"recommendation\":\"%s\"}",
            r.getTotalScore(), r.getExperienceScore(), r.getSkillScore(), r.getEducationScore(),
            r.isExperiencePassed(), r.isDegreePassed(), r.isMustSkillsPassed(),
            r.getRecommendation() != null ? r.getRecommendation() : "REVIEW"
        );
    }
}
