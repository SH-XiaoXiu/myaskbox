package cn.xiuxius.askbox.question.view;

import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.topic.view.TopicSummaryView;

public record PendingQuestionView(Long id, AttachmentView avatar, String question, long ts, TopicSummaryView topic) {}
