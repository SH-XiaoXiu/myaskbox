package cn.xiuxius.askbox.ai.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.xiuxius.askbox.ai.entity.AiReviewEntity;
import cn.xiuxius.askbox.ai.mapper.AiReviewMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AiReviewRepository {
    private final AiReviewMapper mapper;

    public AiReviewEntity findByQuestionId(Long questionId) {
        return mapper.selectOne(new LambdaQueryWrapper<AiReviewEntity>().eq(AiReviewEntity::getQuestionId, questionId));
    }

    public AiReviewEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public List<AiReviewEntity> findByQuestionIds(Collection<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(
                new LambdaQueryWrapper<AiReviewEntity>().in(AiReviewEntity::getQuestionId, questionIds));
    }

    public List<AiReviewMapper.PopularAiReviewExample> findPopularExamples(Long boxUserId, int limit) {
        return mapper.selectPopularExamples(boxUserId, Math.max(1, limit));
    }

    public void insert(AiReviewEntity review) {
        mapper.insert(review);
    }

    public void update(AiReviewEntity review) {
        mapper.updateById(review);
    }

    public void deleteByQuestionId(Long questionId) {
        mapper.delete(new LambdaQueryWrapper<AiReviewEntity>().eq(AiReviewEntity::getQuestionId, questionId));
    }
}
