package cn.xiuxius.askbox.auth.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailCodeMailService {

    private final RegisterMailService registerMailService;
    private final EmailCodeCleanupService cleanupService;

    @Async("askboxMailExecutor")
    public void send(String email, String code, int minutes, String purpose) {
        try {
            if ("login".equals(purpose)) {
                registerMailService.sendLoginCode(email, code, minutes);
            } else {
                registerMailService.sendRegisterCode(email, code, minutes);
            }
        } catch (RuntimeException ex) {
            cleanupService.cleanupAfterSendFailure(email, purpose);
            log.warn("Email code mail failed: purpose={} email={} message={}", purpose, email, ex.getMessage());
        }
    }
}
