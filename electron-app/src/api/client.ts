import axios from "axios";
import type { ApiResponse, AnalysisResult, ProviderInfo } from "@/types";

const DEFAULT_API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:38765";

export function normalizeApiBaseUrl(value?: string | null) {
  let next = (value || "").trim();
  if (!next) next = DEFAULT_API_BASE;
  if (!/^https?:\/\//i.test(next)) next = `http://${next}`;

  try {
    const url = new URL(next);
    return url.href.replace(/\/+$/, "");
  } catch {
    throw new Error("服务器地址格式不正确");
  }
}

export const API_BASE = normalizeApiBaseUrl(DEFAULT_API_BASE);

export function getApiBaseUrl() {
  return API_BASE;
}

export function normalizeAssetPath(url?: string | null) {
  if (!url) return "";
  if (/^(data:|blob:)/i.test(url)) return url;
  if (!/^https?:\/\//i.test(url)) return url;

  try {
    const parsed = new URL(url);
    if (parsed.pathname.startsWith("/uploads/")) {
      return `${parsed.pathname}${parsed.search}${parsed.hash}`;
    }
  } catch {}

  return url;
}

export function resolveAssetUrl(url?: string | null) {
  if (!url) return "";
  if (/^(https?:|data:|blob:)/i.test(url)) return url;

  const base = getApiBaseUrl();
  return url.startsWith("/") ? `${base}${url}` : `${base}/${url}`;
}

const http = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 120000,
});

// 请求拦截器 - 添加Token
http.interceptors.request.use((config) => {
  config.baseURL = getApiBaseUrl();
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && !error.response) {
      error.message = `无法连接服务器：${getApiBaseUrl()}`;
    }
    return Promise.reject(error);
  }
);

export async function getProviders(): Promise<ProviderInfo[]> {
  const res = await http.get("/api/providers");
  return Array.isArray(res.data?.providers) ? res.data.providers : [];
}

export async function analyzeDll(
  file: File,
  apiKey: string,
  provider: string,
  model: string,
  apiUrl?: string,
  onProgress?: (percent: number) => void
): Promise<AnalysisResult> {
  const formData = new FormData();
  formData.append("file", file);
  if (apiKey) formData.append("apiKey", apiKey);
  if (provider) formData.append("provider", provider);
  if (model) formData.append("model", model);
  if (apiUrl) formData.append("apiUrl", apiUrl);

  const res = await http.post<ApiResponse<AnalysisResult>>("/api/analyze", formData, {
    onUploadProgress: (e) => {
      if (e.total && onProgress) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    },
  });

  if (!res.data.success) {
    throw new Error(res.data.error || "分析失败");
  }
  return res.data.data!;
}

export async function testAiConnection(
  apiKey: string,
  provider: string,
  model: string,
  apiUrl?: string
) {
  try {
    const res = await http.post("/api/ai/test", { apiKey, provider, model, apiUrl });
    if (!res.data.success) throw new Error(res.data.error || "AI 接口连接失败");
    return res.data.message || "OK";
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const data = error.response?.data as { error?: string; message?: string } | undefined;
      throw new Error(data?.error || data?.message || error.message || "AI 接口连接失败");
    }
    throw error;
  }
}

export async function healthCheck(): Promise<boolean> {
  try {
    const res = await http.get("/api/health");
    return res.data.status === "ok";
  } catch {
    return false;
  }
}


export async function extractStrings(
  file: File,
  minLength: number = 4,
  encoding: string = "all",
  keyword?: string
) {
  const fd = new FormData();
  fd.append("file", file);
  fd.append("minLength", String(minLength));
  fd.append("encoding", encoding);
  if (keyword) fd.append("keyword", keyword);
  const res = await http.post("/api/tools/strings", fd);
  if (!res.data.success) throw new Error(res.data.error || "提取失败");
  return res.data.data;
}

export async function hexDump(
  file: File,
  offset: number = 0,
  length: number = 0,
  search?: string
) {
  const fd = new FormData();
  fd.append("file", file);
  fd.append("offset", String(offset));
  fd.append("length", String(length));
  if (search) fd.append("search", search);
  const res = await http.post("/api/tools/hex", fd);
  if (!res.data.success) throw new Error(res.data.error || "加载失败");
  return res.data.data;
}

export function getExportUrl(format: "json" | "html") {
  return getApiBaseUrl() + "/api/tools/export/" + format;
}

export default http;
