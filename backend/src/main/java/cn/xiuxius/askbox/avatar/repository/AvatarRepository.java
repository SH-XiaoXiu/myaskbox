package cn.xiuxius.askbox.avatar.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.xiuxius.askbox.avatar.entity.AvatarEntity;
import cn.xiuxius.askbox.avatar.mapper.AvatarMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AvatarRepository {
    private final AvatarMapper mapper;

    public List<AvatarEntity> findAllActive() {
        return mapper.selectList(new LambdaQueryWrapper<AvatarEntity>()
                .eq(AvatarEntity::getIsActive, true)
                .orderByAsc(AvatarEntity::getSortOrder));
    }

    public List<AvatarEntity> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<AvatarEntity>().orderByAsc(AvatarEntity::getSortOrder));
    }

    public IPage<AvatarEntity> page(IPage<AvatarEntity> page) {
        return mapper.selectPage(page, new LambdaQueryWrapper<AvatarEntity>().orderByAsc(AvatarEntity::getSortOrder));
    }

    public AvatarEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public void insert(AvatarEntity a) {
        mapper.insert(a);
    }

    public void update(AvatarEntity a) {
        mapper.updateById(a);
    }

    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    public long countAll() {
        return mapper.selectCount(null);
    }
}
