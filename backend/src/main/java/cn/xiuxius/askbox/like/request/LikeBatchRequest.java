package cn.xiuxius.askbox.like.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record LikeBatchRequest(@NotEmpty @Size(max = 100) List<@Valid LikeTargetRequest> targets) {}
