package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.dao.DepartmentDAO;
import com.irms.model.AuditLog;
import com.irms.model.Department;
import com.irms.model.User;
import com.irms.util.SecurityUtil;

import java.util.*;

/**
 * Xử lý nghiệp vụ Danh mục cơ cấu tổ chức & phòng ban
 * Hỗ trợ cấu trúc cây phân cấp nhiều cấp, người phụ trách và kiểm soát xóa an toàn
 */
public class DepartmentService {

    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public List<Department> getAllDepartments() {
        return departmentDAO.findAll();
    }

    public List<Department> getAllDepartments(String search, String status) {
        return departmentDAO.findAll(search, status);
    }

    public Department getDepartmentById(String id) {
        return departmentDAO.findById(id);
    }

    public Department getDepartmentByCode(String code) {
        return departmentDAO.findByCode(code);
    }

    public List<User> getPotentialManagers() {
        return departmentDAO.findPotentialManagers();
    }

    /**
     * Xây dựng cấu trúc cây phòng ban (Tree Hierarchy)
     */
    public List<Department> getDepartmentTree(String search, String status) {
        List<Department> allDepts = departmentDAO.findAll(null, null);
        if (allDepts.isEmpty()) return Collections.emptyList();

        Map<String, Department> deptMap = new LinkedHashMap<>();
        for (Department d : allDepts) {
            d.getChildren().clear();
            deptMap.put(d.getId(), d);
        }

        List<Department> roots = new ArrayList<>();

        for (Department d : allDepts) {
            String pId = d.getParentId();
            if (pId == null || pId.trim().isEmpty() || !deptMap.containsKey(pId) || pId.equals(d.getId())) {
                roots.add(d);
            } else {
                Department parent = deptMap.get(pId);
                parent.addChild(d);
            }
        }

        // Tính toán độ sâu (level) cho từng node
        for (Department root : roots) {
            calculateTreeLevels(root, 0);
        }

        // Lọc theo tìm kiếm hoặc trạng thái nếu có
        if ((search != null && !search.trim().isEmpty()) || (status != null && !status.trim().isEmpty())) {
            String kw = (search != null) ? search.trim().toLowerCase() : "";
            String st = (status != null) ? status.trim().toUpperCase() : "";

            Set<String> matchedIds = new HashSet<>();
            for (Department d : allDepts) {
                boolean matchSearch = kw.isEmpty() ||
                        (d.getName() != null && d.getName().toLowerCase().contains(kw)) ||
                        (d.getCode() != null && d.getCode().toLowerCase().contains(kw)) ||
                        (d.getDescription() != null && d.getDescription().toLowerCase().contains(kw));

                boolean matchStatus = st.isEmpty() || st.equalsIgnoreCase(d.getStatus());

                if (matchSearch && matchStatus) {
                    // Thêm bản thân và toàn bộ tổ tiên lên tới root để giữ ngữ cảnh cây
                    String currId = d.getId();
                    while (currId != null && deptMap.containsKey(currId)) {
                        matchedIds.add(currId);
                        currId = deptMap.get(currId).getParentId();
                    }
                }
            }

            return filterTreeByMatchedIds(roots, matchedIds);
        }

        return roots;
    }

    private void calculateTreeLevels(Department node, int currentLevel) {
        node.setLevel(currentLevel);
        if (node.hasChildren()) {
            for (Department child : node.getChildren()) {
                calculateTreeLevels(child, currentLevel + 1);
            }
        }
    }

    private List<Department> filterTreeByMatchedIds(List<Department> nodes, Set<String> matchedIds) {
        List<Department> filtered = new ArrayList<>();
        for (Department node : nodes) {
            if (matchedIds.contains(node.getId())) {
                Department copy = cloneDepartmentNode(node);
                copy.setChildren(filterTreeByMatchedIds(node.getChildren(), matchedIds));
                filtered.add(copy);
            }
        }
        return filtered;
    }

