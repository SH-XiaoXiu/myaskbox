package cn.xiuxius.askbox.topic.service;

import java.util.List;
import java.util.Map;

import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.topic.entity.BoxTopicEntity;
import cn.xiuxius.askbox.topic.request.TopicCreateRequest;
import cn.xiuxius.askbox.topic.view.AdminTopicView;
import cn.xiuxius.askbox.topic.view.PublicTopicView;
import cn.xiuxius.askbox.topic.view.TopicSummaryView;
import cn.xiuxius.askbox.topic.view.TopicView;

public interface TopicService {
    TopicView create(Long boxUserId, TopicCreateRequest request);

    List<TopicView> listOwnerTopics(Long boxUserId);

    TopicView close(Long boxUserId, Long topicId);

    AdminTopicView adminClose(Long topicId);

    PageResult<AdminTopicView> listAdminTopics(long page, long pageSize, Long boxUserId, String status, String keyword);

    List<PublicTopicView> listPublicAvailable(String slug);

    PublicTopicView resolvePublic(String slug, String code);

    BoxTopicEntity requireAvailableForSubmit(Long boxUserId, String topicCode);

    BoxTopicEntity requireInBox(Long boxUserId, String topicCode);

    Map<Long, TopicSummaryView> summariesById(Iterable<Long> ids);
}
