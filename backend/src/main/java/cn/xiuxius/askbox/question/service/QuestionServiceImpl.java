package cn.xiuxius.askbox.question.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

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
import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.service.LikeService;
import cn.xiuxius.askbox.question.assembler.QuestionAssembler;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.event.QuestionSubmittedEvent;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.question.view.AdminQuestionView;
import cn.xiuxius.askbox.question.view.PendingQuestionView;
import cn.xiuxius.askbox.question.view.QuestionView;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.topic.entity.BoxTopicEntity;
import cn.xiuxius.askbox.topic.service.TopicService;
import cn.xiuxius.askbox.topic.view.TopicSummaryView;

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
 *   <li>删除：箱主可删除自己已发布或已驳回的问题</li>
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
    private final SysUserRepository sysUserRepository;
    private final TopicService topicService;
    private final ApplicationEventPublisher eventPublisher;
    private final LikeService likeService;

    @Override
    @Transactional
    public void submit(
            String slug,
            Long attachmentId,
            String question,
            String topicCode,
            String ip,
            String userAgent,
            String origin) {
        // 1. 通过 slug 找到提问箱
        BoxUserEntity box = boxUserService.getBySlug(slug);
        BoxTopicEntity topic = topicService.requireAvailableForSubmit(box.getId(), topicCode);
        AttachmentView attachment = attachmentService.getById(attachmentId);
        if (attachment.usageType() != AttachmentUsageType.ANONYMOUS_AVATAR
                || !Boolean.TRUE.equals(attachment.isActive())) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "请选择有效的匿名头像附件");
        }
        // 2. 创建 PENDING 问题
        QuestionEntity q = new QuestionEntity()
                .setBoxUserId(box.getId())
                .setAttachmentId(attachmentId)
                .setTopicId(topic == null ? null : topic.getId())
                .setQuestion(question)
                .setStatus(QuestionStatus.PENDING)
                .setIp(ip)
                .setUserAgent(userAgent);
        questionRepository.insert(q);
        eventPublisher.publishEvent(new QuestionSubmittedEvent(q.getId(), origin));
        log.info("QuestionEntity submitted to box '{}': id={} ip={}", slug, q.getId(), ip);
    }

    @Override
    @Cacheable(value = "published", key = "#slug + ':' + (#topicCode ?: '') + ':' + #page + ':' + #pageSize")
    public PageResult<QuestionView> getPublished(String slug, String topicCode, long page, long pageSize) {
        // 通过 slug 找到提问箱 → 查询该箱子的 PUBLISHED 问题
        BoxUserEntity box = boxUserService.getBySlug(slug);
        BoxTopicEntity topic = topicService.requireInBox(box.getId(), topicCode);
        IPage<QuestionEntity> result = topic == null
                ? questionRepository.findPublishedByBoxId(box.getId(), Page.of(page, pageSize))
                : questionRepository.findPublishedByBoxIdAndTopicId(
                        box.getId(), topic.getId(), Page.of(page, pageSize));
        return PageResult.from(result).map(toQuestionViewWith(topicSummaryMap(result)));
    }

    @Override
    public PageResult<PendingQuestionView> getPending(Long boxUserId, long page, long pageSize) {
        IPage<QuestionEntity> result = questionRepository.findPendingByBoxId(boxUserId, Page.of(page, pageSize));
        return PageResult.from(result).map(toPendingViewWith(topicSummaryMap(result)));
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
    @CacheEvict(value = "published", allEntries = true)
    public void delete(Long boxUserId, Long questionId) {
        QuestionEntity q = getAndValidateOwnership(boxUserId, questionId, ErrorCodes.QUESTION_NOT_FOUND);
        if (q.isPending()) throw new BizException(ErrorCodes.QUESTION_STATUS_INVALID, "待回答的问题不能删除");
        answerService.deleteByQuestionIdIfExists(questionId);
        likeService.deleteTarget(LikeTargetType.QUESTION, questionId);
        questionRepository.deleteById(questionId);
        log.info("QuestionEntity {} deleted by box {}", questionId, boxUserId);
    }

    @Override
    public PageResult<QuestionView> getHistory(Long boxUserId, String status, long page, long pageSize) {
        QuestionStatus qs = parseStatus(status);
        IPage<QuestionEntity> result =
                questionRepository.findByBoxUserIdAndStatus(boxUserId, qs, Page.of(page, pageSize));
        return PageResult.from(result).map(toQuestionViewWith(topicSummaryMap(result)));
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
        return PageResult.from(result).map(toQuestionViewWith(topicSummaryMap(result)));
    }

    @Override
    @Transactional
    public void forceDelete(Long questionId) {
        answerService.deleteByQuestionIdIfExists(questionId);
        likeService.deleteTarget(LikeTargetType.QUESTION, questionId);
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
        AttachmentView ownerAvatar = accountAvatarOrNull(box);
        return QuestionAssembler.toQuestionView(q, avatar, answer, ownerAvatar, topicSummary(q));
    }

    private java.util.function.Function<QuestionEntity, QuestionView> toQuestionViewWith(
            Map<?, TopicSummaryView> topics) {
        return q -> {
            AnswerEntity answer = answerService.getByQuestionId(q.getId());
            AttachmentView avatar = attachmentService.getById(q.getAttachmentId());
            BoxUserEntity box = boxUserRepository.findById(q.getBoxUserId());
            AttachmentView ownerAvatar = accountAvatarOrNull(box);
            return QuestionAssembler.toQuestionView(
                    q, avatar, answer, ownerAvatar, topicFromMap(topics, q.getTopicId()));
        };
    }

    private AttachmentView accountAvatarOrNull(BoxUserEntity box) {
        if (box == null || box.getUserId() == null) {
            return null;
        }
        SysUserEntity user = sysUserRepository.findById(box.getUserId());
        return user != null && user.getAvatarAttachmentId() != null
                ? attachmentService.getById(user.getAvatarAttachmentId())
                : null;
    }

    /** 构建待审问题视图（无回答）。 */
    private PendingQuestionView toPendingView(QuestionEntity q) {
        AttachmentView avatar = attachmentService.getById(q.getAttachmentId());
        return QuestionAssembler.toPendingView(q, avatar, topicSummary(q));
    }

    private java.util.function.Function<QuestionEntity, PendingQuestionView> toPendingViewWith(
            Map<?, TopicSummaryView> topics) {
        return q -> {
            AttachmentView avatar = attachmentService.getById(q.getAttachmentId());
            return QuestionAssembler.toPendingView(q, avatar, topicFromMap(topics, q.getTopicId()));
        };
    }

    private TopicSummaryView topicFromMap(Map<?, TopicSummaryView> topics, Long topicId) {
        if (topicId == null || topics == null) {
            return null;
        }
        TopicSummaryView topic = topics.get(topicId);
        if (topic != null) {
            return topic;
        }
        String expected = topicId.toString();
        for (Map.Entry<?, TopicSummaryView> entry : topics.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Number number && number.longValue() == topicId) {
                return entry.getValue();
            }
            if (key != null && expected.equals(key.toString())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<Long, TopicSummaryView> topicSummaryMap(IPage<QuestionEntity> page) {
        return topicService.summariesById(page.getRecords().stream()
                .map(QuestionEntity::getTopicId)
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    private TopicSummaryView topicSummary(QuestionEntity q) {
        if (q.getTopicId() == null) {
            return null;
        }
        return topicFromMap(topicService.summariesById(java.util.List.of(q.getTopicId())), q.getTopicId());
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
