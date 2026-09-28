'use strict';
const { query } = require('../database/db');

/**
 * PermissionModel - Load and cache role permissions from PostgreSQL
 *
 * Structure: role_code -> Set<permission_code>
 * Cache TTL: 5 minutes (permissions rarely change)
 */
class PermissionModel {
  // In-memory cache: { [roleCode]: Set<permCode>, _loadedAt: Date }
  static _cache = null;
  static _cacheLoadedAt = null;
  static CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

  /**
   * Load all role permissions from DB (with cache)
   * @returns {Object} { roleName: Set<permCode> }
   */
  static async getAll() {
    const now = Date.now();
    if (
      PermissionModel._cache &&
      PermissionModel._cacheLoadedAt &&
      now - PermissionModel._cacheLoadedAt < PermissionModel.CACHE_TTL_MS
    ) {
      return PermissionModel._cache;
    }

    return PermissionModel._reload();
  }

  /**
   * Force reload permissions from DB and update cache
   */
  static async _reload() {
    const res = await query(`
      SELECT rp.role_code, p.code AS permission_code
      FROM role_permissions rp
      JOIN permissions p ON p.id = rp.permission_id
      ORDER BY rp.role_code, p.code
    `);

    const cache = {};
    for (const row of res.rows) {
      if (!cache[row.role_code]) {
        cache[row.role_code] = new Set();
      }
      cache[row.role_code].add(row.permission_code);
    }

    PermissionModel._cache = cache;
    PermissionModel._cacheLoadedAt = Date.now();
    return cache;
  }

  /**
   * Invalidate cache (call after permission update)
   */
  static invalidateCache() {
    PermissionModel._cache = null;
    PermissionModel._cacheLoadedAt = null;
  }

  /**
   * Get all permissions for a specific role
   * @param {string} roleCode
   * @returns {Set<string>}
   */
  static async getPermissionsForRole(roleCode) {
    const all = await PermissionModel.getAll();
    return all[roleCode] || new Set();
  }

  /**
   * Get all roles with their permissions (for admin UI)
   * @returns {Array}
   */
  static async getAllRolesWithPermissions() {
    const res = await query(`
      SELECT
        r.code   AS role_code,
        r.display_name AS role_name,
        r.description AS role_description,
        COALESCE(
          json_agg(
            json_build_object(
              'id', p.id,
              'code', p.code,
              'resource', p.resource,
              'action', p.action,
              'displayName', p.display_name
            ) ORDER BY p.resource, p.action
          ) FILTER (WHERE p.id IS NOT NULL),
          '[]'
        ) AS permissions
      FROM roles r
      LEFT JOIN role_permissions rp ON rp.role_code = r.code
      LEFT JOIN permissions p ON p.id = rp.permission_id
      GROUP BY r.code, r.display_name, r.description
      ORDER BY r.code
    `);
    return res.rows;
  }

  /**
   * Get all available permissions (for admin UI)
   */
  static async getAllPermissions() {
    const res = await query(`
      SELECT id, resource, action, code, display_name, description
      FROM permissions
      ORDER BY resource, action
    `);
    return res.rows;
  }

  /**
   * Grant a permission to a role
   */
  static async grantPermission(roleCode, permissionCode, grantedBy) {
    const permRes = await query(
      `SELECT id FROM permissions WHERE code = $1`,
      [permissionCode]
    );
    if (!permRes.rows[0]) {
      throw new Error(`Permission '${permissionCode}' not found`);
    }
    const permId = permRes.rows[0].id;

    await query(
      `INSERT INTO role_permissions (role_code, permission_id, granted_by)
       VALUES ($1, $2, $3)
       ON CONFLICT (role_code, permission_id) DO UPDATE SET granted_by = $3, granted_at = NOW()`,
      [roleCode, permId, grantedBy]
    );

    PermissionModel.invalidateCache();
  }

  /**
   * Revoke a permission from a role
   */
  static async revokePermission(roleCode, permissionCode) {
    const permRes = await query(
      `SELECT id FROM permissions WHERE code = $1`,
      [permissionCode]
    );
    if (!permRes.rows[0]) {
      throw new Error(`Permission '${permissionCode}' not found`);
    }

    await query(
      `DELETE FROM role_permissions WHERE role_code = $1 AND permission_id = $2`,
      [roleCode, permRes.rows[0].id]
    );

    PermissionModel.invalidateCache();
  }

  /**
   * Bulk update permissions for a role (replace all)
   */
  static async setPermissionsForRole(roleCode, permissionCodes, grantedBy) {
    const { withTransaction } = require('../database/db');
    await withTransaction(async (client) => {
      // Remove all existing
      await client.query(
        `DELETE FROM role_permissions WHERE role_code = $1`,
        [roleCode]
      );

      if (permissionCodes.length > 0) {
        // Bulk insert new permissions
        const permsRes = await client.query(
          `SELECT id, code FROM permissions WHERE code = ANY($1)`,
          [permissionCodes]
        );

        for (const perm of permsRes.rows) {
          await client.query(
            `INSERT INTO role_permissions (role_code, permission_id, granted_by)
             VALUES ($1, $2, $3)
             ON CONFLICT (role_code, permission_id) DO NOTHING`,
            [roleCode, perm.id, grantedBy]
          );
        }
      }
    });

    PermissionModel.invalidateCache();
  }
}

module.exports = PermissionModel;
