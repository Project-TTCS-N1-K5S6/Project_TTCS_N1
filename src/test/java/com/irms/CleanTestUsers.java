package com.irms;

import com.irms.config.DBConnection;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;

public class CleanTestUsers {
    @Test
    public void testClean() throws Exception {
        try (Connection c = DBConnection.getConnection();
             Statement s = c.createStatement()) {
            int rows = s.executeUpdate("DELETE FROM users WHERE employee_code IN ('EMP010', 'EMP011', 'EMP012', 'EMP013', 'EMP014', 'EMP015', 'EMP016')");
            System.out.println("Cleaned up " + rows + " test user rows from DB.");
        }
    }
}
