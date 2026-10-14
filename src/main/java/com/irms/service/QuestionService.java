package com.irms.service;

import com.irms.dao.QuestionDAO;
import com.irms.model.Question;

import java.util.List;
import java.util.UUID;

/**
 * Tầng nghiệp vụ Ngân hàng câu hỏi phỏng vấn (Question Bank Service)
 * Quản lý tìm kiếm, lọc theo tiêu chí, tạo mới, chỉnh sửa và xóa câu hỏi phỏng vấn
 */
public class QuestionService {
    private final QuestionDAO dao = new QuestionDAO();

    /**
     * Chức năng: Tìm kiếm câu hỏi phỏng vấn theo chức danh công việc và tiêu chí năng lực
     *
     * @param jobTitle    Chức danh vị trí tuyển dụng
     * @param criterionId ID tiêu chí năng lực
     * @return Danh sách câu hỏi phù hợp
     */
    public List<Question> searchQuestions(String jobTitle, String criterionId) {
        return dao.search(jobTitle, criterionId);
    }
    
    /**
     * Chức năng: Lấy toàn bộ danh sách câu hỏi phỏng vấn có trong hệ thống
     *
     * @return Danh sách tất cả câu hỏi
     */
    public List<Question> getAllQuestions() {
        return dao.findAll();
    }

    /**
     * Chức năng: Lấy thông tin chi tiết một câu hỏi phỏng vấn theo mã định danh ID
     *
     * @param id ID của câu hỏi
     * @return Thực thể Question tương ứng
     */
    public Question getQuestion(String id) {
        return dao.findById(id);
    }

    /**
     * Chức năng: Tạo mới một câu hỏi phỏng vấn và lưu vào cơ sở dữ liệu
     *
     * @param content              Nội dung câu hỏi
     * @param difficultyLevel      Độ khó (EASY, MEDIUM, HARD)
     * @param goodAnswerSuggestion Gợi ý câu trả lời chuẩn cho người phỏng vấn
     * @param criterionId          ID tiêu chí năng lực liên kết
     * @param jobTitle             Chức danh công việc áp dụng
     */
    public void createQuestion(String content, String difficultyLevel, String goodAnswerSuggestion, String criterionId, String jobTitle) {
        Question q = new Question();
        q.setId(UUID.randomUUID().toString());
        q.setContent(content);
        q.setDifficultyLevel(difficultyLevel);
        q.setGoodAnswerSuggestion(goodAnswerSuggestion);
        q.setCriterionId(criterionId);
        q.setJobTitle(jobTitle);
        
        if (!dao.insert(q)) {
            throw new RuntimeException("Không thể thêm câu hỏi.");
        }
    }

    /**
     * Chức năng: Cập nhật nội dung, độ khó, gợi ý trả lời và tiêu chí cho câu hỏi phỏng vấn
     *
     * @param id                   ID câu hỏi cần cập nhật
     * @param content              Nội dung câu hỏi mới
     * @param difficultyLevel      Độ khó mới
     * @param goodAnswerSuggestion Gợi ý trả lời mới
     * @param criterionId          ID tiêu chí năng lực mới
     * @param jobTitle             Chức danh công việc mới
     */
    public void updateQuestion(String id, String content, String difficultyLevel, String goodAnswerSuggestion, String criterionId, String jobTitle) {
        Question q = new Question();
        q.setId(id);
        q.setContent(content);
        q.setDifficultyLevel(difficultyLevel);
        q.setGoodAnswerSuggestion(goodAnswerSuggestion);
        q.setCriterionId(criterionId);
        q.setJobTitle(jobTitle);
        
        if (!dao.update(q)) {
            throw new RuntimeException("Không thể cập nhật câu hỏi.");
        }
    }

    /**
     * Chức năng: Xóa một câu hỏi phỏng vấn khỏi hệ thống theo mã ID
     *
     * @param id ID của câu hỏi cần xóa
     */
    public void deleteQuestion(String id) {
        if (!dao.delete(id)) {
            throw new RuntimeException("Không thể xóa câu hỏi.");
        }
    }
}
