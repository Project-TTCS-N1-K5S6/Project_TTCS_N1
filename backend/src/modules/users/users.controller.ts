import { Request, Response, NextFunction } from 'express';
import { UsersService } from './users.service';
import {
  createUserSchema,
  updateUserSchema,
  lockUserSchema,
  assignRoleSchema,
} from './users.dto';
import { successResponse } from '../../common/response';

export class UsersController {
  static async getUsers(req: Request, res: Response, next: NextFunction) {
    try {
      const page = req.query.page ? parseInt(req.query.page as string, 10) : 1;
      const pageSize = req.query.pageSize ? parseInt(req.query.pageSize as string, 10) : 20;
      const search = req.query.search as string;
      const departmentId = req.query.departmentId as string;
      const role = req.query.role as string;
      const status = req.query.status as string;
      const sortBy = req.query.sortBy as string;
      const sortDirection = req.query.sortDirection as string;

      const result = await UsersService.getUsers({
        page,
        pageSize,
        search,
        departmentId,
        role,
        status,
        sortBy,
        sortDirection,
      });

      res.status(200).json(successResponse(result, 'Lấy danh sách người dùng thành công.'));
    } catch (error) {
      next(error);
    }
  }

  static async getUserById(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const user = await UsersService.getUserById(id);
      res.status(200).json(successResponse(user));
    } catch (error) {
      next(error);
    }
  }

  static async createUser(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = createUserSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const result = await UsersService.createUser(validated, actorUserId, ip, userAgent);
      res.status(201).json(successResponse(result, 'Đã tạo tài khoản thành công.'));
    } catch (error) {
      next(error);
    }
  }

  static async updateUser(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const validated = updateUserSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const updated = await UsersService.updateUser(id, validated, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(updated, 'Cập nhật tài khoản thành công.'));
    } catch (error) {
      next(error);
    }
  }

  static async lockUser(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const validated = lockUserSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const result = await UsersService.lockUser(id, validated, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(result, result.message));
    } catch (error) {
      next(error);
    }
  }

  static async unlockUser(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const result = await UsersService.unlockUser(id, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(null, result.message));
    } catch (error) {
      next(error);
    }
  }

  static async getUserRoles(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const roles = await UsersService.getUserRoles(id);
      res.status(200).json(successResponse(roles));
    } catch (error) {
      next(error);
    }
  }

  static async assignRole(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const validated = assignRoleSchema.parse(req.body);
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const result = await UsersService.assignRole(id, validated.roleId, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(null, result.message));
    } catch (error) {
      next(error);
    }
  }

  static async revokeRole(req: Request, res: Response, next: NextFunction) {
    try {
      const id = req.params.id as string;
      const roleId = req.params.roleId as string;
      const actorUserId = req.user?.userId;
      const ip = (req.ip || req.socket.remoteAddress) as string | undefined;
      const userAgent = req.headers['user-agent'] as string | undefined;

      const result = await UsersService.revokeRole(id, roleId, actorUserId, ip, userAgent);
      res.status(200).json(successResponse(null, result.message));
    } catch (error) {
      next(error);
    }
  }
}
