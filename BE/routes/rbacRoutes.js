'use strict';
const express = require('express');
const router = express.Router();
const RbacController = require('../controllers/rbacController');
const { authMiddleware } = require('../middleware/authMiddleware');
const { requirePermission } = require('../middleware/rbacMiddleware');

// GET /api/v1/rbac/my-permissions - any authenticated user
router.get('/my-permissions', authMiddleware, RbacController.getMyPermissions);

// All admin routes require permission.manage
router.use(authMiddleware);
router.use(requirePermission('permission.manage', 'Bạn không có quyền quản lý phân quyền hệ thống.'));

// GET /api/v1/rbac/roles
router.get('/roles', RbacController.getRoles);

// GET /api/v1/rbac/permissions
router.get('/permissions', RbacController.getPermissions);

// GET /api/v1/rbac/roles/:roleCode/permissions
router.get('/roles/:roleCode/permissions', RbacController.getRolePermissions);

// PUT /api/v1/rbac/roles/:roleCode/permissions (bulk update)
router.put('/roles/:roleCode/permissions', RbacController.setRolePermissions);

// POST /api/v1/rbac/roles/:roleCode/permissions/:permCode (grant one)
router.post('/roles/:roleCode/permissions/:permCode', RbacController.grantPermission);

// DELETE /api/v1/rbac/roles/:roleCode/permissions/:permCode (revoke one)
router.delete('/roles/:roleCode/permissions/:permCode', RbacController.revokePermission);

module.exports = router;
