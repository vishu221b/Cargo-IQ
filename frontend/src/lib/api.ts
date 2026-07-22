import type {
  AuthResponse,
  DocumentSummary,
  DocumentType,
  HsCode,
  IncotermDetail,
  Me,
  Overview,
  QueryResult,
} from "./types";

// In dev, VITE_API_BASE_URL is empty and requests go to /api via the Vite proxy.
// In a deployed build, point it at the API origin.
const BASE = import.meta.env.VITE_API_BASE_URL ?? "";

const TOKEN_KEY = "cargoiq.token";

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t: string) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

/** Error carrying the HTTP status so views can react (e.g. 401 -> logout). */
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  // JSON by default, but never for FormData — the browser must set the
  // multipart/form-data boundary itself.
  const isFormData = options.body instanceof FormData;
  if (options.body && !isFormData && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const token = tokenStore.get();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const res = await fetch(`${BASE}${path}`, { ...options, headers });

  if (res.status === 204) return undefined as T;

  const text = await res.text();
  const data = text ? safeJson(text) : null;

  if (!res.ok) {
    // Spring renders errors as RFC-7807 problem+json { detail, status, ... }.
    const detail =
      (data && (data.detail || data.message || data.error)) ||
      `Request failed (${res.status})`;
    throw new ApiError(res.status, detail);
  }
  return data as T;
}

function safeJson(text: string): any {
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

export const api = {
  // --- auth ---
  register: (username: string, password: string) =>
    request<unknown>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    }),

  login: (username: string, password: string) =>
    request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    }),

  me: () => request<Me>("/api/v1/auth/me"),

  // --- corpus ---
  overview: () => request<Overview>("/api/v1/overview"),

  listDocuments: (type?: DocumentType, limit = 50, offset = 0) => {
    const params = new URLSearchParams();
    if (type) params.set("type", type);
    params.set("limit", String(limit));
    params.set("offset", String(offset));
    return request<DocumentSummary[]>(`/api/v1/documents?${params}`);
  },

  ingestDocument: (input: {
    title: string;
    type: DocumentType;
    sourceUri?: string;
    text: string;
  }) =>
    request<DocumentSummary>("/api/v1/documents", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  // Multipart upload — PDF/DOCX/HTML/TXT are extracted server-side (Tika).
  // Note: no Content-Type header — the browser sets the multipart boundary.
  uploadDocument: (input: {
    file: File;
    type: DocumentType;
    title?: string;
    sourceUri?: string;
  }) => {
    const form = new FormData();
    form.append("file", input.file);
    form.append("type", input.type);
    if (input.title) form.append("title", input.title);
    if (input.sourceUri) form.append("sourceUri", input.sourceUri);
    return request<DocumentSummary>("/api/v1/documents/upload", {
      method: "POST",
      body: form,
    });
  },

  documentContent: (id: string) =>
    request<import("./types").DocumentContent>(`/api/v1/documents/${id}/content`),

  deleteDocument: (id: string) =>
    request<void>(`/api/v1/documents/${id}`, { method: "DELETE" }),

  // --- query + reference ---
  query: (input: {
    query: string;
    topK?: number;
    filterByType?: DocumentType | null;
    provider?: string;
    model?: string;
    hybrid?: boolean;
    multiQuery?: boolean;
    rerank?: boolean;
    conversationId?: string;
  }) =>
    request<QueryResult>("/api/v1/query", {
      method: "POST",
      body: JSON.stringify(input),
    }),

  // --- chat history (per user) ---
  listConversations: () =>
    request<import("./types").Conversation[]>("/api/v1/conversations"),

  getConversation: (id: string) =>
    request<import("./types").ConversationDetail>(`/api/v1/conversations/${id}`),

  createConversation: () =>
    request<import("./types").Conversation>("/api/v1/conversations", { method: "POST" }),

  deleteConversation: (id: string) =>
    request<void>(`/api/v1/conversations/${id}`, { method: "DELETE" }),

  // --- settings: per-user LLM API keys ---
  getApiKeys: () =>
    request<import("./types").ApiKeysStatus>("/api/v1/settings/api-keys"),

  setApiKey: (provider: string, apiKey: string) =>
    request<void>(`/api/v1/settings/api-keys/${encodeURIComponent(provider)}`, {
      method: "PUT",
      body: JSON.stringify({ apiKey }),
    }),

  deleteApiKey: (provider: string) =>
    request<void>(`/api/v1/settings/api-keys/${encodeURIComponent(provider)}`, {
      method: "DELETE",
    }),

  incoterm: (code: string) =>
    request<IncotermDetail>(`/api/v1/incoterms/${encodeURIComponent(code)}`),

  hsByCode: (code: string) =>
    request<HsCode>(`/api/v1/hs-codes/${encodeURIComponent(code)}`),

  hsSearch: (q: string, limit = 10) =>
    request<HsCode[]>(
      `/api/v1/hs-codes/search?q=${encodeURIComponent(q)}&limit=${limit}`,
    ),
};
