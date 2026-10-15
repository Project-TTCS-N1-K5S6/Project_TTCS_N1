package com.irms.service;

import com.irms.dao.SharedCatalogDAO;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;

import java.util.List;

/**
 * Tầng nghiệp vụ Danh mục dùng chung (Shared Catalog Service)
 * Quản lý các danh mục: Nguồn ứng viên, Lý do loại, Địa điểm làm việc, Hình thức làm việc;
 * Hỗ trợ kiểm tra ràng buộc toàn vẹn dữ liệu trước khi xóa (chống xóa mục đang được sử dụng)
 */
public class SharedCatalogService {
    private final SharedCatalogDAO catalogDAO = new SharedCatalogDAO();

    /**
     * Chức năng: Lấy danh sách các mục danh mục theo một phân loại cụ thể
     *
     * @param type Phân loại danh mục
     * @return Danh sách các mục danh mục tương ứng
     */
    public List<SharedCatalogItem> getItems(SharedCatalogType type) {
        return catalogDAO.findAll(type);
    }

    /**
     * Chức năng: Lấy tất cả các mục danh mục dùng chung thuộc mọi phân loại
     *
     * @return Danh sách toàn bộ mục danh mục
     */
    public List<SharedCatalogItem> getItems() {
        return catalogDAO.findAll();
    }

    /**
     * Chức năng: Đếm tổng số lượng mục của một phân loại danh mục
     *
     * @param type Phân loại danh mục
     * @return Số lượng mục
     */
    public int countItems(SharedCatalogType type) {
        return catalogDAO.countByType(type);
    }

    /**
     * Chức năng: Thêm mới một giá trị vào phân loại danh mục sau khi kiểm tra hợp lệ
     *
     * @param type  Phân loại danh mục
     * @param value Tên giá trị cần thêm
     * @return true nếu thêm thành công
     */
    public boolean create(SharedCatalogType type, String value) {
        return catalogDAO.insert(type, validateValue(value));
    }

    /**
     * Chức năng: Cập nhật tên giá trị của mục danh mục và kiểm tra tính hợp lệ
     *
     * @param id    ID mục danh mục cần sửa
     * @param type  Phân loại danh mục
     * @param value Tên giá trị mới
     * @return true nếu cập nhật thành công
     */
    public boolean update(String id, SharedCatalogType type, String value) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã giá trị danh mục.");
        }
        if (!catalogDAO.isValueInType(id, type)) {
            throw new IllegalArgumentException("Giá trị không thuộc danh mục đã chọn.");
        }
        return catalogDAO.update(id.trim(), validateValue(value));
    }

    /**
     * Chức năng: Xóa mục danh mục, chặn xóa nếu mục đó đang được ứng viên hoặc yêu cầu tuyển dụng sử dụng
     *
     * @param id   ID mục danh mục cần xóa
     * @param type Phân loại danh mục
     * @return true nếu xóa thành công
     */
    public boolean delete(String id, SharedCatalogType type) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã giá trị danh mục.");
        }
        SharedCatalogItem item = catalogDAO.findById(id.trim());
        if (item == null) {
            return false;
        }
        if (item.getType() != type) {
            throw new IllegalArgumentException("Giá trị không thuộc danh mục đã chọn.");
        }
        if (item.getReferenceCount() > 0) {
            throw new IllegalStateException("Giá trị đang được " + item.getReferenceCount()
                    + " hồ sơ/yêu cầu sử dụng nên không thể xóa.");
        }
        if (!catalogDAO.delete(id.trim())) {
            throw new IllegalStateException("Không thể xóa giá trị. Có thể giá trị vừa được sử dụng.");
        }
        return true;
    }

    /**
     * Chức năng: Thay đổi vị trí hiển thị của mục danh mục (lên hoặc xuống)
     *
     * @param id        ID mục danh mục
     * @param type      Phân loại danh mục
     * @param direction Hướng di chuyển ("up" hoặc "down")
     * @return true nếu hoán đổi thành công
     */
    public boolean move(String id, SharedCatalogType type, String direction) {
        if (!"up".equals(direction) && !"down".equals(direction)) {
            throw new IllegalArgumentException("Hướng sắp xếp không hợp lệ.");
        }
        return catalogDAO.move(id, type, "up".equals(direction) ? -1 : 1);
    }

    /**
     * Chức năng: Kiểm tra xem một ID danh mục có thuộc phân loại type chỉ định hay không
     *
     * @param id   ID mục danh mục
     * @param type Phân loại danh mục
     * @return true nếu hợp lệ
     */
    public boolean isValueInType(String id, SharedCatalogType type) {
        return id == null || id.trim().isEmpty() || catalogDAO.isValueInType(id.trim(), type);
    }

    /**
     * Chức năng: Kiểm tra và chuẩn hóa chuỗi giá trị danh mục (không được rỗng, tối đa 255 ký tự)
     *
     * @param value Giá trị đầu vào
     * @return Chuỗi đã loại bỏ khoảng trắng thừa
     */
    private String validateValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên giá trị danh mục không được để trống.");
        }
        String normalized = value.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Tên giá trị danh mục không được vượt quá 255 ký tự.");
        }
        return normalized;
    }
}
