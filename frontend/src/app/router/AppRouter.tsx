import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute, PermissionRoute } from '../../components/common/RouteGuards';
import { AppLayout } from '../../components/layout/AppLayout';

// Auth Pages
import { LoginPage } from '../../pages/auth/LoginPage';
import { ForgotPasswordPage } from '../../pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from '../../pages/auth/ResetPasswordPage';
import { ChangePasswordPage } from '../../pages/auth/ChangePasswordPage';

// Feature Pages
import { DashboardPage } from '../../pages/dashboard/DashboardPage';
import { UsersPage } from '../../pages/users/UsersPage';
import { RolesPage } from '../../pages/roles/RolesPage';
import { AuditLogsPage } from '../../pages/audit/AuditLogsPage';

// Error Pages
import { ForbiddenPage } from '../../pages/errors/ForbiddenPage';
import { NotFoundPage } from '../../pages/errors/NotFoundPage';

export const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        {/* Protected App Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          {/* Dashboard */}
          <Route index element={<DashboardPage />} />

          {/* Change Password */}
          <Route path="change-password" element={<ChangePasswordPage />} />

          {/* User Management */}
          <Route
            path="admin/users"
            element={
              <PermissionRoute permission="users.view">
                <UsersPage />
              </PermissionRoute>
            }
          />

          {/* Roles & Permissions Management */}
          <Route
            path="admin/roles"
            element={
              <PermissionRoute permission="roles.view">
                <RolesPage />
              </PermissionRoute>
            }
          />

          {/* Audit Logs */}
          <Route
            path="admin/audit-logs"
            element={
              <PermissionRoute permission="audit.view">
                <AuditLogsPage />
              </PermissionRoute>
            }
          />

          {/* Error Pages */}
          <Route path="403" element={<ForbiddenPage />} />
          <Route path="404" element={<NotFoundPage />} />
          <Route path="*" element={<Navigate to="/404" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};
