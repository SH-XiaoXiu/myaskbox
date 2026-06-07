package cn.xiuxius.askbox.boxuser.assembler;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxView;
import cn.xiuxius.askbox.boxuser.view.PublicBoxProfileView;

public final class BoxUserAssembler {

    private BoxUserAssembler() {}

    public static BoxProfileView toProfileView(BoxUserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BoxProfileView(
                entity.getId(),
                entity.getUserId(),
                entity.getSlug(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getCreatedAt());
    }

    public static PublicBoxProfileView toPublicProfileView(BoxUserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PublicBoxProfileView(entity.getSlug(), entity.getDisplayName(), entity.getDescription());
    }

    public static BoxView toAdminView(BoxUserEntity entity, String username, long questionCount) {
        return new BoxView(
                entity.getId(),
                username,
                entity.getSlug(),
                entity.getDisplayName(),
                entity.getDescription(),
                questionCount,
                entity.getCreatedAt());
    }
}
