<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<div id="toastNotificationContainer" class="toast-notification-container" aria-live="polite" aria-atomic="true">
    <c:if test="${not empty sessionScope.flashSuccess || param.loginSuccess == 'true'}">
        <div class="toast-notification-item toast-item-success" data-auto-dismiss="5000">
            <div class="toast-icon-box"><i class="bi bi-check-circle-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Thành công</div>
                <div class="toast-message-text">
                    <c:choose>
                        <c:when test="${not empty sessionScope.flashSuccess}">
                            <c:out value="${sessionScope.flashSuccess}" />
                        </c:when>
                        <c:otherwise>
                            Đăng nhập thành công! Chào mừng <c:out value="${not empty currentUser.fullName ? currentUser.fullName : 'bạn'}" /> trở lại hệ thống.
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
        <c:remove var="flashSuccess" scope="session" />
        <script>
            if (window.location.search.includes('loginSuccess=true')) {
                const u = new URL(window.location);
                u.searchParams.delete('loginSuccess');
                window.history.replaceState({}, document.title, u.pathname + (u.search ? u.search : ''));
            }
        </script>
    </c:if>

    <c:if test="${not empty sessionScope.flashError}">
        <div class="toast-notification-item toast-item-error" data-auto-dismiss="6500">
            <div class="toast-icon-box"><i class="bi bi-exclamation-octagon-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Đã xảy ra lỗi</div>
                <div class="toast-message-text"><c:out value="${sessionScope.flashError}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
        <c:remove var="flashError" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.flashWarning}">
        <div class="toast-notification-item toast-item-warning" data-auto-dismiss="7000">
            <div class="toast-icon-box"><i class="bi bi-exclamation-triangle-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Cảnh báo</div>
                <div class="toast-message-text"><c:out value="${sessionScope.flashWarning}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
        <c:remove var="flashWarning" scope="session" />
    </c:if>

    <c:if test="${not empty sessionScope.flashInfo}">
        <div class="toast-notification-item toast-item-info" data-auto-dismiss="5000">
            <div class="toast-icon-box"><i class="bi bi-info-circle-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Thông báo</div>
                <div class="toast-message-text"><c:out value="${sessionScope.flashInfo}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
        <c:remove var="flashInfo" scope="session" />
    </c:if>

    <c:if test="${not empty successMessage}">
        <div class="toast-notification-item toast-item-success" data-auto-dismiss="5000">
            <div class="toast-icon-box"><i class="bi bi-check-circle-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Thành công</div>
                <div class="toast-message-text"><c:out value="${successMessage}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
    </c:if>

    <c:if test="${not empty errorMessage}">
        <div class="toast-notification-item toast-item-error" data-auto-dismiss="6500">
            <div class="toast-icon-box"><i class="bi bi-exclamation-octagon-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Đã xảy ra lỗi</div>
                <div class="toast-message-text"><c:out value="${errorMessage}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
    </c:if>

    <c:if test="${not empty warningMessage}">
        <div class="toast-notification-item toast-item-warning" data-auto-dismiss="7000">
            <div class="toast-icon-box"><i class="bi bi-exclamation-triangle-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Cảnh báo</div>
                <div class="toast-message-text"><c:out value="${warningMessage}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
    </c:if>

    <c:if test="${not empty infoMessage}">
        <div class="toast-notification-item toast-item-info" data-auto-dismiss="5000">
            <div class="toast-icon-box"><i class="bi bi-info-circle-fill"></i></div>
            <div class="toast-body-content">
                <div class="toast-header-title">Thông báo</div>
                <div class="toast-message-text"><c:out value="${infoMessage}" /></div>
            </div>
            <button type="button" class="toast-close-btn" onclick="window.dismissToast(this.closest('.toast-notification-item'))" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
            <div class="toast-progress-bar"></div>
        </div>
    </c:if>
</div>

