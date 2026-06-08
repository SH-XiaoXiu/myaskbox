package cn.xiuxius.askbox.question.view;

import cn.xiuxius.askbox.attachment.view.AttachmentView;

public record PendingQuestionView(Long id, AttachmentView avatar, String question, long ts) {}
