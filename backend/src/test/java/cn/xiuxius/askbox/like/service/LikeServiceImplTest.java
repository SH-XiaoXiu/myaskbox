package cn.xiuxius.askbox.like.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import cn.xiuxius.askbox.answer.entity.AnswerEntity;
import cn.xiuxius.askbox.answer.repository.AnswerRepository;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.repository.LikeCountRepository;
import cn.xiuxius.askbox.like.request.LikeTargetRequest;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.repository.QuestionRepository;

@ExtendWith(MockitoExtension.class)
class LikeServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private LikeCountRepository likeCountRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @InjectMocks
    private LikeServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "cacheTtlSeconds", 1800L);
    }

    @Test
    void changeIncrementsPublishedQuestionAndWritesLedger() {
        QuestionEntity question = new QuestionEntity().setId(10L).setStatus(QuestionStatus.PUBLISHED);
        when(questionRepository.findById(10L)).thenReturn(question);
        when(likeCountRepository.findCount(LikeTargetType.QUESTION, 10L)).thenReturn(3L);
        when(redisTemplate.execute(
                        any(RedisScript.class),
                        anyList(),
                        eq("3"),
                        eq("1"),
                        eq("1800"),
                        eq("QUESTION:10"),
                        anyString()))
                .thenReturn(4L);

        var result = service.change(LikeTargetType.QUESTION, 10L, 1);

        assertThat(result.likeCount()).isEqualTo(4L);
    }

    @Test
    void changeRejectsInvalidDelta() {
        assertThatThrownBy(() -> service.change(LikeTargetType.QUESTION, 10L, 2))
                .isInstanceOf(BizException.class);
    }

    @Test
    void batchRefreshesTtlWhenCacheHits() {
        QuestionEntity published = new QuestionEntity().setId(10L).setStatus(QuestionStatus.PUBLISHED);
        when(questionRepository.findById(10L)).thenReturn(published);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("askbox:likes:count:QUESTION:10")).thenReturn("5");

        var result = service.batch(List.of(new LikeTargetRequest(LikeTargetType.QUESTION, 10L)));

        assertThat(result).extracting("likeCount").containsExactly(5L);
        verify(redisTemplate).expire("askbox:likes:count:QUESTION:10", Duration.ofSeconds(1800));
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void batchBackfillsCacheOnMiss() {
        QuestionEntity published = new QuestionEntity().setId(10L).setStatus(QuestionStatus.PUBLISHED);
        when(questionRepository.findById(10L)).thenReturn(published);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("askbox:likes:count:QUESTION:10")).thenReturn(null);
        when(likeCountRepository.findCount(LikeTargetType.QUESTION, 10L)).thenReturn(7L);

        var result = service.batch(List.of(new LikeTargetRequest(LikeTargetType.QUESTION, 10L)));

        assertThat(result).extracting("likeCount").containsExactly(7L);
        verify(valueOperations).set("askbox:likes:count:QUESTION:10", "7", Duration.ofSeconds(1800));
    }

    @Test
    void syncDirtyCountsAppliesPlusMinusDelta() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.range("askbox:likes:dirty", 0, 199)).thenReturn(Set.of("QUESTION:10"));
        when(hashOperations.get("askbox:likes:delta:plus", "QUESTION:10")).thenReturn("3");
        when(hashOperations.get("askbox:likes:delta:minus", "QUESTION:10")).thenReturn("1");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("QUESTION:10"), eq("3"), eq("1"), anyString()))
                .thenReturn(0L);

        service.syncDirtyCounts(200);

        verify(likeCountRepository).applyDelta(LikeTargetType.QUESTION, 10L, 3L, 1L);
    }

    @Test
    void syncDirtyCountsSkipsDbWriteWhenNoLedgerDelta() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(zSetOperations.range("askbox:likes:dirty", 0, 199)).thenReturn(Set.of("QUESTION:10"));
        when(hashOperations.get("askbox:likes:delta:plus", "QUESTION:10")).thenReturn(null);
        when(hashOperations.get("askbox:likes:delta:minus", "QUESTION:10")).thenReturn(null);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), eq("QUESTION:10"), eq("0"), eq("0"), anyString()))
                .thenReturn(0L);

        service.syncDirtyCounts(200);

        verify(likeCountRepository, never()).applyDelta(any(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void batchReturnsOnlyPublicAnswerTarget() {
        QuestionEntity published = new QuestionEntity().setId(10L).setStatus(QuestionStatus.PUBLISHED);
        AnswerEntity answer = new AnswerEntity().setId(20L).setQuestionId(10L);
        when(answerRepository.findById(20L)).thenReturn(answer);
        when(questionRepository.findById(10L)).thenReturn(published);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("askbox:likes:count:ANSWER:20")).thenReturn("2");

        var result = service.batch(List.of(new LikeTargetRequest(LikeTargetType.ANSWER, 20L)));

        assertThat(result).extracting("likeCount").containsExactly(2L);
    }
}
