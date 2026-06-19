package cn.xiuxius.askbox.attachment.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.xiuxius.askbox.attachment.entity.AttachmentEntity;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.mapper.AttachmentMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AttachmentRepository {
    private final AttachmentMapper mapper;

    public List<AttachmentEntity> findActiveByUsageType(AttachmentUsageType usageType) {
        return mapper.selectList(new LambdaQueryWrapper<AttachmentEntity>()
                .eq(AttachmentEntity::getUsageType, usageType)
                .eq(AttachmentEntity::getIsActive, true)
                .orderByAsc(AttachmentEntity::getSortOrder));
    }

    public IPage<AttachmentEntity> page(IPage<AttachmentEntity> page, AttachmentUsageType usageType) {
        LambdaQueryWrapper<AttachmentEntity> w = new LambdaQueryWrapper<>();
        if (usageType != null) {
            w.eq(AttachmentEntity::getUsageType, usageType);
        }
        w.orderByAsc(AttachmentEntity::getSortOrder).orderByDesc(AttachmentEntity::getCreatedAt);
        return mapper.selectPage(page, w);
    }

    public AttachmentEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public AttachmentEntity findByObjectKey(String objectKey) {
        return mapper.selectOne(
                new LambdaQueryWrapper<AttachmentEntity>().eq(AttachmentEntity::getObjectKey, objectKey));
    }

    public void insert(AttachmentEntity entity) {
        mapper.insert(entity);
    }

    public void update(AttachmentEntity entity) {
        mapper.updateById(entity);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public long countAll() {
        return mapper.selectCount(null);
    }
}
