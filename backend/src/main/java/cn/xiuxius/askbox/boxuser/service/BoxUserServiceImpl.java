package cn.xiuxius.askbox.boxuser.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.boxuser.assembler.BoxUserAssembler;
import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxStatsView;
import cn.xiuxius.askbox.boxuser.view.BoxView;
import cn.xiuxius.askbox.boxuser.view.PublicBoxProfileView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
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
    private final AttachmentService attachmentService;

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
        return toProfileView(b);
    }

    @Override
    public BoxProfileView getProfileById(Long id) {
        return toProfileView(getById(id));
    }

    @Override
    public PublicBoxProfileView getPublicProfileBySlug(String slug) {
        return toPublicProfileView(getBySlug(slug));
    }

    @Override
    public BoxStatsView getStats(Long userId, String zoneId) {
        BoxUserEntity b = repo.findByUserId(userId);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND, "当前用户没有提问箱");
        ZoneId zone = parseZone(zoneId);
        OffsetDateTime start =
                OffsetDateTime.now(zone).toLocalDate().atStartOfDay(zone).toOffsetDateTime();
        return new BoxStatsView(
                questionRepository.countByBoxUserIdAndStatus(b.getId(), QuestionStatus.PENDING),
                questionRepository.countByBoxUserIdAndStatus(b.getId(), QuestionStatus.PUBLISHED),
                questionRepository.countByBoxUserIdAndStatus(b.getId(), QuestionStatus.DISMISSED),
                questionRepository.countByBoxUserIdCreatedAtAfter(b.getId(), start));
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
    public BoxProfileView updateBox(
            Long userId,
            String slug,
            String displayName,
            String description,
            String avatarObjectKey,
            String backgroundObjectKey,
            Boolean emailNotifyEnabled) {
        BoxUserEntity b = repo.findByUserId(userId);
        if (b == null) throw new BizException(ErrorCodes.BOX_NOT_FOUND);
        updateAttachments(b, avatarObjectKey, backgroundObjectKey);
        return updateBoxEntity(b, slug, displayName, description, emailNotifyEnabled);
    }

    @Override
    @Transactional
    public BoxProfileView updateBoxById(
            Long id,
            String slug,
            String displayName,
            String description,
            String avatarObjectKey,
            String backgroundObjectKey,
            Boolean emailNotifyEnabled) {
        BoxUserEntity b = getById(id);
        updateAttachments(b, avatarObjectKey, backgroundObjectKey);
        return updateBoxEntity(b, slug, displayName, description, emailNotifyEnabled);
    }

    private BoxProfileView updateBoxEntity(
            BoxUserEntity b, String slug, String displayName, String description, Boolean emailNotifyEnabled) {
        String normalizedSlug = slug == null ? "" : slug.trim();
        BoxUserEntity sameSlugBox = repo.findBySlug(normalizedSlug);
        if (sameSlugBox != null && !sameSlugBox.getId().equals(b.getId()))
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "地址已被占用");
        b.setSlug(normalizedSlug)
                .setDisplayName(displayName == null ? "" : displayName.trim())
                .setDescription(description == null ? "" : description.trim());
        if (emailNotifyEnabled != null) {
            b.setEmailNotifyEnabled(emailNotifyEnabled);
        }
        repo.update(b);
        return toProfileView(b);
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
                .setDescription(description)
                .setEmailNotifyEnabled(true);
        repo.insert(b);
        return b;
    }

    private void updateAttachments(BoxUserEntity b, String avatarObjectKey, String backgroundObjectKey) {
        if (avatarObjectKey != null) {
            if (avatarObjectKey.isBlank()) {
                b.setAvatarAttachmentId(null);
            } else {
                AttachmentView avatar = attachmentService.createOwnedImage(
                        "box-avatar-" + b.getId(),
                        AttachmentUsageType.BOX_OWNER_AVATAR,
                        avatarObjectKey,
                        "BOX_USER",
                        b.getId());
                b.setAvatarAttachmentId(avatar.id());
            }
        }
        if (backgroundObjectKey != null) {
            if (backgroundObjectKey.isBlank()) {
                b.setBackgroundAttachmentId(null);
            } else {
                AttachmentView background = attachmentService.createOwnedImage(
                        "box-background-" + b.getId(),
                        AttachmentUsageType.BOX_BACKGROUND,
                        backgroundObjectKey,
                        "BOX_USER",
                        b.getId());
                b.setBackgroundAttachmentId(background.id());
            }
        }
    }

    private BoxProfileView toProfileView(BoxUserEntity b) {
        SysUserEntity user = accountOrNull(b.getUserId());
        return BoxUserAssembler.toProfileView(
                b, displayNameOrNull(user), accountAvatarOrNull(user), attachmentOrNull(b.getBackgroundAttachmentId()));
    }

    private PublicBoxProfileView toPublicProfileView(BoxUserEntity b) {
        SysUserEntity user = accountOrNull(b.getUserId());
        return BoxUserAssembler.toPublicProfileView(
                b, displayNameOrNull(user), accountAvatarOrNull(user), attachmentOrNull(b.getBackgroundAttachmentId()));
    }

    private SysUserEntity accountOrNull(Long userId) {
        return userId == null ? null : sysUserRepository.findById(userId);
    }

    private String displayNameOrNull(SysUserEntity user) {
        return user == null ? null : user.getDisplayName();
    }

    private AttachmentView accountAvatarOrNull(SysUserEntity user) {
        return user == null ? null : attachmentOrNull(user.getAvatarAttachmentId());
    }

    private AttachmentView attachmentOrNull(Long id) {
        return id == null ? null : attachmentService.getById(id);
    }

    private ZoneId parseZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(zoneId);
        } catch (RuntimeException ex) {
            return ZoneId.systemDefault();
        }
    }
}
