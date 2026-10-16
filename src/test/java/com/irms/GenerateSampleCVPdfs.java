package com.irms;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class GenerateSampleCVPdfs {

    private static String stripDiacritics(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String result = pattern.matcher(nfd).replaceAll("");
        result = result.replace('đ', 'd').replace('Đ', 'D');
        return result.replaceAll("[^\\x20-\\x7E]", " ");
    }

    private static float drawSectionHeader(PDPageContentStream cs, String title, float y) throws IOException {
        cs.setNonStrokingColor(241, 245, 249);
        cs.addRect(45, y - 5, 505, 20);
        cs.fill();

        cs.beginText();
        cs.setNonStrokingColor(30, 41, 59);
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
        cs.newLineAtOffset(50, y);
        cs.showText(title);
        cs.endText();
        return y - 22;
    }

    private static float drawParagraph(PDPageContentStream cs, String text, float y) throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(51, 65, 85);
        cs.setFont(PDType1Font.HELVETICA, 9);
        cs.newLineAtOffset(50, y);
        cs.showText(text);
        cs.endText();
        return y - 16;
    }

    public static void createCv(File file, String name, String title, String email, String phone, String trackingCode, String skills, String experience) throws IOException {
        file.getParentFile().mkdirs();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Header Banner
                cs.setNonStrokingColor(15, 23, 42); // Navy
                cs.addRect(0, 720, 595, 122);
                cs.fill();

                // Accent Line
                cs.setNonStrokingColor(37, 99, 235); // Blue
                cs.addRect(0, 715, 595, 5);
                cs.fill();

                // Candidate Name
                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.newLineAtOffset(45, 785);
                cs.showText(stripDiacritics(name).toUpperCase());

                // Title
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.setNonStrokingColor(148, 163, 184);
                cs.newLineAtOffset(0, -20);
                cs.showText("POSITION APPLIED: " + stripDiacritics(title).toUpperCase());

                // Contact line
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(226, 232, 240);
                cs.newLineAtOffset(0, -18);
                cs.showText("Email: " + email + "  |  Phone: " + phone + "  |  Tracking Code: " + trackingCode);
                cs.endText();

                // Sections
                float y = 670;
                y = drawSectionHeader(cs, "1. PROFESSIONAL SUMMARY", y);
                y = drawParagraph(cs, "Proactive, detail-oriented professional with extensive practical experience in delivering", y);
                y = drawParagraph(cs, "high-impact software solutions, meeting tight deadlines and collaborating effectively across teams.", y);

                y = drawSectionHeader(cs, "2. CORE COMPETENCIES & TECHNICAL SKILLS", y);
                y = drawParagraph(cs, "- Technical Stack: " + stripDiacritics(skills), y);
                y = drawParagraph(cs, "- System Architecture: Microservices, RESTful APIs, Clean Architecture, Design Patterns", y);
                y = drawParagraph(cs, "- Database Management: MySQL, PostgreSQL, Redis, Performance Tuning, Index Optimization", y);
                y = drawParagraph(cs, "- Development Operations: Docker, Git, CI/CD Pipelines, Linux System Administration", y);

                y = drawSectionHeader(cs, "3. PROFESSIONAL EXPERIENCE", y);
                y = drawParagraph(cs, "Key Project Highlights & Achievements:", y);
                y = drawParagraph(cs, "  * " + stripDiacritics(experience), y);
                y = drawParagraph(cs, "  * Led technical design and development of scalable backend web services.", y);
                y = drawParagraph(cs, "  * Optimized SQL queries and database indexes, reducing query response latency by 45%.", y);
                y = drawParagraph(cs, "  * Conducted peer code reviews, enforced code quality guidelines and test coverage.", y);

                y = drawSectionHeader(cs, "4. EDUCATION & QUALIFICATIONS", y);
                y = drawParagraph(cs, "- Bachelor of Science in Computer Science / Information Technology", y);
                y = drawParagraph(cs, "- Certifications: Oracle Certified Professional (OCP) Java SE Developer", y);
                y = drawParagraph(cs, "- Languages: Vietnamese (Native), English (Professional Working Proficiency - TOEIC 850)", y);

                // Footer
                cs.beginText();
                cs.setNonStrokingColor(148, 163, 184);
                cs.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
                cs.newLineAtOffset(45, 35);
                cs.showText("IRMS Recruitment Management System - Official Candidate Profile Document");
                cs.endText();
            }

            doc.save(file);
        }
    }

    @Test
    public void generateAllCandidateCVs() throws IOException {
        String[] locations = new String[] {
            "src/main/webapp/uploads/cv",
            "backend/src/main/webapp/uploads/cv",
            "uploads/cv"
        };

        for (String loc : locations) {
            File base = new File(loc);
            createCv(new File(base, "an_nguyen_java.pdf"), "Nguyễn Văn An", "Senior Java Backend Engineer", "an.nguyen@email.test", "0912345671", "APP-20261016-0001", "Java, Spring Boot, Servlet, Hibernate, MySQL, Redis, RESTful API", "4 years experience designing and building high-traffic Java backend services.");
            createCv(new File(base, "binh_tran_backend.pdf"), "Trần Bảo Bình", "Backend Developer", "binh.tran@email.test", "0912345672", "APP-20261016-0002", "Java, PostgreSQL, Spring Cloud, Docker, Kafka", "Excellent graduation project in distributed systems, TOEIC 850.");
            createCv(new File(base, "cuong_le_senior.pdf"), "Lê Hữu Cường", "Senior Software Engineer", "cuong.le@email.test", "0912345673", "APP-20261016-0003", "Java Core, Microservices, Kubernetes, AWS, High Concurrency", "Passed technical assessment with outstanding problem-solving skills.");
            createCv(new File(base, "dung_pham_hr.pdf"), "Phạm Minh Dung", "HR Recruiter", "dung.pham@email.test", "0912345674", "APP-20261016-0004", "Talent Acquisition, Headhunting, Tech Sourcing, Interviewing", "2 years experience recruiting IT engineers across multiple domains.");
            createCv(new File(base, "long_hoang_sales.pdf"), "Hoàng Gia Long", "B2B Sales Lead", "long.hoang@email.test", "0912345675", "APP-20261016-0005", "Enterprise B2B Sales, Contract Negotiation, CRM, Team Leadership", "Strong track record of achieving 120% sales KPI in SaaS solutions.");
        }
    }

    @Test
    public void updateDatabaseCvUrls() {
        try (java.sql.Connection conn = com.irms.config.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement("UPDATE candidates SET cv_url = ? WHERE id = ?")) {
            ps.setString(1, "/uploads/cv/an_nguyen_java.pdf");
            ps.setString(2, "cand-001");
            ps.executeUpdate();

            ps.setString(1, "/uploads/cv/binh_tran_backend.pdf");
            ps.setString(2, "cand-002");
            ps.executeUpdate();

            ps.setString(1, "/uploads/cv/cuong_le_senior.pdf");
            ps.setString(2, "cand-003");
            ps.executeUpdate();

            ps.setString(1, "/uploads/cv/dung_pham_hr.pdf");
            ps.setString(2, "cand-004");
            ps.executeUpdate();

            ps.setString(1, "/uploads/cv/long_hoang_sales.pdf");
            ps.setString(2, "cand-005");
            ps.executeUpdate();
            System.out.println("Updated candidates cv_url in database successfully!");
        } catch (Exception e) {
            System.out.println("Could not update DB directly: " + e.getMessage());
        }
    }
}
