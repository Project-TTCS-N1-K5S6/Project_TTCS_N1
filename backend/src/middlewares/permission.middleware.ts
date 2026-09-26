import { Request, Response, NextFunction } from 'express';
import { ForbiddenException, UnauthorizedException } from '../common/exceptions';
import { AuthService } from '../modules/auth/auth.service';

export function requirePermission(permissionCode: string) {
  return async (req: Request, res: Response, next: NextFunction): Promise<void> => {
    if (!req.user) {
      return next(new UnauthorizedException('Vui lòng đăng nhập để tiếp tục.'));
    }

    try {
      // Query fresh roles and permissions from database so any role/permission changes take effect immediately
      const { roles, permissions } = await AuthService.getUserRolesAndPermissions(req.user.userId);
      req.user.roles = roles;
      req.user.permissions = permissions;

      // Admin has superuser access to all features
      if (roles.includes('ADMIN')) {
        return next();
      }

      // Check if user has the specific required permission
      if (permissions.includes(permissionCode)) {
        return next();
      }

      // Deny by default: 403 Forbidden
      return next(new ForbiddenException('Bạn không có quyền thực hiện thao tác này.'));
    } catch (error) {
      return next(error);
    }
  };
}

export function requireAnyRole(...roleCodes: string[]) {
  return (req: Request, res: Response, next: NextFunction): void => {
    if (!req.user) {
      return next(new UnauthorizedException('Vui lòng đăng nhập để tiếp tục.'));
    }

    if (req.user.roles && req.user.roles.includes('ADMIN')) {
      return next();
    }

    const hasRole = req.user.roles && req.user.roles.some((r) => roleCodes.includes(r));
    if (hasRole) {
      return next();
    }

    return next(new ForbiddenException('Bạn không có vai trò phù hợp để truy cập chức năng này.'));
  };
}
