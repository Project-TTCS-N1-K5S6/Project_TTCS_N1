package com.irms;

import com.irms.model.CandidateWithdrawal;
import com.irms.model.Candidate;
import com.irms.util.EmailTemplateUtil;
import com.irms.util.SecurityUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CandidateWithdrawalTest {

    @Test
    public void withdrawalTokenIsUrlSafeAndHashedDeterministically() {
        String token = SecurityUtil.generateSecureToken();

        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(64, SecurityUtil.sha256Hex(token).length());
        assertEquals(SecurityUtil.sha256Hex(token), SecurityUtil.sha256Hex(token));
        assertNotEquals(token, SecurityUtil.sha256Hex(token));
    }

    @Test
    public void applicationConfirmationIncludesFinalConfirmationLink() {
        String link = "https://careers.example/careers/withdraw?token=abc123";

        String email = EmailTemplateUtil.buildApplicationConfirmationEmail(
                "Candidate", "Engineer", "Technology", "APP-1234", "15/10/2026",
                "candidate.pdf", link);

        assertTrue(email.contains("Rút hồ sơ ứng tuyển"));
        assertTrue(email.contains(link));
        assertTrue(email.contains("xác nhận lần cuối"));
        assertTrue(email.contains("7 ngày"));
    }

    @Test
    public void recruiterNotificationContainsWithdrawalDetailsAndEscapesHtml() {
        CandidateWithdrawal withdrawal = new CandidateWithdrawal();
        withdrawal.setFullName("<Candidate>");
        withdrawal.setEmail("candidate@example.test");
        withdrawal.setTrackingCode("APP-1234");
        withdrawal.setRequisitionTitle("Engineer");
        withdrawal.setStatusBeforeWithdrawal("SCREENING");
        withdrawal.setRecruiterName("Recruiter");

        String email = EmailTemplateUtil.buildCandidateWithdrawalNotificationEmail(withdrawal);

        assertTrue(email.contains("&lt;Candidate&gt;"));
        assertTrue(email.contains("APP-1234"));
        assertTrue(email.contains("SCREENING → WITHDRAWN"));
    }

    @Test
    public void withdrawnCandidateHasClearPipelineLabel() {
        Candidate candidate = new Candidate();
        candidate.setStatus("WITHDRAWN");

        assertEquals("Ứng viên rút hồ sơ", candidate.getStatusLabel());
        assertEquals("bg-secondary", candidate.getStatusBadgeClass());
    }
}
