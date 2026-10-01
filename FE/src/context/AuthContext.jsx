import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { apiRequest } from '../services/apiClient';

/**
 * AuthContext - Central permission/auth state management
 *
 * Fetches user permissions from the SERVER (/api/v1/rbac/my-permissions).
 * Never trusts localStorage for permission decisions.
 *
 * State shape:
 * {
 *   role: string | null,
 *   permissions: string[],
 *   canViewSalary: boolean,
 *   canManagePermissions: boolean,
 *   hasRecruiterScope: boolean,
 *   loading: boolean,
 *   error: string | null,
 * }
 */

const AuthContext = createContext(null);

export function AuthProvider({ children, mockRole = null }) {
  const [state, setState] = useState({
    role: null,
    permissions: [],
    canViewSalary: false,
    canManagePermissions: false,
    hasRecruiterScope: false,
    loading: true,
    error: null,
  });

  const loadPermissions = useCallback(async () => {
    setState(prev => ({ ...prev, loading: true, error: null }));

    // Demo/testing mode: use mockRole instead of server call
    if (mockRole) {
      const mockPermissions = getMockPermissionsForRole(mockRole);
      setState({
        role: mockRole,
        permissions: mockPermissions,
        canViewSalary: mockPermissions.includes('salary.view'),
        canManagePermissions: mockPermissions.includes('permission.manage'),
        hasRecruiterScope: mockRole === 'chuyen_vien_tuyen_dung',
        loading: false,
        error: null,
      });
      return;
    }

    try {
      // Fetch from server - this is the authoritative source
      const data = await apiRequest('/api/v1/rbac/my-permissions');
      setState({
        role: data.role,
        permissions: data.permissions || [],
        canViewSalary: data.meta?.canViewSalary || false,
        canManagePermissions: data.meta?.canManagePermissions || false,
        hasRecruiterScope: data.meta?.hasRecruiterScope || false,
        loading: false,
        error: null,
      });
    } catch (err) {
      // If session expired or unauthenticated
      if (err.errorPayload?.status === 401) {
        setState({
          role: null,
          permissions: [],
          canViewSalary: false,
          canManagePermissions: false,
          hasRecruiterScope: false,
          loading: false,
          error: null,
        });
        return;
      }

      setState(prev => ({
        ...prev,
        loading: false,
        error: 'Không thể tải thông tin quyền người dùng.',
      }));
    }
  }, [mockRole]);

  useEffect(() => {
    loadPermissions();
  }, [loadPermissions]);

  /**
   * Check if current user has a specific permission
   * @param {string} permissionCode
   * @returns {boolean}
   */
  const hasPermission = useCallback((permissionCode) => {
    if (!permissionCode) return false;
    return state.permissions.includes(permissionCode);
  }, [state.permissions]);

  /**
   * Check if current user has ANY of the given permissions
   * @param {string[]} permissionCodes
   * @returns {boolean}
   */
  const hasAnyPermission = useCallback((permissionCodes) => {
    return permissionCodes.some(code => state.permissions.includes(code));
  }, [state.permissions]);

  /**
   * Check if current user has ALL of the given permissions
   * @param {string[]} permissionCodes
   * @returns {boolean}
   */
  const hasAllPermissions = useCallback((permissionCodes) => {
    return permissionCodes.every(code => state.permissions.includes(code));
  }, [state.permissions]);

  const value = {
    ...state,
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    refreshPermissions: loadPermissions,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook to access auth context
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}

/**
 * Mock permissions for demo/testing mode
 * Mirrors the database seed data
 */
export function getMockPermissionsForRole(role) {
  const PERMISSIONS = {
    quan_tri: [
      'candidate.view', 'candidate.create', 'candidate.update', 'candidate.delete',
      'candidate.approve', 'candidate.export',
      'job.view', 'job.create', 'job.update', 'job.delete',
      'salary.view', 'interview.view', 'interview.create', 'interview.update',
      'evaluation.view', 'evaluation.create', 'permission.manage'
    ],
    quan_ly_tuyen_dung: [
      'candidate.view', 'candidate.create', 'candidate.update',
      'candidate.approve', 'candidate.export',
      'job.view', 'job.create', 'job.update',
      'salary.view', 'interview.view', 'interview.create', 'interview.update',
      'evaluation.view', 'evaluation.create'
    ],
    chuyen_vien_tuyen_dung: [
      'candidate.view', 'candidate.create', 'candidate.update',
      'job.view', 'salary.view',
      'interview.view', 'interview.create', 'interview.update',
      'evaluation.view', 'evaluation.create'
    ],
    nguoi_phong_van: [
      'candidate.view', 'job.view',
      'interview.view', 'evaluation.view', 'evaluation.create'
      // NO salary.view
    ],
    truong_phong: [
      'candidate.view', 'candidate.approve', 'candidate.export',
      'job.view', 'salary.view',
      'interview.view', 'evaluation.view', 'evaluation.create'
    ],
    giam_doc: [
      'candidate.view', 'candidate.approve', 'candidate.export',
      'job.view', 'salary.view',
      'interview.view', 'evaluation.view'
    ],
    nhan_su: [
      'candidate.view', 'candidate.create', 'candidate.update',
      'job.view', 'job.create', 'job.update',
      'interview.view', 'interview.create', 'interview.update',
      'evaluation.view', 'evaluation.create'
      // NO salary.view
    ],
  };
  return PERMISSIONS[role] || [];
}

export default AuthContext;
