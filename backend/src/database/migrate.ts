import fs from 'fs';
import path from 'path';
import { pool, withTransaction } from './db';

async function runMigrations() {
  console.log('[Migration] Starting database migration...');

  try {
    await withTransaction(async (client) => {
      // Create migration history table
      await client.query(`
        CREATE TABLE IF NOT EXISTS schema_migrations (
          version VARCHAR(50) PRIMARY KEY,
          description VARCHAR(255) NOT NULL,
          applied_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
      `);

      const migrationsDir = path.resolve(__dirname, 'migrations');
      const files = fs.readdirSync(migrationsDir).filter((f) => f.endsWith('.sql')).sort();

      for (const file of files) {
        const version = file.split('__')[0];
        const description = file.split('__')[1]?.replace('.sql', '') || file;

        const checkRes = await client.query(
          'SELECT 1 FROM schema_migrations WHERE version = $1',
          [version]
        );

        if (checkRes.rowCount === 0) {
          console.log(`[Migration] Applying ${file}...`);
          const sql = fs.readFileSync(path.join(migrationsDir, file), 'utf-8');
          await client.query(sql);
          await client.query(
            'INSERT INTO schema_migrations (version, description) VALUES ($1, $2)',
            [version, description]
          );
          console.log(`[Migration] Successfully applied ${file}`);
        } else {
          console.log(`[Migration] Already applied: ${file}`);
        }
      }
    });

    console.log('[Migration] All migrations completed successfully.');
  } catch (error) {
    console.error('[Migration Error]', error);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

if (require.main === module) {
  runMigrations();
}

export { runMigrations };
