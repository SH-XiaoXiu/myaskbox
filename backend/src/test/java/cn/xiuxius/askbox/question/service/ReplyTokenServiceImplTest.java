package cn.xiuxius.askbox.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.entity.QuestionReplyTokenEntity;
import cn.xiuxius.askbox.question.enums.QuestionStatus;
import cn.xiuxius.askbox.question.repository.QuestionReplyTokenRepository;
import cn.xiuxius.askbox.question.repository.QuestionRepository;

@ExtendWith(MockitoExtension.class)
class ReplyTokenServiceImplTest {

    @Mock
    private QuestionReplyTokenRepository tokenRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private BoxUserRepository boxUserRepository;

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private ReplyTokenServiceImpl service;

    @Test
    void createStoresOnlyHashAndReturnsRawToken() {
        String rawToken =
                service.create(10L, 20L, OffsetDateTime.now(ZoneOffset.UTC).plusDays(7));

        ArgumentCaptor<QuestionReplyTokenEntity> captor = ArgumentCaptor.forClass(QuestionReplyTokenEntity.class);
        verify(tokenRepository).insert(captor.capture());
        assertThat(rawToken).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(rawToken);
        assertThat(captor.getValue().getQuestionId()).isEqualTo(10L);
        assertThat(captor.getValue().getBoxUserId()).isEqualTo(20L);
    }

    @Test
    void getRejectsUsedToken() {
        when(tokenRepository.findByTokenHash(any()))
                .thenReturn(new QuestionReplyTokenEntity()
                        .setQuestionId(10L)
                        .setBoxUserId(20L)
                        .setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1))
                        .setUsedAt(OffsetDateTime.now(ZoneOffset.UTC)));

        assertThatThrownBy(() -> service.get("token")).isInstanceOf(BizException.class);
    }

    @Test
    void answerMarksTokenUsedAndPublishesAnswer() {
        QuestionReplyTokenEntity token = new QuestionReplyTokenEntity()
                .setId(1L)
                .setQuestionId(10L)
                .setBoxUserId(20L)
                .setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));
        when(tokenRepository.findByTokenHash(any())).thenReturn(token);
        when(questionRepository.findById(10L))
                .thenReturn(new QuestionEntity()
                        .setId(10L)
                        .setBoxUserId(20L)
                        .setQuestion("Question")
                        .setStatus(QuestionStatus.PENDING));
        when(tokenRepository.markUsedIfUnused(eq(1L), any())).thenReturn(true);
        when(boxUserRepository.findById(20L))
                .thenReturn(new BoxUserEntity().setId(20L).setUserId(30L));

        service.answer("token", "Answer", "127.0.0.1", "ua");

        verify(questionService).answer(20L, 10L, "Answer", 30L, "127.0.0.1", "ua");
    }
}
