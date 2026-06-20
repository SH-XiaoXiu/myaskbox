package cn.xiuxius.askbox.like.service;

import java.util.List;

import cn.xiuxius.askbox.like.enums.LikeTargetType;
import cn.xiuxius.askbox.like.request.LikeTargetRequest;
import cn.xiuxius.askbox.like.view.LikeCountView;

public interface LikeService {
    LikeCountView change(LikeTargetType targetType, Long targetId, int delta);

    List<LikeCountView> batch(List<LikeTargetRequest> targets);

    void syncDirtyCounts(int batchSize);

    void deleteTarget(LikeTargetType targetType, Long targetId);
}
