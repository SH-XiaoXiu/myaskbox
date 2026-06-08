package cn.xiuxius.askbox.auth.service;

import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;
import cn.xiuxius.askbox.auth.view.RegisterConfigView;

public interface AuthService {
    LoginView login(String username, String password);

    RegisterConfigView registerConfig();

    void sendRegisterCode(String email);

    LoginView register(
            String username, String password, String confirmPassword, String email, String code, String displayName);

    MeView current();

    void logout();

    void changePassword(String currentPassword, String newPassword, String confirmPassword);
}
