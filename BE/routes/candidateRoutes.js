'use strict';
const express = require('express');
const router = express.Router();
const CandidateController = require('../controllers/candidateController');
const { authMiddleware } = require('../middleware/authMiddleware');
const { requirePermission, attachPermissions } = require('../middleware/rbacMiddleware');

// All routes require authentication + permission attachment
router.use(authMiddleware);
router.use(attachPermissions);

// GET /api/v1/candidates - list (with scope filter)
router.get('/',
  requirePermission('candidate.view'),
  CandidateController.list
);

// GET /api/v1/candidates/:id - detail (with IDOR check)
router.get('/:id',
  requirePermission('candidate.view'),
  CandidateController.getById
);

// POST /api/v1/candidates - create
router.post('/',
  requirePermission('candidate.create'),
  CandidateController.create
);

// PUT /api/v1/candidates/:id - update
router.put('/:id',
  requirePermission('candidate.update'),
  CandidateController.update
);

// DELETE /api/v1/candidates/:id - delete (admin only)
router.delete('/:id',
  requirePermission('candidate.delete'),
  CandidateController.delete
);

module.exports = router;
