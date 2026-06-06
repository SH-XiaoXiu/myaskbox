package cn.xiuxius.askbox.boxuser.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.mapper.BoxUserMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BoxUserRepository {
    private final BoxUserMapper mapper;

    public BoxUserEntity findById(Long id) {
        return mapper.selectById(id);
    }

    public BoxUserEntity findByUserId(Long userId) {
        return mapper.selectOne(new LambdaQueryWrapper<BoxUserEntity>().eq(BoxUserEntity::getUserId, userId));
    }

    public BoxUserEntity findBySlug(String slug) {
        return mapper.selectOne(new LambdaQueryWrapper<BoxUserEntity>().eq(BoxUserEntity::getSlug, slug));
    }

    public IPage<BoxUserEntity> page(IPage<BoxUserEntity> page, String keyword) {
        LambdaQueryWrapper<BoxUserEntity> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank())
            w.like(BoxUserEntity::getSlug, keyword).or().like(BoxUserEntity::getDisplayName, keyword);
        w.orderByDesc(BoxUserEntity::getCreatedAt);
        return mapper.selectPage(page, w);
    }

    public void insert(BoxUserEntity b) {
        mapper.insert(b);
    }

    public void update(BoxUserEntity b) {
        mapper.updateById(b);
    }

    public long countAll() {
        return mapper.selectCount(null);
    }
}
