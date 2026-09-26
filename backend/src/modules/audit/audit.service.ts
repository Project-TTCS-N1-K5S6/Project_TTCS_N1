import { query } from '../../database/db';

export interface AuditLogEntry {
  userId?: string | null;
  action: string;
  entityType: string;
  entityId?: string | null;
  description?: string;
  ipAddress?: string;
  userAgent?: string;
}

export class AuditService {
  static async log(entry: AuditLogEntry): Promise<void> {
    try {
      await query(
        `INSERT INTO audit_logs (user_id, action, entity_type, entity_id, description, ip_address, user_agent)
         VALUES ($1, $2, $3, $4, $5, $6, $7)`,
        [
          entry.userId || null,
          entry.action,
          entry.entityType,
          entry.entityId || null,
          entry.description || null,
          entry.ipAddress || null,
          entry.userAgent ? entry.userAgent.substring(0, 500) : null,
        ]
      );
    } catch (error) {
      console.error('[Audit Service Error] Failed to write audit log:', error);
    }
  }

  static async getLogs(params: {
    page: number;
    pageSize: number;
    action?: string;
    userId?: string;
  }) {
    const offset = (params.page - 1) * params.pageSize;
    const conditions: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (params.action) {
      conditions.push(`a.action = $${paramIndex++}`);
      values.push(params.action);
    }

    if (params.userId) {
      conditions.push(`a.user_id = $${paramIndex++}`);
      values.push(params.userId);
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

    const countRes = await query(
      `SELECT count(*) FROM audit_logs a ${whereClause}`,
      values
    );
    const total = parseInt(countRes.rows[0].count, 10);

    const itemsRes = await query(
      `SELECT a.*, u.full_name as user_full_name, u.email as user_email
       FROM audit_logs a
       LEFT JOIN users u ON a.user_id = u.id
       ${whereClause}
       ORDER BY a.created_at DESC
       LIMIT $${paramIndex++} OFFSET $${paramIndex++}`,
      [...values, params.pageSize, offset]
    );

    return {
      items: itemsRes.rows,
      page: params.page,
      pageSize: params.pageSize,
      total,
      totalPages: Math.ceil(total / params.pageSize),
    };
  }
}
