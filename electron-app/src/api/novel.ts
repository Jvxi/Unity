import http, { getApiBaseUrl, readApiErrorMessage, readErrorPayload } from "@/api/client";
import type {
  NovelAiSettings,
  NovelBookSummary,
  NovelChapterGenerationResponse,
  NovelChaseDebt,
  NovelCommitRecord,
  NovelEmbeddingSettings,
  NovelLibraryIndex,
  NovelMemoryPack,
  NovelProject,
  NovelProjectEnvelope,
  NovelRagResult,
  NovelReadingPower,
  NovelReviewIssue,
  NovelStoryContract,
  NovelStoryEvent,
  NovelTypeCatalogResponse,
  OnboardingAnswer,
  OnboardingQuestion,
  OutlineBootstrapProposal,
  PublishPlatformInfo,
} from "@/types";

interface StreamEvent {
  type: string;
  [key: string]: unknown;
}

function aiPayload(aiSettings?: NovelAiSettings) {
  return aiSettings ? { aiSettings } : {};
}

function authHeaders(extra?: Record<string, string>) {
  const headers: Record<string, string> = { ...(extra || {}) };
  const token = localStorage.getItem("token");
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function readBusinessError(error: unknown, fallback: string) {
  return readApiErrorMessage(error, fallback);
}

export async function fetchNovelLibrary(): Promise<NovelLibraryIndex> {
  try {
    const res = await http.get<NovelLibraryIndex>("/api/novels/library");
    return res.data;
  } catch (error) {
    throw new Error(readBusinessError(error, "书库加载失败"));
  }
}

export async function fetchNovelProject(): Promise<NovelProjectEnvelope> {
  try {
    const res = await http.get<NovelProjectEnvelope>("/api/novels/project");
    return res.data;
  } catch (error) {
    throw new Error(readBusinessError(error, "作品加载失败"));
  }
}

export async function saveNovelProject(project: NovelProject): Promise<NovelProjectEnvelope> {
  try {
    const res = await http.put<NovelProjectEnvelope>("/api/novels/project", project);
    return res.data;
  } catch (error) {
    throw new Error(readBusinessError(error, "作品保存失败"));
  }
}

export async function createNovelBook(payload: {
  title?: string;
  audienceChannel: string;
  novelType: string;
}): Promise<NovelProjectEnvelope> {
  try {
    const res = await http.post<NovelProjectEnvelope>("/api/novels/library/books", payload);
    return res.data;
  } catch (error) {
    throw new Error(readBusinessError(error, "创建书籍失败"));
  }
}

export async function switchNovelBook(bookId: string): Promise<NovelProjectEnvelope> {
  const res = await http.put<NovelProjectEnvelope>("/api/novels/library/active", { bookId });
  return res.data;
}

export async function deleteNovelBook(bookId: string): Promise<NovelLibraryIndex> {
  const res = await http.delete<NovelLibraryIndex>(`/api/novels/library/books/${encodeURIComponent(bookId)}`);
  return res.data;
}

export async function fetchNovelPlatforms(): Promise<PublishPlatformInfo[]> {
  const res = await http.get<PublishPlatformInfo[]>("/api/novels/platforms");
  return res.data;
}

export async function fetchNovelTypes(): Promise<NovelTypeCatalogResponse> {
  const res = await http.get<NovelTypeCatalogResponse>("/api/novels/novel-types");
  return res.data;
}

export async function testNovelAiConnection(aiSettings: NovelAiSettings) {
  const res = await http.post<{ ok: boolean; message: string }>("/api/novels/ai/test", aiSettings);
  return res.data;
}

export async function generateNovelChapter(chapterId: string, aiSettings?: NovelAiSettings) {
  const res = await http.post<NovelChapterGenerationResponse>(
    `/api/novels/chapters/${encodeURIComponent(chapterId)}/generate`,
    aiPayload(aiSettings)
  );
  return res.data;
}

export async function fetchNovelContext(bookId: string, chapter: number, taskType = "write") {
  const res = await http.post<Record<string, unknown>>(
    `/api/novels/context/${encodeURIComponent(bookId)}/chapter/${chapter}/assemble?taskType=${encodeURIComponent(taskType)}`
  );
  return res.data;
}

export async function fetchNovelMemoryPack(bookId: string, chapter: number) {
  const res = await http.get<NovelMemoryPack>(
    `/api/novels/memory/${encodeURIComponent(bookId)}/pack?chapter=${chapter}&taskType=write`
  );
  return res.data;
}

export async function fetchNovelMemoryStats(bookId: string) {
  const res = await http.get<Record<string, unknown>>(`/api/novels/memory/${encodeURIComponent(bookId)}/stats`);
  return res.data;
}

export async function fetchNovelRagStats(bookId: string) {
  const res = await http.get<Record<string, unknown>>(`/api/novels/rag/${encodeURIComponent(bookId)}/stats`);
  return res.data;
}

export async function searchNovelRag(
  bookId: string,
  query: string,
  topK = 8,
  embeddingSettings?: NovelEmbeddingSettings
) {
  const res = await http.post<NovelRagResult[]>(`/api/novels/rag/${encodeURIComponent(bookId)}/search`, {
    query,
    topK,
    embeddingSettings,
  });
  return res.data;
}

export async function analyzeNovelReadingPower(bookId: string, chapter: number, text: string) {
  const res = await http.post<NovelReadingPower>(
    `/api/novels/reading-power/${encodeURIComponent(bookId)}/chapter/${chapter}/analyze`,
    { text }
  );
  return res.data;
}

export async function fetchNovelReadingPowerStats(bookId: string) {
  const res = await http.get<Record<string, unknown>>(`/api/novels/reading-power/${encodeURIComponent(bookId)}/stats`);
  return res.data;
}

export async function fetchNovelDebts(bookId: string, limit = 10) {
  const res = await http.get<NovelChaseDebt[]>(`/api/novels/reading-power/${encodeURIComponent(bookId)}/debts?limit=${limit}`);
  return res.data;
}

export async function fetchNovelMasterSetting(bookId: string) {
  const res = await http.get<NovelStoryContract>(
    `/api/novels/story-system/${encodeURIComponent(bookId)}/master-setting`
  );
  return res.data;
}

export async function generateNovelMasterSetting(bookId: string, query = "", genre?: string) {
  const res = await http.post<NovelStoryContract>(
    `/api/novels/story-system/${encodeURIComponent(bookId)}/master-setting`,
    { query, genre }
  );
  return res.data;
}

export async function fetchNovelChapterBrief(bookId: string, chapter: number) {
  const res = await http.get<NovelStoryContract>(
    `/api/novels/story-system/${encodeURIComponent(bookId)}/chapter/${chapter}/brief`
  );
  return res.data;
}

export async function generateNovelChapterBrief(bookId: string, chapter: number, directive?: Record<string, unknown>) {
  const res = await http.post<NovelStoryContract>(
    `/api/novels/story-system/${encodeURIComponent(bookId)}/chapter/${chapter}/brief`,
    directive || {}
  );
  return res.data;
}

export async function fetchNovelReviewContract(bookId: string, chapter: number) {
  const res = await http.get<NovelStoryContract>(
    `/api/novels/story-system/${encodeURIComponent(bookId)}/chapter/${chapter}/review-contract`
  );
  return res.data;
}

export async function generateNovelReviewContract(bookId: string, chapter: number) {
  const res = await http.post<NovelStoryContract>(
    `/api/novels/story-system/${encodeURIComponent(bookId)}/chapter/${chapter}/review-contract`
  );
  return res.data;
}

export async function commitNovelChapter(bookId: string, chapter: number, payload: Record<string, unknown>) {
  const res = await http.post<NovelCommitRecord>(
    `/api/novels/commit/${encodeURIComponent(bookId)}/chapter/${chapter}`,
    payload
  );
  return res.data;
}

export async function fetchNovelCommits(bookId: string) {
  const res = await http.get<NovelCommitRecord[]>(`/api/novels/commit/${encodeURIComponent(bookId)}/chapters`);
  return res.data;
}

export async function fetchNovelEvents(bookId: string) {
  const res = await http.get<NovelStoryEvent[]>(`/api/novels/events/${encodeURIComponent(bookId)}`);
  return res.data;
}

export async function generateOnboarding(aiSettings?: NovelAiSettings) {
  const res = await http.post<{ questions: OnboardingQuestion[]; bookId: string; project: NovelProject }>(
    "/api/novels/onboarding/generate",
    aiPayload(aiSettings)
  );
  return res.data;
}

export async function submitOnboarding(answers: OnboardingAnswer[]) {
  const res = await http.post<NovelProject>("/api/novels/onboarding/submit", { answers });
  return res.data;
}

export async function generateOutlineProposals(aiSettings: NovelAiSettings | undefined, answers: OnboardingAnswer[]) {
  const res = await http.post<{ proposals: OutlineBootstrapProposal[] }>("/api/novels/outline-bootstrap/proposals", {
    aiSettings,
    answers,
  });
  return res.data.proposals;
}

export async function applyOutlineProposal(proposal: OutlineBootstrapProposal) {
  const res = await http.post<NovelProjectEnvelope>("/api/novels/outline-bootstrap/apply", { proposal });
  return res.data;
}

export async function analyzeNovelOutlineImport(outlineText: string, aiSettings?: NovelAiSettings) {
  const res = await http.post<Record<string, unknown>>("/api/novels/project/import/analyze/outline", {
    outlineText,
    aiSettings,
  });
  return res.data;
}

export async function analyzeNovelChaptersImport(chaptersText: string, aiSettings?: NovelAiSettings) {
  const res = await http.post<Record<string, unknown>>("/api/novels/project/import/analyze/chapters", {
    chaptersText,
    aiSettings,
  });
  return res.data;
}

function extractSsePayload(block: string) {
  return block
    .split("\n")
    .map((line) => line.trim())
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice(5).trim())
    .join("\n")
    .trim();
}

