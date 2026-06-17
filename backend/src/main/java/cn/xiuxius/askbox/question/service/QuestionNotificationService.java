package cn.xiuxius.askbox.question.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionNotificationService {

    private static final int REPLY_TOKEN_DAYS = 7;

    private final QuestionRepository questionRepository;
    private final BoxUserRepository boxUserRepository;
    private final SysUserRepository sysUserRepository;
    private final SysSettingService settingService;
    private final ReplyTokenServiceImpl replyTokenService;
    private final MailSenderService mailSenderService;
    private final MailTemplateService templateService;

    @Async("askboxMailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendQuestionNotification(QuestionSubmittedEvent event) {
        try {
            QuestionEntity question = questionRepository.findById(event.questionId());
            if (question == null) {
                return;
            }
            BoxUserEntity box = boxUserRepository.findById(question.getBoxUserId());
            if (box == null || !Boolean.TRUE.equals(box.getEmailNotifyEnabled())) {
                return;
            }
            SysUserEntity owner = sysUserRepository.findById(box.getUserId());
            if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
                log.warn("Skip question notification: missing owner email, boxUserId={}", box.getId());
                return;
            }
            String rawToken = replyTokenService.create(
                    question.getId(),
                    box.getId(),
                    OffsetDateTime.now(ZoneOffset.UTC).plusDays(REPLY_TOKEN_DAYS));
            String replyUrl = replyUrl(rawToken, event.origin());
            String subject = "AskBox 收到新问题";
            String html = templateService.render(
                    "mail/question-notification.html",
                    Map.of(
                            "title", templateService.html(subject),
                            "description", templateService.html("你的提问箱收到了一个新问题，可以通过下面的一次性链接免登录回复。"),
                            "question", templateService.html(question.getQuestion()),
                            "replyUrl", templateService.html(replyUrl),
                            "expiresDays", String.valueOf(REPLY_TOKEN_DAYS)));
            mailSenderService.sendHtml(owner.getEmail(), subject, html);
        } catch (RuntimeException ex) {
            log.warn(
                    "Question notification mail failed: questionId={} message={}", event.questionId(), ex.getMessage());
        }
    }

    private String replyUrl(String token, String requestOrigin) {
        String baseUrl = settingService.getString("site.public_url", "").trim();
        if (baseUrl.isBlank()) {
            baseUrl = requestOrigin == null ? "" : requestOrigin.trim();
        }
        if (baseUrl.isBlank()) {
            baseUrl = "http://localhost:5173";
        }
        return UriComponentsBuilder.fromUriString(baseUrl)
                .replacePath("/reply/" + token)
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUriString();
    }
}
