package cn.xiuxius.askbox.auth.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cn.xiuxius.askbox.boxuser.service.BoxUserService;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.repository.SysUserRepository;
import cn.xiuxius.askbox.system.service.SysUserService;
import cn.xiuxius.askbox.system.setting.service.SysSettingService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private BoxUserService boxUserService;

    @Mock
    private SysSettingService sysSettingService;

    @Mock
    private EmailCodeService emailCodeService;

    @InjectMocks
    private AuthServiceImpl service;

    @Test
    void sendLoginCodeDoesNotRevealMissingAccount() {
        when(sysUserRepository.findByEmail("missing@example.com")).thenReturn(null);

        service.sendLoginCode(" Missing@Example.COM ");

        verifyNoInteractions(emailCodeService);
    }

    @Test
    void sendLoginCodeDoesNotSendForDisabledAccount() {
        SysUserEntity disabled =
                new SysUserEntity().setEmail("user@example.com").setStatus("DISABLED");
        when(sysUserRepository.findByEmail("user@example.com")).thenReturn(disabled);

        service.sendLoginCode("user@example.com");

        verifyNoInteractions(emailCodeService);
    }

    @Test
    void sendLoginCodeSendsForActiveAccount() {
        SysUserEntity active = new SysUserEntity().setEmail("user@example.com").setStatus("ACTIVE");
        when(sysUserRepository.findByEmail("user@example.com")).thenReturn(active);

        service.sendLoginCode("user@example.com");

        verify(emailCodeService).sendLoginCode("user@example.com");
    }
}
