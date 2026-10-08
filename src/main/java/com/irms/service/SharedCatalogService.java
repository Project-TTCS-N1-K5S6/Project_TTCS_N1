package com.irms.service;

import com.irms.dao.SharedCatalogDAO;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;

import java.util.List;

public class SharedCatalogService {
    private final SharedCatalogDAO catalogDAO = new SharedCatalogDAO();

    public List<SharedCatalogItem> getItems(SharedCatalogType type) {
        return catalogDAO.findAll(type);
    }

    public List<SharedCatalogItem> getItems() {
        return catalogDAO.findAll();
    }

    public int countItems(SharedCatalogType type) {
        return catalogDAO.countByType(type);
    }

    public boolean create(SharedCatalogType type, String value) {
        return catalogDAO.insert(type, validateValue(value));
    }

    public boolean update(String id, SharedCatalogType type, String value) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã giá trị danh mục.");
        }
        if (!catalogDAO.isValueInType(id, type)) {
            throw new IllegalArgumentException("Giá trị không thuộc danh mục đã chọn.");
        }
        return catalogDAO.update(id.trim(), validateValue(value));
    }

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

    public boolean move(String id, SharedCatalogType type, String direction) {
        if (!"up".equals(direction) && !"down".equals(direction)) {
            throw new IllegalArgumentException("Hướng sắp xếp không hợp lệ.");
        }
        return catalogDAO.move(id, type, "up".equals(direction) ? -1 : 1);
    }

    public boolean isValueInType(String id, SharedCatalogType type) {
        return id == null || id.trim().isEmpty() || catalogDAO.isValueInType(id.trim(), type);
    }

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
