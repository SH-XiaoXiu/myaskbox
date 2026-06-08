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
public class RegisterCodeService {

    private static final int TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final RegisterMailService registerMailService;

    public void send(String email) {
        String normalized = normalize(email);
        String cooldownKey = cooldownKey(normalized);
        Boolean first = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(first)) {
            throw new BizException(ErrorCodes.RATE_LIMITED, "验证码发送过于频繁，请稍后再试");
        }
        String code = RandomUtil.randomNumbers(6);
        redisTemplate.opsForValue().set(codeKey(normalized), code, Duration.ofMinutes(TTL_MINUTES));
        redisTemplate.delete(attemptKey(normalized));
        try {
            registerMailService.sendRegisterCode(normalized, code, TTL_MINUTES);
        } catch (RuntimeException ex) {
            redisTemplate.delete(codeKey(normalized));
            redisTemplate.delete(cooldownKey);
            throw ex;
        }
    }

    public void verify(String email, String code) {
        String normalized = normalize(email);
        String attemptsKey = attemptKey(normalized);
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(TTL_MINUTES));
        }
        if (attempts != null && attempts > MAX_ATTEMPTS) {
            throw new BizException(ErrorCodes.EMAIL_CODE_INVALID, "邮箱验证码错误次数过多，请重新获取");
        }
        String stored = redisTemplate.opsForValue().get(codeKey(normalized));
        if (stored == null || !stored.equals(code)) {
            throw new BizException(ErrorCodes.EMAIL_CODE_INVALID);
        }
        redisTemplate.delete(codeKey(normalized));
        redisTemplate.delete(attemptsKey);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String codeKey(String email) {
        return "askbox:register:email-code:" + email;
    }

    private String cooldownKey(String email) {
        return "askbox:register:email-cooldown:" + email;
    }

    private String attemptKey(String email) {
        return "askbox:register:email-attempt:" + email;
    }
}
