package cn.xiuxius.askbox.question.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cn.xiuxius.askbox.boxuser.entity.BoxUserEntity;
import cn.xiuxius.askbox.boxuser.repository.BoxUserRepository;
import cn.xiuxius.askbox.mail.service.MailSenderService;
import cn.xiuxius.askbox.mail.service.MailTemplateService;
import cn.xiuxius.askbox.question.entity.QuestionEntity;
import cn.xiuxius.askbox.question.event.QuestionSubmittedEvent;
import cn.xiuxius.askbox.question.repository.QuestionRepository;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.system.setting.service.SysSettingService;

@ExtendWith(MockitoExtension.class)
class QuestionNotificationServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private BoxUserRepository boxUserRepository;

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private SysSettingService settingService;

    @Mock
    private ReplyTokenServiceImpl replyTokenService;

    @Mock
    private MailSenderService mailSenderService;

    @Mock
    private MailTemplateService templateService;

    @InjectMocks
    private QuestionNotificationService service;

    @Test
    void disabledBoxDoesNotCreateTokenOrSendMail() {
        when(questionRepository.findById(10L))
                .thenReturn(new QuestionEntity().setId(10L).setBoxUserId(20L));
        when(boxUserRepository.findById(20L))
                .thenReturn(new BoxUserEntity().setId(20L).setEmailNotifyEnabled(false));

        service.sendQuestionNotification(new QuestionSubmittedEvent(10L, "https://askbox.example.com"));

        verify(replyTokenService, never()).create(any(), any(), any());
        verify(mailSenderService, never()).sendHtml(any(), any(), any());
    }

    @Test
    void enabledBoxCreatesTokenAndSendsMail() {
        when(questionRepository.findById(10L))
                .thenReturn(new QuestionEntity().setId(10L).setBoxUserId(20L).setQuestion("Hello"));
        when(boxUserRepository.findById(20L))
                .thenReturn(new BoxUserEntity().setId(20L).setUserId(30L).setEmailNotifyEnabled(true));
        when(sysUserRepository.findById(30L)).thenReturn(new SysUserEntity().setEmail("owner@example.com"));
        when(replyTokenService.create(eq(10L), eq(20L), any(OffsetDateTime.class)))
                .thenReturn("raw-token");
        when(settingService.getString("site.public_url", "")).thenReturn("https://askbox.example.com");
        when(templateService.html(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(templateService.render(eq("mail/question-notification.html"), any()))
                .thenReturn("<html>mail</html>");

        service.sendQuestionNotification(new QuestionSubmittedEvent(10L, "https://fallback.example.com"));

        verify(mailSenderService).sendHtml(eq("owner@example.com"), eq("你的提问箱收到了一个新问题"), eq("<html>mail</html>"));
        verify(templateService).html(contains("https://askbox.example.com/reply/raw-token"));
    }
}
