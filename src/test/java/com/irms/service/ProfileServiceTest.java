package com.irms.service;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileServiceTest {
    @Test
    public void acceptsVietnameseMobileNumbers() {
        String[] validPrefixes = {
                "032", "039", "052", "055", "056", "058", "059",
                "070", "076", "077", "078", "079",
                "081", "089", "090", "099"
        };
        for (String prefix : validPrefixes) {
            assertTrue(prefix, ProfileService.isValidVietnameseMobile(prefix + "1234567"));
        }
        assertTrue(ProfileService.isValidVietnameseMobile("+84321234567"));
    }

    @Test
    public void rejectsInvalidVietnameseMobileNumbers() {
        String[] invalidPrefixes = {"012", "030", "051", "053", "057", "060", "071", "072", "073", "074", "075", "080"};
        for (String prefix : invalidPrefixes) {
            assertFalse(prefix, ProfileService.isValidVietnameseMobile(prefix + "1234567"));
        }
        assertFalse(ProfileService.isValidVietnameseMobile("032123456"));
        assertFalse(ProfileService.isValidVietnameseMobile("03212345678"));
        assertFalse(ProfileService.isValidVietnameseMobile("+84121234567"));
        assertFalse(ProfileService.isValidVietnameseMobile(null));
    }
}
