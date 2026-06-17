package cn.xiuxius.askbox.auth.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncEmailCodeMailServiceTest {

    @Mock
    private RegisterMailService registerMailService;

    @Mock
    private EmailCodeCleanupService cleanupService;

    @InjectMocks
    private AsyncEmailCodeMailService service;

    @Test
    void failedSendCleansCodeAndCooldown() {
        doThrow(new RuntimeException("smtp down"))
                .when(registerMailService)
                .sendLoginCode("user@example.com", "123456", 10);

        service.send("user@example.com", "123456", 10, "login");

        verify(cleanupService).cleanupAfterSendFailure("user@example.com", "login");
    }
}
