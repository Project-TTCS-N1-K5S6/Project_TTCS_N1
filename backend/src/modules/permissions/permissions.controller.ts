import { Request, Response, NextFunction } from 'express';
import { PermissionsService } from './permissions.service';
import { successResponse } from '../../common/response';

export class PermissionsController {
  static async getPermissions(req: Request, res: Response, next: NextFunction) {
    try {
      const result = await PermissionsService.getAllPermissions();
      res.status(200).json(successResponse(result));
    } catch (error) {
      next(error);
    }
  }
}
