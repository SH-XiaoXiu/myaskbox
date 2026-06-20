<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import LiquidErrorShell from "@/components/liquid/LiquidErrorShell.vue";
import { useLogoutConfirm } from "@/composables/useLogoutConfirm";

const router = useRouter();
const auth = useAuthStore();
const { confirmLogout } = useLogoutConfirm();

function goLanding() {
  router.replace(auth.landingPath);
}

function handleLogout() {
  confirmLogout();
}
</script>

<template>
  <LiquidErrorShell
    status-code="403"
    eyebrow="访问受限"
    title="当前账号暂无访问权限"
    description="这个页面需要更高权限或不同角色。"
    detail="如果你认为这有问题，请联系管理员调整角色权限。"
    icon="ri-shield-user-line"
    tone="red"
    primary-label="返回首页"
    primary-icon="ri-home-5-line"
    secondary-label="退出登录"
    secondary-icon="ri-logout-box-r-line"
    @primary="goLanding"
    @secondary="handleLogout"
  />
</template>
