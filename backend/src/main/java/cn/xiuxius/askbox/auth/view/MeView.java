package cn.xiuxius.askbox.auth.view;

import java.util.Set;

import cn.xiuxius.askbox.attachment.view.AttachmentView;

public record MeView(
        Long id,
        String username,
        String email,
        String displayName,
        AttachmentView avatar,
        Set<String> roles,
        Set<String> permissions) {}
