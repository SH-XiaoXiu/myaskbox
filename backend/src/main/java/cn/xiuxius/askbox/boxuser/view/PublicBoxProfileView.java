package cn.xiuxius.askbox.boxuser.view;

import cn.xiuxius.askbox.attachment.view.AttachmentView;

/** 公开提问箱资料视图，只暴露匿名访客页面需要的信息。 */
public record PublicBoxProfileView(
        String slug, String displayName, String description, AttachmentView avatar, AttachmentView background) {}
