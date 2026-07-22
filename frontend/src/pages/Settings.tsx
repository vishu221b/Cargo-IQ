import { useEffect, useState } from "react";
import { KeyRound, Check, Trash2, ShieldCheck, ExternalLink } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import type { ApiKeysStatus } from "@/lib/types";
import { useToast } from "@/components/ui/Toast";
import { Badge, Button, Card, Input, Spinner } from "@/components/ui/primitives";

const PROVIDER_META: Record<string, { label: string; placeholder: string; docs: string }> = {
  openai: {
    label: "OpenAI",
    placeholder: "sk-…",
    docs: "https://platform.openai.com/api-keys",
  },
  anthropic: {
    label: "Anthropic (Claude)",
    placeholder: "sk-ant-…",
    docs: "https://console.anthropic.com/settings/keys",
  },
};

export default function Settings() {
  const toast = useToast();
  const [status, setStatus] = useState<ApiKeysStatus | null>(null);

  async function load() {
    try {
      setStatus(await api.getApiKeys());
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to load settings");
      setStatus({ supported: [], configured: [] });
    }
  }

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="max-w-2xl space-y-7">
      <header>
        <h1 className="text-3xl font-semibold tracking-tight text-fg">Settings</h1>
        <p className="mt-1 text-sm text-muted">
          Bring your own LLM keys to run real models on your account. Keys are{" "}
          <span className="text-fg">encrypted at rest</span> and never shown again after saving.
        </p>
      </header>

      <section className="space-y-3">
        <div className="flex items-center gap-2 text-sm font-medium text-fg">
          <KeyRound className="h-4 w-4 text-accent-glow" /> LLM API keys
        </div>

        {status === null ? (
          <div className="grid place-items-center py-12">
            <Spinner className="h-6 w-6" />
          </div>
        ) : (
          <div className="space-y-3">
            {status.supported.map((provider) => (
              <ProviderKeyCard
                key={provider}
                provider={provider}
                configured={status.configured.includes(provider)}
                onChanged={load}
              />
            ))}
          </div>
        )}

        <Card className="flex items-start gap-3 p-4 text-sm text-muted">
          <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-300" />
          <p>
            Your keys are stored encrypted (AES-GCM) and scoped to your account. When you pick a
            provider in <span className="text-fg">Ask the corpus</span>, the answer runs on your key.
            The built-in <span className="text-fg">mock</span> model and local{" "}
            <span className="text-fg">Ollama</span> need no key.
          </p>
        </Card>
      </section>
    </div>
  );
}

function ProviderKeyCard({
  provider,
  configured,
  onChanged,
}: {
  provider: string;
  configured: boolean;
  onChanged: () => void;
}) {
  const toast = useToast();
  const meta = PROVIDER_META[provider] ?? { label: provider, placeholder: "API key", docs: "" };
  const [value, setValue] = useState("");
  const [busy, setBusy] = useState(false);

  async function save() {
    if (!value.trim()) return;
    setBusy(true);
    try {
      await api.setApiKey(provider, value.trim());
      setValue("");
      toast.success(`${meta.label} key saved.`);
      onChanged();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to save key");
    } finally {
      setBusy(false);
    }
  }

  async function remove() {
    if (!confirm(`Remove your ${meta.label} key?`)) return;
    try {
      await api.deleteApiKey(provider);
      toast.success(`${meta.label} key removed.`);
      onChanged();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Failed to remove key");
    }
  }

  return (
    <Card className="p-4">
      <div className="mb-3 flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <h3 className="font-medium text-fg">{meta.label}</h3>
          {configured ? (
            <Badge tone="emerald">
              <Check className="h-3 w-3" /> Configured
            </Badge>
          ) : (
            <Badge>Not set</Badge>
          )}
        </div>
        {meta.docs && (
          <a
            href={meta.docs}
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1 text-xs text-faint transition hover:text-fg"
          >
            Get a key <ExternalLink className="h-3 w-3" />
          </a>
        )}
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <div className="min-w-0 flex-1">
          <Input
            type="password"
            autoComplete="off"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && save()}
            placeholder={configured ? "Enter a new key to replace…" : meta.placeholder}
          />
        </div>
        <Button onClick={save} loading={busy} disabled={!value.trim()}>
          {configured ? "Replace" : "Save"}
        </Button>
        {configured && (
          <button
            onClick={remove}
            className="rounded-lg p-2.5 text-faint transition hover:bg-rose-500/10 hover:text-rose-300"
            title="Remove key"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        )}
      </div>
    </Card>
  );
}
