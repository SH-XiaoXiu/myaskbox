package cn.xiuxius.askbox.boxuser.assembler;

import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxView;
import cn.xiuxius.askbox.boxuser.view.PublicBoxProfileView;

public final class BoxUserAssembler {

    private BoxUserAssembler() {}

    public static BoxProfileView toProfileView(
            BoxUserEntity entity,
            String ownerDisplayName,
            AttachmentView avatar,
            AttachmentView background,
            Integer topicActiveLimit,
            Boolean aiReviewEnabled) {
        if (entity == null) {
            return null;
        }
        return new BoxProfileView(
                entity.getId(),
                entity.getUserId(),
                entity.getSlug(),
                entity.getDisplayName(),
                ownerDisplayName,
                entity.getDescription(),
                avatar,
                background,
                entity.getEmailNotifyEnabled(),
                topicActiveLimit,
                aiReviewEnabled,
                entity.getCreatedAt());
    }

    public static PublicBoxProfileView toPublicProfileView(
            BoxUserEntity entity, String ownerDisplayName, AttachmentView avatar, AttachmentView background) {
        if (entity == null) {
            return null;
        }
        return new PublicBoxProfileView(
                entity.getSlug(),
                entity.getDisplayName(),
                ownerDisplayName,
                entity.getDescription(),
                avatar,
                background);
    }

    public static BoxView toAdminView(
            BoxUserEntity entity,
            String username,
            long questionCount,
            Integer topicActiveLimit,
            Boolean aiReviewEnabled) {
        return new BoxView(
                entity.getId(),
                username,
                entity.getSlug(),
                entity.getDisplayName(),
                entity.getDescription(),
                questionCount,
                topicActiveLimit,
                aiReviewEnabled,
                entity.getCreatedAt());
    }
}
