'use strict';
const express = require('express');
const RecruitmentApprovalController = require('../controllers/recruitmentApprovalController');
const { authMiddleware } = require('../middleware/authMiddleware');

const router = express.Router();
router.use(authMiddleware);

router.get('/reviewers', RecruitmentApprovalController.listReviewers);
router.get('/', RecruitmentApprovalController.list);
router.post('/', RecruitmentApprovalController.create);
router.get('/:id', RecruitmentApprovalController.get);
router.post('/:id/decisions', RecruitmentApprovalController.decide);
router.post('/:id/resubmit', RecruitmentApprovalController.resubmit);

module.exports = router;
