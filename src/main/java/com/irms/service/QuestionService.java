package com.irms.service;

import com.irms.dao.QuestionDAO;
import com.irms.model.Question;

import java.util.List;
import java.util.UUID;

public class QuestionService {
    private final QuestionDAO dao = new QuestionDAO();

    public List<Question> searchQuestions(String jobTitle, String criterionId) {
        return dao.search(jobTitle, criterionId);
    }
    
    public List<Question> getAllQuestions() {
        return dao.findAll();
    }

    public Question getQuestion(String id) {
        return dao.findById(id);
    }

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

    public void deleteQuestion(String id) {
        if (!dao.delete(id)) {
            throw new RuntimeException("Không thể xóa câu hỏi.");
        }
    }
}
