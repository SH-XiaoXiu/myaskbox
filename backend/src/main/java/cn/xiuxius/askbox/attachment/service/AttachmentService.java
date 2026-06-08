package cn.xiuxius.askbox.attachment.service;

import java.util.List;

import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.common.PageResult;

public interface AttachmentService {

    List<AttachmentView> listAnonymousAvatars();

    PageResult<AttachmentView> pageAll(long page, long pageSize, AttachmentUsageType usageType);

    AttachmentView create(
            String name, AttachmentUsageType usageType, String contentBase64, String bg, Integer sortOrder);

    AttachmentView update(
            Long id,
            String name,
            AttachmentUsageType usageType,
            String contentBase64,
            String bg,
            Integer sortOrder,
            Boolean isActive);

    AttachmentView createOwnedImage(
            String name, AttachmentUsageType usageType, String contentBase64, String ownerType, Long ownerId);

    AttachmentView getById(Long id);

    void delete(Long id);
}
