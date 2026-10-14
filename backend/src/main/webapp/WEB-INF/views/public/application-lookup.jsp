<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tra cứu trạng thái hồ sơ - IRMS Platform</title>
    <meta name="description" content="Tra cứu trạng thái hồ sơ ứng tuyển bằng mã tra cứu và email của bạn.">
    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:ital,wght@0,400;0,500;0,600;0,700;0,800;1,400&display=swap" rel="stylesheet">
    <!-- Bootstrap 5.3.3 -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <!-- Main CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
    <style>
        /* ================================================================
           [KN-244] [KN-248]: Trang tra cứu trạng thái hồ sơ - Public page
           ================================================================ */

        /* --- Background & Layout --- */
        body.lookup-page {
            background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 40%, #1a2e5a 70%, #0f172a 100%);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            font-family: 'Inter', sans-serif;
        }

        /* Animated background dots */
        body.lookup-page::before {
            content: '';
            position: fixed;
            inset: 0;
            background-image:
                radial-gradient(circle at 15% 20%, rgba(37, 99, 235, 0.12) 0%, transparent 50%),
                radial-gradient(circle at 85% 80%, rgba(99, 102, 241, 0.10) 0%, transparent 50%),
                radial-gradient(circle at 50% 50%, rgba(59, 130, 246, 0.05) 0%, transparent 70%);
            pointer-events: none;
            z-index: 0;
        }

        /* --- Topbar --- */
        .lookup-topbar {
            position: relative;
            z-index: 10;
            padding: 1rem 2rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid rgba(255,255,255,0.06);
            backdrop-filter: blur(10px);
        }

        .lookup-brand {
            display: flex;
            align-items: center;
            gap: 0.6rem;
            text-decoration: none;
            color: #fff;
        }

        .lookup-brand .brand-icon {
            width: 36px;
            height: 36px;
            background: linear-gradient(135deg, #2563eb, #6366f1);
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1rem;
            color: #fff;
        }

        .lookup-brand span {
            font-weight: 700;
            font-size: 1rem;
            letter-spacing: 0.04em;
            color: #f1f5f9;
        }

        .back-to-login {
            font-size: 0.85rem;
            color: rgba(255,255,255,0.55);
            text-decoration: none;
            display: flex;
            align-items: center;
            gap: 0.35rem;
            transition: color 0.2s;
        }

        .back-to-login:hover { color: #93c5fd; }

        /* --- Main container --- */
        .lookup-main {
            position: relative;
            z-index: 1;
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 2.5rem 1rem;
        }

        /* --- Card --- */
        .lookup-card {
            background: rgba(15, 23, 42, 0.7);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 20px;
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            padding: 2.5rem 2rem;
            width: 100%;
            max-width: 560px;
            box-shadow:
                0 0 0 1px rgba(255,255,255,0.05),
                0 25px 60px rgba(0,0,0,0.5),
                0 0 80px rgba(37,99,235,0.08);
            animation: cardFadeIn 0.5s ease forwards;
        }

        @keyframes cardFadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to   { opacity: 1; transform: translateY(0); }
        }

        /* --- Card Header --- */
        .lookup-card-header {
            text-align: center;
            margin-bottom: 2rem;
        }

        .lookup-icon-wrap {
            width: 64px;
            height: 64px;
            background: linear-gradient(135deg, rgba(37,99,235,0.25), rgba(99,102,241,0.2));
            border: 1px solid rgba(99,102,241,0.3);
            border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.75rem;
            color: #60a5fa;
            margin: 0 auto 1.1rem;
        }

        .lookup-card-header h1 {
            font-size: 1.45rem;
            font-weight: 800;
            color: #f1f5f9;
            margin-bottom: 0.4rem;
        }

        .lookup-card-header p {
            font-size: 0.875rem;
            color: rgba(255,255,255,0.5);
            margin: 0;
            line-height: 1.6;
        }

        /* --- Form --- */
        .lookup-form-group {
            margin-bottom: 1.25rem;
        }

        .lookup-form-group label {
            display: block;
            font-size: 0.85rem;
            font-weight: 600;
            color: rgba(255,255,255,0.7);
            margin-bottom: 0.5rem;
        }

        .lookup-form-control {
            width: 100%;
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.12);
            border-radius: 10px;
            color: #f1f5f9;
            font-size: 0.95rem;
            padding: 0.75rem 1rem;
            transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
            font-family: 'Inter', sans-serif;
        }

        .lookup-form-control::placeholder { color: rgba(255,255,255,0.28); }

        .lookup-form-control:focus {
            outline: none;
            border-color: #3b82f6;
            background: rgba(255,255,255,0.09);
            box-shadow: 0 0 0 3px rgba(59,130,246,0.2);
        }

        .lookup-form-control.is-invalid {
            border-color: #f87171;
            box-shadow: 0 0 0 3px rgba(248,113,113,0.15);
        }

        /* Input group with icon */
        .input-icon-wrap {
            position: relative;
        }

        .input-icon-wrap .input-icon {
            position: absolute;
            left: 1rem;
            top: 50%;
            transform: translateY(-50%);
            color: rgba(255,255,255,0.3);
            font-size: 1rem;
            pointer-events: none;
        }

        .input-icon-wrap .lookup-form-control {
            padding-left: 2.75rem;
        }

        /* --- Submit button --- */
        .btn-lookup {
            width: 100%;
            background: linear-gradient(135deg, #2563eb, #4f46e5);
            border: none;
            border-radius: 10px;
            color: #fff;
            font-size: 1rem;
            font-weight: 600;
            padding: 0.85rem 1.5rem;
            cursor: pointer;
            transition: all 0.25s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            margin-top: 1.5rem;
            font-family: 'Inter', sans-serif;
        }

        .btn-lookup:hover {
            background: linear-gradient(135deg, #1d4ed8, #4338ca);
            box-shadow: 0 8px 25px rgba(37,99,235,0.4);
            transform: translateY(-1px);
        }

        .btn-lookup:active { transform: translateY(0); }

        /* Loading spinner */
        .btn-lookup .spinner {
            display: none;
            width: 18px;
            height: 18px;
            border: 2px solid rgba(255,255,255,0.3);
            border-top-color: #fff;
            border-radius: 50%;
            animation: spin 0.7s linear infinite;
        }

        @keyframes spin { to { transform: rotate(360deg); } }

        /* --- Info box --- */
        .lookup-info-box {
            background: rgba(37,99,235,0.08);
            border: 1px solid rgba(37,99,235,0.2);
            border-radius: 10px;
            padding: 0.9rem 1rem;
            margin-top: 1.5rem;
            display: flex;
            gap: 0.75rem;
            align-items: flex-start;
        }

        .lookup-info-box i {
            color: #60a5fa;
            font-size: 1rem;
            flex-shrink: 0;
            margin-top: 1px;
        }

        .lookup-info-box p {
            margin: 0;
            font-size: 0.82rem;
            color: rgba(255,255,255,0.5);
            line-height: 1.6;
        }

        /* ================================================================
           [KN-248]: Trang hiển thị kết quả tra cứu
           ================================================================ */

        /* Candidate name header */
        .result-header {
            text-align: center;
            margin-bottom: 1.75rem;
        }

        .result-header .candidate-avatar {
            width: 60px;
            height: 60px;
            border-radius: 50%;
            background: linear-gradient(135deg, #1e40af, #4f46e5);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.4rem;
            font-weight: 700;
            color: #fff;
            margin: 0 auto 0.9rem;
            border: 3px solid rgba(99,102,241,0.4);
        }

        .result-header h2 {
            font-size: 1.2rem;
            font-weight: 700;
            color: #f1f5f9;
            margin-bottom: 0.2rem;
        }

        .result-header .position-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.3rem;
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 20px;
            padding: 0.25rem 0.75rem;
            font-size: 0.8rem;
            color: rgba(255,255,255,0.55);
        }

        /* Timeline steps */
        .status-timeline {
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0;
            margin: 1.75rem 0;
            position: relative;
        }

        .timeline-step {
            display: flex;
            flex-direction: column;
            align-items: center;
            flex: 1;
            position: relative;
            z-index: 1;
        }

        /* Connector line */
        .timeline-step:not(:last-child)::after {
            content: '';
            position: absolute;
            top: 18px;
            left: 50%;
            width: 100%;
            height: 2px;
            background: rgba(255,255,255,0.1);
            z-index: 0;
        }

        .timeline-step.done:not(:last-child)::after {
            background: linear-gradient(90deg, #3b82f6, rgba(99,102,241,0.4));
        }

        .timeline-step.active:not(:last-child)::after {
            background: rgba(255,255,255,0.1);
        }

        .step-circle {
            width: 36px;
            height: 36px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 0.9rem;
            border: 2px solid rgba(255,255,255,0.15);
            background: rgba(255,255,255,0.05);
            color: rgba(255,255,255,0.3);
            position: relative;
            z-index: 2;
            transition: all 0.3s;
        }

        .timeline-step.done .step-circle {
            background: linear-gradient(135deg, #2563eb, #4f46e5);
            border-color: #6366f1;
            color: #fff;
            box-shadow: 0 0 14px rgba(99,102,241,0.45);
        }

        .timeline-step.active .step-circle {
            background: linear-gradient(135deg, #0ea5e9, #6366f1);
            border-color: #38bdf8;
            color: #fff;
            box-shadow: 0 0 20px rgba(14,165,233,0.5);
            animation: pulse-active 2s infinite;
        }

        @keyframes pulse-active {
            0%, 100% { box-shadow: 0 0 20px rgba(14,165,233,0.5); }
            50%       { box-shadow: 0 0 30px rgba(14,165,233,0.8), 0 0 45px rgba(99,102,241,0.3); }
        }

        .step-label {
            font-size: 0.7rem;
            font-weight: 600;
            margin-top: 0.5rem;
            text-align: center;
            color: rgba(255,255,255,0.3);
            line-height: 1.3;
        }

        .timeline-step.done  .step-label { color: #93c5fd; }
        .timeline-step.active .step-label {
            color: #38bdf8;
            font-weight: 700;
        }

        /* Status badge */
        .current-status-badge {
            display: inline-flex;
            align-items: center;
            gap: 0.55rem;
            padding: 0.6rem 1.25rem;
            border-radius: 50px;
            font-size: 0.95rem;
            font-weight: 700;
            margin: 0.75rem 0 1.25rem;
        }

        .badge-status-received {
            background: rgba(16,185,129,0.15);
            border: 1px solid rgba(16,185,129,0.35);
            color: #34d399;
        }

        .badge-status-reviewing {
            background: rgba(245,158,11,0.12);
            border: 1px solid rgba(245,158,11,0.3);
            color: #fbbf24;
        }

        .badge-status-interview {
            background: rgba(139,92,246,0.15);
            border: 1px solid rgba(139,92,246,0.35);
            color: #c4b5fd;
        }

        .badge-status-done {
            background: rgba(100,116,139,0.15);
            border: 1px solid rgba(100,116,139,0.25);
            color: #94a3b8;
        }

        /* Result info rows */
        .result-info-grid {
            background: rgba(255,255,255,0.03);
            border: 1px solid rgba(255,255,255,0.07);
            border-radius: 12px;
            padding: 1rem;
            margin-bottom: 1.25rem;
        }

        .result-info-row {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.55rem 0;
            border-bottom: 1px solid rgba(255,255,255,0.05);
            gap: 1rem;
        }

        .result-info-row:last-child { border-bottom: none; }

        .result-info-label {
            font-size: 0.8rem;
            color: rgba(255,255,255,0.4);
            font-weight: 500;
            flex-shrink: 0;
        }

        .result-info-value {
            font-size: 0.85rem;
            color: #e2e8f0;
            font-weight: 500;
            text-align: right;
        }

        /* Success/End banner */
        .result-end-banner {
            border-radius: 12px;
            padding: 1rem 1.25rem;
            display: flex;
            align-items: flex-start;
            gap: 0.75rem;
            margin-bottom: 1rem;
        }

        .result-end-banner.success {
            background: rgba(16,185,129,0.08);
            border: 1px solid rgba(16,185,129,0.2);
        }

        .result-end-banner.ended {
            background: rgba(100,116,139,0.08);
            border: 1px solid rgba(100,116,139,0.2);
        }

        .result-end-banner i {
            font-size: 1.1rem;
            flex-shrink: 0;
        }

        .result-end-banner.success i { color: #34d399; }
        .result-end-banner.ended i   { color: #94a3b8; }

        .result-end-banner p {
            margin: 0;
            font-size: 0.83rem;
            line-height: 1.6;
        }

        .result-end-banner.success p { color: rgba(255,255,255,0.65); }
        .result-end-banner.ended p   { color: rgba(255,255,255,0.45); }

        /* Retry button */
        .btn-retry {
            width: 100%;
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.12);
            border-radius: 10px;
            color: rgba(255,255,255,0.65);
            font-size: 0.9rem;
            font-weight: 500;
            padding: 0.75rem 1.5rem;
            cursor: pointer;
            transition: all 0.2s;
            text-decoration: none;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 0.5rem;
            font-family: 'Inter', sans-serif;
            margin-top: 0.75rem;
        }

        .btn-retry:hover {
            background: rgba(255,255,255,0.1);
            color: #f1f5f9;
            border-color: rgba(255,255,255,0.2);
        }

        /* ================================================================
           [KN-249]: Trang lỗi / không tìm thấy
           ================================================================ */

        .error-card-inner {
            text-align: center;
        }

        .error-icon-wrap {
            width: 72px;
            height: 72px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 2rem;
            margin: 0 auto 1.25rem;
        }

        .error-icon-wrap.not-found {
            background: rgba(248,113,113,0.1);
            border: 2px solid rgba(248,113,113,0.2);
            color: #f87171;
        }

        .error-icon-wrap.missing {
            background: rgba(245,158,11,0.1);
            border: 2px solid rgba(245,158,11,0.2);
            color: #fbbf24;
        }

        .error-title {
            font-size: 1.2rem;
            font-weight: 700;
            color: #f1f5f9;
            margin-bottom: 0.5rem;
        }

        .error-desc {
            font-size: 0.875rem;
            color: rgba(255,255,255,0.45);
            line-height: 1.65;
            margin-bottom: 1.75rem;
        }

        /* --- Footer --- */
        .lookup-footer {
            position: relative;
            z-index: 1;
            text-align: center;
            padding: 1.25rem;
            font-size: 0.78rem;
            color: rgba(255,255,255,0.25);
            border-top: 1px solid rgba(255,255,255,0.05);
        }

        /* Responsive */
        @media (max-width: 480px) {
            .lookup-card { padding: 1.75rem 1.25rem; }
            .lookup-topbar { padding: 0.85rem 1rem; }
            .step-label { font-size: 0.62rem; }
        }
    </style>
</head>
<body class="lookup-page">

<!-- ===== Topbar ===== -->
<nav class="lookup-topbar">
    <a href="${pageContext.request.contextPath}/auth/login" class="lookup-brand">
        <div class="brand-icon"><i class="bi bi-shield-check"></i></div>
        <span>IRMS PLATFORM</span>
    </a>
    <a href="${pageContext.request.contextPath}/auth/login" class="back-to-login">
        <i class="bi bi-box-arrow-in-right"></i> Đăng nhập hệ thống
    </a>
</nav>

<!-- ===== Main ===== -->
<main class="lookup-main">

    <%-- ================================================================
         [KN-244] [KN-248] [KN-249]: Render theo pageMode
         pageMode = FORM | RESULT | NOT_FOUND | ERROR
         ================================================================ --%>

    <%-- ---- MODE: FORM (Mặc định - nhập mã tra cứu) ---- --%>
    <c:if test="${pageMode == 'FORM'}">
        <div class="lookup-card">
            <div class="lookup-card-header">
                <div class="lookup-icon-wrap">
                    <i class="bi bi-search-heart-fill"></i>
                </div>
                <h1>Tra cứu trạng thái hồ sơ</h1>
                <p>Nhập mã tra cứu và email để xem tiến trình xét duyệt hồ sơ của bạn.</p>
            </div>

            <%-- [KN-244]: Form nhập mã + email --%>
            <form id="lookupForm" method="POST" action="${pageContext.request.contextPath}/application-lookup" novalidate>
                <div class="lookup-form-group">
                    <label for="candidateId">
                        <i class="bi bi-qr-code me-1"></i> Mã tra cứu hồ sơ <span style="color:#f87171">*</span>
                    </label>
                    <div class="input-icon-wrap">
                        <i class="input-icon bi bi-hash"></i>
                        <input type="text"
                               id="candidateId"
                               name="candidateId"
                               class="lookup-form-control"
                               placeholder="Nhập mã tra cứu (ví dụ: abc1-2345-...)"
                               autocomplete="off"
                               spellcheck="false"
                               required>
                    </div>
                    <div class="invalid-feedback-custom" id="err-code" style="display:none; font-size:0.8rem; color:#f87171; margin-top:0.35rem;">
                        <i class="bi bi-exclamation-circle me-1"></i>Vui lòng nhập mã tra cứu.
                    </div>
                </div>

                <div class="lookup-form-group">
                    <label for="email">
                        <i class="bi bi-envelope me-1"></i> Email đã đăng ký <span style="color:#f87171">*</span>
                    </label>
                    <div class="input-icon-wrap">
                        <i class="input-icon bi bi-envelope-fill"></i>
                        <input type="email"
                               id="email"
                               name="email"
                               class="lookup-form-control"
                               placeholder="email@example.com"
                               autocomplete="email"
                               required>
                    </div>
                    <div class="invalid-feedback-custom" id="err-email" style="display:none; font-size:0.8rem; color:#f87171; margin-top:0.35rem;">
                        <i class="bi bi-exclamation-circle me-1"></i>Vui lòng nhập email hợp lệ.
                    </div>
                </div>

                <button type="submit" class="btn-lookup" id="submitBtn">
                    <span class="spinner" id="btnSpinner"></span>
                    <i class="bi bi-search" id="btnIcon"></i>
                    <span id="btnText">Tra cứu ngay</span>
                </button>
            </form>

            <div class="lookup-info-box">
                <i class="bi bi-shield-check"></i>
                <p>Mã tra cứu được cung cấp trong email xác nhận sau khi bạn nộp hồ sơ. Chúng tôi chỉ hiển thị trạng thái ở mức tổng quát và không tiết lộ bất kỳ thông tin nội bộ nào.</p>
            </div>
        </div>
    </c:if>

    <%-- ---- MODE: RESULT (Hiển thị kết quả tra cứu) [KN-248] ---- --%>
    <c:if test="${pageMode == 'RESULT' && lookupResult != null}">
        <div class="lookup-card">

            <%-- Header thông tin ứng viên --%>
            <div class="result-header">
                <div class="candidate-avatar">
                    ${fn:substring(lookupResult.fullName, 0, 1)}
                </div>
                <h2>${lookupResult.fullName}</h2>
                <span class="position-badge">
                    <i class="bi bi-briefcase-fill"></i>
                    <c:choose>
                        <c:when test="${not empty lookupResult.positionTitle}">${lookupResult.positionTitle}</c:when>
                        <c:otherwise>Vị trí đang cập nhật</c:otherwise>
                    </c:choose>
                </span>
            </div>

            <%-- [KN-248]: Badge trạng thái tổng quát hiện tại --%>
            <div style="text-align:center;">
                <div class="current-status-badge ${lookupResult.publicStatusBadgeClass}">
                    <i class="bi ${lookupResult.publicStatusIcon}"></i>
                    ${lookupResult.publicStatusLabel}
                </div>
            </div>

            <%-- [KN-243] [KN-248]: Timeline 4 bước tổng quát --%>
            <div class="status-timeline">

                <%-- Bước 1: Đã nhận --%>
                <div class="timeline-step ${lookupResult.publicStatusStep >= 1 ? (lookupResult.publicStatusStep == 1 ? 'active' : 'done') : ''}">
                    <div class="step-circle">
                        <c:choose>
                            <c:when test="${lookupResult.publicStatusStep > 1}"><i class="bi bi-check-lg"></i></c:when>
                            <c:otherwise><i class="bi bi-inbox-fill"></i></c:otherwise>
                        </c:choose>
                    </div>
                    <span class="step-label">Đã nhận</span>
                </div>

                <%-- Bước 2: Đang xem xét --%>
                <div class="timeline-step ${lookupResult.publicStatusStep >= 2 ? (lookupResult.publicStatusStep == 2 ? 'active' : 'done') : ''}">
                    <div class="step-circle">
                        <c:choose>
                            <c:when test="${lookupResult.publicStatusStep > 2}"><i class="bi bi-check-lg"></i></c:when>
                            <c:otherwise><i class="bi bi-eye-fill"></i></c:otherwise>
                        </c:choose>
                    </div>
                    <span class="step-label">Đang xem xét</span>
                </div>

                <%-- Bước 3: Mời phỏng vấn --%>
                <div class="timeline-step ${lookupResult.publicStatusStep >= 3 ? (lookupResult.publicStatusStep == 3 ? 'active' : 'done') : ''}">
                    <div class="step-circle">
                        <c:choose>
                            <c:when test="${lookupResult.publicStatusStep > 3}"><i class="bi bi-check-lg"></i></c:when>
                            <c:otherwise><i class="bi bi-calendar-check-fill"></i></c:otherwise>
                        </c:choose>
                    </div>
                    <span class="step-label">Mời phỏng vấn</span>
                </div>

                <%-- Bước 4: Kết thúc --%>
                <div class="timeline-step ${lookupResult.publicStatusStep >= 4 ? 'active' : ''}">
                    <div class="step-circle">
                        <i class="bi bi-flag-fill"></i>
                    </div>
                    <span class="step-label">Kết thúc</span>
                </div>

            </div>

            <%-- Thông tin hồ sơ (không có thông tin nội bộ) --%>
            <div class="result-info-grid">
                <div class="result-info-row">
                    <span class="result-info-label"><i class="bi bi-hash me-1"></i>Mã hồ sơ</span>
                    <span class="result-info-value" style="font-family: monospace; font-size:0.8rem; color:#93c5fd;">${lookupResult.candidateId}</span>
                </div>
                <div class="result-info-row">
                    <span class="result-info-label"><i class="bi bi-calendar3 me-1"></i>Ngày nộp</span>
                    <span class="result-info-value">
                        <fmt:formatDate value="${lookupResult.submittedAt}" pattern="dd/MM/yyyy HH:mm"/>
                    </span>
                </div>
                <div class="result-info-row">
                    <span class="result-info-label"><i class="bi bi-clock-history me-1"></i>Cập nhật lần cuối</span>
                    <span class="result-info-value">
                        <fmt:formatDate value="${lookupResult.lastUpdatedAt}" pattern="dd/MM/yyyy HH:mm"/>
                    </span>
                </div>
            </div>

            <%-- [KN-246]: Banner thông báo kết thúc - phân biệt thành công / kết thúc bình thường --%>
            <c:if test="${lookupResult.publicStatus == 'KET_THUC'}">
                <c:choose>
                    <c:when test="${lookupResult.successfulEnd}">
                        <div class="result-end-banner success">
                            <i class="bi bi-stars"></i>
                            <p>Chúc mừng! Hồ sơ của bạn đã hoàn thành quy trình tuyển dụng thành công. Vui lòng kiểm tra email để nhận thông tin chi tiết.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="result-end-banner ended">
                            <i class="bi bi-info-circle-fill"></i>
                            <p>Quy trình xét duyệt hồ sơ của bạn đã kết thúc. Nếu có thắc mắc, vui lòng liên hệ phòng nhân sự qua email đã đăng ký.</p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </c:if>

            <%-- Nút tra cứu lại --%>
            <a href="${pageContext.request.contextPath}/application-lookup" class="btn-retry">
                <i class="bi bi-arrow-left"></i> Tra cứu hồ sơ khác
            </a>
        </div>
    </c:if>

    <%-- ---- MODE: NOT_FOUND [KN-249] ---- --%>
    <c:if test="${pageMode == 'NOT_FOUND' || (param.error == 'NOT_FOUND')}">
        <div class="lookup-card">
            <div class="error-card-inner">
                <div class="error-icon-wrap not-found">
                    <i class="bi bi-file-earmark-x"></i>
                </div>
                <div class="error-title">Không tìm thấy hồ sơ</div>
                <p class="error-desc">
                    Không tìm thấy hồ sơ khớp với mã tra cứu và email bạn cung cấp.<br>
                    Vui lòng kiểm tra lại thông tin hoặc liên hệ phòng nhân sự nếu cần hỗ trợ.
                </p>
                <a href="${pageContext.request.contextPath}/application-lookup" class="btn-lookup" style="text-decoration:none; display:inline-flex; justify-content:center; max-width:300px; margin:0 auto;">
                    <i class="bi bi-arrow-left"></i> Thử lại
                </a>
            </div>
        </div>
    </c:if>

    <%-- ---- MODE: ERROR - Thiếu thông tin [KN-249] ---- --%>
    <c:if test="${param.error == 'MISSING_FIELDS'}">
        <div class="lookup-card">
            <div class="error-card-inner">
                <div class="error-icon-wrap missing">
                    <i class="bi bi-exclamation-triangle-fill"></i>
                </div>
                <div class="error-title">Thông tin chưa đầy đủ</div>
                <p class="error-desc">
                    Vui lòng nhập đầy đủ <strong style="color:#fbbf24;">mã tra cứu</strong> và <strong style="color:#fbbf24;">email</strong> để tìm kiếm hồ sơ.
                </p>
                <a href="${pageContext.request.contextPath}/application-lookup" class="btn-lookup" style="text-decoration:none; display:inline-flex; justify-content:center; max-width:300px; margin:0 auto;">
                    <i class="bi bi-arrow-left"></i> Quay lại nhập thông tin
                </a>
            </div>
        </div>
    </c:if>

</main>

<!-- ===== Footer ===== -->
<footer class="lookup-footer">
    &copy; 2026 IRMS Platform &bull; Thông tin tra cứu được bảo mật theo chính sách nội bộ
</footer>

<script>
    // [KN-249]: Validate form phía client trước khi submit
    (function() {
        const form = document.getElementById('lookupForm');
        if (!form) return;

        form.addEventListener('submit', function(e) {
            let valid = true;
            const codeInput = document.getElementById('candidateId');
            const emailInput = document.getElementById('email');
            const errCode  = document.getElementById('err-code');
            const errEmail = document.getElementById('err-email');

            // Validate mã tra cứu
            if (!codeInput || codeInput.value.trim() === '') {
                if (codeInput) codeInput.classList.add('is-invalid');
                if (errCode) errCode.style.display = 'block';
                valid = false;
            } else {
                if (codeInput) codeInput.classList.remove('is-invalid');
                if (errCode) errCode.style.display = 'none';
            }

            // Validate email
            const emailVal = emailInput ? emailInput.value.trim() : '';
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailVal || !emailRegex.test(emailVal)) {
                if (emailInput) emailInput.classList.add('is-invalid');
                if (errEmail) errEmail.style.display = 'block';
                valid = false;
            } else {
                if (emailInput) emailInput.classList.remove('is-invalid');
                if (errEmail) errEmail.style.display = 'none';
            }

            if (!valid) {
                e.preventDefault();
                return;
            }

            // Loading state
            const btn     = document.getElementById('submitBtn');
            const spinner = document.getElementById('btnSpinner');
            const icon    = document.getElementById('btnIcon');
            const text    = document.getElementById('btnText');
            if (btn && spinner && icon && text) {
                btn.disabled       = true;
                spinner.style.display = 'block';
                icon.style.display    = 'none';
                text.textContent   = 'Đang tra cứu...';
            }
        });

        // Clear validation on input
        ['candidateId', 'email'].forEach(function(id) {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener('input', function() {
                    el.classList.remove('is-invalid');
                    const errEl = document.getElementById('err-' + (id === 'candidateId' ? 'code' : 'email'));
                    if (errEl) errEl.style.display = 'none';
                });
            }
        });
    })();
</script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