function parseSsePayload(payload: string): StreamEvent | null {
  if (!payload) return null;
  try {
    let parsed: unknown = JSON.parse(payload);
    if (typeof parsed === "string") parsed = JSON.parse(parsed);
    return parsed && typeof parsed === "object" ? (parsed as StreamEvent) : null;
  } catch {
    return null;
  }
}

function parseSseChunk(buffer: string) {
  const blocks = buffer.split("\n\n");
  const events: StreamEvent[] = [];
  for (let index = 0; index < blocks.length - 1; index += 1) {
    const parsed = parseSsePayload(extractSsePayload(blocks[index]));
    if (parsed) events.push(parsed);
  }
  return { events, rest: blocks.at(-1) || "" };
}

async function readHttpErrorMessage(response: Response) {
  try {
    const text = await response.text();
    if (!text.trim()) return `请求失败：${response.status}`;
    try {
      return readErrorPayload(JSON.parse(text), `请求失败：${response.status}`);
    } catch {
      return readErrorPayload(text, `请求失败：${response.status}`);
    }
  } catch {
    return `请求失败：${response.status}`;
  }
}

export async function streamNovelChapter(
  chapterId: string,
  aiSettings: NovelAiSettings | undefined,
  handlers: {
    onDelta: (chunk: string, accumulated: string) => void;
    onDone: (result: NovelChapterGenerationResponse) => void;
    onCancelled: (message: string) => void;
    onError: (message: string) => void;
  },
  signal?: AbortSignal,
  continueMode = false
) {
  const url = `${getApiBaseUrl()}/api/novels/chapters/${encodeURIComponent(chapterId)}/generate/stream${
    continueMode ? "?continue=true" : ""
  }`;
  const response = await fetch(url, {
    method: "POST",
    headers: authHeaders({ Accept: "text/event-stream", "Content-Type": "application/json" }),
    body: JSON.stringify(aiPayload(aiSettings)),
    signal,
  });
  if (!response.ok) throw new Error(await readHttpErrorMessage(response));
  if (!response.body) throw new Error("流式生成失败：响应为空");

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let accumulated = "";

  const consume = (events: StreamEvent[]) => {
    for (const event of events) {
      if (event.type === "delta" && typeof event.content === "string") {
        accumulated += event.content;
        handlers.onDelta(event.content, accumulated);
      } else if (event.type === "done" && event.result) {
        handlers.onDone(event.result as NovelChapterGenerationResponse);
      } else if (event.type === "cancelled") {
        handlers.onCancelled(String(event.message || "生成已取消"));
      } else if (event.type === "error") {
        handlers.onError(String(event.message || "流式生成失败"));
      }
    }
  };

  while (true) {
    const { done, value } = await reader.read();
    if (value) {
      buffer += decoder.decode(value, { stream: true });
      const parsed = parseSseChunk(buffer);
      buffer = parsed.rest;
      consume(parsed.events);
    }
    if (done) break;
  }
  if (buffer.trim()) consume(parseSseChunk(`${buffer}\n\n`).events);
}

