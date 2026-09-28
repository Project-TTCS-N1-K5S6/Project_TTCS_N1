import { Router } from 'express';
import { RolesController } from './roles.controller';
import { authenticate } from '../../middlewares/auth.middleware';
import { requirePermission } from '../../middlewares/permission.middleware';

const router = Router();

router.use(authenticate);

router.get('/', requirePermission('roles.view'), RolesController.getRoles);
router.post('/', requirePermission('roles.create'), RolesController.createRole);
router.get('/:id', requirePermission('roles.view'), RolesController.getRoleById);
router.put('/:id', requirePermission('roles.update'), RolesController.updateRole);

router.get('/:id/permissions', requirePermission('roles.view'), RolesController.getRolePermissions);
router.put('/:id/permissions', requirePermission('permissions.manage'), RolesController.updateRolePermissions);

export default router;
