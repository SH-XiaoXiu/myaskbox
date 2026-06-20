package cn.xiuxius.askbox.like.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.xiuxius.askbox.like.entity.LikeCountEntity;
import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.mapper.LikeCountMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LikeCountRepository {
    private final LikeCountMapper mapper;

    public Long findCount(LikeTargetType targetType, Long targetId) {
        LikeCountEntity entity = mapper.selectOne(new LambdaQueryWrapper<LikeCountEntity>()
                .eq(LikeCountEntity::getTargetType, targetType)
                .eq(LikeCountEntity::getTargetId, targetId));
        return entity != null ? entity.getLikeCount() : null;
    }

    public void upsert(LikeTargetType targetType, Long targetId, long likeCount) {
        mapper.upsert(targetType, targetId, Math.max(0, likeCount));
    }

    public long applyDelta(LikeTargetType targetType, Long targetId, long plus, long minus) {
        long current = findCount(targetType, targetId) != null ? findCount(targetType, targetId) : 0L;
        long next = Math.max(0L, current + Math.max(0L, plus) - Math.max(0L, minus));
        mapper.upsert(targetType, targetId, next);
        return next;
    }

    public void deleteByTarget(LikeTargetType targetType, Long targetId) {
        mapper.deleteByTarget(targetType, targetId);
    }
}
