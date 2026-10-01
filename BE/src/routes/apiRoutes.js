import express from 'express';
import {
  getCandidates,
  getCandidateById,
  getVacancies,
  getSalaryReports,
  verifySession,
  triggerServerError,
  triggerServiceUnavailable
} from '../controllers/recruitmentController.js';
import { forgotPassword, resetPassword } from '../controllers/authController.js';

const router = express.Router();

router.get('/recruitment/candidates', getCandidates);
router.get('/recruitment/candidates/:id', getCandidateById);
router.get('/recruitment/vacancies', getVacancies);
router.get('/recruitment/admin/salary-reports', getSalaryReports);
router.get('/auth/verify-session', verifySession);
router.get('/system/crash-test', triggerServerError);
router.get('/system/maintenance', triggerServiceUnavailable);

router.post('/auth/forgot-password', forgotPassword);
router.post('/auth/reset-password', resetPassword);

export default router;
