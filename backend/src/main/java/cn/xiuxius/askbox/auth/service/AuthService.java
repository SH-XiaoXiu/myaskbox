package cn.xiuxius.askbox.auth.service;

import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;
import cn.xiuxius.askbox.auth.view.RegisterConfigView;

public interface AuthService {
    LoginView login(String email, String password);

    void sendLoginCode(String email);

    LoginView loginByCode(String email, String code);

    RegisterConfigView registerConfig();

    void sendRegisterCode(String email);

    LoginView register(String password, String confirmPassword, String email, String code, String displayName);

    MeView current();

    MeView updateProfile(String displayName, String avatarObjectKey);

    void sendEmailChangeCode(String email);

    MeView changeEmail(String email, String code);

    void logout();

    void changePassword(String currentPassword, String newPassword, String confirmPassword);
}
