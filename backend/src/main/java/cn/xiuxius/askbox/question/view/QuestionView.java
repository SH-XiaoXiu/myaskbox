package cn.xiuxius.askbox.question.view;

import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.topic.view.TopicSummaryView;

public record QuestionView(
        Long id,
        AttachmentView avatar,
        String question,
        String answer,
        AttachmentView ownerAvatar,
        long ts,
        TopicSummaryView topic) {}
