'use strict';
/**
 * Generate bcrypt hash for seed password
 * Run: node utils/generateHash.js
 */
const bcrypt = require('bcryptjs');

async function main() {
  const password = 'TempPassword123';
  const hash = await bcrypt.hash(password, 12);
  console.log('Password:', password);
  console.log('Hash:', hash);
  console.log('\nUpdate migration SQL with this hash.');
}

main().catch(console.error);