    private Department cloneDepartmentNode(Department src) {
        Department d = new Department();
        d.setId(src.getId());
        d.setCode(src.getCode());
        d.setName(src.getName());
        d.setDescription(src.getDescription());
        d.setParentId(src.getParentId());
        d.setParentCode(src.getParentCode());
        d.setParentName(src.getParentName());
        d.setManagerId(src.getManagerId());
        d.setManagerName(src.getManagerName());
        d.setManagerEmail(src.getManagerEmail());
        d.setManagerJobTitle(src.getManagerJobTitle());
        d.setManagerAvatarUrl(src.getManagerAvatarUrl());
        d.setStatus(src.getStatus());
        d.setCreatedAt(src.getCreatedAt());
        d.setUpdatedAt(src.getUpdatedAt());
        d.setUserCount(src.getUserCount());
        d.setChildrenCount(src.getChildrenCount());
        d.setOpenRequisitionCount(src.getOpenRequisitionCount());
        d.setTotalRequisitionCount(src.getTotalRequisitionCount());
        d.setLevel(src.getLevel());
        return d;
    }

    /**
     * Lấy danh sách phòng ban dạng phẳng có thụt lề cấp bậc để hiển thị trong thẻ <select>
     * Tự động loại bỏ excludeDeptId và tất cả các node con của nó để chống tạo vòng lặp cây
     */
    public List<Department> getFlatDepartmentsWithIndentation(String excludeDeptId) {
        List<Department> tree = getDepartmentTree(null, null);
        List<Department> result = new ArrayList<>();

        Set<String> excludedSubtree = new HashSet<>();
        if (excludeDeptId != null && !excludeDeptId.trim().isEmpty()) {
            collectDescendantIds(excludeDeptId.trim(), excludedSubtree);
            excludedSubtree.add(excludeDeptId.trim());
        }

        for (Department root : tree) {
            flattenTree(root, result, excludedSubtree);
        }
        return result;
    }

    /**
     * Lấy danh sách cây phòng ban dạng phẳng theo thứ tự DFS (cha đi trước các con)
     * Rất tiện lợi cho việc render trên giao diện bảng Tree Table trong JSP
     */
    public List<Department> getFlatTreeDepartments(String search, String status) {
        List<Department> tree = getDepartmentTree(search, status);
        List<Department> result = new ArrayList<>();
        for (Department root : tree) {
            flattenTree(root, result, Collections.emptySet());
        }
        return result;
    }

    private void flattenTree(Department node, List<Department> list, Set<String> excludedIds) {
        if (excludedIds.contains(node.getId())) return;
        list.add(node);
        if (node.hasChildren()) {
            for (Department child : node.getChildren()) {
                flattenTree(child, list, excludedIds);
            }
        }
    }

    private void collectDescendantIds(String parentId, Set<String> descendants) {
        List<Department> all = departmentDAO.findAll();
        for (Department d : all) {
            if (parentId.equals(d.getParentId())) {
                descendants.add(d.getId());
                collectDescendantIds(d.getId(), descendants);
            }
        }
    }

    /**
     * Kiểm tra ngăn chặn vòng lặp phân cấp cây (Cycle Detection)
     * Ví dụ: A là cha của B thì B không được trở thành cha của A.
     */
    public void validateNoCycle(String deptId, String newParentId) {
        if (deptId == null || newParentId == null || newParentId.trim().isEmpty()) {
            return;
        }
        if (deptId.trim().equals(newParentId.trim())) {
            throw new IllegalArgumentException("Không thể chọn chính phòng ban này làm phòng ban cha của nó!");
        }

        // Duyệt ngược từ newParentId lên đỉnh cây, nếu gặp deptId -> Báo lỗi vòng lặp
        Map<String, String> parentMap = new HashMap<>();
        for (Department d : departmentDAO.findAll()) {
            if (d.getParentId() != null) {
                parentMap.put(d.getId(), d.getParentId());
            }
        }

        String current = newParentId.trim();
        Set<String> visited = new HashSet<>();
        while (current != null) {
            if (current.equals(deptId.trim())) {
                throw new IllegalArgumentException("Không thể chọn phòng ban này làm phòng ban cha vì sẽ tạo thành vòng lặp phân cấp cây!");
            }
            if (!visited.add(current)) {
                // Đã có vòng lặp sẵn trong dữ liệu
                break;
            }
            current = parentMap.get(current);
        }
    }

