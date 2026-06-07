// The chat providers a user can pick per query. The backend honours `mock`
// (no setup) and `ollama` (any locally-pulled model) out of the box; the API-key
// providers work when the server was started with that AI_CHAT_PROVIDER + key.

export interface ProviderDef {
  id: string; // sent to the API as `provider`
  label: string;
  hint: string;
  customModel: boolean; // can the user type any model name?
  models: string[]; // presets / suggestions
  requires: "nothing" | "local Ollama" | "server API key";
}

export const PROVIDERS: ProviderDef[] = [
  {
    id: "mock",
    label: "Mock",
    hint: "No setup. Synthesises a grounded answer from the retrieved passages — great for testing the pipeline offline.",
    customModel: false,
    models: ["mock"],
    requires: "nothing",
  },
  {
    id: "ollama",
    label: "Ollama (local)",
    hint: "Runs fully locally. Pull a model first, e.g. `ollama pull gemma2:9b`.",
    customModel: true,
    models: ["gemma2:9b", "gemma2:2b", "llama3.1", "llama3.2", "mistral", "qwen2.5", "phi3"],
    requires: "local Ollama",
  },
  {
    id: "openai",
    label: "OpenAI",
    hint: "Requires the server started with AI_CHAT_PROVIDER=openai and an API key.",
    customModel: true,
    models: ["gpt-4o-mini", "gpt-4o", "o4-mini"],
    requires: "server API key",
  },
  {
    id: "anthropic",
    label: "Claude",
    hint: "Requires AI_CHAT_PROVIDER=anthropic and an API key.",
    customModel: true,
    models: ["claude-sonnet-4-5", "claude-haiku-4-5"],
    requires: "server API key",
  },
  {
    id: "google-genai",
    label: "Gemini",
    hint: "Requires AI_CHAT_PROVIDER=google-genai and an API key.",
    customModel: true,
    models: ["gemini-2.5-flash", "gemini-2.5-pro"],
    requires: "server API key",
  },
];

export interface ModelChoice {
  provider: string;
  model: string;
}

export const DEFAULT_CHOICE: ModelChoice = { provider: "mock", model: "mock" };

export function providerDef(id: string): ProviderDef {
  return PROVIDERS.find((p) => p.id === id) ?? PROVIDERS[0];
}

const KEY = "cargoiq.model";

export function loadModelChoice(): ModelChoice {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as ModelChoice;
      if (parsed.provider) return parsed;
    }
  } catch {
    /* ignore */
  }
  return DEFAULT_CHOICE;
}

export function saveModelChoice(choice: ModelChoice) {
  localStorage.setItem(KEY, JSON.stringify(choice));
}
