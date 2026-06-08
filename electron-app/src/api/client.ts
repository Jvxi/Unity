import axios from "axios";
import type { ApiResponse, AnalysisResult } from "@/types";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const http = axios.create({
  baseURL: API_BASE,
  timeout: 120000,
});

// 请求拦截器 - 添加Token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = Bearer ;
  }
  return config;
});

export async function getProviders() {
  const res = await http.get("/api/providers");
  return res.data.providers;
}

export async function analyzeDll(
  file: File,
  apiKey: string,
  provider: string,
  model: string,
  onProgress?: (percent: number) => void
): Promise<AnalysisResult> {
  const formData = new FormData();
  formData.append("file", file);
  if (apiKey) formData.append("apiKey", apiKey);
  if (provider) formData.append("provider", provider);
  if (model) formData.append("model", model);

  const res = await http.post<ApiResponse<AnalysisResult>>("/api/analyze", formData, {
    headers: { "Content-Type": "multipart/form-data" },
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

export async function healthCheck(): Promise<boolean> {
  try {
    const res = await http.get("/api/health");
    return res.data.status === "ok";
  } catch {
    return false;
  }
}

export default http;