package cn.xiuxius.askbox.like.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.xiuxius.askbox.like.entity.LikeCountEntity;
import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.mapper.LikeCountMapper;

@ExtendWith(MockitoExtension.class)
class LikeCountRepositoryTest {

    @Mock
    private LikeCountMapper mapper;

    @InjectMocks
    private LikeCountRepository repository;

    @Test
    void upsertPassesApplicationOwnedTimestamps() {
        repository.upsert(LikeTargetType.QUESTION, 10L, 4L);

        verify(mapper)
                .upsert(
                        eq(LikeTargetType.QUESTION),
                        eq(10L),
                        eq(4L),
                        any(OffsetDateTime.class),
                        any(OffsetDateTime.class));
    }

    @Test
    void applyDeltaUsesComputedCountAndApplicationOwnedTimestamps() {
        LikeCountEntity entity = new LikeCountEntity().setLikeCount(5L);
        when(mapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

        long next = repository.applyDelta(LikeTargetType.ANSWER, 20L, 3L, 1L);

        assertThat(next).isEqualTo(7L);
        verify(mapper)
                .upsert(
                        eq(LikeTargetType.ANSWER),
                        eq(20L),
                        eq(7L),
                        any(OffsetDateTime.class),
                        any(OffsetDateTime.class));
    }
}
