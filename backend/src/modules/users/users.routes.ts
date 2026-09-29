import { Router } from 'express';
import { UsersController } from './users.controller';
import { authenticate } from '../../middlewares/auth.middleware';
import { requirePermission } from '../../middlewares/permission.middleware';

const router = Router();

// All user management routes require authentication
router.use(authenticate);

router.get('/', requirePermission('users.view'), UsersController.getUsers);
router.post('/', requirePermission('users.create'), UsersController.createUser);
router.get('/:id', requirePermission('users.view'), UsersController.getUserById);
router.put('/:id', requirePermission('users.update'), UsersController.updateUser);

// Account Lock & Unlock
router.post('/:id/lock', requirePermission('users.lock'), UsersController.lockUser);
router.post('/:id/unlock', requirePermission('users.unlock'), UsersController.unlockUser);

// Roles assignment for user
router.get('/:id/roles', requirePermission('roles.view'), UsersController.getUserRoles);
router.post('/:id/roles', requirePermission('roles.assign'), UsersController.assignRole);
router.delete('/:id/roles/:roleId', requirePermission('roles.revoke'), UsersController.revokeRole);

export default router;
