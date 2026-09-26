import { Router } from 'express';
import { AuditController } from './audit.controller';
import { authenticate } from '../../middlewares/auth.middleware';
import { requirePermission } from '../../middlewares/permission.middleware';

const router = Router();

router.use(authenticate);

// Viewing audit logs requires audit.view permission
router.get('/', requirePermission('audit.view'), AuditController.getLogs);

export default router;
