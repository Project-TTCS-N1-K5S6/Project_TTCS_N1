package com.irms.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * ==============================================================================
 * TIỆN ÍCH XỬ LÝ HÌNH ẢNH (ImageUtil)
 * ==============================================================================
 * Đáp ứng tiêu chí chức năng "Thêm ảnh đại diện":
 * 1. Chấp nhận JPG/PNG tối đa 2MB.
 * 2. Ảnh được cắt vuông và tạo bản thu nhỏ (thumbnail).
 * ==============================================================================
 */
public class ImageUtil {

    public static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    public static final int DEFAULT_AVATAR_SIZE = 400;         // 400x400 px
    public static final int DEFAULT_THUMBNAIL_SIZE = 128;      // 128x128 px

    /**
     * Kiểm tra định dạng đuôi mở rộng có phải JPG hoặc PNG hay không
     */
    public static boolean isValidImageFormat(String extension) {
        if (extension == null) {
            return false;
        }
        String ext = extension.trim().toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        return ".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext);
    }

    /**
     * Cắt ảnh thành hình vuông từ tâm ảnh (Center Square Crop)
     * Đảm bảo ảnh đại diện luôn vuông vắn mà không bị biến dạng tỷ lệ.
     */
    public static BufferedImage cropSquare(BufferedImage src) {
        if (src == null) {
            return null;
        }
        int width = src.getWidth();
        int height = src.getHeight();

        if (width == height) {
            return src;
        }

        int squareSize = Math.min(width, height);
        int cropX = (width - squareSize) / 2;
        int cropY = (height - squareSize) / 2;

        return src.getSubimage(cropX, cropY, squareSize, squareSize);
    }

    /**
     * Thay đổi kích thước ảnh với thuật toán nội suy chất lượng cao
     */
    public static BufferedImage resize(BufferedImage src, int targetWidth, int targetHeight, boolean isPng) {
        if (src == null) {
            return null;
        }
        int imageType = isPng ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D g2d = resized.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!isPng) {
                // Với JPG/JPEG không hỗ trợ nền trong suốt, tô nền trắng để tránh màu đen
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, targetWidth, targetHeight);
            }

            g2d.drawImage(src, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        return resized;
    }

    /**
     * Đọc tệp ảnh, cắt vuông, và xuất đồng thời:
     * 1. Ảnh đại diện chính (kích thước chuẩn vuông)
     * 2. Bản thu nhỏ (Thumbnail vuông)
     */
    public static void processAndSaveAvatar(InputStream inputStream,
                                           File avatarDestFile,
                                           File thumbDestFile,
                                           String formatName) throws IOException {
        BufferedImage originalImage = ImageIO.read(inputStream);
        if (originalImage == null) {
            throw new IOException("Tệp tải lên không phải là định dạng hình ảnh hợp lệ hoặc dữ liệu bị hỏng.");
        }

        boolean isPng = "png".equalsIgnoreCase(formatName);

        // 1. Cắt vuông (Center crop)
        BufferedImage squareImage = cropSquare(originalImage);

        // 2. Tạo ảnh đại diện chuẩn (tối đa DEFAULT_AVATAR_SIZE x DEFAULT_AVATAR_SIZE)
        int avatarSize = Math.min(squareImage.getWidth(), DEFAULT_AVATAR_SIZE);
        if (avatarSize < 100) {
            avatarSize = squareImage.getWidth();
        }
        BufferedImage avatarImage = resize(squareImage, avatarSize, avatarSize, isPng);
        ImageIO.write(avatarImage, formatName, avatarDestFile);

        // 3. Tạo bản thu nhỏ (Thumbnail: DEFAULT_THUMBNAIL_SIZE x DEFAULT_THUMBNAIL_SIZE)
        BufferedImage thumbImage = resize(squareImage, DEFAULT_THUMBNAIL_SIZE, DEFAULT_THUMBNAIL_SIZE, isPng);
        ImageIO.write(thumbImage, formatName, thumbDestFile);
    }
}
