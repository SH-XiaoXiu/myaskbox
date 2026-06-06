package cn.xiuxius.askbox.question.view;

import cn.xiuxius.askbox.avatar.view.AvatarView;

public record PendingQuestionView(Long id, AvatarView avatar, String question, long ts) {}
