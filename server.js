const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

const PORT = 3000;
const PUBLIC_DIR = path.join(__dirname, 'frontend');
const WEBAPP_DIR = path.join(__dirname, 'src', 'main', 'webapp');
const DB_FILE = path.join(__dirname, 'database', 'irms_data.json');

// --- DATABASE HELPER ---
let dbData = null;

function loadDatabase() {
  try {
    if (fs.existsSync(DB_FILE)) {
      const raw = fs.readFileSync(DB_FILE, 'utf8');
      dbData = JSON.parse(raw);
    } else {
      dbData = {
        departments: [],
        roles: [],
        users: [],
        auditLogs: [],
        emailOutbox: [],
        candidates: []
      };
    }
  } catch (err) {
    console.error('Lỗi nạp database JSON:', err);
    dbData = { departments: [], roles: [], users: [], auditLogs: [], emailOutbox: [], candidates: [] };
  }
}

function saveDatabase() {
  try {
    fs.writeFileSync(DB_FILE, JSON.stringify(dbData, null, 2), 'utf8');
  } catch (err) {
    console.error('Lỗi ghi database JSON:', err);
  }
}

// Initial DB Load
loadDatabase();

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
};

// Helper: parse JSON body from request
function getRequestBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';
    req.on('data', chunk => {
      body += chunk.toString();
    });
    req.on('end', () => {
      try {
        if (!body.trim()) {
          resolve({});
        } else {
          resolve(JSON.parse(body));
        }
      } catch (e) {
        reject(e);
      }
    });
    req.on('error', err => reject(err));
  });
}

function sendJson(res, statusCode, data) {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=UTF-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization'
  });
  res.end(JSON.stringify(data));
}

