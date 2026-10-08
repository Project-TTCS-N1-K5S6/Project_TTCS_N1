package com.irms.service;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProfileServiceTest {
    @Test
    public void acceptsVietnameseMobileNumbers() {
        assertTrue(ProfileService.isValidVietnameseMobile("0321234567"));
        assertTrue(ProfileService.isValidVietnameseMobile("0551234567"));
        assertTrue(ProfileService.isValidVietnameseMobile("0701234567"));
        assertTrue(ProfileService.isValidVietnameseMobile("0761234567"));
        assertTrue(ProfileService.isValidVietnameseMobile("0811234567"));
        assertTrue(ProfileService.isValidVietnameseMobile("0911234567"));
        assertTrue(ProfileService.isValidVietnameseMobile("+84321234567"));
    }

    @Test
    public void rejectsInvalidVietnameseMobileNumbers() {
        assertFalse(ProfileService.isValidVietnameseMobile("0121234567"));
        assertFalse(ProfileService.isValidVietnameseMobile("0711234567"));
        assertFalse(ProfileService.isValidVietnameseMobile("0751234567"));
        assertFalse(ProfileService.isValidVietnameseMobile("032123456"));
        assertFalse(ProfileService.isValidVietnameseMobile("03212345678"));
        assertFalse(ProfileService.isValidVietnameseMobile("+84121234567"));
        assertFalse(ProfileService.isValidVietnameseMobile(null));
    }
}
