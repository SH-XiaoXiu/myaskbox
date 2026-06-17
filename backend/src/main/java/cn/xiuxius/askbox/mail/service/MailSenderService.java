package cn.xiuxius.askbox.mail.service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;
import cn.xiuxius.askbox.system.setting.service.SysSettingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MailSenderService {

    private final SysSettingService settingService;

    public void sendHtml(String to, String subject, String html) {
        try {
            JavaMailSenderImpl sender = buildSender();
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            String from = setting("mail.from");
            String fromName = setting("mail.from_name");
            if (fromName.isBlank()) {
                helper.setFrom(from);
            } else {
                helper.setFrom(from, fromName);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
        } catch (BizException ex) {
            throw ex;
        } catch (MailAuthenticationException | AuthenticationFailedException ex) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "SMTP 认证失败，请检查用户名和密码");
        } catch (MailSendException ex) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, mailFailureMessage(ex));
        } catch (MessagingException ex) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮件格式或发件人配置错误");
        } catch (Exception ex) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮件发送失败");
        }
    }

    private JavaMailSenderImpl buildSender() {
        String host = setting("mail.host");
        String username = setting("mail.username");
        String password = settingService.getString("mail.password", "");
        String from = setting("mail.from");
        if (host.isBlank() || username.isBlank() || password.isBlank() || from.isBlank()) {
            throw new BizException(ErrorCodes.VALIDATION_ERROR, "邮件配置不完整");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(settingService.getInt("mail.port", 465));
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        if (settingService.getBoolean("mail.ssl_enabled", true)) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        return sender;
    }

    private String setting(String key) {
        return settingService.getString(key, "").trim();
    }

    private String mailFailureMessage(MailSendException ex) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause != null ? cause.getMessage() : "";
        if (detail == null || detail.isBlank()) {
            return "邮件发送失败";
        }
        return "邮件发送失败：" + detail;
    }
}
