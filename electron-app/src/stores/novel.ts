import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { ElMessage } from "element-plus";
import {
  createNovelBook,
  deleteNovelBook,
  fetchNovelLibrary,
  fetchNovelPlatforms,
  fetchNovelProject,
  fetchNovelTypes,
  saveNovelProject,
  switchNovelBook,
} from "@/api/novel";
import { useSettingsStore } from "@/stores/settings";
import type {
  NovelAiSettings,
  NovelBookSummary,
  NovelChapter,
  NovelEmbeddingSettings,
  NovelProject,
  NovelProjectEnvelope,
  NovelTypeCatalogResponse,
  PublishPlatformInfo,
} from "@/types";

function nextId() {
  return typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}_${Math.random().toString(16).slice(2)}`;
}

function cloneProject(project: NovelProject): NovelProject {
  return JSON.parse(JSON.stringify(project)) as NovelProject;
}

function ensureProjectLists(project: NovelProject) {
  project.outlineNodes ||= [];
  project.characters ||= [];
  project.foreshadowing ||= [];
  project.chapters ||= [];
  project.onboarding ||= { completed: false, questions: [], answers: [] };
  project.meta.styleRules ||= [];
  project.meta.worldRules ||= [];
}

export const useNovelStore = defineStore("novel", () => {
  const activeBookId = ref("");
  const books = ref<NovelBookSummary[]>([]);
  const project = ref<NovelProject | null>(null);
  const platforms = ref<PublishPlatformInfo[]>([]);
  const novelTypes = ref<NovelTypeCatalogResponse | null>(null);
  const loading = ref(false);
  const saving = ref(false);
  const dirty = ref(false);
  const statusText = ref("");
  const errorText = ref("");

  const sortedChapters = computed(() =>
    [...(project.value?.chapters || [])].sort((a, b) => a.order - b.order)
  );
  const sortedOutline = computed(() =>
    [...(project.value?.outlineNodes || [])].sort((a, b) => a.order - b.order)
  );

  const hasProject = computed(() => Boolean(project.value && activeBookId.value));

  function buildAiSettings(): NovelAiSettings {
    const settings = useSettingsStore();
    const apiKey = settings.apiKey.trim();
    const baseUrl = settings.aiApiUrl.trim();
    const model = settings.selectedModel.trim();
    return {
      enabled: Boolean(apiKey && baseUrl && model),
      provider: settings.selectedProvider || "custom",
      baseUrl,
      apiKey,
      model,
      temperature: 0.75,
      maxTokens: 3200,
      contextWindowSize: 0,
      systemPrompt: "",
    };
  }

  function buildEmbeddingSettings(): NovelEmbeddingSettings {
    const settings = useSettingsStore();
    const baseUrl = settings.embeddingApiUrl.trim();
    const apiKey = settings.embeddingApiKey.trim();
    const model = settings.embeddingModel.trim() || "text-embedding-3-small";
    return {
      enabled: Boolean(settings.embeddingEnabled && baseUrl && apiKey && model),
      baseUrl,
      apiKey,
      model,
    };
  }

  const aiReady = computed(() => buildAiSettings().enabled);

  async function loadStaticData() {
    const [platformResult, typeResult] = await Promise.allSettled([
      fetchNovelPlatforms(),
      fetchNovelTypes(),
    ]);
    if (platformResult.status === "fulfilled") platforms.value = platformResult.value;
    if (typeResult.status === "fulfilled") novelTypes.value = typeResult.value;
  }

  async function loadLibraryOnly() {
    const library = await fetchNovelLibrary();
    activeBookId.value = library.activeBookId;
    books.value = library.books;
    return library;
  }

  async function loadAll() {
    loading.value = true;
    errorText.value = "";
    try {
      await loadStaticData();
      const library = await loadLibraryOnly();
      if (!library.activeBookId) {
        project.value = null;
        statusText.value = "书库为空，请先创建一本书。";
        dirty.value = false;
        return;
      }
      const envelope = await fetchNovelProject();
      applyEnvelope(envelope);
      statusText.value = "作品已加载。";
    } catch (error: any) {
      errorText.value = error.message || "小说助手加载失败";
      ElMessage.error(errorText.value);
    } finally {
      loading.value = false;
    }
  }

  function applyEnvelope(envelope: NovelProjectEnvelope) {
    activeBookId.value = envelope.bookId;
    project.value = envelope.project;
    if (project.value) ensureProjectLists(project.value);
    dirty.value = false;
  }

  function updateProject(mutator: (draft: NovelProject) => void) {
    if (!project.value) return;
    const next = cloneProject(project.value);
    mutator(next);
    ensureProjectLists(next);
    project.value = next;
    dirty.value = true;
    statusText.value = "有未保存修改";
  }

  function markDirty() {
    dirty.value = true;
    statusText.value = "有未保存修改";
  }

  async function saveCurrent() {
    if (!project.value) return;
    saving.value = true;
    errorText.value = "";
    try {
      const saved = await saveNovelProject(project.value);
      applyEnvelope(saved);
      await loadLibraryOnly();
      statusText.value = "保存完成。";
    } catch (error: any) {
      errorText.value = error.message || "保存失败";
      ElMessage.error(errorText.value);
    } finally {
      saving.value = false;
    }
  }

  async function createBook(payload: { title?: string; audienceChannel: string; novelType: string }) {
    loading.value = true;
    try {
      if (dirty.value) await saveCurrent();
      const envelope = await createNovelBook(payload);
      applyEnvelope(envelope);
      await loadLibraryOnly();
      statusText.value = "新书已创建。";
    } catch (error: any) {
      errorText.value = error.message || "创建失败";
      ElMessage.error(errorText.value);
    } finally {
      loading.value = false;
    }
  }

  async function switchBook(bookId: string) {
    if (!bookId || bookId === activeBookId.value) return;
    loading.value = true;
    try {
      if (dirty.value) await saveCurrent();
      const envelope = await switchNovelBook(bookId);
      applyEnvelope(envelope);
      statusText.value = "已切换书籍。";
    } catch (error: any) {
      errorText.value = error.message || "切换失败";
      ElMessage.error(errorText.value);
    } finally {
      loading.value = false;
    }
  }

  async function removeBook(bookId: string) {
    loading.value = true;
    try {
      const library = await deleteNovelBook(bookId);
      books.value = library.books;
      activeBookId.value = library.activeBookId;
      if (!library.activeBookId) {
        project.value = null;
        dirty.value = false;
        statusText.value = "书籍已删除，书库为空。";
        return;
      }
      const envelope = await fetchNovelProject();
      applyEnvelope(envelope);
      statusText.value = "书籍已删除。";
    } catch (error: any) {
      errorText.value = error.message || "删除失败";
      ElMessage.error(errorText.value);
    } finally {
      loading.value = false;
    }
  }

  function addChapter() {
    updateProject((draft) => {
      const nextOrder = Math.max(0, ...draft.chapters.map((item) => item.order || 0)) + 1;
      const chapter: NovelChapter = {
        id: nextId(),
        order: nextOrder,
        title: `第 ${nextOrder} 章`,
        summary: "",
        purpose: "",
        outlineNodeIds: [],
        characterIds: [],
        foreshadowingIds: [],
        mandatoryBeats: [],
        forbiddenContent: [],
        notes: "",
        draft: "",
      };
      draft.chapters.push(chapter);
    });
  }

  function removeChapter(chapterId: string) {
    updateProject((draft) => {
      draft.chapters = draft.chapters
        .filter((chapter) => chapter.id !== chapterId)
        .map((chapter, index) => ({ ...chapter, order: index + 1 }));
    });
  }

  return {
    activeBookId,
    books,
    project,
    platforms,
    novelTypes,
    loading,
    saving,
    dirty,
    statusText,
    errorText,
    sortedChapters,
    sortedOutline,
    hasProject,
    aiReady,
    buildAiSettings,
    buildEmbeddingSettings,
    loadAll,
    loadLibraryOnly,
    applyEnvelope,
    updateProject,
    markDirty,
    saveCurrent,
    createBook,
    switchBook,
    removeBook,
    addChapter,
    removeChapter,
  };
});
