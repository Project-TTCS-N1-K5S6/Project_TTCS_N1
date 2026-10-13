'use strict';
const RecruitmentApprovalModel = require('../models/recruitmentApprovalModel');

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

class RecruitmentApprovalController {
  static async listReviewers(req, res) {
    try {
      const reviewers = await RecruitmentApprovalModel.listReviewers(req.user.id);
      return res.status(200).json({ success: true, reviewers });
    } catch (err) {
      console.error('[RecruitmentApprovalController.listReviewers] Error:', err);
      return res.status(500).json({ success: false, message: 'Không thể tải danh sách người phê duyệt.' });
    }
  }

  static async list(req, res) {
    try {
      const requests = await RecruitmentApprovalModel.listForUser(req.user.id);
      return res.status(200).json({ success: true, requests });
    } catch (err) {
      console.error('[RecruitmentApprovalController.list] Error:', err);
      return res.status(500).json({ success: false, message: 'Không thể tải danh sách hồ sơ phê duyệt.' });
    }
  }

  static async get(req, res) {
    if (!UUID_PATTERN.test(req.params.id)) {
      return res.status(400).json({ success: false, message: 'Mã hồ sơ không hợp lệ.' });
    }
    try {
      const request = await RecruitmentApprovalModel.getForUser(req.params.id, req.user.id);
      if (!request) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy hồ sơ hoặc bạn không có quyền xem.' });
      }
      return res.status(200).json({ success: true, request });
    } catch (err) {
      console.error('[RecruitmentApprovalController.get] Error:', err);
      return res.status(500).json({ success: false, message: 'Không thể tải chi tiết hồ sơ.' });
    }
  }

  static async create(req, res) {
    const title = typeof req.body.title === 'string' ? req.body.title.trim() : '';
    const details = typeof req.body.details === 'string' ? req.body.details.trim() : '';
    const { reviewerIds } = req.body;

    if (!title || title.length > 200) {
      return res.status(400).json({ success: false, message: 'Tiêu đề bắt buộc và không quá 200 ký tự.' });
    }
    if (!details || details.length > 10000) {
      return res.status(400).json({ success: false, message: 'Nội dung bắt buộc và không quá 10.000 ký tự.' });
    }
    if (!Array.isArray(reviewerIds) || reviewerIds.length < 1 || reviewerIds.length > 5
      || reviewerIds.some((id) => typeof id !== 'string' || !UUID_PATTERN.test(id))
      || new Set(reviewerIds).size !== reviewerIds.length) {
      return res.status(400).json({ success: false, message: 'Chọn từ 1 đến 5 người phê duyệt khác nhau theo đúng thứ tự.' });
    }

    try {
      const request = await RecruitmentApprovalModel.create({
        title,
        details,
        createdBy: req.user.id,
        reviewerIds
      });
      if (request.error === 'INVALID_REVIEWERS') {
        return res.status(400).json({ success: false, message: 'Người phê duyệt phải là tài khoản đang hoạt động và khác người tạo.' });
      }
      return res.status(201).json({ success: true, request });
    } catch (err) {
      console.error('[RecruitmentApprovalController.create] Error:', err);
      return res.status(500).json({ success: false, message: 'Không thể tạo hồ sơ phê duyệt.' });
    }
  }

  static async decide(req, res) {
    if (!UUID_PATTERN.test(req.params.id)) {
      return res.status(400).json({ success: false, message: 'Mã hồ sơ không hợp lệ.' });
    }
    const { action } = req.body;
    const comment = typeof req.body.comment === 'string' ? req.body.comment.trim() : '';
    if (!['APPROVE', 'REJECT', 'REQUEST_MORE_INFO'].includes(action)) {
      return res.status(400).json({ success: false, message: 'Hành động phê duyệt không hợp lệ.' });
    }
    if (action !== 'APPROVE' && !comment) {
      return res.status(400).json({ success: false, message: 'Vui lòng nhập ý kiến khi từ chối hoặc yêu cầu bổ sung.' });
    }
    if (comment.length > 4000) {
      return res.status(400).json({ success: false, message: 'Ý kiến không được vượt quá 4.000 ký tự.' });
    }

    try {
      const result = await RecruitmentApprovalModel.decide({
        requestId: req.params.id,
        userId: req.user.id,
        action,
        comment
      });
      if (result.error === 'NOT_FOUND') {
        return res.status(404).json({ success: false, message: 'Không tìm thấy hồ sơ.' });
      }
      if (result.error === 'FORBIDDEN') {
        return res.status(403).json({ success: false, message: 'Bạn không phải người phê duyệt ở bước hiện tại.' });
      }
      if (result.error === 'NOT_PENDING') {
        return res.status(409).json({ success: false, message: 'Hồ sơ không còn chờ xử lý ở bước này.' });
      }
      return res.status(200).json({ success: true, ...result });
    } catch (err) {
      console.error('[RecruitmentApprovalController.decide] Error:', err);
      return res.status(500).json({ success: false, message: 'Không thể ghi nhận quyết định phê duyệt.' });
    }
  }

  static async resubmit(req, res) {
    if (!UUID_PATTERN.test(req.params.id)) {
      return res.status(400).json({ success: false, message: 'Mã hồ sơ không hợp lệ.' });
    }
    const response = typeof req.body.response === 'string' ? req.body.response.trim() : '';
    if (!response || response.length > 4000) {
      return res.status(400).json({ success: false, message: 'Thông tin bổ sung bắt buộc và không quá 4.000 ký tự.' });
    }

    try {
      const result = await RecruitmentApprovalModel.resubmit({
        requestId: req.params.id,
        userId: req.user.id,
        response
      });
      if (result.error === 'NOT_FOUND') {
        return res.status(404).json({ success: false, message: 'Không tìm thấy hồ sơ.' });
      }
      if (result.error === 'FORBIDDEN') {
        return res.status(403).json({ success: false, message: 'Chỉ người tạo hồ sơ mới có thể gửi bổ sung.' });
      }
      if (result.error === 'NOT_NEED_MORE_INFO') {
        return res.status(409).json({ success: false, message: 'Hồ sơ hiện không chờ bổ sung.' });
      }
      if (result.error === 'STEP_NOT_FOUND') {
        return res.status(409).json({ success: false, message: 'Không tìm thấy cấp phê duyệt cần tiếp tục.' });
      }
      return res.status(200).json({ success: true, ...result });
    } catch (err) {
      console.error('[RecruitmentApprovalController.resubmit] Error:', err);
      return res.status(500).json({ success: false, message: 'Không thể gửi lại hồ sơ bổ sung.' });
    }
  }
}

module.exports = RecruitmentApprovalController;
