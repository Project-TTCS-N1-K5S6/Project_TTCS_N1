import { query } from '../../database/db';

export class PermissionsService {
  static async getAllPermissions() {
    const res = await query(
      `SELECT id, code, name, module, action, description, created_at
       FROM permissions
       ORDER BY module, code`
    );

    // Group by module for convenient UI matrix display
    const grouped: Record<string, any[]> = {};
    for (const p of res.rows) {
      if (!grouped[p.module]) {
        grouped[p.module] = [];
      }
      grouped[p.module].push(p);
    }

    return {
      all: res.rows,
      byModule: grouped,
    };
  }
}
