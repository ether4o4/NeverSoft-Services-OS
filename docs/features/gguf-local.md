# Local GGUF (llama.cpp in the sandbox)

**Last verified:** 2026-06-07

MorsVitaEst can run GGUF models from Hugging Face fully on-device, without a separate
Ollama install and without the user typing anything into a terminal. It reuses two pieces
the app already has: the **Linux sandbox** (Alpine + proot, see [sandbox.md](sandbox.md))
and **OpenAI-Compatible service** support.

This is a separate path from [on-device LiteRT inference](on-device-inference.md). LiteRT-LM
loads `.litertlm` files and **cannot** load GGUF; the GGUF format is llama.cpp's. So GGUF
models are served by llama.cpp's own OpenAI-compatible server, running inside the sandbox,
and consumed over loopback HTTP exactly like any remote OpenAI-compatible endpoint.

## How It Works

A bundled shell tool, `morsllm` (`androidMain/assets/sandbox/morsllm.sh`), owns the runtime:

1. **Provision** -- `apk add build-base cmake git curl jq`, shallow-clone llama.cpp, and build
   **only** the `llama-server` target (`cmake --build ... --target llama-server`). The binary is
   cached at `/root/.morsvitaest/llm/bin/llama-server`, so this one-time compile (several
   minutes on a phone CPU) never repeats unless the user resets the sandbox.
2. **Pull** -- resolve a model and download it to `/root/.morsvitaest/llm/models`. The input is
   either a Hugging Face **repo id** (the app queries the HF tree API, lists the `.gguf` quant
   files with sizes, and downloads the chosen one -- or a sensible default, preferring
   `Q4_K_M`) or a **direct `.../resolve/main/<file>.gguf` URL**. Downloads resume with
   `curl -C -` and are validated by checking the `GGUF` magic bytes, which rejects the HTML
   pages and Git-LFS pointer stubs that otherwise show up as "model file missing or too small".
3. **Serve** -- launch `llama-server --host 127.0.0.1 --port 8080 --model <file>` as a detached
   background process, then poll `GET /health` until the model is loaded. A PID file and a
   small JSON meta file track the running server.
4. **Consume** -- the app points an **OpenAI-Compatible** service instance at
   `http://127.0.0.1:8080/v1`. From there the model flows through the normal chat, tool, and
   fallback machinery with no changes to the inference layer.

`GgufServerManager` (`androidMain/.../sandbox/GgufServerManager.kt`) is the typed Kotlin layer
over the script. It installs the script into the sandbox on first use and drives each
subcommand through `SandboxController.executeCommand` on the `SYSTEM` shell session, so a long
provision or multi-GB download never blocks a chat's own shell. JSON-returning subcommands are
run with stderr suppressed so the manager parses clean JSON.

## Behavior

- **Input formats**: HF repo id (e.g. `bartowski/Qwen2.5-0.5B-Instruct-GGUF`) with an optional
  quant filter (`Q4_K_M`, `IQ4_XS`, ...), or a full `resolve/main` GGUF URL. Gated/private repos
  work if `HF_TOKEN` is exported in the sandbox.
- **CPU inference**: the provisioned build is CPU-only (`--n-gpu-layers 0`). GPU offload is not
  attempted -- Android GPU backends for llama.cpp are not part of this path.
- **One server at a time**: starting a new model stops the previous server first. The server is
  loopback-only (`127.0.0.1`), never exposed off-device.
- **Lifecycle**: the server is an ordinary sandbox background process. Like every sandbox
  process it dies when Android reclaims the app (see sandbox.md "App backgrounding"); the next
  serve call restarts it.

## Limitations

- **Android only** -- the sandbox (and therefore this path) does not exist on iOS, desktop, or web.
- **First-run compile is slow** -- building `llama-server` on a phone CPU takes minutes and needs
  the build toolchain plus headroom in the rootfs. It is cached afterward.
- **CPU-bound throughput** -- expect small models (0.5-3B, Q4) to be usable; large models will be
  slow and memory-heavy. The performance heuristics in the LiteRT card do not apply here.
- **No automatic model selection in chat** -- the served model is reached via its OpenAI-Compatible
  service instance; the user selects that service like any other.
- **Settings UI lives in the Sandbox tab** -- a one-tap "Local Models (GGUF)" card drives the whole
  flow without the terminal (see below). The in-app Terminal flow (`morsllm provision` / `pull` /
  `serve`) still works for power users.

## Settings UI

The **Local Models (GGUF)** card (Sandbox settings tab, Android only) is the one-tap front end:

- **Engine status** -- shows whether the engine is built and, when serving, which model and loopback
  URL are live.
- **Set up engine** -- triggers the one-time `llama.cpp` build inside the sandbox, with a clear warning
  that it is slow on a phone (10-30 min).
- **Quick install** -- curated, known-working models so new users don't have to know what to type. Each
  button just fills in the repo field; the user still taps Download:
  - **Included 1B** -- an uncensored 1B (Llama 3.2 1B abliterated), ~0.8 GB, for instant on-device chat.
  - **Tool-caller 3B** -- Hermes 3 (Llama 3.2 3B), ~2 GB, low-refusal with native function calling.
  - **Uncensored 3B** -- Qwen2.5 3B abliterated, ~2 GB.
- **Manual entry** -- a Hugging Face repo id, repo URL, or direct `.gguf` URL (URLs are normalised to a
  repo id), plus an optional quant override (defaults to `Q4_K_M`).
- **Downloaded models** -- each shows its size with Run / Stop controls; only one model serves at a time.
- **Add as service** -- registers the running loopback server as an OpenAI-Compatible service instance
  and validates it, so the model shows up in the normal service/model pickers.
- **Errors** -- a failure opens a dialog with the detail, a suggested fix, and the last 8 KB of the build
  log, with one-tap Copy.

## Key Files

| File | Purpose |
| --- | --- |
| `composeApp/src/androidMain/assets/sandbox/morsllm.sh` | The runtime: provision, HF resolve (repo id + URL), resumable download with GGUF validation, serve/stop/status. JSON on stdout, progress on stderr. |
| `composeApp/src/androidMain/kotlin/com/ether4o4/morsvitaest/sandbox/GgufServerManager.kt` | Typed orchestration over the script via `SandboxController` on the `SYSTEM` session; exposes `provision`, `listQuants`, `pull`, `listModels`, `serve`, `stop`, `status`, and `openAiBaseUrl`. |
| `composeApp/src/androidMain/kotlin/com/ether4o4/morsvitaest/ui/settings/GgufModelsCard.android.kt` | The one-tap Local Models (GGUF) settings card: engine setup, curated quick-install, download, run/stop, add-as-service, error dialog. |
| `composeApp/src/commonMain/kotlin/com/ether4o4/morsvitaest/data/Service.kt` | `Service.OpenAICompatible` -- the service the served model is consumed through. |
| `composeApp/src/androidMain/kotlin/com/ether4o4/morsvitaest/sandbox/LinuxSandboxManager.kt` | Owns the rootfs the runtime builds into and the shell sessions it runs on. |
