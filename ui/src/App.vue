<script setup>
import { ref, watch } from "vue";
import { useRoute } from "vue-router";

const routeOrder = [
  "/login",
  "/register",
  "/home",
  "/topics",
  "/password",
  "/admin",
  "/box",
  "/reply",
  "/forbidden",
  "/unauthorized",
];

const currentRoute = useRoute();
const lastRouteIndex = ref(routeIndex(currentRoute.path));
const routeTransitionName = ref("ubuntu-route-forward");

function routeIndex(path) {
  const index = routeOrder.findIndex((item) => path === item || path.startsWith(`${item}/`));
  return index >= 0 ? index : routeOrder.length;
}

function routeKey(route) {
  if (route.path === "/home" || route.path.startsWith("/home/")) return "home";
  if (route.path.startsWith("/box/")) return route.fullPath;
  return route.name || route.path;
}

watch(
  () => currentRoute.path,
  (path) => {
    const nextIndex = routeIndex(path);
    routeTransitionName.value = nextIndex < lastRouteIndex.value ? "ubuntu-route-back" : "ubuntu-route-forward";
    lastRouteIndex.value = nextIndex;
  },
  { flush: "sync" },
);

function refreshRouteEffects() {
  window.dispatchEvent(new Event("resize"));
}
</script>

<template>
  <RouterView v-slot="{ Component, route }">
    <div class="route-stage">
      <Transition :name="routeTransitionName" mode="out-in" @after-enter="refreshRouteEffects">
        <component :is="Component" :key="routeKey(route)" class="route-page" />
      </Transition>
    </div>
  </RouterView>
</template>
