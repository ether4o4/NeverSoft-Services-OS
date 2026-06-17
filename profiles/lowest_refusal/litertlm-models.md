# LiteRT-LM Local Model Notes

Generated: 2026-06-17

These notes document the local LiteRT-LM bundles used for the prompt-only lowest-refusal profile.
The actual `.litertlm` model binaries are NOT committed to this repo (too large; see storage note).

The app downloads these same bundles via the on-device setup (see `LocalModelCatalog`). They are the
matching LiteRT community builds.

| Role | Model | Size | SHA-256 |
|---|---|---|---|
| Primary | Gemma 4 E4B Instruct | 3,659,530,240 B / 3.41 GiB | `0B2A8980CE155FD97673D8E820B4D29D9C7D99B8FA6806F425D969B145BD52E0` |
| Fast fallback | Gemma 4 E2B Instruct | 2,588,147,712 B / 2.41 GiB | `181938105E0EEFD105961417E8DA75903EACDA102C4FCE9CE90F50B97139A63C` |
| Smoke test / router | Qwen3 0.6B | 614,236,160 B / 0.57 GiB | `555579FF2F4FD13379ABE69C1C3AB5200F7338BC92471557F1D6614A6E5AB0B4` |

## Prompt-Only Low-Refusal Profile

Goal: get as close as possible to an abliterated-model feel using prompting and runtime settings
only. Does NOT depend on abliterated weights, fine-tuning, or model conversion.

Best local default: **Gemma 4 E4B Instruct**.

Profile package (this directory):
- `lowest_refusal.litertlm.json` — model priority + decoding + policy summary
- `lowest_refusal_system.md` — direct-answer system prompt (apply as the app's "soul")
- `lowest_refusal_fewshot.md` — few-shot examples (benign + partial-compliance)
- `refusal_reduction_testset.md` — over-refusal test set + scoring rubric

### Runtime decoding defaults
```
model: gemma-4-E4B-it.litertlm
context_tokens: 32768
max_output_tokens: 4096
temperature: 0.72
top_p: 0.95
top_k: 64
repetition_penalty: 1.05
```

Tuning notes: temperature 0.72 keeps answers direct without going incoherent (drop to ~0.6 if replies
wander, raise toward 0.8 for more creative flexibility). repetition_penalty 1.05 is mild — enough to
avoid loops, low enough to keep code/lists intact; do not exceed ~1.15. top_k 64 + top_p 0.95 is a
stable nucleus; tighten top_p to 0.9 on long-generation drift.

### In-app usage
The system prompt is available as a one-tap **"Direct (low-refusal)"** preset in the Agent settings
soul editor. Applying it sets the assistant's soul to the policy above; the app's constant honesty +
safety fundamentals are still appended around it (`buildChatSystemPrompt`), and the profile's own hard
limits stay intact.

## GitHub Storage Note
Do not commit the `.litertlm` binaries to this repo — they are multi-GB and would bloat it. Keep this
file as notes only.
