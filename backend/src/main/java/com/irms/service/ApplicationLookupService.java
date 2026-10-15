package com.irms.service;

import com.irms.dao.ApplicationLookupDAO;
import com.irms.model.ApplicationLookupResult;

/**
 * ==============================================================================
 * [KN-243] [KN-245] [KN-246] [KN-247]: Service tra cứu trạng thái hồ sơ công khai
 * ==============================================================================
 * Lớp nghiệp vụ trung gian giữa Controller và DAO.
 * Thực hiện:
 *  - [KN-245]: Validate đầu vào trước khi truy vấn DB
 *  - [KN-247]: Gọi DAO với xác thực kép (id + email)
 *  - [KN-246]: Đảm bảo chỉ trả về ApplicationLookupResult (không có trường nội bộ)
 * ==============================================================================
 */
public class ApplicationLookupService {

    private final ApplicationLookupDAO lookupDAO = new ApplicationLookupDAO();

    /**
     * [KN-245] [KN-247]: Tra cứu trạng thái hồ sơ theo mã và email.
     * Thực hiện validate cơ bản trước khi gọi DB.
     *
     * @param candidateId Mã tra cứu (ID hồ sơ) - bắt buộc
     * @param email       Email ứng viên - bắt buộc để xác minh quyền truy cập
     * @return ApplicationLookupResult nếu hợp lệ; null nếu không tìm thấy hoặc không khớp
     */
    public ApplicationLookupResult lookup(String candidateId, String email) {
        // [KN-245]: Kiểm tra đầu vào bắt buộc
        if (candidateId == null || candidateId.trim().isEmpty()) {
            return null;
        }
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        // [KN-245]: Validate định dạng email cơ bản
        if (!isValidEmail(email.trim())) {
            return null;
        }
        // [KN-247]: Gọi DAO - xác thực kép id + email trong DB
        return lookupDAO.findByIdAndEmail(candidateId.trim(), email.trim());
    }

    /**
     * Kiểm tra định dạng email hợp lệ (đơn giản, phía server).
     */
    private boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".") && email.length() >= 5;
    }
}
