package cn.xiuxius.askbox.like.request;

import jakarta.validation.constraints.NotNull;

import cn.xiuxius.askbox.like.enums.LikeTargetType;

public record LikeTargetRequest(@NotNull LikeTargetType targetType, @NotNull Long targetId) {}
