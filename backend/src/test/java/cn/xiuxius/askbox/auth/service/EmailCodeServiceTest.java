package cn.xiuxius.askbox.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import cn.xiuxius.askbox.common.BizException;

@ExtendWith(MockitoExtension.class)
class EmailCodeServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private AsyncEmailCodeMailService asyncEmailCodeMailService;

    @InjectMocks
    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void sendLoginCodeUsesLoginPurposeKeysAndMailTemplate() {
        when(valueOperations.setIfAbsent("askbox:email-cooldown:login:user@example.com", "1", Duration.ofSeconds(60)))
                .thenReturn(true);

        service.sendLoginCode(" User@Example.COM ");

        verify(valueOperations)
                .set(eq("askbox:email-code:login:user@example.com"), anyString(), eq(Duration.ofMinutes(10)));
        verify(redisTemplate).delete("askbox:email-attempt:login:user@example.com");
        verify(asyncEmailCodeMailService).send(eq("user@example.com"), anyString(), eq(10), eq("login"));
    }

    @Test
    void verifyRegisterCodeDeletesCodeAfterSuccess() {
        when(valueOperations.increment("askbox:email-attempt:register:user@example.com"))
                .thenReturn(1L);
        when(valueOperations.get("askbox:email-code:register:user@example.com")).thenReturn("123456");

        service.verifyRegisterCode("user@example.com", "123456");

        verify(redisTemplate).expire("askbox:email-attempt:register:user@example.com", Duration.ofMinutes(10));
        verify(redisTemplate).delete("askbox:email-code:register:user@example.com");
        verify(redisTemplate).delete("askbox:email-attempt:register:user@example.com");
    }

    @Test
    void verifyLoginCodeRejectsTooManyAttempts() {
        when(valueOperations.increment("askbox:email-attempt:login:user@example.com"))
                .thenReturn(6L);

        assertThatThrownBy(() -> service.verifyLoginCode("user@example.com", "123456"))
                .isInstanceOf(BizException.class);
    }
}
