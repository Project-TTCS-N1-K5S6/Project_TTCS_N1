import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import { config } from '../config/env';
import { UnauthorizedException } from '../common/exceptions';
import { UserSessionPayload } from '../modules/auth/auth.service';

declare global {
  namespace Express {
    interface Request {
      user?: UserSessionPayload;
    }
  }
}

export function authenticate(req: Request, res: Response, next: NextFunction): void {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return next(new UnauthorizedException('Vui lòng đăng nhập để tiếp tục.'));
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, config.jwt.secret) as UserSessionPayload;
    req.user = decoded;
    next();
  } catch (err: any) {
    if (err.name === 'TokenExpiredError') {
      return next(new UnauthorizedException('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'));
    }
    return next(new UnauthorizedException('Mã xác thực không hợp lệ.'));
  }
}
