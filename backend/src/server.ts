import { app } from './app';
import { config } from './config/env';
import { pool } from './database/db';

async function bootstrap() {
  try {
    // Verify database connectivity
    await pool.query('SELECT 1');
    console.log('[Database] Connected successfully to PostgreSQL ttcs_db');

    const server = app.listen(config.port, () => {
      console.log(`============================================================`);
      console.log(`  IRMS Backend API Server Started Successfully`);
      console.log(`  Environment : ${config.env}`);
      console.log(`  Port        : ${config.port}`);
      console.log(`  API Base    : http://localhost:${config.port}/api`);
      console.log(`  Swagger UI  : http://localhost:${config.port}/api/docs`);
      console.log(`============================================================`);
    });

    const shutdown = async () => {
      console.log('\n[Server] Gracefully shutting down...');
      server.close(async () => {
        await pool.end();
        console.log('[Server] Database pool closed. Goodbye!');
        process.exit(0);
      });
    };

    process.on('SIGINT', shutdown);
    process.on('SIGTERM', shutdown);
  } catch (error) {
    console.error('[Server Error] Failed to bootstrap application:', error);
    process.exit(1);
  }
}

bootstrap();
