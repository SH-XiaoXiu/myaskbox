package cn.xiuxius.askbox.question.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.service.AnswerService;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.question.assembler.QuestionAssembler;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.event.QuestionSubmittedEvent;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.question.view.AdminQuestionView;
import cn.xiuxius.askbox.question.view.PendingQuestionView;
import cn.xiuxius.askbox.question.view.QuestionView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 问题服务实现。
 *
 * <p>核心业务逻辑：
 * <ul>
 *   <li>匿名提交问题：通过 slug 定位提问箱，创建 PENDING 状态的问题记录</li>
 *   <li>回答发布：创建独立的 AnswerEntity 记录，更新问题状态为 PUBLISHED</li>
 *   <li>驳回：将问题状态改为 DISMISSED</li>
 *   <li>删除：仅 DISMISSED 状态可物理删除</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final BoxUserService boxUserService;
    private final BoxUserRepository boxUserRepository;
    private final AnswerService answerService;
    private final AttachmentService attachmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void submit(String slug, Long attachmentId, String question, String ip, String userAgent, String origin) {
        // 1. 通过 slug 找到提问箱
        BoxUserEntity box = boxUserService.getBySlug(slug);
        AttachmentView attachment = attachmentService.getById(attachmentId);
        if (attachment.usageType() != AttachmentUsageType.ANONYMOUS_AVATAR
                || !Boolean.TRUE.equals(attachment.isActive())) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "请选择有效的匿名头像附件");
        }
        // 2. 创建 PENDING 问题
        QuestionEntity q = new QuestionEntity()
                .setBoxUserId(box.getId())
                .setAttachmentId(attachmentId)
                .setQuestion(question)
                .setStatus(QuestionStatus.PENDING)
                .setIp(ip)
                .setUserAgent(userAgent);
        questionRepository.insert(q);
        eventPublisher.publishEvent(new QuestionSubmittedEvent(q.getId(), origin));
        log.info("QuestionEntity submitted to box '{}': id={} ip={}", slug, q.getId(), ip);
    }

    @Override
    @Cacheable(value = "published", key = "#slug + ':' + #page + ':' + #pageSize")
    public PageResult<QuestionView> getPublished(String slug, long page, long pageSize) {
        // 通过 slug 找到提问箱 → 查询该箱子的 PUBLISHED 问题
        BoxUserEntity box = boxUserService.getBySlug(slug);
        IPage<QuestionEntity> result = questionRepository.findPublishedByBoxId(box.getId(), Page.of(page, pageSize));
        // 转换为视图（含头像、回答信息）
        return PageResult.from(result.convert(this::toQuestionView));
    }

    @Override
    public PageResult<PendingQuestionView> getPending(Long boxUserId, long page, long pageSize) {
        IPage<QuestionEntity> result = questionRepository.findPendingByBoxId(boxUserId, Page.of(page, pageSize));
        return PageResult.from(result.convert(this::toPendingView));
    }

    @Override
    @Transactional
    @CacheEvict(value = "published", allEntries = true)
    public QuestionView answer(
            Long boxUserId, Long questionId, String answerContent, Long answeredBy, String ip, String ua) {
        // 1. 验证问题所有权 + 状态
        QuestionEntity q = getAndValidateOwnership(boxUserId, questionId, ErrorCodes.QUESTION_NOT_FOUND);
        if (!q.isPending()) throw new BizException(ErrorCodes.QUESTION_STATUS_INVALID, "问题不是待回答状态");
        // 2. 创建独立的 AnswerEntity 记录
        AnswerEntity answer = answerService.create(questionId, answerContent, answeredBy, ip, ua);
        // 3. 更新问题状态
        q.setStatus(QuestionStatus.PUBLISHED).setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        questionRepository.update(q);
        log.info("QuestionEntity {} answered by user {}", questionId, answeredBy);
        // 4. 返回视图（含新的答案）
        return toQuestionView(q, answer);
    }

    @Override
    @Transactional
    @CacheEvict(value = "published", allEntries = true)
    public void dismiss(Long boxUserId, Long questionId) {
        QuestionEntity q = getAndValidateOwnership(boxUserId, questionId, ErrorCodes.QUESTION_NOT_FOUND);
        q.setStatus(QuestionStatus.DISMISSED);
        questionRepository.update(q);
        log.info("QuestionEntity {} dismissed by box {}", questionId, boxUserId);
    }

    @Override
    @Transactional
    public void delete(Long boxUserId, Long questionId) {
        QuestionEntity q = getAndValidateOwnership(boxUserId, questionId, ErrorCodes.QUESTION_NOT_FOUND);
        if (!q.isDismissed()) throw new BizException(ErrorCodes.QUESTION_STATUS_INVALID, "只能删除已驳回的问题");
        // 如有回答也一并删除
        answerService.deleteByQuestionIdIfExists(questionId);
        questionRepository.deleteById(questionId);
        log.info("QuestionEntity {} deleted by box {}", questionId, boxUserId);
    }

    @Override
    public PageResult<QuestionView> getHistory(Long boxUserId, String status, long page, long pageSize) {
        QuestionStatus qs = parseStatus(status);
        IPage<QuestionEntity> result =
                questionRepository.findByBoxUserIdAndStatus(boxUserId, qs, Page.of(page, pageSize));
        return PageResult.from(result.convert(this::toQuestionView));
    }

    @Override
    public QuestionView getById(Long questionId) {
        QuestionEntity q = questionRepository.findById(questionId);
        if (q == null) throw new BizException(ErrorCodes.QUESTION_NOT_FOUND);
        return toQuestionView(q);
    }

    @Override
    public AdminQuestionView getAdminById(Long questionId) {
        QuestionEntity q = questionRepository.findById(questionId);
        if (q == null) throw new BizException(ErrorCodes.QUESTION_NOT_FOUND);
        BoxUserEntity box = boxUserRepository.findById(q.getBoxUserId());
        String boxSlug = box != null ? box.getSlug() : "unknown";
        AnswerEntity answer = answerService.getByQuestionId(q.getId());
        return QuestionAssembler.toAdminView(q, boxSlug, answer);
    }

    @Override
    public PageResult<AdminQuestionView> listAllAdmin(
            long page, long pageSize, Long boxUserId, String status, String keyword) {
        IPage<QuestionEntity> result =
                questionRepository.pageAll(Page.of(page, pageSize), boxUserId, parseNullableStatus(status), keyword);
        return PageResult.from(result.convert(q -> {
            BoxUserEntity box = boxUserRepository.findById(q.getBoxUserId());
            String boxSlug = box != null ? box.getSlug() : "unknown";
            AnswerEntity answer = answerService.getByQuestionId(q.getId());
            return QuestionAssembler.toAdminView(q, boxSlug, answer);
        }));
    }

    @Override
    public PageResult<QuestionView> listAll(long page, long pageSize, Long boxUserId, String status, String keyword) {
        IPage<QuestionEntity> result =
                questionRepository.pageAll(Page.of(page, pageSize), boxUserId, parseNullableStatus(status), keyword);
        return PageResult.from(result.convert(this::toQuestionView));
    }

    @Override
    @Transactional
    public void forceDelete(Long questionId) {
        answerService.deleteByQuestionIdIfExists(questionId);
        questionRepository.deleteById(questionId);
        log.info("QuestionEntity {} force-deleted by admin", questionId);
    }

    /** 验证问题归属于指定提问箱。 */
    private QuestionEntity getAndValidateOwnership(Long boxUserId, Long questionId, ErrorCodes code) {
        QuestionEntity q = questionRepository.findById(questionId);
        if (q == null) throw new BizException(code);
        if (!q.isOwnedByBox(boxUserId)) throw new BizException(ErrorCodes.FORBIDDEN, "无权操作此问题");
        return q;
    }

    /** 构建已发布问题视图（含回答）。 */
    private QuestionView toQuestionView(QuestionEntity q) {
        AnswerEntity answer = answerService.getByQuestionId(q.getId());
        return toQuestionView(q, answer);
    }

    private QuestionView toQuestionView(QuestionEntity q, AnswerEntity answer) {
        AttachmentView avatar = attachmentService.getById(q.getAttachmentId());
        BoxUserEntity box = boxUserRepository.findById(q.getBoxUserId());
        AttachmentView ownerAvatar = box != null && box.getAvatarAttachmentId() != null
                ? attachmentService.getById(box.getAvatarAttachmentId())
                : null;
        return QuestionAssembler.toQuestionView(q, avatar, answer, ownerAvatar);
    }

    /** 构建待审问题视图（无回答）。 */
    private PendingQuestionView toPendingView(QuestionEntity q) {
        AttachmentView avatar = attachmentService.getById(q.getAttachmentId());
        return QuestionAssembler.toPendingView(q, avatar);
    }

    private QuestionStatus parseStatus(String status) {
        try {
            return QuestionStatus.valueOf(status);
        } catch (RuntimeException ex) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "问题状态不合法");
        }
    }

    private QuestionStatus parseNullableStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return parseStatus(status);
    }
}
