package com.irms.controller;

import com.irms.model.CompetencyCriterion;
import com.irms.model.Question;
import com.irms.service.CompetencyCriterionService;
import com.irms.service.QuestionService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

@WebServlet(name = "QuestionBankServlet", urlPatterns = {
        "/admin/questions",
        "/admin/questions/create",
        "/admin/questions/edit",
        "/admin/questions/delete",
        "/admin/criteria/create",
        "/admin/criteria/edit",
        "/admin/criteria/delete"
})
public class QuestionBankServlet extends HttpServlet {
    private final QuestionService questionService = new QuestionService();
    private final CompetencyCriterionService criterionService = new CompetencyCriterionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String jobTitle = request.getParameter("jobTitle");
        String criterionId = request.getParameter("criterionId");

        List<Question> questions = questionService.searchQuestions(jobTitle, criterionId);
        List<CompetencyCriterion> criteria = criterionService.getAllCriteria();

        request.setAttribute("questions", questions);
        request.setAttribute("criteria", criteria);
        request.setAttribute("searchJobTitle", jobTitle);
        request.setAttribute("searchCriterionId", criterionId);
        
        request.getRequestDispatcher("/WEB-INF/views/questions/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);

        try {
            switch (path) {
                case "/admin/questions/create": {
                    String content = request.getParameter("content");
                    String difficulty = request.getParameter("difficultyLevel");
                    String goodAnswer = request.getParameter("goodAnswerSuggestion");
                    String critId = request.getParameter("criterionId");
                    String jobTitle = request.getParameter("jobTitle");
                    questionService.createQuestion(content, difficulty, goodAnswer, critId, jobTitle);
                    if (session != null) session.setAttribute("flashSuccess", "Thêm mới câu hỏi thành công!");
                    break;
                }
                case "/admin/questions/edit": {
                    String id = request.getParameter("id");
                    String content = request.getParameter("content");
                    String difficulty = request.getParameter("difficultyLevel");
                    String goodAnswer = request.getParameter("goodAnswerSuggestion");
                    String critId = request.getParameter("criterionId");
                    String jobTitle = request.getParameter("jobTitle");
                    questionService.updateQuestion(id, content, difficulty, goodAnswer, critId, jobTitle);
                    if (session != null) session.setAttribute("flashSuccess", "Cập nhật câu hỏi thành công!");
                    break;
                }
                case "/admin/questions/delete": {
                    String id = request.getParameter("id");
                    questionService.deleteQuestion(id);
                    if (session != null) session.setAttribute("flashSuccess", "Đã xóa câu hỏi thành công!");
                    break;
                }
                case "/admin/criteria/create": {
                    String name = request.getParameter("name");
                    String desc = request.getParameter("description");
                    criterionService.createCriterion(name, desc);
                    if (session != null) session.setAttribute("flashSuccess", "Thêm tiêu chí thành công!");
                    break;
                }
                case "/admin/criteria/edit": {
                    String id = request.getParameter("id");
                    String name = request.getParameter("name");
                    String desc = request.getParameter("description");
                    criterionService.updateCriterion(id, name, desc);
                    if (session != null) session.setAttribute("flashSuccess", "Cập nhật tiêu chí thành công!");
                    break;
                }
                case "/admin/criteria/delete": {
                    String id = request.getParameter("id");
                    criterionService.deleteCriterion(id);
                    if (session != null) session.setAttribute("flashSuccess", "Đã xóa tiêu chí thành công!");
                    break;
                }
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/questions");
    }
}
