'use strict';
const express = require('express');
const router = express.Router();
const EvaluationController = require('../controllers/evaluationController');
const { authMiddleware, requireRole } = require('../middleware/authMiddleware');
const { body, param } = require('express-validator');

// All evaluation routes require authentication
router.use(authMiddleware);

// Draft auto-save routes
router.post('/drafts/:draftKey', EvaluationController.saveDraft);
router.get('/drafts/:draftKey', EvaluationController.getDraft);
router.get('/drafts', EvaluationController.listDrafts);
router.delete('/drafts/:draftKey', EvaluationController.deleteDraft);

// Form submission & history
router.post('/submit', EvaluationController.submitEvaluation);
router.get('/history', EvaluationController.listEvaluations);

module.exports = router;
