package cn.xiuxius.askbox.avatar.assembler;

import cn.xiuxius.askbox.avatar.entity.AvatarEntity;
import cn.xiuxius.askbox.avatar.view.AvatarView;

public final class AvatarAssembler {

    private AvatarAssembler() {}

    public static AvatarView toView(AvatarEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AvatarView(
                entity.getId(),
                entity.getName(),
                entity.getIconBase64(),
                entity.getBg(),
                entity.getSortOrder(),
                entity.getIsActive());
    }
}
