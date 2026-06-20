package cn.xiuxius.askbox.attachment.assembler;

import cn.xiuxius.askbox.attachment.entity.AttachmentEntity;
import cn.xiuxius.askbox.attachment.view.AttachmentView;

public final class AttachmentAssembler {

    private AttachmentAssembler() {}

    public static AttachmentView toView(AttachmentEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AttachmentView(
                entity.getId(),
                entity.getName(),
                entity.getUsageType(),
                entity.getStorageType(),
                entity.getObjectKey(),
                entity.getMimeType(),
                entity.getSizeBytes(),
                entity.getSha256(),
                entity.getBg(),
                entity.getSortOrder(),
                entity.getIsActive());
    }
}
