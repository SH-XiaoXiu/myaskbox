<script setup>
import { useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import LiquidErrorShell from "@/components/liquid/LiquidErrorShell.vue";

const router = useRouter();
const auth = useAuthStore();

function targetPath() {
  return auth.isLoggedIn ? auth.landingPath : "/login";
}

function goPrimary() {
  router.replace(targetPath());
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.replace(targetPath());
}
</script>

<template>
  <LiquidErrorShell
    status-code="404"
    eyebrow="页面不存在"
    title="没有找到这个页面"
    description="这个链接可能已经失效，或者地址输入有误。"
    icon="ri-compass-3-line"
    tone="amber"
    :primary-label="auth.isLoggedIn ? '返回工作台' : '返回登录'"
    :primary-icon="auth.isLoggedIn ? 'ri-home-5-line' : 'ri-login-box-line'"
    secondary-label="返回上一页"
    secondary-icon="ri-arrow-left-line"
    @primary="goPrimary"
    @secondary="goBack"
  />
</template>
