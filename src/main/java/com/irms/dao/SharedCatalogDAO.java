package com.irms.dao;

import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Data Access Object quản lý dữ liệu Danh mục dùng chung (bảng shared_catalogs)
 * Hỗ trợ các thao tác CRUD, sắp xếp thứ tự hiển thị (move up/down) và tính toán số lượng tham chiếu (reference_count)
 */
public class SharedCatalogDAO extends BaseDAO {

    private static final String SELECT_COLUMNS =
            "SELECT sc.id, sc.type_code, sc.value, sc.display_order, " +
            "CASE sc.type_code " +
            "WHEN 'APPLICATION_SOURCE' THEN (SELECT COUNT(*) FROM candidates c WHERE c.application_source_id = sc.id) " +
            "WHEN 'REJECTION_REASON' THEN (SELECT COUNT(*) FROM candidates c WHERE c.rejection_reason_id = sc.id) " +
            "WHEN 'WORK_LOCATION' THEN (SELECT COUNT(*) FROM recruitment_requisitions r WHERE r.work_location_id = sc.id) " +
            "WHEN 'WORK_MODE' THEN (SELECT COUNT(*) FROM recruitment_requisitions r WHERE r.work_mode_id = sc.id) " +
            "ELSE 0 END AS reference_count FROM shared_catalogs sc ";

    private static final String SELECT_COLUMNS_SIMPLE =
            "SELECT sc.id, sc.type_code, sc.value, sc.display_order, 0 AS reference_count FROM shared_catalogs sc ";

