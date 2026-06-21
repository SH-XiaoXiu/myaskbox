package cn.xiuxius.askbox.like.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import cn.xiuxius.askbox.ai.entity.AiReviewEntity;
import cn.xiuxius.askbox.ai.enums.AiReviewStatus;
import cn.xiuxius.askbox.ai.repository.AiReviewRepository;
import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.repository.AnswerRepository;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.repository.LikeCountRepository;
import cn.xiuxius.askbox.like.request.LikeTargetRequest;
import cn.xiuxius.askbox.like.view.LikeCountView;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeServiceImpl implements LikeService {

    private static final String COUNT_PREFIX = "askbox:likes:count:";
    private static final String DIRTY_KEY = "askbox:likes:dirty";
    private static final String PLUS_KEY = "askbox:likes:delta:plus";
    private static final String MINUS_KEY = "askbox:likes:delta:minus";
    private static final DefaultRedisScript<Long> CHANGE_SCRIPT = new DefaultRedisScript<>(
            """
            local current = redis.call('GET', KEYS[1])
            if not current then
              current = ARGV[1]
            end
            local nextValue = tonumber(current) + tonumber(ARGV[2])
            if nextValue < 0 then
              nextValue = 0
            end
            redis.call('SET', KEYS[1], nextValue, 'EX', ARGV[3])
            if tonumber(ARGV[2]) > 0 then
              redis.call('HINCRBY', KEYS[2], ARGV[4], tonumber(ARGV[2]))
            elseif tonumber(ARGV[2]) < 0 then
              redis.call('HINCRBY', KEYS[3], ARGV[4], math.abs(tonumber(ARGV[2])))
            end
            redis.call('ZADD', KEYS[4], ARGV[5], ARGV[4])
            return nextValue
            """,
            Long.class);
    private static final DefaultRedisScript<Long> ACK_SCRIPT = new DefaultRedisScript<>(
            """
            local plusValue = tonumber(redis.call('HGET', KEYS[1], ARGV[1]) or '0')
            local minusValue = tonumber(redis.call('HGET', KEYS[2], ARGV[1]) or '0')
            local ackPlus = tonumber(ARGV[2])
            local ackMinus = tonumber(ARGV[3])

            local remainPlus = plusValue - ackPlus
            if remainPlus > 0 then
              redis.call('HSET', KEYS[1], ARGV[1], remainPlus)
            else
              redis.call('HDEL', KEYS[1], ARGV[1])
            end

            local remainMinus = minusValue - ackMinus
            if remainMinus > 0 then
              redis.call('HSET', KEYS[2], ARGV[1], remainMinus)
            else
              redis.call('HDEL', KEYS[2], ARGV[1])
            end

            if remainPlus <= 0 and remainMinus <= 0 then
              redis.call('ZREM', KEYS[3], ARGV[1])
              return 0
            end

            redis.call('ZADD', KEYS[3], ARGV[4], ARGV[1])
            return 1
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final LikeCountRepository likeCountRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AiReviewRepository aiReviewRepository;

    @Value("${askbox.likes.cache-ttl-seconds:1800}")
    private long cacheTtlSeconds;

    @Override
    public LikeCountView change(LikeTargetType targetType, Long targetId, int delta) {
        validateDelta(delta);
        validatePublicTarget(targetType, targetId);
        long initialCount = dbCountOrZero(targetType, targetId);
        long now = System.currentTimeMillis();
        Long count = redisTemplate.execute(
                CHANGE_SCRIPT,
                List.of(countKey(targetType, targetId), PLUS_KEY, MINUS_KEY, DIRTY_KEY),
                Long.toString(initialCount),
                Integer.toString(delta),
                Long.toString(Math.max(1L, cacheTtlSeconds)),
                dirtyMember(targetType, targetId),
                Long.toString(now));
        return new LikeCountView(targetType, targetId, count != null ? count : initialCount);
    }

    @Override
    public List<LikeCountView> batch(List<LikeTargetRequest> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        Map<String, LikeTargetRequest> uniqueTargets = new LinkedHashMap<>();
        for (LikeTargetRequest target : targets) {
            if (target == null || target.targetType() == null || target.targetId() == null) {
                continue;
            }
            uniqueTargets.put(dirtyMember(target.targetType(), target.targetId()), target);
        }
        List<LikeCountView> result = new ArrayList<>();
        for (LikeTargetRequest target : uniqueTargets.values()) {
            if (!isPublicTarget(target.targetType(), target.targetId())) {
                continue;
            }
            result.add(new LikeCountView(
                    target.targetType(), target.targetId(), currentCount(target.targetType(), target.targetId())));
        }
        return result;
    }

    @Override
    public void syncDirtyCounts(int batchSize) {
        if (batchSize <= 0) {
            return;
        }
        Set<String> members = redisTemplate.opsForZSet().range(DIRTY_KEY, 0, batchSize - 1);
        if (members == null || members.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String member : members) {
            Target target = parseMember(member);
            if (target == null) {
                redisTemplate.opsForZSet().remove(DIRTY_KEY, member);
                continue;
            }
            try {
                long plus = hashValue(PLUS_KEY, member);
                long minus = hashValue(MINUS_KEY, member);
                if (plus > 0 || minus > 0) {
                    likeCountRepository.applyDelta(target.type(), target.id(), plus, minus);
                }
                redisTemplate.execute(
                        ACK_SCRIPT,
                        List.of(PLUS_KEY, MINUS_KEY, DIRTY_KEY),
                        member,
                        Long.toString(plus),
                        Long.toString(minus),
                        Long.toString(now));
            } catch (RuntimeException ex) {
                redisTemplate.opsForZSet().add(DIRTY_KEY, member, now);
                log.warn(
                        "Like count sync failed: target={} error={}",
                        member,
                        ex.getClass().getSimpleName());
            }
        }
    }

    @Override
    public void deleteTarget(LikeTargetType targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return;
        }
        String member = dirtyMember(targetType, targetId);
        likeCountRepository.deleteByTarget(targetType, targetId);
        redisTemplate.delete(countKey(targetType, targetId));
        redisTemplate.opsForHash().delete(PLUS_KEY, member);
        redisTemplate.opsForHash().delete(MINUS_KEY, member);
        redisTemplate.opsForZSet().remove(DIRTY_KEY, member);
    }

    private long currentCount(LikeTargetType targetType, Long targetId) {
        String key = countKey(targetType, targetId);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            redisTemplate.expire(key, Duration.ofSeconds(Math.max(1L, cacheTtlSeconds)));
            return parseNonNegativeLong(cached);
        }
        long dbCount = dbCountOrZero(targetType, targetId);
        redisTemplate.opsForValue().set(key, Long.toString(dbCount), Duration.ofSeconds(Math.max(1L, cacheTtlSeconds)));
        return dbCount;
    }

    private long hashValue(String hashKey, String field) {
        Object value = redisTemplate.opsForHash().get(hashKey, field);
        return parseNonNegativeLong(value != null ? value.toString() : null);
    }

    private long dbCountOrZero(LikeTargetType targetType, Long targetId) {
        Long count = likeCountRepository.findCount(targetType, targetId);
        return count != null ? Math.max(0, count) : 0L;
    }

    private void validateDelta(int delta) {
        if (delta != 1 && delta != -1) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "点赞变更值不合法");
        }
    }

    private void validatePublicTarget(LikeTargetType targetType, Long targetId) {
        if (!isPublicTarget(targetType, targetId)) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "点赞目标不存在");
        }
    }

    private boolean isPublicTarget(LikeTargetType targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return false;
        }
        return switch (targetType) {
            case QUESTION -> {
                QuestionEntity question = questionRepository.findById(targetId);
                yield question != null && question.getStatus() == QuestionStatus.PUBLISHED;
            }
            case ANSWER -> {
                AnswerEntity answer = answerRepository.findById(targetId);
                if (answer == null) {
                    yield false;
                }
                QuestionEntity question = questionRepository.findById(answer.getQuestionId());
                yield question != null && question.getStatus() == QuestionStatus.PUBLISHED;
            }
            case AI_REVIEW -> {
                AiReviewEntity review = aiReviewRepository.findById(targetId);
                if (review == null || review.getStatus() != AiReviewStatus.SUCCEEDED) {
                    yield false;
                }
                QuestionEntity question = questionRepository.findById(review.getQuestionId());
                yield question != null && question.getStatus() == QuestionStatus.PUBLISHED;
            }
        };
    }

    private String countKey(LikeTargetType targetType, Long targetId) {
        return COUNT_PREFIX + targetType.name() + ":" + targetId;
    }

    private String dirtyMember(LikeTargetType targetType, Long targetId) {
        return targetType.name() + ":" + targetId;
    }

    private long parseNonNegativeLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private Target parseMember(String member) {
        if (member == null || member.isBlank()) {
            return null;
        }
        String[] parts = member.split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Target(LikeTargetType.valueOf(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private record Target(LikeTargetType type, Long id) {}
}
