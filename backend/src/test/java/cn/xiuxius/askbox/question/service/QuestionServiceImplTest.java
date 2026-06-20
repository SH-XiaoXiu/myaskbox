package cn.xiuxius.askbox.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.xiuxius.askbox.answer.service.AnswerService;
import cn.xiuxius.askbox.attachment.enums.AttachmentStorageType;
import cn.xiuxius.askbox.attachment.enums.AttachmentUsageType;
import cn.xiuxius.askbox.attachment.service.AttachmentService;
import cn.xiuxius.askbox.attachment.view.AttachmentView;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.common.PageResult;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.question.view.PendingQuestionView;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.topic.service.TopicService;
import cn.xiuxius.askbox.topic.view.TopicSummaryView;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private BoxUserService boxUserService;

    @Mock
    private BoxUserRepository boxUserRepository;

    @Mock
    private AnswerService answerService;

    @Mock
    private AttachmentService attachmentService;

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private TopicService topicService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private QuestionServiceImpl service;

    @Test
    void getPendingAllowsQuestionWithoutTopic() {
        QuestionEntity question = new QuestionEntity()
                .setId(10L)
                .setBoxUserId(20L)
                .setAttachmentId(1L)
                .setTopicId(null)
                .setQuestion("hello")
                .setStatus(QuestionStatus.PENDING)
                .setCreatedAt(OffsetDateTime.of(2026, 6, 19, 12, 0, 0, 0, ZoneOffset.UTC));
        Page<QuestionEntity> page = Page.of(1, 20);
        page.setRecords(List.of(question));
        page.setTotal(1);
        AttachmentView avatar = new AttachmentView(
                1L,
                "匿名 01",
                AttachmentUsageType.ANONYMOUS_AVATAR,
                AttachmentStorageType.S3,
                "avatar.svg",
                "image/svg+xml",
                328L,
                "#e94f64",
                1,
                true);

        when(questionRepository.findPendingByBoxId(eq(20L), argThat(p -> p.getCurrent() == 1 && p.getSize() == 20)))
                .thenReturn(page);
        when(topicService.summariesById(any())).thenReturn(Map.of());
        when(attachmentService.getById(1L)).thenReturn(avatar);

        PageResult<PendingQuestionView> result = service.getPending(20L, 1, 20);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().getFirst().topic()).isNull();
        assertThat(result.records().getFirst().avatar()).isEqualTo(avatar);
    }

    @Test
    void getPendingKeepsTopicWhenCachedSummaryMapUsesStringKey() {
        QuestionEntity question = new QuestionEntity()
                .setId(11L)
                .setBoxUserId(20L)
                .setAttachmentId(1L)
                .setTopicId(99L)
                .setQuestion("hello")
                .setStatus(QuestionStatus.PENDING)
                .setCreatedAt(OffsetDateTime.of(2026, 6, 19, 12, 0, 0, 0, ZoneOffset.UTC));
        Page<QuestionEntity> page = Page.of(1, 20);
        page.setRecords(List.of(question));
        page.setTotal(1);
        AttachmentView avatar = new AttachmentView(
                1L,
                "匿名 01",
                AttachmentUsageType.ANONYMOUS_AVATAR,
                AttachmentStorageType.S3,
                "avatar.svg",
                "image/svg+xml",
                328L,
                "#e94f64",
                1,
                true);
        TopicSummaryView topic = new TopicSummaryView(99L, "abc", "测试话题");

        when(questionRepository.findPendingByBoxId(eq(20L), argThat(p -> p.getCurrent() == 1 && p.getSize() == 20)))
                .thenReturn(page);
        @SuppressWarnings({"rawtypes", "unchecked"})
        Map<Long, TopicSummaryView> cachedTopics = (Map) Map.of("99", topic);
        when(topicService.summariesById(any())).thenReturn(cachedTopics);
        when(attachmentService.getById(1L)).thenReturn(avatar);

        PageResult<PendingQuestionView> result = service.getPending(20L, 1, 20);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().getFirst().topic()).isEqualTo(topic);
    }
}
