package cn.xiuxius.askbox.boxuser.service;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.view.BoxProfileView;
import cn.xiuxius.askbox.boxuser.view.BoxView;
import cn.xiuxius.askbox.common.PageResult;

public interface BoxUserService {
    BoxUserEntity getBySlug(String slug);

    BoxUserEntity getByUserId(Long userId);

    BoxUserEntity getById(Long id);

    Long getBoxIdByUserId(Long userId);

    BoxProfileView getProfileByUserId(Long userId);

    BoxProfileView getProfileById(Long id);

    /** 管理端 Box 列表视图（含 username + questionCount）。 */
    PageResult<BoxView> listBoxViews(long page, long pageSize, String keyword);

    BoxProfileView updateBox(Long userId, String slug, String displayName, String description);

    BoxProfileView updateBoxById(Long id, String slug, String displayName, String description);

    BoxUserEntity createBox(Long userId, String slug, String displayName, String description);
}
