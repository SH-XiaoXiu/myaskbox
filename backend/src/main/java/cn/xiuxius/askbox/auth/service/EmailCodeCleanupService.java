package cn.xiuxius.askbox.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailCodeCleanupService {

    private final StringRedisTemplate redisTemplate;

    public void cleanupAfterSendFailure(String email, String purpose) {
        redisTemplate.delete(codeKey(purpose, email));
        redisTemplate.delete(cooldownKey(purpose, email));
    }

    private String codeKey(String purpose, String email) {
        return "askbox:email-code:" + purpose + ":" + email;
    }

    private String cooldownKey(String purpose, String email) {
        return "askbox:email-cooldown:" + purpose + ":" + email;
    }
}
