package cn.xiuxius.askbox.topic.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.xiuxius.askbox.topic.entity.BoxTopicEntity;
import cn.xiuxius.askbox.topic.enums.TopicStatus;
import cn.xiuxius.askbox.topic.mapper.BoxTopicMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BoxTopicRepository {
    private final BoxTopicMapper mapper;

    public void insert(BoxTopicEntity topic) {
        mapper.insert(topic);
    }

    public void update(BoxTopicEntity topic) {
        mapper.updateById(topic);
    }

    public BoxTopicEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public BoxTopicEntity findByCode(String code) {
        return mapper.selectOne(new LambdaQueryWrapper<BoxTopicEntity>().eq(BoxTopicEntity::getCode, code));
    }

    public List<BoxTopicEntity> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<BoxTopicEntity>().in(BoxTopicEntity::getId, ids));
    }

    public List<BoxTopicEntity> findByBoxId(Long boxUserId) {
        return mapper.selectList(new LambdaQueryWrapper<BoxTopicEntity>()
                .eq(BoxTopicEntity::getBoxUserId, boxUserId)
                .orderByDesc(BoxTopicEntity::getCreatedAt));
    }

    public List<BoxTopicEntity> findAvailableByBoxId(Long boxUserId, OffsetDateTime now, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<BoxTopicEntity>()
                .eq(BoxTopicEntity::getBoxUserId, boxUserId)
                .eq(BoxTopicEntity::getStatus, TopicStatus.ACTIVE)
                .gt(BoxTopicEntity::getExpiresAt, now)
                .orderByDesc(BoxTopicEntity::getCreatedAt)
                .last("LIMIT " + Math.max(1, limit)));
    }

    public long countAvailableByBoxId(Long boxUserId, OffsetDateTime now) {
        return mapper.selectCount(new LambdaQueryWrapper<BoxTopicEntity>()
                .eq(BoxTopicEntity::getBoxUserId, boxUserId)
                .eq(BoxTopicEntity::getStatus, TopicStatus.ACTIVE)
                .gt(BoxTopicEntity::getExpiresAt, now));
    }

    public IPage<BoxTopicEntity> pageAll(
            IPage<BoxTopicEntity> page, Long boxUserId, String status, String keyword, OffsetDateTime now) {
        LambdaQueryWrapper<BoxTopicEntity> w = new LambdaQueryWrapper<>();
        if (boxUserId != null) {
            w.eq(BoxTopicEntity::getBoxUserId, boxUserId);
        }
        if ("ACTIVE".equals(status)) {
            w.eq(BoxTopicEntity::getStatus, TopicStatus.ACTIVE).gt(BoxTopicEntity::getExpiresAt, now);
        } else if ("EXPIRED".equals(status)) {
            w.eq(BoxTopicEntity::getStatus, TopicStatus.ACTIVE).le(BoxTopicEntity::getExpiresAt, now);
        } else if ("CLOSED".equals(status)) {
            w.eq(BoxTopicEntity::getStatus, TopicStatus.CLOSED);
        }
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(BoxTopicEntity::getTitle, keyword).or().like(BoxTopicEntity::getDescription, keyword));
        }
        w.orderByDesc(BoxTopicEntity::getCreatedAt);
        return mapper.selectPage(page, w);
    }
}
