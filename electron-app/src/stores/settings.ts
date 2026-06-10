import { defineStore } from "pinia";
import { ref } from "vue";

export const useSettingsStore = defineStore("settings", () => {
  const apiKey = ref(localStorage.getItem("unity_apiKey") || "");
  const selectedProvider = ref(localStorage.getItem("unity_provider") || "deepseek");
  const selectedModel = ref(localStorage.getItem("unity_model") || "deepseek-v4-flash");
  const aiApiUrl = ref(localStorage.getItem("unity_aiApiUrl") || "");
  const menuCollapsed = ref(localStorage.getItem("unity_menuCollapsed") === "true");
  const theme = ref<'light' | 'dark'>((localStorage.getItem("unity_theme") as 'light' | 'dark') || "light");

  function setApiKey(val: string) { apiKey.value = val; localStorage.setItem("unity_apiKey", val); }
  function setProvider(val: string) { selectedProvider.value = val; localStorage.setItem("unity_provider", val); }
  function setModel(val: string) { selectedModel.value = val; localStorage.setItem("unity_model", val); }
  function setAiApiUrl(val: string) { aiApiUrl.value = val; localStorage.setItem("unity_aiApiUrl", val); }
  function setMenuCollapsed(val: boolean) { menuCollapsed.value = val; localStorage.setItem("unity_menuCollapsed", String(val)); }
  function setTheme(val: 'light' | 'dark') { theme.value = val; localStorage.setItem("unity_theme", val); document.documentElement.setAttribute('data-theme', val); }

  // Apply on load
  if (theme.value === 'dark') document.documentElement.setAttribute('data-theme', 'dark');
  localStorage.removeItem("unity_fontSize");
  document.documentElement.style.fontSize = "";

  return { apiKey, selectedProvider, selectedModel, aiApiUrl, menuCollapsed, theme, setApiKey, setProvider, setModel, setAiApiUrl, setMenuCollapsed, setTheme };
});
