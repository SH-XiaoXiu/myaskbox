package cn.xiuxius.askbox.security;

/**
 * ThreadLocal 持有当前请求的用户信息。
 * <p>
 * 请求结束时清理，避免线程池复用导致数据泄露。
 */
public final class UserContextHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
