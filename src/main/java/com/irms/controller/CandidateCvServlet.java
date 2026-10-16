package com.irms.controller;

import com.irms.dao.CandidateDAO;
import com.irms.dao.CandidateDAO.CandidateFile;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.regex.Pattern;

@WebServlet("/api/candidates/*")
public class CandidateCvServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Pattern CV_STORAGE_NAME =
            Pattern.compile("^[a-f0-9]{32}\\.(pdf|doc|docx)$");
    private static final Pattern CANDIDATE_ID =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private CandidateDAO candidateDAO;
    private Path uploadDirectory;

    @Override
    public void init() throws ServletException {
        candidateDAO = new CandidateDAO();
        String configuredDirectory = System.getenv("IRMS_UPLOAD_DIR");
        uploadDirectory = (configuredDirectory == null || configuredDirectory.trim().isEmpty()
                ? Paths.get(System.getProperty("user.home"), ".irms", "candidate-cv")
                : Paths.get(configuredDirectory)).toAbsolutePath().normalize();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (path == null || !path.endsWith("/cv")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String candidateId = path.substring(1, path.length() - 3);
        if (!CANDIDATE_ID.matcher(candidateId).matches()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            CandidateFile cv = candidateDAO.findCvByCandidateId(candidateId);
            if (cv == null || !CV_STORAGE_NAME.matcher(cv.getStorageName()).matches()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Path file = uploadDirectory.resolve(cv.getStorageName()).normalize();
            if (!file.getParent().equals(uploadDirectory) || !Files.isRegularFile(file)) {
                getServletContext().log("CV file is missing for candidate " + candidateId + ".");
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String storageName = cv.getStorageName();
            String extension = storageName.substring(storageName.lastIndexOf('.') + 1);
            boolean inline = "1".equals(request.getParameter("inline")) && "pdf".equals(extension);
            response.setContentType(inline ? "application/pdf" : "application/octet-stream");
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, no-store");
            response.setHeader("Content-Disposition", (inline ? "inline" : "attachment")
                    + "; filename=\"candidate-cv." + extension + "\"");
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
        } catch (SQLException exception) {
            getServletContext().log("Failed to load candidate CV.", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
