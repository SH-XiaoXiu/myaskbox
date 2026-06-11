package cn.xiuxius.askbox.auth.view;

import java.util.Set;

public record MeView(Long id, String username, String email, Set<String> roles, Set<String> permissions) {}
