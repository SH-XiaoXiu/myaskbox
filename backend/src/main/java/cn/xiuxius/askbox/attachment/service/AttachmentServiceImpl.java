package cn.xiuxius.askbox.attachment.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.attachment.assembler.AttachmentAssembler;
import cn.xiuxius.askbox.attachment.entity.AttachmentEntity;
import cn.xiuxius.askbox.attachment.enums.AttachmentStorageType;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.repository.AttachmentRepository;
import cn.xiuxius.askbox.attachment.service.Base64ImageInspector.ImagePayload;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.common.PageResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentServiceImpl implements AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Override
    @Cacheable(value = "anonymous-avatars", key = "'active'")
    public List<AttachmentView> listAnonymousAvatars() {
        return attachmentRepository.findActiveByUsageType(AttachmentUsageType.ANONYMOUS_AVATAR).stream()
                .map(AttachmentAssembler::toView)
                .toList();
    }

    @Override
    public PageResult<AttachmentView> pageAll(long page, long pageSize, AttachmentUsageType usageType) {
        return PageResult.from(attachmentRepository.page(Page.of(page, pageSize), usageType))
                .map(AttachmentAssembler::toView);
    }

    @Override
    @Transactional
    @CacheEvict(value = "anonymous-avatars", key = "'active'")
    public AttachmentView create(
            String name, AttachmentUsageType usageType, String contentBase64, String bg, Integer sortOrder) {
        AttachmentEntity entity = buildEntity(name, usageType, contentBase64, bg, sortOrder, true, null, null);
        attachmentRepository.insert(entity);
        log.info("Attachment created: id={} usage={} name={}", entity.getId(), usageType, name);
        return AttachmentAssembler.toView(entity);
    }

    @Override
    @Transactional
    @CacheEvict(value = "anonymous-avatars", key = "'active'")
    public AttachmentView update(
            Long id,
            String name,
            AttachmentUsageType usageType,
            String contentBase64,
            String bg,
            Integer sortOrder,
            Boolean isActive) {
        AttachmentEntity entity = attachmentRepository.findById(id);
        if (entity == null) throw new BizException(ErrorCodes.ATTACHMENT_NOT_FOUND);
        ImagePayload payload = Base64ImageInspector.inspect(contentBase64, usageType);
        entity.setName(name)
                .setUsageType(usageType)
                .setStorageType(AttachmentStorageType.BASE64)
                .setContentBase64(contentBase64)
                .setMimeType(payload.mimeType())
                .setSizeBytes(payload.sizeBytes())
                .setSha256(payload.sha256())
                .setBg(bg)
                .setSortOrder(sortOrder != null ? sortOrder : 0)
                .setIsActive(isActive != null ? isActive : true);
        attachmentRepository.update(entity);
        return AttachmentAssembler.toView(entity);
    }

    @Override
    @Transactional
    public AttachmentView createOwnedImage(
            String name, AttachmentUsageType usageType, String contentBase64, String ownerType, Long ownerId) {
        AttachmentEntity entity = buildEntity(name, usageType, contentBase64, null, 0, true, ownerType, ownerId);
        attachmentRepository.insert(entity);
        return AttachmentAssembler.toView(entity);
    }

    @Override
    public AttachmentView getById(Long id) {
        return AttachmentAssembler.toView(findEntityById(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "anonymous-avatars", key = "'active'")
    public void delete(Long id) {
        attachmentRepository.deleteById(id);
        log.info("Attachment deleted: id={}", id);
    }

    private AttachmentEntity findEntityById(Long id) {
        AttachmentEntity entity = attachmentRepository.findById(id);
        if (entity == null) throw new BizException(ErrorCodes.ATTACHMENT_NOT_FOUND);
        return entity;
    }

    private AttachmentEntity buildEntity(
            String name,
            AttachmentUsageType usageType,
            String contentBase64,
            String bg,
            Integer sortOrder,
            Boolean isActive,
            String ownerType,
            Long ownerId) {
        ImagePayload payload = Base64ImageInspector.inspect(contentBase64, usageType);
        return new AttachmentEntity()
                .setName(name)
                .setUsageType(usageType)
                .setStorageType(AttachmentStorageType.BASE64)
                .setContentBase64(contentBase64)
                .setMimeType(payload.mimeType())
                .setSizeBytes(payload.sizeBytes())
                .setSha256(payload.sha256())
                .setBg(bg)
                .setSortOrder(sortOrder != null ? sortOrder : 0)
                .setIsActive(isActive != null ? isActive : true)
                .setOwnerType(ownerType)
                .setOwnerId(ownerId);
    }
}
