import swaggerJsdoc from 'swagger-jsdoc';

const options: swaggerJsdoc.Options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'IRMS - Internal Recruitment Management System API',
      version: '1.0.0',
      description:
        'Tài liệu API chính thức cho Hệ thống Tuyển dụng Nội bộ IRMS - Sprint 1: Tài khoản, Phân quyền & Quản trị người dùng.',
      contact: {
        name: 'IRMS Core Team',
        email: 'dev@company.local',
      },
    },
    servers: [
      {
        url: 'http://localhost:5000',
        description: 'Development Server',
      },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          description: 'Nhập JWT access token thu được từ API /api/auth/login.',
        },
      },
      schemas: {
        StandardSuccessResponse: {
          type: 'object',
          properties: {
            success: { type: 'boolean', example: true },
            data: { type: 'object' },
            message: { type: 'string', example: 'Thao tác thành công.' },
          },
        },
        StandardErrorResponse: {
          type: 'object',
          properties: {
            success: { type: 'boolean', example: false },
            data: { type: 'null', example: null },
            message: { type: 'string', example: 'Bạn không có quyền thực hiện thao tác này.' },
            errors: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  field: { type: 'string', example: 'email' },
                  message: { type: 'string', example: 'Email không hợp lệ.' },
                },
              },
            },
          },
        },
      },
    },
  },
  apis: ['./src/modules/**/*.ts', './src/app.ts'],
};

export const swaggerSpec = swaggerJsdoc(options);
