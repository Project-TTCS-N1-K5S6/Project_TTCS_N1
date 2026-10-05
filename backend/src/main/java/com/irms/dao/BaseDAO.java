package com.irms.dao;

import com.irms.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp DAO cơ sở cung cấp các tiện ích đóng tài nguyên JDBC an toàn
 */
public abstract class BaseDAO {
    protected final Logger logger = Logger.getLogger(getClass().getName());

    protected Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    protected void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Lỗi đóng ResultSet", e); }
        }
        if (ps != null) {
            try { ps.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Lỗi đóng PreparedStatement", e); }
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Lỗi đóng Connection", e); }
        }
    }

    protected void close(Connection conn, PreparedStatement ps) {
        close(conn, ps, null);
    }
}
