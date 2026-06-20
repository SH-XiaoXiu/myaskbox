package cn.xiuxius.askbox.like.view;

import cn.xiuxius.askbox.like.enums.LikeTargetType;

public record LikeCountView(LikeTargetType targetType, Long targetId, long likeCount) {}
