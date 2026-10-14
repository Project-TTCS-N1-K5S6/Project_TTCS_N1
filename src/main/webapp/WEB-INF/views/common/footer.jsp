<%--
  ==============================================================================
  THÀNH PHẦN CHÂN TRANG DÙNG CHUNG (common/footer.jsp)
  Chức năng:
  - Hiển thị thông tin bản quyền và phiên bản hệ thống ở chân trang giao diện nội bộ.
  - Tải thư viện Bootstrap 5.3.3 JavaScript bundle và file kịch bản tương tác chung (main.js).
  - Đóng các thẻ cấu trúc trang (<main>, <div>, <body>, <html>).
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

        <footer class="app-footer">
            <div>
                &copy; 2026 <strong>IRMS Platform</strong> - Hệ thống quản lý tuyển dụng nội bộ. Bảo lưu mọi quyền.
            </div>
            <div class="d-none d-sm-block text-muted">
                Nền tảng: <strong>Java Servlet</strong> &bull; <strong>MySQL</strong> &bull; <strong>RisApps Theme</strong>
            </div>
        </footer>
    </main>
</div>

<!-- Bootstrap 5.3.3 Bundle with Popper -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<!-- Main App Interactivity -->
<script src="${pageContext.request.contextPath}/assets/js/main.js"></script>
</body>
</html>
