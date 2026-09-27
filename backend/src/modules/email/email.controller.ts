import { Request, Response, NextFunction } from 'express';
import { EmailService } from './email.service';
import { successResponse } from '../../common/response';

export class EmailController {
  static async getOutbox(req: Request, res: Response, next: NextFunction) {
    try {
      const limit = req.query.limit ? parseInt(req.query.limit as string, 10) : 50;
      const emails = await EmailService.getOutboxEmails(limit);
      res.status(200).json(successResponse(emails));
    } catch (error) {
      next(error);
    }
  }
}
