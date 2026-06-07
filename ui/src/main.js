import { createApp } from "vue";
import { createPinia } from "pinia";
import Vant from "vant";
import "vant/lib/index.css";
import "remixicon/fonts/remixicon.css";
import App from "./App.vue";
import router from "./router";
import "./style.css";
import { setUnauthorizedHandler } from "./api/client";
import { useAuthStore } from "./stores/auth";

const app = createApp(App);
const pinia = createPinia();
app.use(pinia);
app.use(router);
app.use(Vant);

setUnauthorizedHandler(() => {
  const auth = useAuthStore();
  auth.clearSession();
  const current = router.currentRoute.value.fullPath;
  if (router.currentRoute.value.path === "/login") return;
  router.replace({ path: "/login", query: { redirect: current } });
});

app.mount("#app");
