package cn.xiuxius.askbox.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt;

import cn.xiuxius.askbox.system.entity.SysRoleEntity;
import cn.xiuxius.askbox.system.entity.SysUserEntity;
import cn.xiuxius.askbox.system.mapper.SysUserRoleMapper;
import cn.xiuxius.askbox.system.repository.SysRoleRepository;
import cn.xiuxius.askbox.system.repository.SysUserRepository;

@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    @Mock
    private SysUserRepository sysUserRepository;

    @Mock
    private SysRoleRepository sysRoleRepository;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @InjectMocks
    private SysUserServiceImpl service;

    @Test
    void createUserUsesNormalizedEmailAsUsernameAndEmail() {
        SysRoleEntity ownerRole = new SysRoleEntity();
        ownerRole.setId(2L);
        when(sysUserRepository.findByEmail("owner@example.com")).thenReturn(null);
        when(sysRoleRepository.findByCode("BOX_OWNER")).thenReturn(ownerRole);
        doAnswer(invocation -> {
                    SysUserEntity user = invocation.getArgument(0);
                    user.setId(42L);
                    return null;
                })
                .when(sysUserRepository)
                .insert(any(SysUserEntity.class));

        SysUserEntity created = service.createUser(" Owner@Example.COM ", "secret123", "Owner", null);

        assertThat(created.getUsername()).isEqualTo("owner@example.com");
        assertThat(created.getEmail()).isEqualTo("owner@example.com");
        assertThat(created.getDisplayName()).isEqualTo("Owner");
        assertThat(created.getPasswordHash()).isNotEqualTo("secret123");
        assertThat(BCrypt.checkpw("secret123", created.getPasswordHash())).isTrue();
        verify(sysUserRoleMapper).insertUserRoles(eq(42L), eq(List.of(2L)));
    }

    @Test
    void updateUserKeepsUsernameAndEmailInSync() {
        SysUserEntity user = new SysUserEntity()
                .setId(7L)
                .setUsername("old@example.com")
                .setEmail("old@example.com")
                .setDisplayName("Old");
        when(sysUserRepository.findById(7L)).thenReturn(user);
        when(sysUserRepository.findByEmail("new@example.com")).thenReturn(null);

        service.updateUser(7L, "New", " New@Example.COM ", null);

        ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
        verify(sysUserRepository).update(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("New");
    }

    @Test
    void authenticateFindsUserByNormalizedEmail() {
        SysUserEntity user = new SysUserEntity()
                .setId(9L)
                .setEmail("owner@example.com")
                .setUsername("owner@example.com")
                .setPasswordHash(BCrypt.hashpw("secret123", BCrypt.gensalt()))
                .setStatus("ACTIVE");
        when(sysUserRepository.findByEmail("owner@example.com")).thenReturn(user);

        SysUserEntity authenticated = service.authenticate(" Owner@Example.COM ", "secret123");

        assertThat(authenticated).isSameAs(user);
    }
}