    /**
     * Chức năng: Lấy danh sách các mục danh mục theo loại (type), kèm số lượng bản ghi đang tham chiếu
     *
     * @param type Phân loại danh mục (APPLICATION_SOURCE, REJECTION_REASON, WORK_LOCATION, WORK_MODE)
     * @return Danh sách mục danh mục sắp xếp theo thứ tự hiển thị
     */
    public List<SharedCatalogItem> findAll(SharedCatalogType type) {
        List<SharedCatalogItem> items = new ArrayList<>();
        String sql = SELECT_COLUMNS + "WHERE sc.type_code = ? ORDER BY sc.display_order, sc.value, sc.id";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, type.getCode());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể tải danh mục có reference_count, chuyển sang chế độ cơ bản: " + e.getMessage());
            String fallbackSql = SELECT_COLUMNS_SIMPLE + "WHERE sc.type_code = ? ORDER BY sc.display_order, sc.value, sc.id";
            try (Connection conn = getConnection();
                 PreparedStatement statement = conn.prepareStatement(fallbackSql)) {
                statement.setString(1, type.getCode());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Không thể tải danh mục " + type.getCode(), ex);
                throw new IllegalStateException("Không thể tải danh sách danh mục.", ex);
            }
        }
        return items;
    }

    /**
     * Chức năng: Lấy tất cả các mục danh mục dùng chung thuộc mọi phân loại
     *
     * @return Danh sách tất cả mục danh mục
     */
    public List<SharedCatalogItem> findAll() {
        List<SharedCatalogItem> items = new ArrayList<>();
        String sql = SELECT_COLUMNS + "ORDER BY sc.type_code, sc.display_order, sc.value, sc.id";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                items.add(map(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể tải danh mục dùng chung có reference_count, chuyển sang chế độ cơ bản: " + e.getMessage());
            String fallbackSql = SELECT_COLUMNS_SIMPLE + "ORDER BY sc.type_code, sc.display_order, sc.value, sc.id";
            try (Connection conn = getConnection();
                 PreparedStatement statement = conn.prepareStatement(fallbackSql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    items.add(map(rs));
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Không thể tải danh mục dùng chung", ex);
                throw new IllegalStateException("Không thể tải danh sách danh mục.", ex);
            }
        }
        return items;
    }

    /**
     * Chức năng: Đếm tổng số lượng mục danh mục thuộc một phân loại cụ thể
     *
     * @param type Phân loại danh mục cần đếm
     * @return Số lượng mục danh mục
     */
    public int countByType(SharedCatalogType type) {
        String sql = "SELECT COUNT(*) FROM shared_catalogs WHERE type_code = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, type.getCode());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể đếm danh mục " + type.getCode(), e);
            throw new IllegalStateException("Không thể kiểm tra danh mục.", e);
        }
    }

    /**
     * Chức năng: Thêm mới một giá trị danh mục và tự động tính thứ tự hiển thị display_order tiếp theo
     *
     * @param type  Phân loại danh mục
     * @param value Tên/giá trị của mục danh mục
     * @return true nếu thêm thành công, ngược lại false
     */
    public boolean insert(SharedCatalogType type, String value) {
        String sql = "INSERT INTO shared_catalogs (id, type_code, value, display_order) " +
                     "VALUES (?, ?, ?, ?)";
        String orderSql = "SELECT COALESCE(MAX(display_order), -1) + 1 FROM shared_catalogs WHERE type_code = ?";
        try (Connection conn = getConnection()) {
            int nextOrder;
            try (PreparedStatement orderStatement = conn.prepareStatement(orderSql)) {
                orderStatement.setString(1, type.getCode());
                try (ResultSet rs = orderStatement.executeQuery()) {
                    rs.next();
                    nextOrder = rs.getInt(1);
                }
            }
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, SecurityUtil.generateUUID());
                statement.setString(2, type.getCode());
                statement.setString(3, value);
                statement.setInt(4, nextOrder);
                return statement.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể thêm giá trị danh mục " + type.getCode(), e);
            return false;
        }
    }

    /**
     * Chức năng: Cập nhật giá trị hiển thị (value) của mục danh mục theo ID
     *
     * @param id    ID của mục danh mục cần cập nhật
     * @param value Giá trị hiển thị mới
     * @return true nếu cập nhật thành công, ngược lại false
     */
    public boolean update(String id, String value) {
        String sql = "UPDATE shared_catalogs SET value = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, value);
            statement.setString(2, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể cập nhật giá trị danh mục " + id, e);
            return false;
        }
    }

    /**
     * Chức năng: Tìm kiếm thông tin chi tiết một mục danh mục theo mã ID
     *
     * @param id ID của mục danh mục
     * @return Thực thể SharedCatalogItem nếu tìm thấy, ngược lại null
     */
    public SharedCatalogItem findById(String id) {
        String sql = SELECT_COLUMNS + "WHERE sc.id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể tải giá trị danh mục có reference_count, thử chế độ cơ bản: " + id, e);
            String fallbackSql = SELECT_COLUMNS_SIMPLE + "WHERE sc.id = ?";
            try (Connection conn = getConnection();
                 PreparedStatement statement = conn.prepareStatement(fallbackSql)) {
                statement.setString(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    return rs.next() ? map(rs) : null;
                }
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Không thể tải giá trị danh mục " + id, ex);
                throw new IllegalStateException("Không thể kiểm tra giá trị danh mục.", ex);
            }
        }
    }

    /**
     * Chức năng: Xóa một mục danh mục theo ID
     *
     * @param id ID của mục danh mục cần xóa
     * @return true nếu xóa thành công, ngược lại false
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM shared_catalogs WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể xóa giá trị danh mục " + id, e);
            return false;
        }
    }

    /**
     * Chức năng: Hoán đổi thứ tự hiển thị display_order của mục danh mục (lên hoặc xuống) trong transaction an toàn
     *
     * @param id        ID của mục cần thay đổi vị trí
     * @param type      Phân loại danh mục
     * @param direction Hướng di chuyển: -1 (lên trên), 1 (xuống dưới)
     * @return true nếu hoán đổi thành công, ngược lại false
     */
    public boolean move(String id, SharedCatalogType type, int direction) {
        if (direction != -1 && direction != 1) {
            return false;
        }

        String selectSql = "SELECT id FROM shared_catalogs WHERE type_code = ? " +
                           "ORDER BY display_order, value, id FOR UPDATE";
        String updateSql = "UPDATE shared_catalogs SET display_order = ? WHERE id = ? AND type_code = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            List<String> ids = new ArrayList<>();
            try (PreparedStatement select = conn.prepareStatement(selectSql)) {
                select.setString(1, type.getCode());
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getString("id"));
                    }
                }
            }

            int index = ids.indexOf(id);
            int target = index + direction;
            if (index < 0 || target < 0 || target >= ids.size()) {
                conn.rollback();
                return false;
            }
            Collections.swap(ids, index, target);
            try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                for (int position = 0; position < ids.size(); position++) {
                    update.setInt(1, position);
                    update.setString(2, ids.get(position));
                    update.setString(3, type.getCode());
                    update.addBatch();
                }
                update.executeBatch();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
            }
            logger.log(Level.SEVERE, "Không thể sắp xếp danh mục " + type.getCode(), e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Không thể khôi phục chế độ transaction", e);
                }
                close(conn, null);
            }
        }
    }

    /**
     * Chức năng: Kiểm tra xem một ID danh mục có thuộc đúng phân loại type chỉ định hay không
     *
     * @param id   ID của mục danh mục
     * @param type Phân loại danh mục
     * @return true nếu hợp lệ, ngược lại false
     */
    public boolean isValueInType(String id, SharedCatalogType type) {
        if (id == null || id.trim().isEmpty()) {
            return true;
        }
        String sql = "SELECT 1 FROM shared_catalogs WHERE id = ? AND type_code = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, type.getCode());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Không thể xác thực danh mục " + id, e);
            throw new IllegalStateException("Không thể xác thực giá trị danh mục.", e);
        }
    }


    /**
     * Chức năng: Chuyển đổi dòng ResultSet thành đối tượng SharedCatalogItem
     *
     * @param rs Con trỏ ResultSet
     * @return Thực thể SharedCatalogItem
     */
    private SharedCatalogItem map(ResultSet rs) throws SQLException {
        SharedCatalogItem item = new SharedCatalogItem();
        item.setId(rs.getString("id"));
        item.setType(SharedCatalogType.fromCode(rs.getString("type_code")));
        item.setValue(rs.getString("value"));
        item.setDisplayOrder(rs.getInt("display_order"));
        item.setReferenceCount(rs.getInt("reference_count"));
        return item;
    }
}
