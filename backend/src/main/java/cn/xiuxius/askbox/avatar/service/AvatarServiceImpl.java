package cn.xiuxius.askbox.avatar.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.avatar.assembler.AvatarAssembler;
import cn.xiuxius.askbox.avatar.entity.AvatarEntity;
import cn.xiuxius.askbox.avatar.repository.AvatarRepository;
import cn.xiuxius.askbox.avatar.view.AvatarView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarServiceImpl implements AvatarService {

    private final AvatarRepository avatarRepository;

    @Override
    @Cacheable(value = "avatars", key = "'active'")
    public List<AvatarView> listActive() {
        return avatarRepository.findAllActive().stream()
                .map(AvatarAssembler::toView)
                .toList();
    }

    @Override
    public List<AvatarView> listAll() {
        return avatarRepository.findAll().stream().map(AvatarAssembler::toView).toList();
    }

    @Override
    public PageResult<AvatarView> pageAll(long page, long pageSize) {
        return PageResult.from(avatarRepository.page(Page.of(page, pageSize))).map(AvatarAssembler::toView);
    }

    @Override
    @Transactional
    @CacheEvict(value = "avatars", key = "'active'")
    public AvatarView create(String name, String iconBase64, String bg, Integer sortOrder) {
        AvatarEntity a = new AvatarEntity()
                .setName(name)
                .setIconBase64(iconBase64)
                .setBg(bg)
                .setSortOrder(sortOrder != null ? sortOrder : 0)
                .setIsActive(true);
        avatarRepository.insert(a);
        log.info("AvatarEntity created: id={} name={}", a.getId(), name);
        return AvatarAssembler.toView(a);
    }

    @Override
    @Transactional
    @CacheEvict(value = "avatars", key = "'active'")
    public AvatarView update(Long id, String name, String iconBase64, String bg, Integer sortOrder) {
        AvatarEntity a = avatarRepository.findById(id);
        if (a == null) throw new BizException(ErrorCodes.AVATAR_NOT_FOUND);
        a.setName(name).setIconBase64(iconBase64).setBg(bg);
        if (sortOrder != null) a.setSortOrder(sortOrder);
        avatarRepository.update(a);
        return AvatarAssembler.toView(a);
    }

    @Override
    @Transactional
    @CacheEvict(value = "avatars", key = "'active'")
    public void delete(Long id) {
        avatarRepository.deleteById(id);
        log.info("AvatarEntity deleted: id={}", id);
    }
}
