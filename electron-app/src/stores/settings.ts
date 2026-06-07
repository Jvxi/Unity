import { defineStore } from "pinia";
import { ref } from "vue";

export const useSettingsStore = defineStore("settings", () => {
  const apiKey = ref(localStorage.getItem("unity_apiKey") || "");
  const selectedModel = ref(localStorage.getItem("unity_model") || "deepseek-chat");
  const menuCollapsed = ref(localStorage.getItem("unity_menuCollapsed") === "true");

  function setApiKey(val: string) {
    apiKey.value = val;
    localStorage.setItem("unity_apiKey", val);
  }

  function setModel(val: string) {
    selectedModel.value = val;
    localStorage.setItem("unity_model", val);
  }

  function setMenuCollapsed(val: boolean) {
    menuCollapsed.value = val;
    localStorage.setItem("unity_menuCollapsed", String(val));
  }

  return { apiKey, selectedModel, menuCollapsed, setApiKey, setModel, setMenuCollapsed };
});