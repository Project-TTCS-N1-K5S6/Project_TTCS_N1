package com.irms;

import com.irms.model.SalaryRange;
import com.irms.service.SalaryRangeService.OfferLimitResult;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ KHAI BÁO DẢI LƯƠNG & HẠN MỨC DUYỆT OFFER (SalaryRangeTest)
 * ==============================================================================
 * Đáp ứng các tiêu chí hoàn thành của Ticket Jira KN-103:
 * 1. Mỗi chức danh có mã, tên, cấp bậc, dải lương tối thiểu và tối đa.
 * 2. Dải lương dùng làm hạn mức duyệt offer về sau.
 * ==============================================================================
 */
public class SalaryRangeTest {

    /**
     * Tiêu chí 1 [KN-103]: Kiểm tra cấu trúc thông tin dải lương chức danh:
     * Mã chức danh, tên chức danh, cấp bậc, mức lương tối thiểu và tối đa.
     */
    @Test
    public void testSalaryRangeAttributes() {
        SalaryRange sr = new SalaryRange();
        sr.setId("sal-test-01");
        sr.setPositionCode("DEV-SR");
        sr.setPositionTitle("Kỹ sư Java Backend");
        sr.setLevel("Senior");
        sr.setMinSalary(new BigDecimal("25000000"));
        sr.setMaxSalary(new BigDecimal("45000000"));
        sr.setCurrency("VND");
        sr.setNote("Hạn mức chuẩn");

        assertEquals("DEV-SR", sr.getPositionCode());
        assertEquals("Kỹ sư Java Backend", sr.getPositionTitle());
        assertEquals("Senior", sr.getLevel());
        assertEquals(new BigDecimal("25000000"), sr.getMinSalary());
        assertEquals(new BigDecimal("45000000"), sr.getMaxSalary());
        assertEquals("VND", sr.getCurrency());
        assertEquals("Hạn mức chuẩn", sr.getNote());

        // Kiểm tra định dạng hiển thị
        assertTrue("Formatted range phải chứa 25.000.000", sr.getFormattedRange().contains("25.000.000"));
        assertTrue("Formatted range phải chứa 45.000.000", sr.getFormattedRange().contains("45.000.000"));
        assertTrue("Formatted range phải chứa VND", sr.getFormattedRange().contains("VND"));
    }

    /**
     * Tiêu chí 2 [KN-103]: Kiểm tra logic đối soát mức lương Offer với Dải lương:
     * - Hợp lệ (trong dải min - max)
     * - Vượt trần (lớn hơn max -> EXCEEDED_MAX)
     * - Dưới sàn (nhỏ hơn min -> BELOW_MIN)
     */
    @Test
    public void testOfferLimitValidationLogic() {
        SalaryRange sr = new SalaryRange();
        sr.setPositionCode("DEV-SR");
        sr.setPositionTitle("Kỹ sư Java Backend");
        sr.setLevel("Senior");
        sr.setMinSalary(new BigDecimal("25000000"));
        sr.setMaxSalary(new BigDecimal("45000000"));

        // Trường hợp 1: Mức offer nằm trong dải lương (30M)
        BigDecimal offerValid = new BigDecimal("30000000");
        assertTrue("Offer 30M phải nằm trong dải 25M-45M", sr.isSalaryWithinRange(offerValid));
        assertEquals("WITHIN_RANGE", sr.checkOfferLimitStatus(offerValid));

        // Trường hợp 2: Mức offer đúng bằng biên tối thiểu (25M)
        assertTrue("Offer 25M (bằng min) phải hợp lệ", sr.isSalaryWithinRange(new BigDecimal("25000000")));
        assertEquals("WITHIN_RANGE", sr.checkOfferLimitStatus(new BigDecimal("25000000")));

        // Trường hợp 3: Mức offer đúng bằng biên tối đa (45M)
        assertTrue("Offer 45M (bằng max) phải hợp lệ", sr.isSalaryWithinRange(new BigDecimal("45000000")));
        assertEquals("WITHIN_RANGE", sr.checkOfferLimitStatus(new BigDecimal("45000000")));

        // Trường hợp 4: Mức offer vượt quá trần dải lương (50M > 45M) -> Không hợp lệ, cảnh báo vượt trần
        BigDecimal offerExceeded = new BigDecimal("50000000");
        assertFalse("Offer 50M vượt trần không được tự động duyệt", sr.isSalaryWithinRange(offerExceeded));
        assertEquals("EXCEEDED_MAX", sr.checkOfferLimitStatus(offerExceeded));

        // Trường hợp 5: Mức offer thấp hơn mức tối thiểu (20M < 25M)
        BigDecimal offerBelow = new BigDecimal("20000000");
        assertFalse("Offer 20M thấp hơn min", sr.isSalaryWithinRange(offerBelow));
        assertEquals("BELOW_MIN", sr.checkOfferLimitStatus(offerBelow));
    }

    /**
     * Kiểm tra phân loại badge màu CSS theo từng cấp bậc
     */
    @Test
    public void testLevelBadgeStyles() {
        SalaryRange sr1 = new SalaryRange();
        sr1.setLevel("Junior");
        assertTrue("Junior badge phải có class teal", sr1.getLevelBadgeClass().contains("teal"));

        SalaryRange sr2 = new SalaryRange();
        sr2.setLevel("Senior");
        assertTrue("Senior badge phải có class indigo", sr2.getLevelBadgeClass().contains("indigo"));

        SalaryRange sr3 = new SalaryRange();
        sr3.setLevel("Lead");
        assertTrue("Lead badge phải có class purple", sr3.getLevelBadgeClass().contains("purple"));

        SalaryRange sr4 = new SalaryRange();
        sr4.setLevel("Manager");
        assertTrue("Manager badge phải có class warning", sr4.getLevelBadgeClass().contains("warning"));
    }

    /**
     * Kiểm tra xử lý biên: Mức offer âm, bằng 0, hoặc null -> Phải là INVALID
     */
    @Test
    public void testEdgeCaseOfferSalaries() {
        SalaryRange sr = new SalaryRange();
        sr.setMinSalary(new BigDecimal("20000000"));
        sr.setMaxSalary(new BigDecimal("40000000"));

        assertFalse("Offer null phải trả về false", sr.isSalaryWithinRange(null));
        assertEquals("INVALID", sr.checkOfferLimitStatus(null));

        assertFalse("Offer 0 phải trả về false", sr.isSalaryWithinRange(BigDecimal.ZERO));
        assertEquals("INVALID", sr.checkOfferLimitStatus(BigDecimal.ZERO));

        assertFalse("Offer âm phải trả về false", sr.isSalaryWithinRange(new BigDecimal("-1000000")));
        assertEquals("INVALID", sr.checkOfferLimitStatus(new BigDecimal("-1000000")));
    }
}