export async function streamNovelReview(
  aiSettings: NovelAiSettings | undefined,
  handlers: {
    onProgress: (chapterId: string, title: string, index: number, total: number) => void;
    onResult: (chapterId: string, title: string, issues: NovelReviewIssue[]) => void;
    onDone: (reviewedCount: number, issueCount: number) => void;
    onError: (message: string) => void;
  },
  signal?: AbortSignal
) {
  const response = await fetch(`${getApiBaseUrl()}/api/novels/chapters/review/stream`, {
    method: "POST",
    headers: authHeaders({ Accept: "text/event-stream", "Content-Type": "application/json" }),
    body: JSON.stringify(aiPayload(aiSettings)),
    signal,
  });
  if (!response.ok) throw new Error(await readHttpErrorMessage(response));
  if (!response.body) throw new Error("审查失败：响应为空");

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const consume = (events: StreamEvent[]) => {
    for (const event of events) {
      if (event.type === "progress") {
        handlers.onProgress(
          String(event.chapterId || ""),
          String(event.chapterTitle || ""),
          Number(event.chapterIndex || 0),
          Number(event.totalChapters || 0)
        );
      } else if (event.type === "result") {
        handlers.onResult(
          String(event.chapterId || ""),
          String(event.chapterTitle || ""),
          Array.isArray(event.issues) ? (event.issues as NovelReviewIssue[]) : []
        );
      } else if (event.type === "done") {
        handlers.onDone(Number(event.reviewedCount || 0), Number(event.issueCount || 0));
      } else if (event.type === "error") {
        handlers.onError(String(event.message || "审查失败"));
      }
    }
  };

  while (true) {
    const { done, value } = await reader.read();
    if (value) {
      buffer += decoder.decode(value, { stream: true });
      const parsed = parseSseChunk(buffer);
      buffer = parsed.rest;
      consume(parsed.events);
    }
    if (done) break;
  }
  if (buffer.trim()) consume(parseSseChunk(`${buffer}\n\n`).events);
}
