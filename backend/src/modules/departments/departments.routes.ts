import { Router } from 'express';
import { DepartmentsController } from './departments.controller';
import { authenticate } from '../../middlewares/auth.middleware';

const router = Router();

router.use(authenticate);
router.get('/', DepartmentsController.getDepartments);

export default router;
