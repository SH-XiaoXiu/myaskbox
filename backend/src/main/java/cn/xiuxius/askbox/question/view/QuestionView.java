package cn.xiuxius.askbox.question.view;

import cn.xiuxius.askbox.attachment.view.AttachmentView;

public record QuestionView(
        Long id, AttachmentView avatar, String question, String answer, AttachmentView ownerAvatar, long ts) {}