    /**
     * Thêm mới phòng ban
     */
    public Department createDepartment(Department dept, String userId, String ip, String userAgent) {
        validateDepartmentData(dept, null);

        if (dept.getId() == null || dept.getId().trim().isEmpty()) {
            dept.setId(SecurityUtil.generateUUID());
        }
        dept.setCreatedBy(userId);
        dept.setUpdatedBy(userId);

        boolean success = departmentDAO.insert(dept);
        if (!success) {
            throw new RuntimeException("Lỗi lưu phòng ban vào cơ sở dữ liệu!");
        }

        logAudit(userId, "DEPARTMENT_CREATED", "DEPARTMENT", dept.getId(),
                "Tạo mới phòng ban: " + dept.getCode() + " - " + dept.getName() +
                (dept.getParentId() != null ? " (Trực thuộc: " + dept.getParentId() + ")" : " (Cấp cao nhất)"),
                ip, userAgent);

        return departmentDAO.findById(dept.getId());
    }

    /**
     * Cập nhật thông tin phòng ban
     */
    public Department updateDepartment(Department dept, String userId, String ip, String userAgent) {
        if (dept.getId() == null || dept.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID phòng ban không hợp lệ!");
        }

        Department existing = departmentDAO.findById(dept.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Phòng ban không tồn tại trên hệ thống!");
        }

        validateDepartmentData(dept, dept.getId());
        validateNoCycle(dept.getId(), dept.getParentId());

        dept.setUpdatedBy(userId);

        boolean success = departmentDAO.update(dept);
        if (!success) {
            throw new RuntimeException("Lỗi cập nhật thông tin phòng ban!");
        }

        logAudit(userId, "DEPARTMENT_UPDATED", "DEPARTMENT", dept.getId(),
                "Cập nhật phòng ban: " + dept.getCode() + " - " + dept.getName(),
                ip, userAgent);

        return departmentDAO.findById(dept.getId());
    }

    /**
     * Chuyển đổi trạng thái (Kích hoạt / Ngừng áp dụng)
     */
    public void changeStatus(String id, String status, String userId, String ip, String userAgent) {
        Department existing = departmentDAO.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Phòng ban không tồn tại!");
        }

        status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : Department.STATUS_ACTIVE;
        if (!Department.STATUS_ACTIVE.equals(status) && !Department.STATUS_INACTIVE.equals(status)) {
            throw new IllegalArgumentException("Trạng thái phòng ban không hợp lệ!");
        }

        boolean success = departmentDAO.updateStatus(id, status, userId);
        if (!success) {
            throw new RuntimeException("Lỗi cập nhật trạng thái phòng ban!");
        }

