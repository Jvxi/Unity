import { createRouter, createWebHashHistory } from "vue-router";

const publicRoutes = ["/login", "/register"];

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
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
      path: "/",
      component: () => import("../layout/MainLayout.vue"),
      children: [
        {
          path: "",
          name: "home",
          component: () => import("../views/HomeView.vue"),
        },
        {
          path: "analysis",
          name: "analysis",
          component: () => import("../views/BinaryWorkbenchView.vue"),
        },
        {
          path: "strings",
          name: "strings",
          redirect: { path: "/analysis", query: { tool: "strings" } },
        },
        {
          path: "hex",
          name: "hex",
          redirect: { path: "/analysis", query: { tool: "hex" } },
        },
        {
          path: "history",
          name: "history",
          component: () => import("../views/HistoryView.vue"),
        },
        {
          path: "chat",
          name: "chat",
          component: () => import("../views/ChatView.vue"),
        },
        {
          path: "novels",
          name: "novels",
          component: () => import("../views/NovelWorkspaceView.vue"),
        },
        {
          path: "settings",
          name: "settings",
          component: () => import("../views/SettingsView.vue"),
        },
        {
          path: ":pathMatch(.*)*",
          name: "not-found",
          redirect: "/",
        },
      ],
    },
  ],
});

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  if (!publicRoutes.includes(to.path) && !token) {
    next("/login");
  } else if ((to.path === "/login" || to.path === "/register") && token) {
    next("/");
  } else {
    next();
  }
});

export default router;
