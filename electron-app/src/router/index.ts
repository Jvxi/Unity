import { createRouter, createWebHashHistory } from "vue-router";

const publicRoutes = ["/login", "/register"];

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: "/",
      name: "home",
      component: () => import("../views/HomeView.vue"),
    },
    {
      path: "/login",
      name: "login",
      component: () => import("../views/LoginView.vue"),
    },
    {
      path: "/register",
      name: "register",
      component: () => import("../views/RegisterView.vue"),
    },
    {
      path: "/analysis",
      name: "analysis",
      component: () => import("../views/DllAnalysisView.vue"),
    },
    {
      path: "/strings",
      name: "strings",
      component: () => import("../views/StringsView.vue"),
    },
    {
      path: "/hex",
      name: "hex",
      component: () => import("../views/HexView.vue"),
    },
    {
      path: "/history",
      name: "history",
      component: () => import("../views/HistoryView.vue"),
    },
    {
      path: "/settings",
      name: "settings",
      component: () => import("../views/SettingsView.vue"),
    },
  ],
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  if (!publicRoutes.includes(to.path) && !token) {
    next("/login");
  } else if (to.path === "/login" && token) {
    next("/");
  } else {
    next();
  }
});

export default router;
