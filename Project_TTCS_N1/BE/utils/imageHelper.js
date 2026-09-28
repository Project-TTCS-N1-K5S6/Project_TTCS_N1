// utils/imageHelper.js

/**
 * @desc    Xử lý cấu trúc đường dẫn ảnh và định dạng dữ liệu hệ thống (KN-70)
 */

// Hàm chuẩn hóa URL ảnh đại diện / tài nguyên hệ thống
const formatImageUrl = (imagePath) => {
  if (!imagePath) return '/uploads/default-avatar.png';
  if (imagePath.startsWith('http://') || imagePath.startsWith('https://')) {
    return imagePath;
  }
  // Gắn domain hệ thống nếu là đường dẫn tương đối
  const baseUrl = process.env.BASE_URL || 'http://localhost:5000';
  return `${baseUrl}/${imagePath.replace(/^\/+/, '')}`;
};

// Hàm chuẩn hóa cấu trúc dữ liệu hệ thống trả về
const formatSystemData = (data) => {
  if (!data) return null;
  return {
    ...data,
    avatar: formatImageUrl(data.avatar),
    createdAt: data.createdAt ? new Date(data.createdAt).toISOString() : null,
    updatedAt: data.updatedAt ? new Date(data.updatedAt).toISOString() : null,
  };
};

module.exports = {
  formatImageUrl,
  formatSystemData
};