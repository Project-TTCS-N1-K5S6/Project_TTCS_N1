import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { message } from 'antd';
import { api, setAccessToken, getAccessToken } from '../../services/api';
import { User, ApiResponse } from '../../types';

interface AuthContextType {
  user: User | null;
  roles: string[];
  permissions: string[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  hasPermission: (permissionCode: string) => boolean;
  hasRole: (roleCode: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const roles = user?.roles ? user.roles.map((r) => r.code) : [];
  const permissions = user?.permissions || [];
  const isAuthenticated = !!user;

  const hasRole = useCallback(
    (roleCode: string) => {
      if (!user) return false;
      if (roles.includes('ADMIN')) return true;
      return roles.includes(roleCode);
    },
    [user, roles]
  );

  const hasPermission = useCallback(
    (permissionCode: string) => {
      if (!user) return false;
      // Admin has all permissions
      if (roles.includes('ADMIN')) return true;
      return permissions.includes(permissionCode);
    },
    [user, roles, permissions]
  );

  const refreshProfile = useCallback(async () => {
    try {
      const res = await api.get<ApiResponse<User>>('/auth/me');
      if (res.data.success && res.data.data) {
        setUser(res.data.data);
      }
    } catch {
      setUser(null);
      setAccessToken(null);
    }
  }, []);

  // Check initial session on app mount
  useEffect(() => {
    const initAuth = async () => {
      setIsLoading(true);
      const token = getAccessToken();

      try {
        if (token) {
          const res = await api.get<ApiResponse<User>>('/auth/me');
          if (res.data.success && res.data.data) {
            setUser(res.data.data);
          }
        } else {
          // Attempt silent refresh via HttpOnly cookie
          const refreshRes = await api.post<ApiResponse<{ accessToken: string }>>('/auth/refresh', {});
          if (refreshRes.data.success && refreshRes.data.data?.accessToken) {
            setAccessToken(refreshRes.data.data.accessToken);
            const userRes = await api.get<ApiResponse<User>>('/auth/me');
            if (userRes.data.success && userRes.data.data) {
              setUser(userRes.data.data);
            }
          }
        }
      } catch {
        setUser(null);
        setAccessToken(null);
      } finally {
        setIsLoading(false);
      }
    };

    initAuth();

    // Listen to session expired events from axios interceptor
    const handleSessionExpired = () => {
      setUser(null);
      setAccessToken(null);
      message.warning('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    };

    window.addEventListener('irms:session_expired', handleSessionExpired);
    return () => {
      window.removeEventListener('irms:session_expired', handleSessionExpired);
    };
  }, []);

  const login = async (email: string, password: string): Promise<User> => {
    const res = await api.post<
      ApiResponse<{
        accessToken: string;
        user: User;
      }>
    >('/auth/login', { email, password });

    if (res.data.success && res.data.data) {
      setAccessToken(res.data.data.accessToken);
      setUser(res.data.data.user);
      return res.data.data.user;
    } else {
      throw new Error(res.data.message || 'Đăng nhập không thành công.');
    }
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout');
    } catch (err) {
      console.warn('Logout API error:', err);
    } finally {
      setUser(null);
      setAccessToken(null);
      message.success('Đã đăng xuất an toàn.');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        roles,
        permissions,
        isAuthenticated,
        isLoading,
        login,
        logout,
        refreshProfile,
        hasPermission,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
