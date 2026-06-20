import { useRouter } from "vue-router";
import { showConfirmDialog } from "vant";
import { useAuthStore } from "@/stores/auth";

export function useLogoutConfirm() {
  const router = useRouter();
  const auth = useAuthStore();
  let logoutPending = false;

  async function confirmLogout() {
    if (logoutPending) return;
    logoutPending = true;
    try {
      await showConfirmDialog({
        title: "确认退出登录？",
        message: "退出后需要重新登录。",
        confirmButtonText: "确认",
        cancelButtonText: "取消",
        confirmButtonColor: "#2f6fed",
        round: true,
      });
      await auth.logout();
      await router.replace("/login");
    } catch {
      // 用户取消退出
    } finally {
      logoutPending = false;
    }
  }

  return {
    confirmLogout,
  };
}
