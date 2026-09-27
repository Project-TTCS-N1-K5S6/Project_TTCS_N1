import { Router } from 'express';
import { EmailController } from './email.controller';
import { authenticate } from '../../middlewares/auth.middleware';
import { config } from '../../config/env';

const router = Router();

router.get('/', (req, res, next) => {
  if (config.env === 'development') {
    return EmailController.getOutbox(req, res, next);
  }
  return authenticate(req, res, () => EmailController.getOutbox(req, res, next));
});

export default router;
