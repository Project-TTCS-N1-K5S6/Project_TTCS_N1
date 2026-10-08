package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.CompanyProfile;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thao tác dữ liệu cấu hình trang giới thiệu công ty (bảng company_profile)
 */
public class CompanyProfileDAO extends BaseDAO {

    private static final Logger LOGGER = Logger.getLogger(CompanyProfileDAO.class.getName());

    static {
        ensureSchemaUpToDate();
    }

    /**
     * Tự động khởi tạo bảng và dữ liệu mẫu nếu chưa tồn tại
     */
    public static void ensureSchemaUpToDate() {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return;

            // 1. Tạo bảng company_profile
            String createTableSql = "CREATE TABLE IF NOT EXISTS company_profile (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "company_name VARCHAR(255) NOT NULL, " +
                    "brand_name VARCHAR(100), " +
                    "slogan VARCHAR(255), " +
                    "tagline VARCHAR(255), " +
                    "logo_url TEXT, " +
                    "banner_url TEXT, " +
                    "overview TEXT, " +
                    "history_story TEXT, " +
                    "mission TEXT, " +
                    "vision TEXT, " +
                    "core_values TEXT, " +
                    "culture_desc TEXT, " +
                    "gallery_urls TEXT, " +
                    "perks TEXT, " +
                    "headquarters_address VARCHAR(255), " +
                    "contact_email VARCHAR(255), " +
                    "contact_phone VARCHAR(50), " +
                    "website_url VARCHAR(255), " +
                    "facebook_url VARCHAR(255), " +
                    "linkedin_url VARCHAR(255), " +
                    "youtube_url VARCHAR(255), " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', " +
                    "updated_by VARCHAR(36), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP " +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";
            ps = conn.prepareStatement(createTableSql);
            ps.execute();
            ps.close();

            // 2. Thêm quyền permissions
            String permSql = "INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES " +
                    "('perm-comp-view', 'company.view', 'Xem cấu hình trang giới thiệu công ty', 'COMPANY_PROFILE', 'VIEW', 'Xem trang cấu hình công ty'), " +
                    "('perm-comp-manage', 'company.manage', 'Quản lý cấu hình trang giới thiệu công ty', 'COMPANY_PROFILE', 'MANAGE', 'Soạn thảo và lưu trang giới thiệu công ty');";
            ps = conn.prepareStatement(permSql);
            ps.execute();
            ps.close();

            // 3. Gán quyền cho ADMIN và HR_MANAGER
            String rolePermSql = "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                    "SELECT r.id, p.id FROM roles r, permissions p " +
                    "WHERE r.code IN ('ADMIN', 'HR_MANAGER') AND p.code IN ('company.view', 'company.manage');";
            ps = conn.prepareStatement(rolePermSql);
            ps.execute();
            ps.close();

            // 4. Khởi tạo bản ghi mẫu nếu bảng trống
            String checkCountSql = "SELECT COUNT(*) FROM company_profile";
            ps = conn.prepareStatement(checkCountSql);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                rs.close();
                ps.close();

                String seedSql = "INSERT INTO company_profile (" +
                        "id, company_name, brand_name, slogan, tagline, logo_url, banner_url, " +
                        "overview, history_story, mission, vision, core_values, culture_desc, gallery_urls, perks, " +
                        "headquarters_address, contact_email, contact_phone, website_url, facebook_url, linkedin_url, youtube_url, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                ps = conn.prepareStatement(seedSql);
                ps.setString(1, "comp-default-01");
                ps.setString(2, "Tập đoàn Công nghệ & Tuyển dụng IRMS");
                ps.setString(3, "IRMS Platform");
                ps.setString(4, "Tiên phong công nghệ - Kiến tạo tương lai nghề nghiệp");
                ps.setString(5, "Nơi tài năng hội tụ và phát triển vượt bậc");
                ps.setString(6, "/assets/img/logo-default.png");
                ps.setString(7, "/assets/img/banner-hero-default.jpg");
                ps.setString(8, "IRMS là nền tảng quản trị và tuyển dụng nhân tài nội bộ hàng đầu, kết nối nguồn nhân lực chất lượng cao với các dự án công nghệ đột phá. Chúng tôi không ngừng đổi mới để mang lại giá trị bền vững cho nhân sự và khách hàng.");
                ps.setString(9, "Thành lập từ năm 2020, IRMS đã trải qua hơn 6 năm phát triển mạnh mẽ với quy mô hơn 500 nhân sự chất lượng cao và văn phòng tại các thành phố lớn.");
                ps.setString(10, "Xây dựng hệ sinh thái công nghệ nhân sự thông minh, minh bạch và gắn kết dài lâu.");
                ps.setString(11, "Trở thành biểu tượng xuất sắc trong chuyển đổi số nhân tài tại khu vực Đông Nam Á.");
                ps.setString(12, "[{\"icon\":\"bi-star\",\"title\":\"Chất lượng vượt trội\",\"desc\":\"Luôn đặt chuẩn mực cao nhất trong từng sản phẩm và dịch vụ.\"},{\"icon\":\"bi-lightbulb\",\"title\":\"Đổi mới sáng tạo\",\"desc\":\"Khuyến khích tư duy mới, không ngừng thử nghiệm và bứt phá.\"},{\"icon\":\"bi-shield-check\",\"title\":\"Chính trực & Minh bạch\",\"desc\":\"Minh bạch trong mọi quy trình và tôn trọng cam kết với nhân sự.\"},{\"icon\":\"bi-people\",\"title\":\"Đồng đội gắn kết\",\"desc\":\"Cùng nhau vượt qua thử thách và chia sẻ thành công.\"}]");
                ps.setString(13, "Tại IRMS, chúng tôi xây dựng một môi trường làm việc cởi mở, tôn trọng sự đa dạng và khuyến khích mỗi cá nhân làm chủ công việc. Văn hóa học hỏi liên tục và cân bằng giữa công việc - cuộc sống là giá trị cốt lõi giúp đội ngũ gắn bó.");
                ps.setString(14, "[\"https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80\",\"https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80\",\"https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80\"]");
                ps.setString(15, "[{\"icon\":\"bi-cash-coin\",\"title\":\"Lương & Thưởng hấp dẫn\",\"desc\":\"Thu nhập cạnh tranh, đánh giá tăng lương 2 lần/năm và thưởng dự án xuất sắc.\"},{\"icon\":\"bi-heart-pulse\",\"title\":\"Bảo hiểm sức khỏe cao cấp\",\"desc\":\"Gói bảo hiểm sức khỏe toàn diện cho nhân viên và người thân.\"},{\"icon\":\"bi-laptop\",\"title\":\"Trang thiết bị hiện đại\",\"desc\":\"Cấp Macbook/Laptop hiệu năng cao và hỗ trợ làm việc linh hoạt.\"},{\"icon\":\"bi-mortarboard\",\"title\":\"Đào tạo & Chứng chỉ\",\"desc\":\"Tài trợ 100% học phí các khóa đào tạo chuyên sâu và thi chứng chỉ quốc tế.\"}]");
                ps.setString(16, "Tòa nhà IRMS Tower, Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội");
                ps.setString(17, "career@irms.local");
                ps.setString(18, "(+84) 24 3869 1234");
                ps.setString(19, "https://irms.local");
                ps.setString(20, "https://facebook.com/irms.career");
                ps.setString(21, "https://linkedin.com/company/irms");
                ps.setString(22, "https://youtube.com/@irms-careers");
                ps.setString(23, "PUBLISHED");
                ps.executeUpdate();
            } else {
                rs.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "[CompanyProfileDAO] Lỗi khởi tạo cấu trúc CSDL company_profile", e);
        } finally {
            if (ps != null) { try { ps.close(); } catch (SQLException ignored) {} }
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
    }

    /**
     * Lấy bản ghi cấu hình giới thiệu công ty hiện tại
     */
    public CompanyProfile getProfile() {
        String sql = "SELECT cp.*, u.full_name AS updated_by_name " +
                     "FROM company_profile cp " +
                     "LEFT JOIN users u ON cp.updated_by = u.id " +
                     "ORDER BY cp.created_at ASC LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSet(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy thông tin cấu hình công ty", e);
        } finally {
            close(conn, ps, rs);
        }
        return createDefaultProfile();
    }

    /**
     * Lưu thông tin cấu hình công ty (Thêm mới hoặc cập nhật)
     */
    public boolean saveProfile(CompanyProfile profile) {
        if (profile == null) return false;

        CompanyProfile existing = getProfile();
        boolean isUpdate = (existing != null && existing.getId() != null);

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            if (isUpdate) {
                String sql = "UPDATE company_profile SET " +
                        "company_name = ?, brand_name = ?, slogan = ?, tagline = ?, " +
                        "logo_url = ?, banner_url = ?, overview = ?, history_story = ?, " +
                        "mission = ?, vision = ?, core_values = ?, culture_desc = ?, " +
                        "gallery_urls = ?, perks = ?, headquarters_address = ?, " +
                        "contact_email = ?, contact_phone = ?, website_url = ?, " +
                        "facebook_url = ?, linkedin_url = ?, youtube_url = ?, " +
                        "status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ?";
                ps = conn.prepareStatement(sql);
                setPreparedStatementParams(ps, profile);
                ps.setString(23, profile.getUpdatedBy());
                ps.setString(24, existing.getId());
                return ps.executeUpdate() > 0;
            } else {
                if (profile.getId() == null || profile.getId().trim().isEmpty()) {
                    profile.setId(SecurityUtil.generateUUID());
                }
                String sql = "INSERT INTO company_profile (" +
                        "company_name, brand_name, slogan, tagline, " +
                        "logo_url, banner_url, overview, history_story, " +
                        "mission, vision, core_values, culture_desc, " +
                        "gallery_urls, perks, headquarters_address, " +
                        "contact_email, contact_phone, website_url, " +
                        "facebook_url, linkedin_url, youtube_url, status, updated_by, id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                ps = conn.prepareStatement(sql);
                setPreparedStatementParams(ps, profile);
                ps.setString(23, profile.getUpdatedBy());
                ps.setString(24, profile.getId());
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lưu cấu hình công ty", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật trạng thái hiển thị (PUBLISHED / DRAFT)
     */
    public boolean updateStatus(String status, String updatedBy) {
        CompanyProfile existing = getProfile();
        if (existing == null) return false;

        String sql = "UPDATE company_profile SET status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setString(2, updatedBy);
            ps.setString(3, existing.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái trang giới thiệu công ty", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    private void setPreparedStatementParams(PreparedStatement ps, CompanyProfile p) throws SQLException {
        ps.setString(1, p.getCompanyName());
        ps.setString(2, p.getBrandName());
        ps.setString(3, p.getSlogan());
        ps.setString(4, p.getTagline());
        ps.setString(5, p.getLogoUrl());
        ps.setString(6, p.getBannerUrl());
        ps.setString(7, p.getOverview());
        ps.setString(8, p.getHistoryStory());
        ps.setString(9, p.getMission());
        ps.setString(10, p.getVision());
        ps.setString(11, p.getCoreValues());
        ps.setString(12, p.getCultureDesc());
        ps.setString(13, p.getGalleryUrls());
        ps.setString(14, p.getPerks());
        ps.setString(15, p.getHeadquartersAddress());
        ps.setString(16, p.getContactEmail());
        ps.setString(17, p.getContactPhone());
        ps.setString(18, p.getWebsiteUrl());
        ps.setString(19, p.getFacebookUrl());
        ps.setString(20, p.getLinkedinUrl());
        ps.setString(21, p.getYoutubeUrl());
        ps.setString(22, p.getStatus() != null ? p.getStatus() : CompanyProfile.STATUS_PUBLISHED);
    }

    private CompanyProfile mapResultSet(ResultSet rs) throws SQLException {
        CompanyProfile p = new CompanyProfile();
        p.setId(rs.getString("id"));
        p.setCompanyName(rs.getString("company_name"));
        p.setBrandName(rs.getString("brand_name"));
        p.setSlogan(rs.getString("slogan"));
        p.setTagline(rs.getString("tagline"));
        p.setLogoUrl(rs.getString("logo_url"));
        p.setBannerUrl(rs.getString("banner_url"));
        p.setOverview(rs.getString("overview"));
        p.setHistoryStory(rs.getString("history_story"));
        p.setMission(rs.getString("mission"));
        p.setVision(rs.getString("vision"));
        p.setCoreValues(rs.getString("core_values"));
        p.setCultureDesc(rs.getString("culture_desc"));
        p.setGalleryUrls(rs.getString("gallery_urls"));
        p.setPerks(rs.getString("perks"));
        p.setHeadquartersAddress(rs.getString("headquarters_address"));
        p.setContactEmail(rs.getString("contact_email"));
        p.setContactPhone(rs.getString("contact_phone"));
        p.setWebsiteUrl(rs.getString("website_url"));
        p.setFacebookUrl(rs.getString("facebook_url"));
        p.setLinkedinUrl(rs.getString("linkedin_url"));
        p.setYoutubeUrl(rs.getString("youtube_url"));
        p.setStatus(rs.getString("status"));
        p.setUpdatedBy(rs.getString("updated_by"));
        try {
            p.setUpdatedByName(rs.getString("updated_by_name"));
        } catch (SQLException ignored) {}
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setUpdatedAt(rs.getTimestamp("updated_at"));
        return p;
    }

    private CompanyProfile createDefaultProfile() {
        CompanyProfile p = new CompanyProfile();
        p.setId(SecurityUtil.generateUUID());
        p.setCompanyName("Tập đoàn Công nghệ & Tuyển dụng IRMS");
        p.setBrandName("IRMS Platform");
        p.setSlogan("Tiên phong công nghệ - Kiến tạo tương lai nghề nghiệp");
        p.setTagline("Nơi tài năng hội tụ và phát triển vượt bậc");
        p.setLogoUrl("/assets/img/logo-default.png");
        p.setBannerUrl("/assets/img/banner-hero-default.jpg");
        p.setOverview("IRMS là nền tảng quản trị và tuyển dụng nhân tài nội bộ hàng đầu...");
        p.setStatus(CompanyProfile.STATUS_PUBLISHED);
        return p;
    }
}
