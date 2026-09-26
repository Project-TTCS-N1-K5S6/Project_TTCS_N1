import { Request, Response, NextFunction } from 'express';
import { AuditService } from './audit.service';
import { successResponse } from '../../common/response';

export class AuditController {
  static async getLogs(req: Request, res: Response, next: NextFunction) {
    try {
      const page = req.query.page ? parseInt(req.query.page as string, 10) : 1;
      const pageSize = req.query.pageSize ? parseInt(req.query.pageSize as string, 10) : 20;
      const action = req.query.action as string;
      const userId = req.query.userId as string;

      const result = await AuditService.getLogs({
        page,
        pageSize,
        action,
        userId,
      });

      res.status(200).json(successResponse(result, 'Lấy danh sách nhật ký hệ thống thành công.'));
    } catch (error) {
      next(error);
    }
  }
}
