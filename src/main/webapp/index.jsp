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
