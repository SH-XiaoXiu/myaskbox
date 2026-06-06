package cn.xiuxius.askbox.question.view;

import cn.xiuxius.askbox.avatar.view.AvatarView;

public record QuestionView(Long id, AvatarView avatar, String question, String answer, long ts) {}
