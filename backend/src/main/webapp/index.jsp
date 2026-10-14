<%--
  ==============================================================================
  TRANG ĐIỀU HƯỚNG GỐC CỦA ỨNG DỤNG (index.jsp)
  Chức năng:
  - Kiểm tra trạng thái phiên đăng nhập của người dùng khi truy cập địa chỉ gốc (/).
  - Nếu chưa đăng nhập: Chuyển hướng tới trang Đăng nhập (/auth/login).
  - Nếu là Recruiter / Interviewer (không có quyền quản trị): Chuyển hướng thẳng tới Pipeline ứng viên (/candidates).
  - Nếu là Admin / HR Manager / Approver: Chuyển hướng tới Bảng điều khiển tổng quan (/dashboard).
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.irms.model.User" %>
<%
    User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
    if (currentUser == null) {
        response.sendRedirect(request.getContextPath() + "/auth/login");
    } else if ((currentUser.hasRole("RECRUITER") || currentUser.hasRole("INTERVIEWER"))
            && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")) {
        response.sendRedirect(request.getContextPath() + "/candidates");
    } else {
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }
%>
