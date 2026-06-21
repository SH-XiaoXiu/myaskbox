package cn.xiuxius.askbox.usersetting.service;

import java.util.Map;

public interface UserSettingService {
    String KEY_TOPIC_ACTIVE_LIMIT = "topic.active_limit";
    String KEY_AI_REVIEW_ENABLED = "ai.review.enabled";

    int getTopicActiveLimit(Long userId);

    boolean isAiReviewEnabled(Long userId);

    Map<Long, Integer> topicActiveLimits(Iterable<Long> userIds);

    void updateBoxOwnerSettings(Long userId, Integer topicActiveLimit, Boolean aiReviewEnabled);
}