        logAudit(userId, "DEPARTMENT_STATUS_CHANGED", "DEPARTMENT", id,
                "Chuyển trạng thái phòng ban '" + existing.getCode() + "' sang: " + status,
                ip, userAgent);
    }

    /**
     * Kết quả kiểm tra điều kiện xóa phòng ban
     */
    public static class DeleteCheckResult {
        public boolean canDelete = true;
        public boolean hasOpenRequisitions = false;
        public boolean hasChildren = false;
        public boolean hasUsers = false;
        public boolean hasSalaryRanges = false;
        public boolean hasHistoryRequisitions = false;
        public String message = "Phòng ban đủ điều kiện xóa.";
    }

    /**
     * Kiểm tra điều kiện có được phép xóa phòng ban hay không
     */
    public DeleteCheckResult checkCanDelete(String id) {
        DeleteCheckResult res = new DeleteCheckResult();
        Department d = departmentDAO.findById(id);
        if (d == null) {
            res.canDelete = false;
            res.message = "Phòng ban không tồn tại!";
            return res;
        }

        // 1. BUSINESS RULE BẮT BUỘC: Nếu phòng ban đang có yêu cầu tuyển dụng mở => KHÔNG ĐƯỢC XÓA
        int openReqs = departmentDAO.countOpenRecruitmentRequests(id);
        if (openReqs > 0) {
            res.canDelete = false;
            res.hasOpenRequisitions = true;
            res.message = "Không thể xóa phòng ban vì đang có yêu cầu tuyển dụng mở. Vui lòng ngừng áp dụng phòng ban thay vì xóa.";
            return res;
        }

        // 2. Kiểm tra phòng ban con
        int childCount = departmentDAO.countChildren(id);
        if (childCount > 0) {
            res.canDelete = false;
            res.hasChildren = true;
            res.message = "Không thể xóa phòng ban khi còn " + childCount + " phòng ban con trực thuộc. Vui lòng chuyển hoặc xóa các phòng ban con trước.";
            return res;
        }

        // 3. Kiểm tra nhân sự trực thuộc
        int users = departmentDAO.countUsers(id);
        if (users > 0) {
            res.canDelete = false;
            res.hasUsers = true;
            res.message = "Không thể xóa phòng ban đang có " + users + " nhân sự trực thuộc. Vui lòng chuyển nhân sự sang phòng ban khác hoặc chọn 'Ngừng áp dụng'.";
            return res;
        }

        // 4. Kiểm tra dải lương chức danh
        int salaries = departmentDAO.countSalaryRanges(id);
        if (salaries > 0) {
            res.canDelete = false;
            res.hasSalaryRanges = true;
            res.message = "Không thể xóa phòng ban đang liên kết với dải lương chức danh. Vui lòng chọn 'Ngừng áp dụng' để bảo toàn dữ liệu lịch sử.";
            return res;
        }

        // 5. Kiểm tra lịch sử yêu cầu tuyển dụng đã đóng
        int totalReqs = departmentDAO.countTotalRecruitmentRequests(id);
        if (totalReqs > 0) {
            res.canDelete = false;
            res.hasHistoryRequisitions = true;
            res.message = "Phòng ban có dữ liệu lịch sử yêu cầu tuyển dụng đã lưu trữ. Vui lòng chọn 'Ngừng áp dụng' thay vì xóa hoàn toàn.";
            return res;
        }

        return res;
    }

    /**
     * Xóa phòng ban - TUÂN THỦ NGHIÊM NGẶT BUSINESS RULE:
     * - Nếu có yêu cầu tuyển dụng mở => TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA!
     * - Báo lỗi cụ thể: "Không thể xóa phòng ban vì đang có yêu cầu tuyển dụng mở. Vui lòng ngừng áp dụng phòng ban thay vì xóa."
     */
    public boolean deleteDepartment(String id, String userId, String ip, String userAgent) {
        DeleteCheckResult check = checkCanDelete(id);
        if (!check.canDelete) {
            throw new IllegalStateException(check.message);
        }

        Department existing = departmentDAO.findById(id);
        boolean success = departmentDAO.delete(id);
        if (!success) {
            throw new RuntimeException("Lỗi khi thực hiện xóa phòng ban trong cơ sở dữ liệu!");
        }

        logAudit(userId, "DEPARTMENT_DELETED", "DEPARTMENT", id,
                "Đã xóa phòng ban: " + (existing != null ? existing.getCode() + " - " + existing.getName() : id),
                ip, userAgent);

        return true;
    }

    // Tương thích với các gọi hàm cũ
    public boolean createDepartment(String code, String name, String description) {
        Department d = new Department(SecurityUtil.generateUUID(), code, name, description);
        return departmentDAO.insert(d);
    }

    public boolean updateDepartment(String id, String code, String name, String description) {
        Department d = new Department(id, code, name, description);
        return departmentDAO.update(d);
    }

    public boolean deleteDepartment(String id) {
        return deleteDepartment(id, null, "127.0.0.1", "System");
    }

    private void validateDepartmentData(Department d, String excludeId) {
        if (d.getCode() == null || d.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã phòng ban không được để trống!");
        }
        d.setCode(d.getCode().trim().toUpperCase());

        if (d.getName() == null || d.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên phòng ban không được để trống!");
        }
        d.setName(d.getName().trim());

        if (departmentDAO.existsByCode(d.getCode(), excludeId)) {
            throw new IllegalArgumentException("Mã phòng ban '" + d.getCode() + "' đã tồn tại trên hệ thống!");
        }

        if (d.getParentId() != null && !d.getParentId().trim().isEmpty()) {
            Department parent = departmentDAO.findById(d.getParentId().trim());
            if (parent == null) {
                throw new IllegalArgumentException("Phòng ban cha không tồn tại!");
            }
        }
    }

    private void logAudit(String userId, String action, String entityType, String entityId, String desc, String ip, String userAgent) {
        try {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(userId);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDescription(desc);
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        } catch (Exception e) {
            // Không làm ngắt luồng chính nếu lỗi ghi audit
        }
    }
}
