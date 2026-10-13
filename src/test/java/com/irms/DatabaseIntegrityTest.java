package com.irms;

import com.irms.config.DBConnection;
import org.junit.Test;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

public class DatabaseIntegrityTest {

    @Test
    public void testDatabaseConnectionAndTables() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            assertNotNull("Database connection should not be null", conn);

            // Tables defined in MySQL schema.sql
            String[] tables = {
                "departments", "roles", "permissions", "users", "user_roles",
                "role_permissions", "refresh_tokens", "password_reset_tokens",
                "audit_logs", "email_outbox", "shared_catalogs",
                "recruitment_requisitions", "candidates", "salary_ranges",
                "competency_criteria", "questions", "competency_frameworks",
                "competency_framework_criteria", "position_competency_frameworks",
                "approval_matrices", "approval_matrix_steps", "requisition_approval_steps"
            };

            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    assertTrue("Should be able to count rows in table " + table, rs.next());
                    int count = rs.getInt(1);
                    assertTrue("Row count should be non-negative for table " + table, count >= 0);
                }
            }

            // Check table status in MySQL
            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("CHECK TABLE " + table)) {
                    while (rs.next()) {
                        String msgType = rs.getString("Msg_type");
                        String msgText = rs.getString("Msg_text");
                        if ("status".equalsIgnoreCase(msgType)) {
                            assertEquals("Table " + table + " status should be OK", "OK", msgText.toUpperCase());
                        }
                    }
                }
            }
        }
    }
}
