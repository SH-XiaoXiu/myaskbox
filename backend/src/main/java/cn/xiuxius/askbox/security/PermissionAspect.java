package cn.xiuxius.askbox.security;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import cn.dev33.satoken.stp.StpUtil;

@Aspect
@Component
public class PermissionAspect {

    @Before(
            "@within(cn.xiuxius.askbox.security.RequiresRole) || @annotation(cn.xiuxius.askbox.security.RequiresRole) || "
                    + "@within(cn.xiuxius.askbox.security.RequiresPermission) || @annotation(cn.xiuxius.askbox.security.RequiresPermission)")
    public void check(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RequiresRole role = annotation(method, targetClass, RequiresRole.class);
        RequiresPermission permission = annotation(method, targetClass, RequiresPermission.class);
        if (role != null) {
            checkRoles(role.value());
        }
        if (permission != null) {
            checkPermissions(permission.value());
        }
    }

    private void checkRoles(String[] roles) {
        StpUtil.checkLogin();
        boolean ok = Arrays.stream(roles).anyMatch(StpUtil::hasRole);
        if (!ok) {
            StpUtil.checkRole(roles.length == 0 ? "" : roles[0]);
        }
    }

    private void checkPermissions(String[] permissions) {
        StpUtil.checkLogin();
        Arrays.stream(permissions).forEach(StpUtil::checkPermission);
    }

    private <A extends java.lang.annotation.Annotation> A annotation(
            Method method, Class<?> targetClass, Class<A> type) {
        A methodAnnotation = method.getAnnotation(type);
        return methodAnnotation != null ? methodAnnotation : targetClass.getAnnotation(type);
    }
}
