export class HttpException extends Error {
  public statusCode: number;
  public errors: Array<{ field?: string; message: string }>;

  constructor(statusCode: number, message: string, errors: Array<{ field?: string; message: string }> = []) {
    super(message);
    this.statusCode = statusCode;
    this.errors = errors;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

export class BadRequestException extends HttpException {
  constructor(message: string = 'Yêu cầu không hợp lệ.', errors: Array<{ field?: string; message: string }> = []) {
    super(400, message, errors);
  }
}

export class UnauthorizedException extends HttpException {
  constructor(message: string = 'Chưa đăng nhập hoặc phiên đăng nhập không hợp lệ.') {
    super(401, message);
  }
}

export class ForbiddenException extends HttpException {
  constructor(message: string = 'Bạn không có quyền thực hiện thao tác này.') {
    super(403, message);
  }
}

export class NotFoundException extends HttpException {
  constructor(message: string = 'Không tìm thấy dữ liệu yêu cầu.') {
    super(404, message);
  }
}

export class ConflictException extends HttpException {
  constructor(message: string = 'Dữ liệu đã tồn tại hoặc xảy ra xung đột.') {
    super(409, message);
  }
}

export class ValidationException extends HttpException {
  constructor(message: string = 'Dữ liệu không hợp lệ.', errors: Array<{ field?: string; message: string }> = []) {
    super(422, message, errors);
  }
}
