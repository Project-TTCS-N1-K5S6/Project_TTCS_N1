import { Router } from 'express';
import { PermissionsController } from './permissions.controller';
import { authenticate } from '../../middlewares/auth.middleware';
import { requirePermission } from '../../middlewares/permission.middleware';

const router = Router();

router.use(authenticate);

router.get('/', requirePermission('permissions.view'), PermissionsController.getPermissions);

export default router;
