'use strict';
require('dotenv').config({ path: require('path').join(__dirname, '../.env') });
const fs = require('fs');
const path = require('path');
const { pool } = require('./db');

async function runMigrations() {
  console.log('────────────────────────────────────────────────────────');
  console.log('🔄 Bắt đầu chạy Migration cơ sở dữ liệu TTCS HR...');
  console.log(`   Host:     ${process.env.DB_HOST}:${process.env.DB_PORT}`);
  console.log(`   Database: ${process.env.DB_NAME}`);
  console.log(`   User:     ${process.env.DB_USER}`);
  console.log('────────────────────────────────────────────────────────');

  const client = await pool.connect();
  try {
    const migrationsDir = path.join(__dirname, 'migrations');
    const files = fs.readdirSync(migrationsDir).filter(f => f.endsWith('.sql')).sort();

    for (const file of files) {
      const filePath = path.join(migrationsDir, file);
      console.log(`📄 Đang thực thi: ${file}...`);
      const sql = fs.readFileSync(filePath, 'utf8');

      await client.query('BEGIN');
      await client.query(sql);
      await client.query('COMMIT');
      console.log(`✅ Thành công: ${file}`);
    }

    console.log('────────────────────────────────────────────────────────');
    console.log('🎉 Toàn bộ migration và seed data đã hoàn tất thành công!');
    console.log('   Tài khoản mặc định:');
    console.log('   - hoang.ta@company.com / TempPassword123 (Nhân sự)');
    console.log('   - cuong.sv@company.com / TempPassword123 (Quản trị)');
    console.log('   - mai.nt@company.com   / TempPassword123 (Trưởng phòng)');
    console.log('────────────────────────────────────────────────────────');

  } catch (err) {
    await client.query('ROLLBACK').catch(() => { });
    console.error('❌ Lỗi khi thực thi migration:', err.message);
    if (err.code === 'ECONNREFUSED') {
      console.error('👉 Vui lòng kiểm tra lại dịch vụ PostgreSQL có đang chạy và thông số trong .env đã chính xác chưa.');
    }
    process.exitCode = 1;
  } finally {
    client.release();
    await pool.end();
  }
}

if (require.main === module) {
  runMigrations();
}

module.exports = { runMigrations };
