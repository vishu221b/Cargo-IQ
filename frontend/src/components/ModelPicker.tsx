import { Cpu, Check } from "lucide-react";
import {
  PROVIDERS,
  providerDef,
  saveModelChoice,
  type ModelChoice,
} from "@/lib/models";
import { Select, Input, Badge } from "@/components/ui/primitives";
import { cn } from "@/lib/utils";

const REQUIRES_TONE = {
  nothing: "emerald",
  "local Ollama": "cyan",
  "server API key": "amber",
} as const;

/**
 * Lets the user choose which model answers a query — provider + model name.
 * `mock` needs nothing; `ollama` accepts any pulled model (free text); the
 * API-key providers offer presets. The choice is persisted so it sticks.
 */
export default function ModelPicker({
  value,
  onChange,
  className,
}: {
  value: ModelChoice;
  onChange: (choice: ModelChoice) => void;
  className?: string;
}) {
  const def = providerDef(value.provider);

  function update(next: ModelChoice) {
    saveModelChoice(next);
    onChange(next);
  }

  function setProvider(id: string) {
    const d = providerDef(id);
    update({ provider: id, model: d.models[0] ?? "" });
  }

  return (
    <div className={cn("space-y-2.5", className)}>
      <div className="flex items-center gap-2 text-xs font-medium uppercase tracking-wider text-faint">
        <Cpu className="h-3.5 w-3.5" /> Model
        <Badge tone={REQUIRES_TONE[def.requires]} className="ml-auto normal-case">
          {def.requires === "nothing" ? (
            <>
              <Check className="h-3 w-3" /> no setup
            </>
          ) : (
            def.requires
          )}
        </Badge>
      </div>

      <div className="grid grid-cols-2 gap-2">
        <Select
          value={value.provider}
          onChange={(e) => setProvider(e.target.value)}
          aria-label="Provider"
        >
          {PROVIDERS.map((p) => (
            <option key={p.id} value={p.id}>
              {p.label}
            </option>
          ))}
        </Select>

        {def.customModel ? (
          <>
            <Input
              list={`models-${def.id}`}
              value={value.model}
              onChange={(e) => update({ provider: value.provider, model: e.target.value })}
              placeholder="model name"
              aria-label="Model"
            />
            <datalist id={`models-${def.id}`}>
              {def.models.map((m) => (
                <option key={m} value={m} />
              ))}
            </datalist>
          </>
        ) : (
          <Select
            value={value.model}
            onChange={(e) => update({ provider: value.provider, model: e.target.value })}
            aria-label="Model"
            disabled={def.models.length <= 1}
          >
            {def.models.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </Select>
        )}
      </div>

      <p className="text-xs leading-relaxed text-faint">{def.hint}</p>
    </div>
  );
}
