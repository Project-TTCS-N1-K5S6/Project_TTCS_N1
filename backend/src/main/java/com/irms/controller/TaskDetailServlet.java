package com.irms.controller;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "TaskDetailServlet", urlPatterns = {
        "/task-detail",
        "/task-detail/*",
        "/tasks/kn-287",
        "/issues/kn-287",
        "/kn-287"
})
public class TaskDetailServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        List<Map<String, Object>> timeline = Arrays.asList(
                createEvent("Đánh giá", "Mới nhất", true),
                createEvent("Lịch phỏng vấn", null, false),
                createEvent("Email đã gửi", null, false),
                createEvent("Chuyển giai đoạn", null, false),
                createEvent("Nộp hồ sơ", null, false)
        );

        request.setAttribute("issueCode", "KN-287");
        request.setAttribute("parentCode", "KN-209");
        request.setAttribute("timeline", timeline);
        request.setAttribute("assignee", "Văn Bác");
        request.setAttribute("reporter", "SIN VĂN CUÔNG");
        request.setAttribute("sprint", "KN Sprint 5");
        request.setAttribute("storyPoint", 2);
        request.setAttribute("currentUserInitials", "VB");
        request.setAttribute("reporterInitials", "SC");

        request.getRequestDispatcher("/WEB-INF/views/tasks/detail.jsp").forward(request, response);
    }

    private Map<String, Object> createEvent(String title, String meta, boolean latest) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("meta", meta);
        item.put("latest", latest);
        return item;
    }
}
