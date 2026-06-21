package cn.xiuxius.askbox.ai.tools;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import cn.xiuxius.askbox.ai.config.AiProperties;
import cn.xiuxius.askbox.ai.entity.AiBoxProfileEntity;
import cn.xiuxius.askbox.ai.mapper.AiReviewMapper;
import cn.xiuxius.askbox.ai.repository.AiBoxProfileRepository;
import cn.xiuxius.askbox.ai.repository.AiReviewRepository;
import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.service.AnswerService;
import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiReviewTools {
    private final AiProperties properties;
    private final QuestionRepository questionRepository;
    private final AnswerService answerService;
    private final BoxUserService boxUserService;
    private final AiBoxProfileRepository profileRepository;
    private final AiReviewRepository reviewRepository;

    @Tool(description = "获取当前要点评的问答上下文。一楼是匿名提问者消息，二楼是箱主回复，AI只能生成三楼网友跟帖。")
    public CurrentQaContext getCurrentQaContext(@ToolParam(description = "当前问题ID") Long questionId) {
        QuestionEntity question = questionRepository.findById(questionId);
        if (question == null || question.getStatus() != QuestionStatus.PUBLISHED) {
            return null;
        }
        AnswerEntity answer = answerService.getByQuestionId(questionId);
        BoxUserEntity box = boxUserService.getById(question.getBoxUserId());
        return new CurrentQaContext(
                question.getId(),
                question.getBoxUserId(),
                box == null ? null : box.getDisplayName(),
                box == null ? null : box.getSlug(),
                question.getTopicId(),
                question.getQuestion(),
                question.getQuestion(),
                answer == null ? null : answer.getId(),
                answer == null ? null : answer.getContent(),
                answer == null ? null : answer.getContent());
    }

    @Tool(description = "获取当前箱主的相关历史公开问答。一楼是匿名提问者消息，二楼是箱主回复，用来参考语气和上下文。")
    public List<HistoricalQa> getRelevantHistoricalQa(@ToolParam(description = "当前问题ID") Long questionId) {
        QuestionEntity question = questionRepository.findById(questionId);
        if (question == null) {
            return List.of();
        }
        return questionRepository
                .findRecentPublishedCandidates(
                        question.getBoxUserId(),
                        question.getId(),
                        question.getTopicId(),
                        properties.getHistoryCandidateLimit())
                .stream()
                .map(candidate -> {
                    AnswerEntity answer = answerService.getByQuestionId(candidate.getId());
                    if (answer == null) {
                        return null;
                    }
                    return new HistoricalQa(
                            candidate.getId(),
                            candidate.getTopicId(),
                            candidate.getQuestion(),
                            candidate.getQuestion(),
                            answer.getContent(),
                            answer.getContent());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Tool(description = "获取箱主历史回答风格摘要，用来让点评逐步贴近箱主个人风格。")
    public BoxStyleProfile getBoxStyleProfile(@ToolParam(description = "箱主ID") Long boxUserId) {
        AiBoxProfileEntity profile = profileRepository.findByBoxUserId(boxUserId);
        if (profile == null) {
            return new BoxStyleProfile(boxUserId, "暂无", 0, 0);
        }
        return new BoxStyleProfile(
                boxUserId,
                profile.getStyleSummary() == null ? "暂无" : profile.getStyleSummary(),
                profile.getSampleCount(),
                profile.getVersion());
    }

    @Tool(description = "获取高赞 AI 点评样本，优先同箱主，其次全站高赞，用来学习观众喜欢的吐槽风格。")
    public List<PopularReviewExample> getPopularAiReviewExamples(@ToolParam(description = "箱主ID") Long boxUserId) {
        return reviewRepository.findPopularExamples(boxUserId, properties.getPopularExampleLimit()).stream()
                .map(AiReviewTools::toPopularReviewExample)
                .toList();
    }

    private static PopularReviewExample toPopularReviewExample(AiReviewMapper.PopularAiReviewExample example) {
        return new PopularReviewExample(
                example.aiReviewId(),
                example.questionId(),
                example.boxUserId(),
                example.content(),
                example.likeCount() == null ? 0 : example.likeCount());
    }

    public record CurrentQaContext(
            Long questionId,
            Long boxUserId,
            String boxDisplayName,
            String boxSlug,
            Long topicId,
            String askerMessage,
            String question,
            Long answerId,
            String boxOwnerReply,
            String answer) {}

    public record HistoricalQa(
            Long questionId, Long topicId, String askerMessage, String question, String boxOwnerReply, String answer) {}

    public record BoxStyleProfile(Long boxUserId, String styleSummary, int sampleCount, int version) {}

    public record PopularReviewExample(
            Long aiReviewId, Long questionId, Long boxUserId, String content, long likeCount) {}
}
