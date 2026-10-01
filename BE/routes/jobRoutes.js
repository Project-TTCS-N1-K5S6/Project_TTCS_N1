'use strict';
const express = require('express');
const router = express.Router();
const JobController = require('../controllers/jobController');
const { authMiddleware } = require('../middleware/authMiddleware');
const { requirePermission, attachPermissions } = require('../middleware/rbacMiddleware');

router.use(authMiddleware);
router.use(attachPermissions);

// GET /api/v1/jobs - list (salary conditionally included)
router.get('/', requirePermission('job.view'), JobController.list);

// GET /api/v1/jobs/:id - detail
router.get('/:id', requirePermission('job.view'), JobController.getById);

// POST /api/v1/jobs - create
router.post('/', requirePermission('job.create'), JobController.create);

// PUT /api/v1/jobs/:id - update
router.put('/:id', requirePermission('job.update'), JobController.update);

// DELETE /api/v1/jobs/:id - delete (admin/manager only)
router.delete('/:id', requirePermission('job.delete'), JobController.delete);

module.exports = router;
