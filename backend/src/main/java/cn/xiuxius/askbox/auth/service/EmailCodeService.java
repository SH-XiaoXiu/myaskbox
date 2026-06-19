package cn.xiuxius.askbox.auth.service;

import java.time.Duration;
import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import cn.hutool.core.util.RandomUtil;
import cn.xiuxius.askbox.common.BizException;
import cn.xiuxius.askbox.common.ErrorCodes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailCodeService {

    private static final int TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final String PURPOSE_REGISTER = "register";
    private static final String PURPOSE_LOGIN = "login";
    private static final String PURPOSE_EMAIL_CHANGE = "email-change";

    private final StringRedisTemplate redisTemplate;
    private final AsyncEmailCodeMailService asyncEmailCodeMailService;

    public void sendRegisterCode(String email) {
        send(email, PURPOSE_REGISTER);
    }

    public void verifyRegisterCode(String email, String code) {
        verify(email, code, PURPOSE_REGISTER);
    }

    public void sendLoginCode(String email) {
        send(email, PURPOSE_LOGIN);
    }

    public void verifyLoginCode(String email, String code) {
        verify(email, code, PURPOSE_LOGIN);
    }

    public void sendEmailChangeCode(String email) {
        send(email, PURPOSE_EMAIL_CHANGE);
    }

    public void verifyEmailChangeCode(String email, String code) {
        verify(email, code, PURPOSE_EMAIL_CHANGE);
    }

    private void send(String email, String purpose) {
        String normalized = normalize(email);
        String cooldownKey = cooldownKey(purpose, normalized);
        Boolean first = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(first)) {
            throw new BizException(ErrorCodes.RATE_LIMITED, "验证码发送过于频繁，请稍后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(codeKey(purpose, normalized), code, Duration.ofMinutes(TTL_MINUTES));
        redisTemplate.delete(attemptKey(purpose, normalized));
        asyncEmailCodeMailService.send(normalized, code, TTL_MINUTES, purpose);
    }

    private void verify(String email, String code, String purpose) {
        String normalized = normalize(email);
        String attemptsKey = attemptKey(purpose, normalized);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(TTL_MINUTES));
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            throw new BizException(ErrorCodes.EMAIL_CODE_INVALID, "邮箱验证码错误次数过多，请重新获取");
        }
        String stored = redisTemplate.opsForValue().get(codeKey(purpose, normalized));
        if (stored == null || !stored.equals(code)) {
            throw new BizException(ErrorCodes.EMAIL_CODE_INVALID);
        }
        redisTemplate.delete(codeKey(purpose, normalized));
        redisTemplate.delete(attemptsKey);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String codeKey(String purpose, String email) {
        return "askbox:email-code:" + purpose + ":" + email;
    }

    private String cooldownKey(String purpose, String email) {
        return "askbox:email-cooldown:" + purpose + ":" + email;
    }

    private String attemptKey(String purpose, String email) {
        return "askbox:email-attempt:" + purpose + ":" + email;
    }
}
