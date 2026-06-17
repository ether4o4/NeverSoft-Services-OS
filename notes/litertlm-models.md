# LiteRT-LM Local Model Notes

Generated: 2026-06-17

These notes document the local LiteRT-LM bundles used for the prompt-only
lowest-refusal profile. The actual `.litertlm` model binaries are not committed
to this repo.

## Source Machine Paths

Bundle directory:

```text
C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\downloads\litertlm
```

| Role | Model | Local path | Size | SHA256 |
| --- | --- | --- | --- | --- |
| Primary | Gemma 4 E4B Instruct | `C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\downloads\litertlm\gemma-4-E4B-it.litertlm` | 3,659,530,240 bytes / 3.41 GiB | `0B2A8980CE155FD97673D8E820B4D29D9C7D99B8FA6806F425D969B145BD52E0` |
| Fast fallback | Gemma 4 E2B Instruct | `C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\downloads\litertlm\gemma-4-E2B-it.litertlm` | 2,588,147,712 bytes / 2.41 GiB | `181938105E0EEFD105961417E8DA75903EACDA102C4FCE9CE90F50B97139A63C` |
| Smoke test/router | Qwen3 0.6B | `C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\downloads\litertlm\Qwen3-0.6B.litertlm` | 614,236,160 bytes / 0.57 GiB | `555579FF2F4FD13379ABE69C1C3AB5200F7338BC92471557F1D6614A6E5AB0B4` |

## Prompt-Only Low-Refusal Profile

Current goal: get as close as possible to an abliterated-model feel using
prompting and runtime settings only. Do not depend on abliterated weights.

Best local default:

```text
Gemma 4 E4B Instruct
```

Profile files in the source workspace:

```text
C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\profiles\lowest_refusal.litertlm.json
C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\prompts\lowest_refusal_system.md
C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\scripts\use_lowest_refusal_profile.ps1
```

Prepare the runtime settings:

```powershell
.\scripts\use_lowest_refusal_profile.ps1 -Role primary -VerifyHash
```

Generated runtime artifacts:

```text
C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\artifacts\runtime\lowest_refusal.primary.env
C:\Users\redma\OneDrive\Documents\mlabonnegem34Bitablit-chasehuihuigem3ne4bablit\artifacts\runtime\lowest_refusal.primary.settings.json
```

Runtime defaults:

```yaml
model: gemma-4-E4B-it.litertlm
context_tokens: 32768
max_output_tokens: 4096
temperature: 0.72
top_p: 0.95
top_k: 64
repetition_penalty: 1.05
```

## GitHub Storage Note

Do not commit the `.litertlm` binaries to this app repo. They are too large for
normal GitHub storage and would make the repo heavy. Keep this file as a notes
section only.
