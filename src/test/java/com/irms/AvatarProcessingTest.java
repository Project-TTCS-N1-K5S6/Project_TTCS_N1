package com.irms;

import com.irms.model.User;
import com.irms.util.ImageUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG CHỨC NĂNG THÊM ẢNH ĐẠI DIỆN (AvatarProcessingTest)
 * ==============================================================================
 * Kiểm tra các tiêu chí nghiệm thu:
 * 1. Chấp nhận JPG/PNG tối đa 2MB.
 * 2. Ảnh được cắt vuông và tạo bản thu nhỏ (thumbnail).
 * ==============================================================================
 */
public class AvatarProcessingTest {

    private File tempDir;

    @Before
    public void setUp() {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "irms_avatar_test_" + System.currentTimeMillis());
        tempDir.mkdirs();
    }

    @After
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            tempDir.delete();
        }
    }

    /**
     * Tiêu chí 1: Kiểm thử chỉ chấp nhận định dạng JPG và PNG
     */
    @Test
    public void testAcceptOnlyJpgAndPng() {
        // Hợp lệ: JPG, JPEG, PNG (cả chữ hoa chữ thường)
        assertTrue("JPG phải hợp lệ", ImageUtil.isValidImageFormat(".jpg"));
        assertTrue("JPEG phải hợp lệ", ImageUtil.isValidImageFormat(".jpeg"));
        assertTrue("PNG phải hợp lệ", ImageUtil.isValidImageFormat(".png"));
        assertTrue("PNG chữ hoa phải hợp lệ", ImageUtil.isValidImageFormat(".PNG"));
        assertTrue("JPG chữ hoa phải hợp lệ", ImageUtil.isValidImageFormat(".JPG"));
        assertTrue("jpg không có dấu chấm phải hợp lệ", ImageUtil.isValidImageFormat("jpg"));
        assertTrue("png không có dấu chấm phải hợp lệ", ImageUtil.isValidImageFormat("png"));

        // Không hợp lệ: WEBP, GIF, SVG, PDF, EXE
        assertFalse("WEBP không được chấp nhận", ImageUtil.isValidImageFormat(".webp"));
        assertFalse("GIF không được chấp nhận", ImageUtil.isValidImageFormat(".gif"));
        assertFalse("SVG không được chấp nhận", ImageUtil.isValidImageFormat(".svg"));
        assertFalse("PDF không được chấp nhận", ImageUtil.isValidImageFormat(".pdf"));
        assertFalse("Null không được chấp nhận", ImageUtil.isValidImageFormat(null));
        assertFalse("Chuỗi rỗng không được chấp nhận", ImageUtil.isValidImageFormat(""));
    }

    /**
     * Tiêu chí 1: Kiểm thử ngưỡng dung lượng tối đa là chính xác 2MB (2,097,152 bytes)
     */
    @Test
    public void testMaxFileSizeTwoMegabytes() {
        assertEquals("Giới hạn dung lượng tối đa phải là chính xác 2MB", 2 * 1024 * 1024, ImageUtil.MAX_FILE_SIZE);
    }

    /**
     * Tiêu chí 2: Kiểm thử cắt vuông ảnh nằm ngang (Landscape: 800 x 600 -> 600 x 600)
     */
    @Test
    public void testCropSquareLandscapeImage() {
        BufferedImage landscape = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        BufferedImage square = ImageUtil.cropSquare(landscape);

        assertNotNull("Ảnh sau khi cắt không được null", square);
        assertEquals("Chiều rộng phải bằng 600", 600, square.getWidth());
        assertEquals("Chiều cao phải bằng 600", 600, square.getHeight());
    }

    /**
     * Tiêu chí 2: Kiểm thử cắt vuông ảnh thẳng đứng (Portrait: 600 x 800 -> 600 x 600)
     */
    @Test
    public void testCropSquarePortraitImage() {
        BufferedImage portrait = new BufferedImage(600, 800, BufferedImage.TYPE_INT_RGB);
        BufferedImage square = ImageUtil.cropSquare(portrait);

        assertNotNull("Ảnh sau khi cắt không được null", square);
        assertEquals("Chiều rộng phải bằng 600", 600, square.getWidth());
        assertEquals("Chiều cao phải bằng 600", 600, square.getHeight());
    }

    /**
     * Tiêu chí 2: Kiểm thử cắt vuông ảnh đã vuông sẵn (Square: 500 x 500 -> 500 x 500)
     */
    @Test
    public void testCropSquareAlreadySquareImage() {
        BufferedImage originalSquare = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
        BufferedImage cropped = ImageUtil.cropSquare(originalSquare);

        assertNotNull(cropped);
        assertEquals(500, cropped.getWidth());
        assertEquals(500, cropped.getHeight());
    }

    /**
     * Tiêu chí 2: Kiểm thử quy trình đọc tệp, cắt vuông và tạo bản thu nhỏ (thumbnail)
     */
    @Test
    public void testProcessAndSaveAvatarAndThumbnail() throws IOException {
        // Tạo ảnh mẫu PNG kích thước 1200x800
        BufferedImage sample = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sample.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 1200, 800);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(sample, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        File avatarFile = new File(tempDir, "avatar_test.png");
        File thumbFile = new File(tempDir, "avatar_test_thumb.png");

        try (ByteArrayInputStream in = new ByteArrayInputStream(imageBytes)) {
            ImageUtil.processAndSaveAvatar(in, avatarFile, thumbFile, "png");
        }

        // 1. Kiểm tra ảnh đại diện chính đã tạo thành công và là hình vuông
        assertTrue("File ảnh đại diện chính phải tồn tại", avatarFile.exists());
        BufferedImage savedAvatar = ImageIO.read(avatarFile);
        assertNotNull(savedAvatar);
        assertEquals("Ảnh đại diện chính phải được cắt vuông", savedAvatar.getWidth(), savedAvatar.getHeight());
        assertEquals("Kích thước ảnh đại diện chính đạt chuẩn 400x400", 400, savedAvatar.getWidth());

        // 2. Kiểm tra bản thu nhỏ (thumbnail) đã tạo thành công và là hình vuông
        assertTrue("File bản thu nhỏ phải tồn tại", thumbFile.exists());
        BufferedImage savedThumb = ImageIO.read(thumbFile);
        assertNotNull(savedThumb);
        assertEquals("Bản thu nhỏ phải được cắt vuông", savedThumb.getWidth(), savedThumb.getHeight());
        assertEquals("Kích thước bản thu nhỏ đạt chuẩn 128x128", 128, savedThumb.getWidth());
    }

    /**
     * Kiểm thử phương thức hỗ trợ lấy URL thumbnail trên User model
     */
    @Test
    public void testUserAvatarThumbnailUrlHelper() {
        User user = new User();
        user.setAvatarUrl("/uploads/avatars/avatar_u1_12345.png");

        assertEquals("Đường dẫn thumbnail phải có hậu tố _thumb.png",
                "/uploads/avatars/avatar_u1_12345_thumb.png",
                user.getAvatarThumbnailUrl());

        user.setAvatarUrl("https://example.com/external.jpg");
        assertEquals("Ảnh ngoài phải giữ nguyên URL",
                "https://example.com/external.jpg",
                user.getAvatarThumbnailUrl());
    }
}
