package cn.xiuxius.askbox.mail.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class MailTemplateService {

    public String render(String templatePath, Map<String, String> values) {
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            String template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : values.entrySet()) {
                template = template.replace(
                        "{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
            }
            return template;
        } catch (Exception ex) {
            throw new IllegalStateException("邮件模板渲染失败: " + templatePath, ex);
        }
    }

    public String html(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }
}
