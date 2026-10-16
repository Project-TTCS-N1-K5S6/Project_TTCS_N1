package com.irms.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CandidateScreeningValidationTest {

    @Test
    public void acceptsRatingBoundariesAndOptionalRating() {
        CandidateService.validateRating(1);
        CandidateService.validateRating(5);
        CandidateService.validateRating(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRatingOutsideOneToFive() {
        CandidateService.validateRating(6);
    }

    @Test
    public void trimsOptionalInternalNote() {
        assertEquals("Gọi lại ứng viên", CandidateService.normalizeInternalNote("  Gọi lại ứng viên  "));
        assertNull(CandidateService.normalizeInternalNote("  "));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInternalNoteOverMaximumLength() {
        CandidateService.normalizeInternalNote(new String(new char[5001]).replace('\0', 'x'));
    }
}
