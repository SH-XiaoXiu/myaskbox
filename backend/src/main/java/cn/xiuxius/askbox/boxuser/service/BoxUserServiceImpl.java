package cn.xiuxius.askbox.boxuser.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.boxuser.assembler.BoxUserAssembler;
import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxView;
import cn.xiuxius.askbox.boxuser.view.PublicBoxProfileView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.repository.SysUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoxUserServiceImpl implements BoxUserService {
    private final BoxUserRepository repo;
    private final SysUserRepository sysUserRepository;
    private final QuestionRepository questionRepository;

    @Override
    public BoxUserEntity getBySlug(String slug) {
        BoxUserEntity b = repo.findBySlug(slug);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND);
        return b;
    }

    @Override
    public BoxUserEntity getByUserId(Long userId) {
        return repo.findByUserId(userId);
    }

    @Override
    public BoxUserEntity getById(Long id) {
        BoxUserEntity b = repo.findById(id);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND);
        return b;
    }

    @Override
    public Long getBoxIdByUserId(Long userId) {
        BoxUserEntity b = repo.findByUserId(userId);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND, "当前用户没有提问箱");
        return b.getId();
    }

    @Override
    public BoxProfileView getProfileByUserId(Long userId) {
        BoxUserEntity b = repo.findByUserId(userId);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND, "当前用户没有提问箱");
        return BoxUserAssembler.toProfileView(b);
    }

    @Override
    public BoxProfileView getProfileById(Long id) {
        return BoxUserAssembler.toProfileView(getById(id));
    }

    @Override
    public PublicBoxProfileView getPublicProfileBySlug(String slug) {
        return BoxUserAssembler.toPublicProfileView(getBySlug(slug));
    }

    @Override
    public PageResult<BoxView> listBoxViews(long page, long pageSize, String keyword) {
        IPage<BoxUserEntity> result = repo.page(Page.of(page, pageSize), keyword);
        return PageResult.from(result.convert(box -> {
            SysUserEntity user = sysUserRepository.findById(box.getUserId());
            String username = user != null ? user.getUsername() : "unknown";
            long questionCount = questionRepository.countByBoxUserId(box.getId());
            return BoxUserAssembler.toAdminView(box, username, questionCount);
        }));
    }

    @Override
    @Transactional
    public BoxProfileView updateBox(Long userId, String slug, String displayName, String description) {
        BoxUserEntity b = repo.findByUserId(userId);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND);
        return updateBoxEntity(b, slug, displayName, description);
    }

    @Override
    @Transactional
    public BoxProfileView updateBoxById(Long id, String slug, String displayName, String description) {
        return updateBoxEntity(getById(id), slug, displayName, description);
    }

    private BoxProfileView updateBoxEntity(BoxUserEntity b, String slug, String displayName, String description) {
        String normalizedSlug = slug == null ? "" : slug.trim();
        BoxUserEntity sameSlugBox = repo.findBySlug(normalizedSlug);
        if (sameSlugBox != null && !sameSlugBox.getId().equals(b.getId()))
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "slug 已被占用");
        b.setSlug(normalizedSlug)
                .setDisplayName(displayName == null ? "" : displayName.trim())
                .setDescription(description == null ? "" : description.trim());
        repo.update(b);
        return BoxUserAssembler.toProfileView(b);
    }

    @Override
    @Transactional
    public BoxUserEntity createBox(Long userId, String slug, String displayName, String description) {
        if (repo.findBySlug(slug) != null) {
            slug = slug + "-" + userId;
        }
        BoxUserEntity b = new BoxUserEntity()
                .setUserId(userId)
                .setSlug(slug)
                .setDisplayName(displayName)
                .setDescription(description);
        repo.insert(b);
        return b;
    }
}
