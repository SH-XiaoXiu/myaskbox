package cn.xiuxius.askbox.avatar.service;

import java.util.List;

import cn.xiuxius.askbox.avatar.view.AvatarView;
import cn.xiuxius.askbox.common.PageResult;

public interface AvatarService {
    List<AvatarView> listActive();

    List<AvatarView> listAll();

    PageResult<AvatarView> pageAll(long page, long pageSize);

    AvatarView create(String name, String iconBase64, String bg, Integer sortOrder);

    AvatarView update(Long id, String name, String iconBase64, String bg, Integer sortOrder);

    void delete(Long id);
}
