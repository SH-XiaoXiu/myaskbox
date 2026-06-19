<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import LiquidErrorShell from "@/components/liquid/LiquidErrorShell.vue";

const route = useRoute();
const router = useRouter();

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  return typeof redirect === "string" && redirect.startsWith("/") && !redirect.startsWith("//")
    ? redirect
    : "/";
});

function goLogin() {
  router.replace({ path: "/login", query: { redirect: redirectTarget.value } });
}

function goBack() {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.replace("/login");
}
</script>

<template>
  <LiquidErrorShell
    status-code="401"
    eyebrow="需要登录"
    title="请先登录 AskBox"
    description="你访问的内容需要登录后才能继续。"
    icon="ri-lock-password-line"
    tone="blue"
    primary-label="去登录"
    primary-icon="ri-login-box-line"
    secondary-label="返回上一页"
    secondary-icon="ri-arrow-left-line"
    @primary="goLogin"
    @secondary="goBack"
  />
</template>
