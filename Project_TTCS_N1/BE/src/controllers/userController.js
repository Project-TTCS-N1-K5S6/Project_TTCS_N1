// Hàm lấy thông tin chi tiết người dùng đang đăng nhập (KN-69)
const getUserProfile = async (req, res) => {
  try {
    // req.user.id được lấy từ Middleware xác thực token (Auth Middleware)
    const userId = req.user?.id; 

    if (!userId) {
      return res.status(401).json({ 
        success: false, 
        message: "Người dùng chưa xác thực hoặc Token không hợp lệ" 
      });
    }

    // Truy vấn dữ liệu người dùng trong Database (Ví dụ dùng Sequelize/Mongoose/Prisma)
    const user = await User.findById(userId).select('-password'); // Loại bỏ trường password

    if (!user) {
      return res.status(404).json({ 
        success: false, 
        message: "Không tìm thấy thông tin người dùng" 
      });
    }

    // Trả về dữ liệu thông tin người dùng thành công
    return res.status(200).json({
      success: true,
      message: "Lấy thông tin người dùng thành công",
      data: user
    });

  } catch (error) {
    return res.status(500).json({
      success: false,
      message: "Lỗi máy chủ nội bộ",
      error: error.message
    });
  }
};

module.exports = { getUserProfile };