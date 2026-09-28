import { Request, Response, NextFunction } from 'express';
import { query } from '../../database/db';
import { successResponse } from '../../common/response';

export class DepartmentsController {
  static async getDepartments(req: Request, res: Response, next: NextFunction) {
    try {
      const resDb = await query('SELECT id, code, name, description FROM departments ORDER BY code');
      res.status(200).json(successResponse(resDb.rows));
    } catch (error) {
      next(error);
    }
  }
}
