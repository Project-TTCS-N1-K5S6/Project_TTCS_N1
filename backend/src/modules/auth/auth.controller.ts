import { Request, Response, NextFunction } from 'express';
import { AuthService } from './auth.service';
import {
  loginSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  changePasswordSchema,
} from './auth.dto';
import { successResponse } from '../../common/response';

export class AuthController {
  static async login(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = loginSchema.parse(req.body);
      const ip = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      const result = await AuthService.login(validated, ip, userAgent);

      // Set HttpOnly refresh token cookie
      res.cookie('irms_refresh_token', result.refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/api/auth',
        expires: result.refreshExpiresAt,
      });

      res.status(200).json(
        successResponse(
          {
            accessToken: result.accessToken,
            refreshToken: result.refreshToken,
            user: result.user,
          },
          'Đăng nhập thành công.'
        )
      );
    } catch (error) {
      next(error);
    }
  }

  static async refresh(req: Request, res: Response, next: NextFunction) {
    try {
      const rawToken = req.cookies?.irms_refresh_token || req.body?.refreshToken;
      const ip = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      const result = await AuthService.refreshToken(rawToken, ip, userAgent);

      // Set updated HttpOnly cookie
      res.cookie('irms_refresh_token', result.refreshToken, {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/api/auth',
        expires: result.refreshExpiresAt,
      });

      res.status(200).json(
        successResponse(
          {
            accessToken: result.accessToken,
            refreshToken: result.refreshToken,
          },
          'Làm mới phiên thành công.'
        )
      );
    } catch (error) {
      next(error);
    }
  }

  static async logout(req: Request, res: Response, next: NextFunction) {
    try {
      const rawToken = req.cookies?.irms_refresh_token || req.body?.refreshToken;
      const userId = req.user?.userId;
      const ip = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      await AuthService.logout(rawToken, userId, ip, userAgent);

      // Clear cookie
      res.clearCookie('irms_refresh_token', { path: '/api/auth' });

      res.status(200).json(successResponse(null, 'Đăng xuất thành công.'));
    } catch (error) {
      next(error);
    }
  }

  static async forgotPassword(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = forgotPasswordSchema.parse(req.body);
      const ip = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      const result = await AuthService.forgotPassword(validated, ip, userAgent);
      res.status(200).json(successResponse({ devResetUrl: result.devResetUrl }, result.message));
    } catch (error) {
      next(error);
    }
  }

  static async resetPassword(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = resetPasswordSchema.parse(req.body);
      const ip = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      const result = await AuthService.resetPassword(validated, ip, userAgent);
      res.status(200).json(successResponse(null, result.message));
    } catch (error) {
      next(error);
    }
  }

  static async changePassword(req: Request, res: Response, next: NextFunction) {
    try {
      const validated = changePasswordSchema.parse(req.body);
      const userId = req.user!.userId;
      const currentRawRefreshToken = req.cookies?.irms_refresh_token || req.body?.refreshToken;
      const ip = req.ip || req.socket.remoteAddress;
      const userAgent = req.headers['user-agent'];

      const result = await AuthService.changePassword(
        userId,
        validated,
        currentRawRefreshToken,
        ip,
        userAgent
      );
      res.status(200).json(successResponse(null, result.message));
    } catch (error) {
      next(error);
    }
  }

  static async me(req: Request, res: Response, next: NextFunction) {
    try {
      const userId = req.user!.userId;
      const user = await AuthService.getCurrentUser(userId);
      res.status(200).json(successResponse(user));
    } catch (error) {
      next(error);
    }
  }
}
