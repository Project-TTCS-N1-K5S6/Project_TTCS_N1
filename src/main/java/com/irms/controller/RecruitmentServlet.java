package com.irms.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.irms.config.Database;
import com.irms.dao.RecruitmentRepository;
import com.irms.dao.RecruitmentRepository.RecruitmentCopy;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet(urlPatterns = {"/api/jobs", "/api/job-copies"})
public class RecruitmentServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RecruitmentServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MAX_REQUEST_LENGTH = 65536;
    private static final int MAX_DESCRIPTION_LENGTH = 10000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            RecruitmentRepository repository = new RecruitmentRepository(Database.getDataSource());
            if ("/api/jobs".equals(request.getServletPath())) {
                writeJson(response, HttpServletResponse.SC_OK, repository.findAllJobs());
                return;
            }
            writeJson(response, HttpServletResponse.SC_OK, repository.findAllCopies());
        } catch (SQLException | RuntimeException exception) {
            handleServerError(response, exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        try {
            CreateCopyRequest input = parseRequest(request);
            validate(input);

            RecruitmentRepository repository = new RecruitmentRepository(Database.getDataSource());
            RecruitmentCopy copy = repository.createCopy(
                input.sourceJobId.trim(),
                input.title.trim(),
                input.description.trim(),
                input.requirements.trim()
            );
            if (copy == null) {
                writeError(response, HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tin tuyển dụng gốc.");
                return;
            }
            writeJson(response, HttpServletResponse.SC_CREATED, copy);
        } catch (IllegalArgumentException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (JsonParseException exception) {
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Dữ liệu JSON không hợp lệ.");
        } catch (SQLException | RuntimeException exception) {
            handleServerError(response, exception);
        }
    }

    private CreateCopyRequest parseRequest(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        char[] buffer = new char[2048];
        try (BufferedReader reader = request.getReader()) {
            int length;
            while ((length = reader.read(buffer)) != -1) {
                if (body.length() + length > MAX_REQUEST_LENGTH) {
                    throw new IllegalArgumentException("Dữ liệu gửi lên vượt quá giới hạn cho phép.");
                }
                body.append(buffer, 0, length);
            }
        }

        CreateCopyRequest input = GSON.fromJson(body.toString(), CreateCopyRequest.class);
        if (input == null) {
            throw new IllegalArgumentException("Vui lòng gửi dữ liệu JSON hợp lệ.");
        }
        return input;
    }

    private void validate(CreateCopyRequest input) {
        requireText(input.sourceJobId, "Vui lòng chọn tin tuyển dụng gốc.", 64);
        requireText(input.title, "Vui lòng nhập tên tin tuyển dụng.", 160);
        requireText(input.description, "Vui lòng nhập mô tả công việc.", MAX_DESCRIPTION_LENGTH);
        requireText(input.requirements, "Vui lòng nhập yêu cầu ứng viên.", MAX_DESCRIPTION_LENGTH);
    }

    private void requireText(String value, String message, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException("Một trong các trường dữ liệu vượt quá độ dài cho phép.");
        }
    }

    private void handleServerError(HttpServletResponse response, Exception exception) throws IOException {
        LOGGER.log(Level.SEVERE, "Recruitment API request failed.", exception);
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String message = "Không thể xử lý dữ liệu tuyển dụng.";
        if (isConnectionFailure(exception)) {
            status = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            message = "Không thể kết nối cơ sở dữ liệu. Hãy kiểm tra MySQL và cấu hình kết nối.";
        }
        writeError(response, status, message);
    }

    private boolean isConnectionFailure(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException) {
                String sqlState = ((SQLException) cause).getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        writeJson(response, status, new ApiError(message));
    }

    private void writeJson(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.getWriter().write(GSON.toJson(value));
    }

    private static class CreateCopyRequest {
        private String sourceJobId;
        private String title;
        private String description;
        private String requirements;
    }

    private static class ApiError {
        private final String error;

        private ApiError(String error) {
            this.error = error;
        }
    }
}
