package cn.xiuxius.askbox.auth.view;

public record LoginView(String token) {
    public static LoginView of(String token) {
        return new LoginView(token);
    }
}
