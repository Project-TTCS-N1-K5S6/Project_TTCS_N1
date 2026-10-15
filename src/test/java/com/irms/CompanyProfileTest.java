package com.irms;

import com.irms.model.CompanyProfile;
import com.irms.service.CompanyProfileService;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit Test cho chức năng Cấu hình trang giới thiệu công ty (Company Profile / About Us)
 * Kiểm tra các tiêu chuẩn:
 * 1. Khởi tạo đối tượng và các giá trị mặc định.
 * 2. Đọc và phân tích JSON giá trị cốt lõi (Core Values).
 * 3. Đọc và phân tích JSON chế độ đãi ngộ (Perks & Benefits).
 * 4. Đọc và phân tích JSON danh sách ảnh gallery.
 * 5. Xử lý trạng thái hiển thị (PUBLISHED / DRAFT) và nhãn hiển thị.
 * 6. Kiểm tra quy tắc xác thực dữ liệu đầu vào trong Service layer.
 */
public class CompanyProfileTest {

    private CompanyProfile profile;
    private CompanyProfileService profileService;

    @Before
    public void setUp() {
        profileService = new CompanyProfileService();
        profile = new CompanyProfile();
        profile.setId("comp-test-01");
        profile.setCompanyName("IRMS Corporation");
        profile.setBrandName("IRMS Tech");
        profile.setSlogan("Kiến tạo tương lai nhân sự số");
        profile.setTagline("Hệ sinh thái tuyển dụng và phát triển nhân tài toàn diện");
        profile.setLogoUrl("/uploads/company/logo.png");
        profile.setHeroBannerUrl("/uploads/company/banner.jpg");
        profile.setOverview("IRMS là tổ chức tiên phong về giải pháp nhân sự.");
        profile.setHistoryStory("Được thành lập từ năm 2020...");
        profile.setMission("Tối ưu hóa nguồn lực nhân sự.");
        profile.setVision("Trở thành nền tảng tuyển dụng hàng đầu.");
        profile.setStatus(CompanyProfile.STATUS_PUBLISHED);
    }

    @Test
    public void testProfileBasicProperties() {
        assertNotNull("Profile không được null", profile);
        assertEquals("comp-test-01", profile.getId());
        assertEquals("IRMS Corporation", profile.getCompanyName());
        assertEquals("IRMS Tech", profile.getBrandName());
        assertEquals("Kiến tạo tương lai nhân sự số", profile.getSlogan());
        assertTrue("Trạng thái phải là PUBLISHED", profile.isPublished());
        assertEquals("Đã xuất bản", profile.getStatusLabel());
        assertEquals("bg-success-subtle text-success border border-success-subtle", profile.getStatusBadgeClass());
    }

    @Test
    public void testStatusDraftProperties() {
        profile.setStatus(CompanyProfile.STATUS_DRAFT);
        assertFalse("Trạng thái không phải PUBLISHED khi là DRAFT", profile.isPublished());
        assertEquals("Bản nháp", profile.getStatusLabel());
        assertEquals("bg-warning-subtle text-warning border border-warning-subtle", profile.getStatusBadgeClass());
    }

    @Test
    public void testCoreValuesJsonParsing() {
        String json = "[{\"title\":\"Chính trực\",\"desc\":\"Hành động minh bạch và trung thực\",\"icon\":\"bi-shield-check\"}," +
                      "{\"title\":\"Sáng tạo\",\"desc\":\"Đổi mới không ngừng nghỉ\",\"icon\":\"bi-lightbulb\"}]";
        profile.setCoreValuesJson(json);

        List<CompanyProfile.CoreValueItem> items = profile.getCoreValueList();
        assertNotNull("Danh sách giá trị cốt lõi không được null", items);
        assertEquals("Số lượng giá trị cốt lõi phải bằng 2", 2, items.size());
        assertEquals("Chính trực", items.get(0).getTitle());
        assertEquals("Hành động minh bạch và trung thực", items.get(0).getDesc());
        assertEquals("bi-shield-check", items.get(0).getIcon());

        assertEquals("Sáng tạo", items.get(1).getTitle());
        assertEquals("bi-lightbulb", items.get(1).getIcon());
    }

    @Test
    public void testPerksJsonParsing() {
        String json = "[{\"title\":\"Lương thưởng hấp dẫn\",\"desc\":\"Thưởng tháng 13 và KPI\",\"icon\":\"bi-cash-coin\"}," +
                      "{\"title\":\"Bảo hiểm sức khỏe\",\"desc\":\"Gói chăm sóc cao cấp toàn diện\",\"icon\":\"bi-heart-pulse\"}]";
        profile.setPerksJson(json);

        List<CompanyProfile.PerkItem> perks = profile.getPerkList();
        assertNotNull("Danh sách phúc lợi không được null", perks);
        assertEquals("Số lượng phúc lợi phải bằng 2", 2, perks.size());
        assertEquals("Lương thưởng hấp dẫn", perks.get(0).getTitle());
        assertEquals("Thưởng tháng 13 và KPI", perks.get(0).getDesc());
        assertEquals("bi-cash-coin", perks.get(0).getIcon());
    }

    @Test
    public void testGalleryUrlsJsonParsing() {
        String json = "[\"https://images.unsplash.com/photo-1\",\"https://images.unsplash.com/photo-2\"]";
        profile.setGalleryUrlsJson(json);

        List<String> gallery = profile.getGalleryList();
        assertNotNull("Danh sách ảnh gallery không được null", gallery);
        assertEquals("Số lượng ảnh gallery phải bằng 2", 2, gallery.size());
        assertEquals("https://images.unsplash.com/photo-1", gallery.get(0));
        assertEquals("https://images.unsplash.com/photo-2", gallery.get(1));
    }

    @Test
    public void testEmptyJsonHandling() {
        profile.setCoreValuesJson("");
        profile.setPerksJson(null);
        profile.setGalleryUrlsJson("[]");

        List<CompanyProfile.CoreValueItem> coreValues = profile.getCoreValueList();
        assertNotNull("Kết quả rỗng không được null khi JSON trống", coreValues);
        assertTrue("Danh sách phải rỗng", coreValues.isEmpty());

        List<CompanyProfile.PerkItem> perks = profile.getPerkList();
        assertNotNull("Kết quả rỗng không được null khi JSON null", perks);
        assertTrue("Danh sách phải rỗng", perks.isEmpty());

        List<String> gallery = profile.getGalleryList();
        assertNotNull("Kết quả rỗng không được null khi JSON mảng rỗng", gallery);
        assertTrue("Danh sách phải rỗng", gallery.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationNullProfile() {
        profileService.saveProfile(null, "admin", "127.0.0.1", "JUnit-Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationEmptyCompanyName() {
        CompanyProfile invalid = new CompanyProfile();
        invalid.setCompanyName("   ");
        profileService.saveProfile(invalid, "admin", "127.0.0.1", "JUnit-Test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationInvalidStatus() {
        profileService.updateStatus("INVALID_STATUS", "admin", "127.0.0.1", "JUnit-Test");
    }
}
