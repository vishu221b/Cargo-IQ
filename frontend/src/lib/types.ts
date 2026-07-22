// Mirrors of the cargo-iq REST DTOs. Kept in one place so the API client and
// the views share a single source of truth for the wire shapes.

export type Role = "USER" | "ADMIN";

export const DOCUMENT_TYPES = [
  "BILL_OF_LADING",
  "SEA_WAYBILL",
  "COMMERCIAL_INVOICE",
  "PACKING_LIST",
  "LETTER_OF_CREDIT",
  "CERTIFICATE_OF_ORIGIN",
  "CHARTER_PARTY",
  "REFERENCE",
  "OTHER",
] as const;
export type DocumentType = (typeof DOCUMENT_TYPES)[number];

export const INCOTERMS = [
  "EXW", "FCA", "CPT", "CIP", "DAP", "DPU", "DDP",
  "FAS", "FOB", "CFR", "CIF",
] as const;
export type Incoterm = (typeof INCOTERMS)[number];

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  roles: Role[];
}

export interface Me {
  username: string;
  userId: string;
  roles: Role[];
}

export interface DocumentMetadata {
  vesselName: string | null;
  blNumber: string | null;
  portOfLoading: string | null;
  portOfDischarge: string | null;
  incoterm: Incoterm | null;
  invoiceValue: number | null;
  currency: string | null;
  issueDate: string | null;
  shipper: string | null;
  consignee: string | null;
}

export interface DocumentSummary {
  id: string;
  title: string;
  type: DocumentType;
  sourceUri: string | null;
  ingestedAt: string;
  chunkCount: number;
  metadata: DocumentMetadata;
}

export interface DocumentContent {
  id: string;
  title: string;
  type: DocumentType;
  content: string;
}

export interface Citation {
  documentId: string;
  chunkId: string;
  documentTitle: string;
  chunkSequence: number;
  snippet: string;
  score: number;
}

export interface QueryResult {
  answer: string;
  grounded: boolean;
  retrievalStrategy: string;
  conversationId: string | null;
  citations: Citation[];
}

export interface RetrievalOptions {
  hybrid: boolean;
  multiQuery: boolean;
  rerank: boolean;
}

export interface Conversation {
  id: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
}

export interface ConversationMessage {
  role: "USER" | "ASSISTANT";
  content: string;
  createdAt: string;
}

export interface ConversationDetail {
  id: string;
  title: string | null;
  createdAt: string;
  updatedAt: string;
  messages: ConversationMessage[];
}

export interface Overview {
  totalDocuments: number;
  documentsByType: Record<string, number>;
  documentsByIncoterm: Record<string, number>;
}

export interface IncotermDetail {
  rule: Incoterm;
  summary: string;
  sellerObligations: string;
  buyerObligations: string;
  riskTransfer: string;
  costTransfer: string;
  typicalUseCase: string;
}

export interface HsCode {
  code: string;
  description: string;
  chapter: string;
}

export interface ApiKeysStatus {
  supported: string[];
  configured: string[];
}