// Utility formatting date
function nowString() {
  const d = new Date();
  const pad = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

// REST API Handler
async function handleApiRequest(req, res, parsedUrl) {
  const pathname = parsedUrl.pathname;
  const method = req.method;
  const query = parsedUrl.query;

  // 1. Auth Login: POST /api/auth/login
  if (pathname === '/api/auth/login' && method === 'POST') {
    const { email, password } = await getRequestBody(req);
    const user = dbData.users.find(u => u.email.toLowerCase() === (email || '').trim().toLowerCase());

    if (!user) {
      return sendJson(res, 401, {
        success: false,
        message: 'Tài khoản hoặc mật khẩu không chính xác.'
      });
    }

    if (user.status === 'LOCKED') {
      return sendJson(res, 403, {
        success: false,
        message: `Tài khoản đã bị khóa. Lý do: ${user.lockReason || 'Vi phạm chính sách hệ thống'}. Vui lòng liên hệ Quản trị viên.`
      });
    }

    // Default password or temporary password matches
    const isValidPassword = (password === 'Admin@123456') ||
      (user.tempPassword && password === user.tempPassword) ||
      (password && password.startsWith('Temp@')) ||
      (password && password.startsWith('Reset@'));

    if (!isValidPassword) {
      user.failedLoginAttempts = (user.failedLoginAttempts || 0) + 1;
      if (user.failedLoginAttempts >= 5) {
        user.status = 'LOCKED';
        user.lockReason = 'Tự động khóa do nhập sai mật khẩu 5 lần liên tiếp';
        saveDatabase();
        return sendJson(res, 403, {
          success: false,
          message: 'Tài khoản đã bị tự động khóa do nhập sai mật khẩu 5 lần liên tiếp!'
        });
      }
      saveDatabase();
      return sendJson(res, 401, {
        success: false,
        message: `Mật khẩu không chính xác. Bạn còn ${5 - user.failedLoginAttempts} lần thử.`
      });
    }

    // Login success
    user.failedLoginAttempts = 0;
    user.lastLoginAt = nowString();

    // Map department & roles
    const dept = dbData.departments.find(d => d.id === user.departmentId);
    const roleObjs = dbData.roles.filter(r => (user.roles || []).includes(r.id));

    // Audit Log
    dbData.auditLogs.unshift({
      id: 'aud-' + Date.now(),
      userId: user.id,
      userName: user.fullName,
      action: 'LOGIN_SUCCESS',
      entityType: 'AUTH',
      entityId: user.id,
      description: `Đăng nhập thành công tài khoản: ${user.email} (${user.fullName})`,
      ipAddress: '127.0.0.1',
      createdAt: nowString()
    });

    saveDatabase();

    return sendJson(res, 200, {
      success: true,
      message: 'Đăng nhập thành công!',
      token: 'token-' + user.id + '-' + Date.now(),
      user: {
        id: user.id,
        employeeCode: user.employeeCode,
        fullName: user.fullName,
        email: user.email,
        phone: user.phone,
        jobTitle: user.jobTitle,
        departmentId: user.departmentId,
        departmentName: dept ? dept.name : 'Chưa phân bổ',
        roles: roleObjs,
        status: user.status,
        lastLoginAt: user.lastLoginAt
      }
    });
  }

  // 2. Auth Logout: POST /api/auth/logout
  if (pathname === '/api/auth/logout' && method === 'POST') {
    return sendJson(res, 200, { success: true, message: 'Đăng xuất thành công!' });
  }

  // 3. Departments: GET /api/departments
  if (pathname === '/api/departments' && method === 'GET') {
    return sendJson(res, 200, { success: true, data: dbData.departments });
  }

  // 4. Roles: GET /api/roles
  if (pathname === '/api/roles' && method === 'GET') {
    return sendJson(res, 200, { success: true, data: dbData.roles });
  }

  // 5. Users List with Pagination & Multi-filter: GET /api/users
  // (KN-79, KN-80, KN-83)
  if (pathname === '/api/users' && method === 'GET') {
    const search = (query.search || '').trim().toLowerCase();
    const deptId = (query.deptId || '').trim();
    const roleId = (query.roleId || '').trim();
    const status = (query.status || '').trim();
    const page = Math.max(1, parseInt(query.page || '1', 10));
    // Default 20 rows as specified in Jira task description: "Danh sách phân trang, mặc định 20 dòng"
    const limit = Math.max(1, parseInt(query.limit || '20', 10));

    let filtered = dbData.users.filter(u => {
      // Search by fullName, email, employeeCode
      if (search) {
        const matchName = (u.fullName || '').toLowerCase().includes(search);
        const matchEmail = (u.email || '').toLowerCase().includes(search);
        const matchCode = (u.employeeCode || '').toLowerCase().includes(search);
        if (!matchName && !matchEmail && !matchCode) return false;
      }
      // Department filter
      if (deptId && u.departmentId !== deptId) return false;
      // Status filter
      if (status && u.status !== status) return false;
      // Role filter
      if (roleId && !(u.roles || []).includes(roleId)) return false;

      return true;
    });

    const totalCount = filtered.length;
    const totalPages = Math.ceil(totalCount / limit) || 1;
    const startIndex = (page - 1) * limit;
    const pagedUsers = filtered.slice(startIndex, startIndex + limit).map(u => {
      const dept = dbData.departments.find(d => d.id === u.departmentId);
      const roleObjs = dbData.roles.filter(r => (u.roles || []).includes(r.id));
      return {
        ...u,
        departmentName: dept ? dept.name : 'Chưa phân bổ',
        roleObjects: roleObjs
      };
    });

    return sendJson(res, 200, {
      success: true,
      data: pagedUsers,
      totalCount,
      totalPages,
      currentPage: page,
      pageSize: limit
    });
  }

  // 6. Create User: POST /api/users
  // (KN-81, KN-84, KN-85)
  if (pathname === '/api/users' && method === 'POST') {
    const body = await getRequestBody(req);
    const { employeeCode, fullName, email, phone, jobTitle, departmentId, roles } = body;

    if (!employeeCode || !fullName || !email) {
      return sendJson(res, 400, {
        success: false,
        message: 'Vui lòng điền đầy đủ Mã nhân viên, Họ và tên và Email.'
      });
    }

    const cleanEmail = email.trim().toLowerCase();

    // Check duplicate email (KN-84 criterion: "Email trùng bị từ chối kèm thông báo cụ thể")
    const existingEmail = dbData.users.find(u => u.email.toLowerCase() === cleanEmail);
    if (existingEmail) {
      return sendJson(res, 409, {
        success: false,
        message: `Email '${email}' đã tồn tại trong hệ thống.`
      });
    }

    // Check duplicate employee code
    const existingCode = dbData.users.find(u => (u.employeeCode || '').toUpperCase() === employeeCode.trim().toUpperCase());
    if (existingCode) {
      return sendJson(res, 409, {
        success: false,
        message: `Mã nhân viên '${employeeCode}' đã tồn tại trong hệ thống.`
      });
    }

    // Generate random secure temporary password (KN-85: "Cơ chế Sinh Mật khẩu tạm & Gửi Email Kích hoạt")
    const randomDigits = Math.floor(100000 + Math.random() * 900000);
    const tempPass = `Temp@${randomDigits}`;

    const newUserId = 'usr-' + (dbData.users.length + 1).toString().padStart(3, '0') + '-' + Date.now().toString().slice(-4);
    const userRoles = Array.isArray(roles) && roles.length > 0 ? roles : ['role-007'];

    const newUser = {
      id: newUserId,
      employeeCode: employeeCode.trim().toUpperCase(),
      fullName: fullName.trim(),
      email: cleanEmail,
      phone: (phone || '').trim(),
      jobTitle: (jobTitle || '').trim() || 'Nhân viên',
      departmentId: departmentId || 'dept-002',
      roles: userRoles,
      status: 'ACTIVE',
      lockReason: null,
      failedLoginAttempts: 0,
      mustChangePassword: true,
      tempPassword: tempPass,
      lastLoginAt: null,
      createdAt: nowString()
    };

    dbData.users.unshift(newUser);

    // Enqueue activation email into Outbox (KN-85)
    const emailItem = {
      id: 'eml-' + Date.now(),
      recipientEmail: cleanEmail,
      subject: 'Kích hoạt tài khoản IRMS - Mật khẩu tạm thời',
      emailType: 'ACCOUNT_ACTIVATION',
      temporaryPassword: tempPass,
      fullName: fullName.trim(),
      status: 'SENT',
      sentAt: nowString()
    };
    dbData.emailOutbox.unshift(emailItem);

    // Audit Log
    dbData.auditLogs.unshift({
      id: 'aud-' + Date.now(),
      userId: 'usr-001',
      userName: 'Quản trị viên',
      action: 'USER_CREATED',
      entityType: 'USER',
      entityId: newUserId,
      description: `Tạo mới tài khoản người dùng: ${cleanEmail} (${fullName.trim()}) kèm mật khẩu tạm: ${tempPass}`,
      ipAddress: '127.0.0.1',
      createdAt: nowString()
    });

    saveDatabase();

    const dept = dbData.departments.find(d => d.id === newUser.departmentId);
    const roleObjs = dbData.roles.filter(r => (newUser.roles || []).includes(r.id));

    return sendJson(res, 201, {
      success: true,
      message: 'Tạo tài khoản thành công! Mật khẩu tạm đã được sinh tự động và gửi vào hòm thư kích hoạt.',
      temporaryPassword: tempPass,
      emailSent: emailItem,
      user: {
        ...newUser,

        departmentName: dept ? dept.name : 'Chưa phân bổ',
        roleObjects: roleObjs
      }
    });
  }

  // 7. Update User: PUT /api/users/:id (KN-86)
  if (pathname.startsWith('/api/users/') && method === 'PUT') {
    const userId = pathname.replace('/api/users/', '').split('/')[0];
    const user = dbData.users.find(u => u.id === userId);

    if (!user) {
      return sendJson(res, 404, { success: false, message: 'Không tìm thấy tài khoản người dùng.' });
    }

    const body = await getRequestBody(req);
    const { fullName, phone, jobTitle, departmentId, status, roles, currentAdminId } = body;

    // Protection rule: Admin cannot revoke their own ADMIN role or lock themselves
    if (currentAdminId && currentAdminId === user.id) {
      if (status === 'LOCKED') {
        return sendJson(res, 400, { success: false, message: 'Bạn không thể tự khóa tài khoản của chính mình!' });
      }
      if (Array.isArray(roles) && !roles.includes('role-001')) {
        return sendJson(res, 400, { success: false, message: 'Bạn không thể tự thu hồi vai trò Quản trị viên của chính mình!' });
      }
    }

    if (fullName) user.fullName = fullName.trim();
    if (phone !== undefined) user.phone = phone.trim();
    if (jobTitle !== undefined) user.jobTitle = jobTitle.trim();
    if (departmentId) user.departmentId = departmentId;
    if (status) user.status = status;
    if (Array.isArray(roles)) user.roles = roles;

    // Audit Log
    dbData.auditLogs.unshift({
      id: 'aud-' + Date.now(),
      userId: currentAdminId || 'usr-001',
      userName: 'Quản trị viên',
      action: 'USER_UPDATED',
      entityType: 'USER',
      entityId: user.id,
      description: `Cập nhật thông tin tài khoản: ${user.email} (${user.fullName})`,
      ipAddress: '127.0.0.1',
      createdAt: nowString()
    });

    saveDatabase();

    const dept = dbData.departments.find(d => d.id === user.departmentId);
    const roleObjs = dbData.roles.filter(r => (user.roles || []).includes(r.id));

    return sendJson(res, 200, {
      success: true,
      message: 'Cập nhật thông tin tài khoản thành công!',
      user: {
        ...user,
        departmentName: dept ? dept.name : 'Chưa phân bổ',
        roleObjects: roleObjs
      }
    });
  }

  // 8. Lock User: POST /api/users/:id/lock (KN-82)
  if (pathname.endsWith('/lock') && method === 'POST') {
    const parts = pathname.split('/');
    const userId = parts[parts.length - 2];
    const user = dbData.users.find(u => u.id === userId);

    if (!user) {
      return sendJson(res, 404, { success: false, message: 'Không tìm thấy tài khoản người dùng.' });
    }

    const { reason, currentAdminId } = await getRequestBody(req);
    if (!reason || !reason.trim()) {
      return sendJson(res, 400, { success: false, message: 'Bắt buộc phải nhập lý do khóa tài khoản.' });
    }

    if (currentAdminId && currentAdminId === user.id) {
      return sendJson(res, 400, { success: false, message: 'Bạn không thể tự khóa tài khoản của chính mình!' });
    }

    user.status = 'LOCKED';
    user.lockReason = reason.trim();

    dbData.auditLogs.unshift({
      id: 'aud-' + Date.now(),
      userId: currentAdminId || 'usr-001',
      userName: 'Quản trị viên',
      action: 'USER_LOCKED',
      entityType: 'USER',
      entityId: user.id,
      description: `Khóa tài khoản ${user.email}. Lý do: ${user.lockReason}`,
      ipAddress: '127.0.0.1',
      createdAt: nowString()
    });

    saveDatabase();

    return sendJson(res, 200, {
      success: true,
      message: `Đã khóa tài khoản ${user.fullName} thành công!`
    });
  }

  // 9. Unlock User: POST /api/users/:id/unlock (KN-82)
  if (pathname.endsWith('/unlock') && method === 'POST') {
    const parts = pathname.split('/');
    const userId = parts[parts.length - 2];
    const user = dbData.users.find(u => u.id === userId);

    if (!user) {
      return sendJson(res, 404, { success: false, message: 'Không tìm thấy tài khoản người dùng.' });
    }

    user.status = 'ACTIVE';
    user.lockReason = null;
    user.failedLoginAttempts = 0;

    dbData.auditLogs.unshift({
      id: 'aud-' + Date.now(),
      userId: 'usr-001',
      userName: 'Quản trị viên',
      action: 'USER_UNLOCKED',
      entityType: 'USER',
      entityId: user.id,
      description: `Mở khóa tài khoản: ${user.email}`,
      ipAddress: '127.0.0.1',
      createdAt: nowString()
    });

    saveDatabase();

    return sendJson(res, 200, {
      success: true,
      message: `Đã mở khóa tài khoản ${user.fullName} thành công!`
    });
  }

  // 10. Reset Password: POST /api/users/:id/reset-password (KN-85)
  if (pathname.endsWith('/reset-password') && method === 'POST') {
    const parts = pathname.split('/');
    const userId = parts[parts.length - 2];
    const user = dbData.users.find(u => u.id === userId);

    if (!user) {
      return sendJson(res, 404, { success: false, message: 'Không tìm thấy tài khoản người dùng.' });
    }

    const randomDigits = Math.floor(100000 + Math.random() * 900000);
    const newTempPass = `Reset@${randomDigits}`;

    user.tempPassword = newTempPass;
    user.mustChangePassword = true;

    const emailItem = {
      id: 'eml-' + Date.now(),
      recipientEmail: user.email,
      subject: 'Cấp lại mật khẩu tạm thời hệ thống IRMS',
      emailType: 'PASSWORD_RESET',
      temporaryPassword: newTempPass,
      fullName: user.fullName,
      status: 'SENT',
      sentAt: nowString()
    };
    dbData.emailOutbox.unshift(emailItem);

    dbData.auditLogs.unshift({
      id: 'aud-' + Date.now(),
      userId: 'usr-001',
      userName: 'Quản trị viên',
      action: 'PASSWORD_RESET',
      entityType: 'USER',
      entityId: user.id,
      description: `Cấp lại mật khẩu tạm thời cho ${user.email}: ${newTempPass}`,
      ipAddress: '127.0.0.1',
      createdAt: nowString()
    });

    saveDatabase();

    return sendJson(res, 200, {
      success: true,
      message: `Đã cấp lại mật khẩu tạm thời thành công: ${newTempPass}`,
      temporaryPassword: newTempPass
    });
  }

  // 11. Email Outbox: GET /api/email-outbox
  if (pathname === '/api/email-outbox' && method === 'GET') {
    return sendJson(res, 200, { success: true, data: dbData.emailOutbox });
  }

  // 12. Dashboard Stats: GET /api/dashboard/stats
  if (pathname === '/api/dashboard/stats' && method === 'GET') {
    const totalUsers = dbData.users.length;
    const activeUsers = dbData.users.filter(u => u.status === 'ACTIVE').length;
    const lockedUsers = dbData.users.filter(u => u.status === 'LOCKED').length;
    const totalDepartments = dbData.departments.length;
    const totalCandidates = (dbData.candidates || []).length;

    return sendJson(res, 200, {
      success: true,
      data: {
        totalUsers,
        activeUsers,
        lockedUsers,
        totalDepartments,
        totalCandidates,
        recentUsers: dbData.users.slice(0, 5),
        recentEmails: dbData.emailOutbox.slice(0, 5),
        recentLogs: dbData.auditLogs.slice(0, 6)
      }
    });
  }

  // 13. Audit logs: GET /api/audit-logs
  if (pathname === '/api/audit-logs' && method === 'GET') {
    return sendJson(res, 200, { success: true, data: dbData.auditLogs.slice(0, 50) });
  }

  // 14. Candidates: GET /api/candidates
  if (pathname === '/api/candidates' && method === 'GET') {
    return sendJson(res, 200, { success: true, data: dbData.candidates || [] });
  }

  return sendJson(res, 404, { success: false, message: 'API endpoint không tồn tại: ' + pathname });
}

// HTTP Server
const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = url.parse(req.url, true);
  let requestPath = parsedUrl.pathname;

  // Route API endpoints
  if (requestPath.startsWith('/api/')) {
    try {
      await handleApiRequest(req, res, parsedUrl);
    } catch (err) {
      console.error('API Error:', err);
      sendJson(res, 500, { success: false, message: 'Lỗi máy chủ nội bộ: ' + err.message });
    }
    return;
  }

  // Default redirect root to /index.html
  if (requestPath === '/' || requestPath === '') {
    requestPath = '/index.html';
  }

  // Resolve static files
  let filePath = '';

  // Check if requested from src/main/webapp path
  if (requestPath.includes('/src/main/webapp/')) {
    const sub = requestPath.replace(/^.*\/src\/main\/webapp\//, '');
    filePath = path.join(WEBAPP_DIR, sub);
  } else if (requestPath.startsWith('/assets/')) {
    // Try frontend/assets first, then webapp/assets
    const testPath = path.join(PUBLIC_DIR, requestPath);
    if (fs.existsSync(testPath)) {
      filePath = testPath;
    } else {
      filePath = path.join(WEBAPP_DIR, requestPath);
    }
  } else {
    filePath = path.join(PUBLIC_DIR, requestPath);
    if (!path.extname(filePath)) {
      if (fs.existsSync(filePath + '.html')) {
        filePath += '.html';
      }
    }
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/html; charset=UTF-8' });
      res.end(`<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>404 Not Found - IRMS</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="p-5 text-center bg-light">
  <div class="card p-5 mx-auto shadow-sm" style="max-width: 500px;">
    <h1 class="display-5 text-danger mb-3">404</h1>
    <h5 class="mb-3">Không tìm thấy trang</h5>
    <p class="text-muted">Đường dẫn <code>${requestPath}</code> không tồn tại trên hệ thống.</p>
    <a href="/index.html" class="btn btn-primary mt-2">Về trang đăng nhập</a>
  </div>
</body>
</html>`);
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    res.writeHead(200, { 'Content-Type': contentType });
    fs.createReadStream(filePath).pipe(res);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`================================================================`);
  console.log(`🚀 IRMS PLATFORM ĐANG CHẠY TẠI: http://localhost:${PORT}/`);
  console.log(`🔑 Tài khoản Quản trị: admin@company.local / Admin@123456`);
  console.log(`🔑 Tài khoản HR Manager: hr.manager@company.local / Admin@123456`);
  console.log(`🔒 Tài khoản bị khóa kiểm thử: locked.user@company.local / Admin@123456`);
  console.log(`📁 Dữ liệu lưu tại: ${DB_FILE}`);
  console.log(`================================================================`);
});
