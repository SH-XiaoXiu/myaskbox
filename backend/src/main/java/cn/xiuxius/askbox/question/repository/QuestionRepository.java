package cn.xiuxius.askbox.question.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.mapper.QuestionMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class QuestionRepository {
    private final QuestionMapper mapper;

    public void insert(QuestionEntity q) {
        mapper.insert(q);
    }

    public QuestionEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public void update(QuestionEntity q) {
        mapper.updateById(q);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public IPage<QuestionEntity> findPublishedByBoxId(Long boxUserId, IPage<QuestionEntity> page) {
        return mapper.selectPage(
                page,
                new LambdaQueryWrapper<QuestionEntity>()
                        .eq(QuestionEntity::getBoxUserId, boxUserId)
                        .eq(QuestionEntity::getStatus, QuestionStatus.PUBLISHED)
                        .orderByDesc(QuestionEntity::getCreatedAt));
    }

    public IPage<QuestionEntity> findPublishedByBoxIdAndTopicId(
            Long boxUserId, Long topicId, IPage<QuestionEntity> page) {
        return mapper.selectPage(
                page,
                new LambdaQueryWrapper<QuestionEntity>()
                        .eq(QuestionEntity::getBoxUserId, boxUserId)
                        .eq(QuestionEntity::getTopicId, topicId)
                        .eq(QuestionEntity::getStatus, QuestionStatus.PUBLISHED)
                        .orderByDesc(QuestionEntity::getCreatedAt));
    }

    public IPage<QuestionEntity> findPendingByBoxId(Long boxUserId, IPage<QuestionEntity> page) {
        return mapper.selectPage(
                page,
                new LambdaQueryWrapper<QuestionEntity>()
                        .eq(QuestionEntity::getBoxUserId, boxUserId)
                        .eq(QuestionEntity::getStatus, QuestionStatus.PENDING)
                        .orderByDesc(QuestionEntity::getCreatedAt));
    }

    public IPage<QuestionEntity> pageAll(
            IPage<QuestionEntity> page, Long boxUserId, QuestionStatus status, String keyword) {
        LambdaQueryWrapper<QuestionEntity> w = new LambdaQueryWrapper<>();
        if (boxUserId != null) w.eq(QuestionEntity::getBoxUserId, boxUserId);
        if (status != null) w.eq(QuestionEntity::getStatus, status);
        if (keyword != null && !keyword.isBlank()) w.like(QuestionEntity::getQuestion, keyword);
        w.orderByDesc(QuestionEntity::getCreatedAt);
        return mapper.selectPage(page, w);
    }

    public long countByBoxUserId(Long boxUserId) {
        return mapper.selectCount(new LambdaQueryWrapper<QuestionEntity>().eq(QuestionEntity::getBoxUserId, boxUserId));
    }

    public long countAll() {
        return mapper.selectCount(null);
    }

    public long countByBoxUserIdAndStatus(Long boxUserId, QuestionStatus status) {
        return mapper.selectCount(new LambdaQueryWrapper<QuestionEntity>()
                .eq(QuestionEntity::getBoxUserId, boxUserId)
                .eq(QuestionEntity::getStatus, status));
    }

    public long countByBoxUserIdAndTopicId(Long boxUserId, Long topicId) {
        return mapper.selectCount(new LambdaQueryWrapper<QuestionEntity>()
                .eq(QuestionEntity::getBoxUserId, boxUserId)
                .eq(QuestionEntity::getTopicId, topicId));
    }

    public long countByBoxUserIdCreatedAtAfter(Long boxUserId, java.time.OffsetDateTime start) {
        return mapper.selectCount(new LambdaQueryWrapper<QuestionEntity>()
                .eq(QuestionEntity::getBoxUserId, boxUserId)
                .ge(QuestionEntity::getCreatedAt, start));
    }

    public IPage<QuestionEntity> findByBoxUserIdAndStatus(
            Long boxUserId, QuestionStatus status, IPage<QuestionEntity> page) {
        return mapper.selectPage(
                page,
                new LambdaQueryWrapper<QuestionEntity>()
                        .eq(QuestionEntity::getBoxUserId, boxUserId)
                        .eq(QuestionEntity::getStatus, status)
                        .orderByDesc(QuestionEntity::getCreatedAt));
    }
}
