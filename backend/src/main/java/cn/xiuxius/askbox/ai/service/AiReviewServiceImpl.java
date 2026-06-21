package cn.xiuxius.askbox.ai.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.xiuxius.askbox.ai.config.AiProperties;
import cn.xiuxius.askbox.ai.entity.AiBoxProfileEntity;
import cn.xiuxius.askbox.ai.entity.AiReviewEntity;
import cn.xiuxius.askbox.ai.enums.AiReviewStatus;
import cn.xiuxius.askbox.ai.enums.AiReviewTriggerType;
import cn.xiuxius.askbox.ai.event.AiReviewQueuedEvent;
import cn.xiuxius.askbox.ai.repository.AiBoxProfileRepository;
import cn.xiuxius.askbox.ai.repository.AiCallLogRepository;
import cn.xiuxius.askbox.ai.repository.AiReviewRepository;
import cn.xiuxius.askbox.ai.tools.AiReviewTools;
import cn.xiuxius.askbox.ai.view.AiReviewView;
import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.service.AnswerService;
import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.event.AnswerPublishedEvent;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.usersetting.service.UserSettingService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AiReviewServiceImpl implements AiReviewService {
    private static final String PROMPT_VERSION = "ai-review-v2-spring-ai-tools-toxic-comedy";

    private final AiProperties properties;
    private final AiReviewRepository reviewRepository;
    private final AiBoxProfileRepository profileRepository;
    private final AiCallLogRepository callLogRepository;
    private final QuestionRepository questionRepository;
    private final AnswerService answerService;
    private final BoxUserService boxUserService;
    private final UserSettingService userSettingService;
    private final AiRateGuard rateGuard;
    private final ChatClient chatClient;
    private final AiReviewTools aiReviewTools;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;
    private final Resource systemPromptResource;
    private final Resource userPromptResource;

    public AiReviewServiceImpl(
            AiProperties properties,
            AiReviewRepository reviewRepository,
            AiBoxProfileRepository profileRepository,
            AiCallLogRepository callLogRepository,
            QuestionRepository questionRepository,
            AnswerService answerService,
            BoxUserService boxUserService,
            UserSettingService userSettingService,
            AiRateGuard rateGuard,
            ChatClient chatClient,
            AiReviewTools aiReviewTools,
            ObjectMapper objectMapper,
            CacheManager cacheManager,
            ApplicationEventPublisher eventPublisher,
            @Value("classpath:prompts/ai-review-system.md") Resource systemPromptResource,
            @Value("classpath:prompts/ai-review-user.md") Resource userPromptResource) {
        this.properties = properties;
        this.reviewRepository = reviewRepository;
        this.profileRepository = profileRepository;
        this.callLogRepository = callLogRepository;
        this.questionRepository = questionRepository;
        this.answerService = answerService;
        this.boxUserService = boxUserService;
        this.userSettingService = userSettingService;
        this.rateGuard = rateGuard;
        this.chatClient = chatClient;
        this.aiReviewTools = aiReviewTools;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
        this.eventPublisher = eventPublisher;
        this.systemPromptResource = systemPromptResource;
        this.userPromptResource = userPromptResource;
    }

    @Override
    public void enqueueAuto(Long questionId) {
        QuestionEntity question = questionRepository.findById(questionId);
        if (question == null) {
            return;
        }
        BoxUserEntity box = boxUserService.getById(question.getBoxUserId());
        if (!userSettingService.isAiReviewEnabled(box.getUserId())) {
            return;
        }
        enqueue(question, AiReviewTriggerType.AUTO, null, false);
    }

    @Override
    public AiReviewView enqueueManual(Long questionId, Long adminUserId) {
        QuestionEntity question = requirePublishedQuestion(questionId);
        return toView(enqueue(question, AiReviewTriggerType.MANUAL, adminUserId, true));
    }

    @Override
    public AiReviewView getByQuestionId(Long questionId) {
        return toView(reviewRepository.findByQuestionId(questionId));
    }

    @Override
    public AiReviewView getAdminByQuestionId(Long questionId) {
        return getByQuestionId(questionId);
    }

    @Override
    public List<AiReviewView> batchSucceededPublic(Iterable<Long> questionIds) {
        return succeededByQuestionIds(questionIds).values().stream().toList();
    }

    @Override
    public Map<Long, AiReviewView> succeededByQuestionIds(Iterable<Long> questionIds) {
        java.util.List<Long> ids = new java.util.ArrayList<>();
        questionIds.forEach(id -> {
            if (id != null) {
                ids.add(id);
            }
        });
        Map<Long, AiReviewView> result = new LinkedHashMap<>();
        reviewRepository.findByQuestionIds(ids).stream()
                .filter(review -> review.getStatus() == AiReviewStatus.SUCCEEDED)
                .filter(review -> isPublishedQuestion(review.getQuestionId()))
                .forEach(review -> result.put(review.getQuestionId(), toView(review)));
        return result;
    }

    @Override
    public void deleteByQuestionId(Long questionId) {
        reviewRepository.deleteByQuestionId(questionId);
    }

    @EventListener
    public void onAnswerPublished(AnswerPublishedEvent event) {
        enqueueAuto(event.questionId());
    }

    @Async("askboxAiExecutor")
    @EventListener
    public void onAiReviewQueued(AiReviewQueuedEvent event) {
        generate(event.questionId());
    }

    public void generate(Long questionId) {
        AiReviewEntity review = reviewRepository.findByQuestionId(questionId);
        if (review == null) {
            return;
        }
        try {
            if (!properties.isEnabled()
                    || properties.getApiKey() == null
                    || properties.getApiKey().isBlank()) {
                logCall(review, "CONFIG", false, null, null, "AI配置未启用或缺少 API Key", null);
                fail(review, AiReviewStatus.SKIPPED, "AI配置未启用或缺少 API Key");
                return;
            }
            AiRateGuard.GuardResult guard = rateGuard.tryAcquire();
            if (!guard.allowed()) {
                logCall(review, "RATE_GUARD", false, null, null, guard.reason(), null);
                fail(review, AiReviewStatus.FAILED, guard.reason());
                return;
            }
            review.setStatus(AiReviewStatus.RUNNING).setErrorMessage(null).setUpdatedAt(now());
            reviewRepository.update(review);
            long startedAt = System.nanoTime();
            String systemPrompt = prompt(systemPromptResource);
            String userPrompt =
                    prompt(userPromptResource).replace("{questionId}", String.valueOf(review.getQuestionId()));
            String requestPayload = requestPayload(systemPrompt, userPrompt);
            String content = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .tools(aiReviewTools)
                    .call()
                    .content();
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            logCall(review, "CHAT_COMPLETION", true, requestPayload, responsePayload(content), null, durationMs);
            JsonNode root = objectMapper.readTree(content);
            String reviewText = text(root, "review");
            String styleSummary = text(root, "updatedStyleSummary");
            if (reviewText.isBlank()) {
                throw new IllegalStateException("AI响应缺少 review");
            }
            review.setStatus(AiReviewStatus.SUCCEEDED)
                    .setContent(reviewText)
                    .setErrorMessage(null)
                    .setModel(properties.getModel())
                    .setPromptVersion(PROMPT_VERSION)
                    .setCompletedAt(now())
                    .setUpdatedAt(now());
            reviewRepository.update(review);
            evictPublishedCache();
            if (!styleSummary.isBlank()) {
                upsertProfile(review.getBoxUserId(), styleSummary);
            }
        } catch (Exception ex) {
            log.warn("AI review generation failed: questionId={} message={}", questionId, ex.getMessage());
            logCall(review, "ERROR", false, null, null, ex.getMessage(), null);
            fail(review, AiReviewStatus.FAILED, ex.getMessage());
        }
    }

    @Transactional
    protected AiReviewEntity enqueue(
            QuestionEntity question, AiReviewTriggerType triggerType, Long triggeredBy, boolean overwrite) {
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new BizException(ErrorCodes.QUESTION_STATUS_INVALID, "只有已发布问答可以生成AI点评");
        }
        AnswerEntity answer = answerService.getByQuestionId(question.getId());
        if (answer == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "问题还没有回答");
        }
        AiReviewEntity existing = reviewRepository.findByQuestionId(question.getId());
        if (existing != null
                && !overwrite
                && (existing.getStatus() == AiReviewStatus.PENDING || existing.getStatus() == AiReviewStatus.RUNNING)) {
            return existing;
        }
        OffsetDateTime now = now();
        AiReviewEntity review = existing == null
                ? new AiReviewEntity().setQuestionId(question.getId()).setBoxUserId(question.getBoxUserId())
                : existing;
        review.setAnswerId(answer.getId())
                .setStatus(AiReviewStatus.PENDING)
                .setContent(overwrite ? null : review.getContent())
                .setErrorMessage(null)
                .setModel(properties.getModel())
                .setPromptVersion(PROMPT_VERSION)
                .setTriggerType(triggerType)
                .setTriggeredBy(triggeredBy)
                .setCompletedAt(null)
                .setUpdatedAt(now);
        if (existing == null) {
            review.setCreatedAt(now);
            reviewRepository.insert(review);
        } else {
            reviewRepository.update(review);
        }
        eventPublisher.publishEvent(new AiReviewQueuedEvent(question.getId()));
        return review;
    }

    private QuestionEntity requirePublishedQuestion(Long questionId) {
        QuestionEntity question = questionRepository.findById(questionId);
        if (question == null) {
            throw new BizException(ErrorCodes.QUESTION_NOT_FOUND);
        }
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new BizException(ErrorCodes.QUESTION_STATUS_INVALID, "只有已发布问答可以生成AI点评");
        }
        return question;
    }

    private void upsertProfile(Long boxUserId, String styleSummary) {
        AiBoxProfileEntity existing = profileRepository.findByBoxUserId(boxUserId);
        int nextSampleCount = existing == null ? 1 : Math.max(0, existing.getSampleCount()) + 1;
        int nextVersion = existing == null ? 1 : Math.max(1, existing.getVersion()) + 1;
        profileRepository.upsert(new AiBoxProfileEntity()
                .setBoxUserId(boxUserId)
                .setStyleSummary(styleSummary)
                .setSampleCount(nextSampleCount)
                .setVersion(nextVersion)
                .setUpdatedAt(now()));
    }

    private void fail(AiReviewEntity review, AiReviewStatus status, String message) {
        review.setStatus(status)
                .setErrorMessage(message == null ? "AI点评生成失败" : message)
                .setCompletedAt(now())
                .setUpdatedAt(now());
        reviewRepository.update(review);
        evictPublishedCache();
    }

    private void evictPublishedCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache("published");
        if (cache != null) {
            cache.clear();
        }
    }

    private void logCall(
            AiReviewEntity review,
            String stage,
            boolean success,
            String requestPayload,
            String responsePayload,
            String errorMessage,
            Long durationMs) {
        try {
            callLogRepository.insert(new cn.xiuxius.askbox.ai.entity.AiCallLogEntity()
                    .setQuestionId(review.getQuestionId())
                    .setAiReviewId(review.getId())
                    .setBoxUserId(review.getBoxUserId())
                    .setTriggerType(review.getTriggerType())
                    .setTriggeredBy(review.getTriggeredBy())
                    .setStage(stage)
                    .setSuccess(success)
                    .setModel(properties.getModel())
                    .setBaseUrl(properties.getBaseUrl())
                    .setRequestPayload(requestPayload)
                    .setResponsePayload(responsePayload)
                    .setErrorMessage(errorMessage)
                    .setDurationMs(durationMs)
                    .setCreatedAt(now()));
        } catch (RuntimeException ex) {
            log.warn("Failed to write AI call log: {}", ex.getMessage());
        }
    }

    private AiReviewView toView(AiReviewEntity review) {
        if (review == null) {
            return null;
        }
        return new AiReviewView(
                review.getId(),
                review.getQuestionId(),
                review.getStatus().name(),
                review.getContent(),
                review.getErrorMessage(),
                review.getTriggerType() == null ? null : review.getTriggerType().name(),
                review.getCompletedAt() == null
                        ? null
                        : review.getCompletedAt().toInstant().toEpochMilli());
    }

    private boolean isPublishedQuestion(Long questionId) {
        QuestionEntity question = questionRepository.findById(questionId);
        return question != null && question.getStatus() == QuestionStatus.PUBLISHED;
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root == null ? null : root.get(field);
        return node == null || node.isNull() ? "" : node.asText("").trim();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String prompt(Resource resource) {
        try {
            return resource.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("读取AI提示词失败: " + resource.getFilename(), ex);
        }
    }

    private String requestPayload(String systemPrompt, String userPrompt) {
        return serialize(Map.of(
                "model",
                properties.getModel(),
                "promptVersion",
                PROMPT_VERSION,
                "system",
                systemPrompt,
                "user",
                userPrompt,
                "tools",
                List.of(
                        "getCurrentQaContext",
                        "getRelevantHistoricalQa",
                        "getBoxStyleProfile",
                        "getPopularAiReviewExamples")));
    }

    private String responsePayload(String content) {
        return serialize(Map.of("content", content == null ? "" : content));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
