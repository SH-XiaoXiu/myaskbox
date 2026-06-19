package cn.xiuxius.askbox.auth.service;

import org.springframework.stereotype.Service;

import cn.xiuxius.askbox.mail.service.MailSenderService;
import cn.xiuxius.askbox.mail.service.MailTemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterMailService {

    private final MailSenderService mailSenderService;
    private final MailTemplateService templateService;

    public void sendRegisterCode(String email, String code, int minutes) {
        sendCode(email, code, minutes, "AskBox 注册验证码", "你正在注册 AskBox 箱主账号。请在页面中输入下面的验证码：");
    }

    public void sendLoginCode(String email, String code, int minutes) {
        sendCode(email, code, minutes, "AskBox 登录验证码", "你正在登录 AskBox。请在页面中输入下面的验证码：");
    }

    public void sendEmailChangeCode(String email, String code, int minutes) {
        sendCode(email, code, minutes, "AskBox 更换邮箱验证码", "你正在更换 AskBox 登录邮箱。请在页面中输入下面的验证码：");
    }

    private void sendCode(String email, String code, int minutes, String subject, String description) {
        String html = templateService.render(
                "mail/code.html",
                java.util.Map.of(
                        "title", templateService.html(subject),
                        "description", templateService.html(description),
                        "code", templateService.html(code),
                        "minutes", String.valueOf(minutes)));
        mailSenderService.sendHtml(email, subject, html);
    }
}
