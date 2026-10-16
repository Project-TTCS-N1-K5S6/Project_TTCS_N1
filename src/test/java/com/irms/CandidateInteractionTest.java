package com.irms;

import com.irms.service.CandidateInteractionService;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CandidateInteractionTest {

    private final CandidateInteractionService service = new CandidateInteractionService();

    @Test
    public void rejectsUnsupportedInteractionTypes() {
        assertInvalid(() -> service.record("candidate-1", "EMAIL_SENT", "subject", null, "user-1"),
                "Loại tương tác không hợp lệ.");
    }

    @Test
    public void requiresInteractionDetails() {
        assertInvalid(() -> service.record("candidate-1", "EVALUATION_RECORDED", " ", null, "user-1"),
                "Vui lòng nhập nội dung tương tác (tối đa 2000 ký tự).");
    }

    @Test
    public void requiresInterviewTime() {
        assertInvalid(() -> service.record("candidate-1", "INTERVIEW_SCHEDULED", "Phỏng vấn vòng 1", null, "user-1"),
                "Vui lòng nhập thời gian phỏng vấn.");
    }

    @Test
    public void rejectsPastInterviewTime() {
        assertInvalid(() -> service.record("candidate-1", "INTERVIEW_SCHEDULED", "Phỏng vấn vòng 1",
                "2020-01-01T09:00", "user-1"), "Thời gian phỏng vấn phải ở tương lai.");
    }

    private void assertInvalid(Runnable action, String expectedMessage) {
        try {
            action.run();
            fail("Expected interaction validation to reject the input.");
        } catch (IllegalArgumentException e) {
            assertEquals(expectedMessage, e.getMessage());
        }
    }
}
