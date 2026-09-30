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
import { forgotPassword, resetPassword, register } from '../controllers/authController.js';
import {
  getRoles,
  getUsers,
  getUserById,
  updateUserRoles,
  checkOperationAccess
} from '../controllers/roleController.js';

const router = express.Router();

// Recruitment Routes
router.get('/recruitment/candidates', getCandidates);
router.get('/recruitment/candidates/:id', getCandidateById);
router.get('/recruitment/vacancies', getVacancies);
router.get('/recruitment/admin/salary-reports', getSalaryReports);

// Auth Routes
router.get('/auth/verify-session', verifySession);
router.post('/auth/register', register);
router.post('/auth/forgot-password', forgotPassword);
router.post('/auth/reset-password', resetPassword);

// Role & User Permission Management Routes (KN-19: KN-90, KN-91, KN-92)
router.get('/roles', getRoles);
router.get('/users', getUsers);
router.all('/users/check-access', checkOperationAccess); // Put static subpath check-access before dynamic param :id
router.get('/users/:id', getUserById);
router.put('/users/:id/roles', updateUserRoles);

// System Test Routes
router.get('/system/crash-test', triggerServerError);
router.get('/system/maintenance', triggerServiceUnavailable);

export default router;
