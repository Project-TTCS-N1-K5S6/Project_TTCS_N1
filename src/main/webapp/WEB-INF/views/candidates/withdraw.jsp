<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="referrer" content="no-referrer">
    <title>Rút hồ sơ ứng tuyển | IRMS Careers</title>
    <style>
        * { box-sizing: border-box; }
        body { margin: 0; min-height: 100vh; display: grid; place-items: center; padding: 24px; background: #f1f5f9; color: #334155; font-family: "Segoe UI", Arial, sans-serif; }
        .card { width: min(100%, 560px); padding: 36px; background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; box-shadow: 0 12px 32px rgba(15, 23, 42, .08); }
        .brand { color: #2563eb; font-size: 13px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
        h1 { margin: 12px 0; color: #0f172a; font-size: 25px; }
        p { line-height: 1.65; }
        dl { margin: 24px 0; padding: 18px; background: #f8fafc; border-radius: 10px; }
        dt { margin-top: 12px; color: #64748b; font-size: 13px; }
        dt:first-child { margin-top: 0; }
        dd { margin: 4px 0 0; color: #0f172a; font-weight: 600; overflow-wrap: anywhere; }
        .notice { padding: 14px 16px; border-left: 4px solid #f97316; border-radius: 6px; background: #fff7ed; color: #9a3412; font-size: 14px; }
        .error { padding: 14px 16px; border-radius: 8px; background: #fef2f2; color: #991b1b; }
        .success { padding: 14px 16px; border-radius: 8px; background: #f0fdf4; color: #166534; }
        button { width: 100%; margin-top: 22px; padding: 13px 18px; border: 0; border-radius: 8px; background: #dc2626; color: #fff; font-size: 15px; font-weight: 700; cursor: pointer; }
        button:hover { background: #b91c1c; }
        .help { margin-top: 18px; color: #64748b; font-size: 12px; }
        @media (max-width: 480px) { .card { padding: 26px 22px; } }
    </style>
</head>
<body>
<main class="card">
    <div class="brand">IRMS Careers</div>
    <c:choose>
        <c:when test="${withdrawalCompleted}">
            <h1>Đã rút hồ sơ</h1>
            <div class="success">Yêu cầu của bạn đã được xác nhận. Hồ sơ đã được chuyển sang trạng thái “Ứng viên rút hồ sơ” và không còn trong pipeline đang hoạt động.</div>
            <p>Recruiter phụ trách đã được thông báo. Cảm ơn bạn đã quan tâm đến cơ hội nghề nghiệp tại IRMS.</p>
        </c:when>
        <c:when test="${not empty withdrawalError}">
            <h1>Không thể rút hồ sơ</h1>
            <div class="error"><c:out value="${withdrawalError}" /></div>
            <p class="help">Vui lòng kiểm tra email xác nhận hoặc liên hệ bộ phận Tuyển dụng nếu bạn cần hỗ trợ.</p>
        </c:when>
        <c:otherwise>
            <h1>Xác nhận rút hồ sơ ứng tuyển</h1>
            <p>Bạn đang yêu cầu rút hồ sơ khỏi quy trình tuyển dụng. Vui lòng kiểm tra thông tin và xác nhận lần cuối.</p>
            <dl>
                <dt>Ứng viên</dt><dd><c:out value="${withdrawal.fullName}" /></dd>
                <dt>Vị trí ứng tuyển</dt><dd><c:out value="${withdrawal.requisitionTitle}" /></dd>
                <dt>Mã hồ sơ</dt><dd><c:out value="${withdrawal.trackingCode}" /></dd>
            </dl>
            <div class="notice">Thao tác này sẽ chuyển hồ sơ sang trạng thái “Ứng viên rút hồ sơ”. Recruiter phụ trách sẽ nhận được thông báo. Liên kết chỉ sử dụng một lần.</div>
            <form method="post" action="${pageContext.request.contextPath}/careers/withdraw">
                <input type="hidden" name="token" value="<c:out value='${withdrawalToken}' />">
                <button type="submit">Xác nhận rút hồ sơ</button>
            </form>
            <p class="help">Nếu bạn không muốn rút hồ sơ, chỉ cần đóng trang này. Trạng thái hồ sơ sẽ không thay đổi.</p>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
