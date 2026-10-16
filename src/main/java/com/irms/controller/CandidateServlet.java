package com.irms.controller;

import com.google.gson.Gson;
import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;
import com.irms.service.CandidateService;
import com.irms.service.CandidateService.DuplicateCandidateException;
import com.irms.service.CandidateService.ValidationException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@WebServlet("/api/candidates")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5 * 1024 * 1024,
        maxRequestSize = 6 * 1024 * 1024
)
public class CandidateServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new Gson();
    private static final Map<String, String> MIME_TYPES = new HashMap<String, String>();

    static {
        MIME_TYPES.put(".pdf", "application/pdf");
        MIME_TYPES.put(".doc", "application/msword");
        MIME_TYPES.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private CandidateService candidateService;
    private Path uploadDirectory;

    @Override
    public void init() throws ServletException {
        candidateService = new CandidateService(new CandidateDAO());
        String configuredDirectory = System.getenv("IRMS_UPLOAD_DIR");
        uploadDirectory = configuredDirectory == null || configuredDirectory.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home"), ".irms", "candidate-cv")
                : Paths.get(configuredDirectory);
        uploadDirectory = uploadDirectory.toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new ServletException("Unable to create candidate CV storage directory.", exception);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJsonHeaders(response);
        try {
            writeJson(response, HttpServletResponse.SC_OK, candidateService.findRecent());
        } catch (Exception exception) {
            getServletContext().log("Failed to load recent candidates.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể tải danh sách ứng viên. Vui lòng thử lại.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        setJsonHeaders(response);
        String requestContentType = request.getContentType();
        if (requestContentType == null
                || !requestContentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            writeError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Yêu cầu phải sử dụng multipart/form-data.");
            return;
        }
        Path savedFile = null;
        try {
            Part cvPart;
            try {
                cvPart = request.getPart("cv");
            } catch (IllegalStateException exception) {
                writeError(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                        "Tệp CV vượt quá giới hạn 5 MB.");
                return;
            } catch (ServletException exception) {
                getServletContext().log("Invalid multipart candidate request.", exception);
                writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                        "Dữ liệu upload không hợp lệ.");
                return;
            }
            String originalName = cvPart == null || cvPart.getSize() == 0
                    ? null : safeOriginalName(cvPart.getSubmittedFileName());
            String extension = originalName == null ? null : extensionOf(originalName);
            String storageName = null;
            String contentType = null;
            if (originalName != null) {
                if (!MIME_TYPES.containsKey(extension)) {
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "CV chỉ chấp nhận định dạng PDF, DOC hoặc DOCX.");
                    return;
                }
                contentType = MIME_TYPES.get(extension);
                if (!hasExpectedSignature(cvPart, extension)) {
                    writeError(response, HttpServletResponse.SC_BAD_REQUEST,
                            "Nội dung tệp không khớp với định dạng CV đã chọn.");
                    return;
                }
                storageName = UUID.randomUUID().toString().replace("-", "") + extension;
                savedFile = uploadDirectory.resolve(storageName).normalize();
                if (!savedFile.getParent().equals(uploadDirectory)) {
                    throw new IOException("Invalid CV storage path.");
                }
                try (InputStream input = cvPart.getInputStream()) {
                    Files.copy(input, savedFile);
                }
            }

            Candidate candidate = candidateService.create(
                    request.getParameter("fullName"),
                    request.getParameter("email"),
                    request.getParameter("phone"),
                    request.getParameter("position"),
                    request.getParameter("source"),
                    originalName,
                    storageName,
                    contentType,
                    request.getRemoteAddr(),
                    "create_anyway".equals(request.getParameter("duplicateAction")));
            savedFile = null;
            writeJson(response, HttpServletResponse.SC_CREATED, candidate);
        } catch (ValidationException exception) {
            deleteSavedFile(savedFile);
            writeError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (DuplicateCandidateException exception) {
            deleteSavedFile(savedFile);
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("error", "duplicate_candidate");
            body.put("message", exception.getMessage());
            body.put("matches", exception.getMatches());
            writeJson(response, HttpServletResponse.SC_CONFLICT, body);
        } catch (Exception exception) {
            deleteSavedFile(savedFile);
            getServletContext().log("Failed to create a candidate.", exception);
            writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Không thể lưu hồ sơ. Vui lòng thử lại.");
        }
    }

    private String safeOriginalName(String submittedName) {
        String name = submittedName == null ? "" : submittedName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (name.isEmpty()) {
            return "cv";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private boolean hasExpectedSignature(Part part, String extension) throws IOException {
        byte[] header = new byte[8];
        int count;
        try (InputStream input = part.getInputStream()) {
            count = 0;
            while (count < header.length) {
                int read = input.read(header, count, header.length - count);
                if (read < 0) {
                    break;
                }
                count += read;
            }
        }
        if (".pdf".equals(extension)) {
            return count >= 5 && header[0] == '%' && header[1] == 'P'
                    && header[2] == 'D' && header[3] == 'F' && header[4] == '-';
        }
        if (".doc".equals(extension)) {
            byte[] oleSignature = new byte[] {
                    (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                    (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
            };
            return count == 8 && Arrays.equals(header, oleSignature);
        }
        return ".docx".equals(extension) && count >= 4
                && header[0] == 'P' && header[1] == 'K'
                && header[2] == 3 && header[3] == 4;
    }

    private void deleteSavedFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            getServletContext().log("Unable to remove an uncommitted candidate CV.", exception);
        }
    }

    private void setJsonHeaders(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        Map<String, String> body = new HashMap<String, String>();
        body.put("message", message);
        writeJson(response, status, body);
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.getWriter().write(GSON.toJson(body));
    }
}
