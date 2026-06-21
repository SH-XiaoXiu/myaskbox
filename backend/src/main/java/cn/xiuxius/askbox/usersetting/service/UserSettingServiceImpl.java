package cn.xiuxius.askbox.usersetting.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.xiuxius.askbox.usersetting.entity.UserSettingEntity;
import cn.xiuxius.askbox.usersetting.repository.UserSettingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSettingServiceImpl implements UserSettingService {
    private static final int DEFAULT_TOPIC_ACTIVE_LIMIT = 5;

    private final UserSettingRepository repository;

    @Override
    public int getTopicActiveLimit(Long userId) {
        return parsePositiveInt(value(userId, KEY_TOPIC_ACTIVE_LIMIT), DEFAULT_TOPIC_ACTIVE_LIMIT);
    }

    @Override
    public boolean isAiReviewEnabled(Long userId) {
        return Boolean.parseBoolean(value(userId, KEY_AI_REVIEW_ENABLED));
    }

    @Override
    public Map<Long, Integer> topicActiveLimits(Iterable<Long> userIds) {
        var ids = StreamSupport.stream(userIds.spliterator(), false)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Integer> result = new HashMap<>();
        ids.forEach(id -> result.put(id, DEFAULT_TOPIC_ACTIVE_LIMIT));
        repository
                .findByUserIdsAndKey(ids, KEY_TOPIC_ACTIVE_LIMIT)
                .forEach(setting -> result.put(
                        setting.getUserId(), parsePositiveInt(setting.getValue(), DEFAULT_TOPIC_ACTIVE_LIMIT)));
        return result;
    }

    @Override
    @Transactional
    public void updateBoxOwnerSettings(Long userId, Integer topicActiveLimit, Boolean aiReviewEnabled) {
        if (topicActiveLimit != null) {
            upsert(userId, KEY_TOPIC_ACTIVE_LIMIT, String.valueOf(Math.max(1, topicActiveLimit)));
        }
        if (aiReviewEnabled != null) {
            upsert(userId, KEY_AI_REVIEW_ENABLED, String.valueOf(aiReviewEnabled));
        }
    }

    private String value(Long userId, String key) {
        if (userId == null) {
            return null;
        }
        UserSettingEntity setting = repository.find(userId, key);
        return setting == null ? null : setting.getValue();
    }

    private void upsert(Long userId, String key, String value) {
        repository.upsert(new UserSettingEntity()
                .setUserId(userId)
                .setKey(key)
                .setValue(value)
                .setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC)));
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
