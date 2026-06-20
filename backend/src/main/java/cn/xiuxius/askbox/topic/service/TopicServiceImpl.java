package cn.xiuxius.askbox.topic.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.service.SysUserService;
import cn.xiuxius.askbox.topic.entity.BoxTopicEntity;
import cn.xiuxius.askbox.topic.enums.TopicStatus;
import cn.xiuxius.askbox.topic.repository.BoxTopicRepository;
import cn.xiuxius.askbox.topic.request.TopicCreateRequest;
import cn.xiuxius.askbox.topic.view.AdminTopicView;
import cn.xiuxius.askbox.topic.view.PublicTopicView;
import cn.xiuxius.askbox.topic.view.TopicSummaryView;
import cn.xiuxius.askbox.topic.view.TopicView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    private static final String CODE_ALPHABET = "23456789abcdefghijkmnpqrstuvwxyz";
    private static final int CODE_LENGTH = 10;
    private static final int DEFAULT_ACTIVE_LIMIT = 5;
    private static final int MAX_DURATION_DAYS = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BoxTopicRepository topicRepository;
    private final BoxUserService boxUserService;
    private final QuestionRepository questionRepository;
    private final SysUserService sysUserService;

    @Override
    @Transactional
    @CacheEvict(
            value = {"published", "public-topics", "topic-summaries"},
            allEntries = true)
    public TopicView create(Long boxUserId, TopicCreateRequest request) {
        String title = request.getTitle() == null ? "" : request.getTitle().trim();
        if (title.isBlank()) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "话题标题不能为空");
        }
        OffsetDateTime now = now();
        int activeLimit = topicActiveLimit(boxUserId);
        if (topicRepository.countAvailableByBoxId(boxUserId, now) >= activeLimit) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "同时有效话题不能超过 " + activeLimit + " 个");
        }
        OffsetDateTime expiresAt = request.getExpiresAt();
        if (expiresAt == null) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "到期时间不能为空");
        }
        if (!expiresAt.isAfter(now)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "到期时间必须晚于当前时间");
        }
        if (expiresAt.isAfter(now.plusDays(MAX_DURATION_DAYS))) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "话题最长不能超过7天");
        }
        BoxTopicEntity topic = new BoxTopicEntity()
                .setBoxUserId(boxUserId)
                .setCode(generateUniqueCode())
                .setTitle(title)
                .setDescription(
                        request.getDescription() == null
                                ? ""
                                : request.getDescription().trim())
                .setStatus(TopicStatus.ACTIVE)
                .setExpiresAt(expiresAt);
        topicRepository.insert(topic);
        return toTopicView(topic);
    }

    @Override
    public List<TopicView> listOwnerTopics(Long boxUserId) {
        return topicRepository.findByBoxId(boxUserId).stream()
                .map(this::toTopicView)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {"published", "public-topics", "topic-summaries"},
            allEntries = true)
    public TopicView close(Long boxUserId, Long topicId) {
        BoxTopicEntity topic = topicRepository.findById(topicId);
        if (topic == null || !topic.isOwnedByBox(boxUserId)) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "话题不存在");
        }
        closeTopic(topic);
        return toTopicView(topic);
    }

    @Override
    @Transactional
    @CacheEvict(
            value = {"published", "public-topics", "topic-summaries"},
            allEntries = true)
    public AdminTopicView adminClose(Long topicId) {
        BoxTopicEntity topic = topicRepository.findById(topicId);
        if (topic == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "话题不存在");
        }
        closeTopic(topic);
        return toAdminTopicView(topic);
    }

    @Override
    public PageResult<AdminTopicView> listAdminTopics(
            long page, long pageSize, Long boxUserId, String status, String keyword) {
        String statusFilter = parseAdminStatus(status);
        IPage<BoxTopicEntity> result =
                topicRepository.pageAll(Page.of(page, pageSize), boxUserId, statusFilter, keyword, now());
        return PageResult.from(result.convert(this::toAdminTopicView));
    }

    @Override
    @Cacheable(value = "public-topics", key = "'available:' + #slug")
    public List<PublicTopicView> listPublicAvailable(String slug) {
        BoxUserEntity box = boxUserService.getBySlug(slug);
        OffsetDateTime now = now();
        int activeLimit = topicActiveLimit(box.getId());
        return topicRepository.findAvailableByBoxId(box.getId(), now, activeLimit).stream()
                .map(topic -> toPublicView(topic, now))
                .toList();
    }

    @Override
    @Cacheable(value = "public-topics", key = "'resolve:' + #slug + ':' + #code")
    public PublicTopicView resolvePublic(String slug, String code) {
        BoxUserEntity box = boxUserService.getBySlug(slug);
        BoxTopicEntity topic = findByCodeInBox(code, box.getId());
        return toPublicView(topic, now());
    }

    @Override
    public BoxTopicEntity requireAvailableForSubmit(Long boxUserId, String topicCode) {
        if (topicCode == null || topicCode.isBlank()) {
            return null;
        }
        BoxTopicEntity topic = findByCodeInBox(topicCode, boxUserId);
        if (!topic.isAvailable(now())) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "话题已结束");
        }
        return topic;
    }

    @Override
    public BoxTopicEntity requireInBox(Long boxUserId, String topicCode) {
        if (topicCode == null || topicCode.isBlank()) {
            return null;
        }
        return findByCodeInBox(topicCode, boxUserId);
    }

    @Override
    @Cacheable(value = "topic-summaries", key = "#ids.toString()")
    public Map<Long, TopicSummaryView> summariesById(Iterable<Long> ids) {
        Set<Long> normalized = new HashSet<>();
        ids.forEach(id -> {
            if (id != null) {
                normalized.add(id);
            }
        });
        if (normalized.isEmpty()) {
            return Map.of();
        }
        return topicRepository.findByIds(normalized).stream()
                .map(topic -> new TopicSummaryView(topic.getId(), topic.getCode(), topic.getTitle()))
                .collect(Collectors.toMap(TopicSummaryView::id, Function.identity()));
    }

    private BoxTopicEntity findByCodeInBox(String code, Long boxUserId) {
        if (code == null || code.isBlank()) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "话题不存在");
        }
        BoxTopicEntity topic = topicRepository.findByCode(code.trim());
        if (topic == null || !topic.isOwnedByBox(boxUserId)) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "话题不存在");
        }
        return topic;
    }

    private void closeTopic(BoxTopicEntity topic) {
        if (topic.getStatus() != TopicStatus.CLOSED) {
            topic.setStatus(TopicStatus.CLOSED).setClosedAt(now()).setUpdatedAt(now());
            topicRepository.update(topic);
        }
    }

    private int topicActiveLimit(Long boxUserId) {
        BoxUserEntity box = boxUserService.getById(boxUserId);
        SysUserEntity user = sysUserService.getById(box.getUserId());
        Integer limit = user.getTopicActiveLimit();
        return limit == null ? DEFAULT_ACTIVE_LIMIT : Math.max(1, limit);
    }

    private String parseAdminStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "EXPIRED", "CLOSED").contains(normalized)) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "话题状态不正确");
        }
        return normalized;
    }

    private TopicView toTopicView(BoxTopicEntity topic) {
        OffsetDateTime now = now();
        return new TopicView(
                topic.getId(),
                topic.getCode(),
                topic.getTitle(),
                topic.getDescription(),
                effectiveStatus(topic, now),
                topic.isAvailable(now),
                topic.getExpiresAt(),
                topic.getClosedAt(),
                topic.getCreatedAt(),
                questionRepository.countByBoxUserIdAndTopicId(topic.getBoxUserId(), topic.getId()));
    }

    private PublicTopicView toPublicView(BoxTopicEntity topic, OffsetDateTime now) {
        boolean available = topic.isAvailable(now);
        return new PublicTopicView(
                topic.getCode(),
                topic.getTitle(),
                topic.getDescription(),
                available,
                available ? null : unavailableReason(topic, now),
                topic.getExpiresAt());
    }

    private AdminTopicView toAdminTopicView(BoxTopicEntity topic) {
        OffsetDateTime now = now();
        BoxUserEntity box = boxUserService.getById(topic.getBoxUserId());
        SysUserEntity owner = sysUserService.getById(box.getUserId());
        return new AdminTopicView(
                topic.getId(),
                topic.getCode(),
                topic.getTitle(),
                topic.getDescription(),
                effectiveStatus(topic, now),
                topic.isAvailable(now),
                topic.getExpiresAt(),
                topic.getClosedAt(),
                topic.getCreatedAt(),
                questionRepository.countByBoxUserIdAndTopicId(topic.getBoxUserId(), topic.getId()),
                box.getId(),
                box.getSlug(),
                box.getDisplayName(),
                owner.getId(),
                owner.getEmail(),
                owner.getDisplayName());
    }

    private String effectiveStatus(BoxTopicEntity topic, OffsetDateTime now) {
        if (topic.getStatus() == TopicStatus.CLOSED) {
            return "CLOSED";
        }
        if (topic.isExpired(now)) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    private String unavailableReason(BoxTopicEntity topic, OffsetDateTime now) {
        if (topic.getStatus() == TopicStatus.CLOSED) {
            return "CLOSED";
        }
        if (topic.isExpired(now)) {
            return "EXPIRED";
        }
        return "UNAVAILABLE";
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 8; attempt++) {
            String code = randomCode();
            if (topicRepository.findByCode(code) == null) {
                return code;
            }
        }
        throw new BizException(ErrorCodes.INTERNAL_ERROR, "话题链接生成失败");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
