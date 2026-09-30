export interface Department {
  id: string;
  code: string;
  name: string;
  description?: string;
}

export interface Role {
  id: string;
  code: string;
  name: string;
  description?: string;
  is_system_role?: boolean;
  permissions_count?: number;
  users_count?: number;
  permissions?: Permission[];
}

export interface Permission {
  id: string;
  code: string;
  name: string;
  module: string;
  action: string;
  description?: string;
}

export interface User {
  id: string;
  employeeCode?: string;
  fullName: string;
  email: string;
  phone?: string;
  jobTitle?: string;
  status: 'ACTIVE' | 'LOCKED' | 'INACTIVE';
  failedLoginAttempts?: number;
  lockedUntil?: string | null;
  lockedAt?: string | null;
  lockReason?: string | null;
  mustChangePassword?: boolean;
  lastLoginAt?: string | null;
  createdAt: string;
  updatedAt?: string;
  activeRequisitionCount?: number;
  department?: Department | null;
  roles: Array<{ id: string; code: string; name: string }>;
  permissions?: string[];
}

export interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  message: string | null;
  errors?: Array<{ field?: string; message: string }>;
}

export interface PaginatedResult<T> {
  items: T[];
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}
