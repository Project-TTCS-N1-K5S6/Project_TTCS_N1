import { Request, Response, NextFunction } from 'express';
import { RolesService } from './roles.service';
import {
  createRoleSchema,
  updateRoleSchema,
  updateRolePermissionsSchema,
} from './roles.dto';
import { successResponse } from '../../common/response';

export class RolesController {
  static async getRoles(req: Request, res: Response, next: NextFunction) {
    try {
      const roles = await RolesService.getRoles();
      res.status(200).json(successResponse(roles));
    } catch (error) {
      next(error);
    }
  }

  static async getRoleById(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const role = await RolesService.getRoleById(id);
      res.status(200).json(successResponse(role));
    } catch (error) {
      next(error);
    }
  }

  static async createRole(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = createRoleSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const newRole = await RolesService.createRole(validated, actorUserId, ip, userAgent);
      res.status(201).json(successResponse(newRole, 'Tạo vai trò mới thành công.'));
    } catch (error) {
      next(error);
    }
  }

  static async updateRole(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const validated = updateRoleSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const updated = await RolesService.updateRole(id, validated, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(updated, 'Cập nhật vai trò thành công.'));
    } catch (error) {
      next(error);
    }
  }

  static async getRolePermissions(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const data = await RolesService.getRolePermissions(id);
      res.status(200).json(successResponse(data));
    } catch (error) {
      next(error);
    }
  }

  static async updateRolePermissions(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const validated = updateRolePermissionsSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const result = await RolesService.updateRolePermissions(id, validated, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(null, result.message));
    } catch (error) {
      next(error);
    }
  }
}
