package cn.xiuxius.askbox.auth.service;

import cn.xiuxius.askbox.auth.view.LoginView;
import cn.xiuxius.askbox.auth.view.MeView;

public interface AuthService {
    LoginView login(String username, String password);

    MeView current();

    void logout();

    void changePassword(String currentPassword, String newPassword, String confirmPassword);
}
