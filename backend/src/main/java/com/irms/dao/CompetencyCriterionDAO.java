package com.irms.dao;

import com.irms.model.CompetencyCriterion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CompetencyCriterionDAO extends BaseDAO {

    public List<CompetencyCriterion> findAll() {
        List<CompetencyCriterion> list = new ArrayList<>();
        String sql = "SELECT * FROM competency_criteria ORDER BY name ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                CompetencyCriterion c = new CompetencyCriterion();
                c.setId(rs.getString("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách tiêu chí năng lực", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public CompetencyCriterion findById(String id) {
        String sql = "SELECT * FROM competency_criteria WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                CompetencyCriterion c = new CompetencyCriterion();
                c.setId(rs.getString("id"));
                c.setName(rs.getString("name"));
                c.setDescription(rs.getString("description"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                return c;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm tiêu chí năng lực theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean insert(CompetencyCriterion c) {
        String sql = "INSERT INTO competency_criteria (id, name, description) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getId());
            ps.setString(2, c.getName());
            ps.setString(3, c.getDescription());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm mới tiêu chí", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean update(CompetencyCriterion c) {
        String sql = "UPDATE competency_criteria SET name = ?, description = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getName());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật tiêu chí", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM competency_criteria WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa tiêu chí", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}
