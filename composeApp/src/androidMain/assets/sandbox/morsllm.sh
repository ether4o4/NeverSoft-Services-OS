#!/usr/bin/env bash
# morsllm: local GGUF runtime for MorsVitaEst on Android (Alpine + proot sandbox).
# Builds llama.cpp's llama-server CPU-only, resolves and downloads GGUF models
# from Hugging Face, and serves an OpenAI-compatible API on loopback:8080.
#
# Subcommands emit a single JSON object on stdout (success or error). Progress
# and human-readable logs go to stderr.

set -eu
set -o pipefail

ROOT="${MORSLLM_ROOT:-/root/.morsvitaest/llm}"
BIN_DIR="$ROOT/bin"
MODELS_DIR="$ROOT/models"
BUILD_DIR="$ROOT/build"
RUN_DIR="$ROOT/run"
LOGS_DIR="$ROOT/logs"
LLAMA_SERVER="$BIN_DIR/llama-server"
PID_FILE="$RUN_DIR/server.pid"
META_FILE="$RUN_DIR/server.json"
DEFAULT_PORT=8080
DEFAULT_QUANT_PREFERENCE="Q4_K_M"
LLAMA_CPP_REPO="https://github.com/ggerganov/llama.cpp"

mkdir -p "$BIN_DIR" "$MODELS_DIR" "$BUILD_DIR" "$RUN_DIR" "$LOGS_DIR"

log()  { printf '[%s] %s\n' "$(date -u +%H:%M:%S)" "$*" >&2; }
emit() { printf '%s\n' "$*"; }

# GGUF files start with the four ASCII bytes "GGUF". Rejects HTML error pages
# and Git LFS pointer stubs that otherwise sit around looking model-shaped.
is_gguf() {
    local f="$1"
    [ -s "$f" ] || return 1
    local first4
    first4=$(head -c 4 "$f" 2>/dev/null || true)
    [ "$first4" = "GGUF" ]
}

require_jq() {
    command -v jq >/dev/null 2>&1 || {
        emit '{"ok":false,"error":"jq_missing","hint":"apk add jq"}'
        exit 1
    }
}

cmd_provision() {
    # Catch-all so an unexpected exit (set -e + pipefail tripping on something
    # we didn't anticipate, OOM kill, etc.) doesn't leave the UI staring at
    # `provision_unparseable`. provision_emitted is flipped to 1 immediately
    # after every explicit emit below; the trap only fires when no emit ran.
    provision_emitted=0
    trap '[ "${provision_emitted:-0}" = "1" ] || emit "{\"ok\":false,\"error\":\"provision_crashed\",\"detail\":\"line ${LINENO}\"}"' EXIT

    log "provision: refreshing alpine package index"
    apk_log="$LOGS_DIR/apk.log"
    mkdir -p "$LOGS_DIR"
    if ! apk update --quiet >"$apk_log" 2>&1; then
        detail=$(tail -n 1 "$apk_log" 2>/dev/null | tr -d '"\\' | head -c 200)
        emit "{\"ok\":false,\"error\":\"apk_update_failed\",\"detail\":\"$detail\",\"log_path\":\"$apk_log\",\"hint\":\"Network or apk repo unreachable. Test in terminal: ping -c1 dl-cdn.alpinelinux.org\"}"
        provision_emitted=1
        return 1
    fi

    # Reconcile any partially-installed packages from a prior aborted attempt.
    # Idempotent on a clean db; cheap to always run.
    log "provision: reconciling apk db (fix)"
    apk fix --quiet >"$apk_log" 2>&1 || true

    log "provision: installing build deps (build-base cmake git curl jq)"
    log "provision: clearing potential rename targets and stale apk temp files"
    rm -f /usr/bin/g++ /usr/bin/gcc /usr/bin/cc /usr/bin/c++ \
          /usr/bin/cpp /usr/bin/ar /usr/bin/as /usr/bin/ld 2>/dev/null || true
    find /usr -name ".apk.*" -type f -delete 2>/dev/null || true
    find /lib -name ".apk.*" -type f -delete 2>/dev/null || true

    # Up to 3 attempts: proot's renameat() emulation is occasionally flaky
    # and a fresh attempt after parsing the failing path from the error and
    # clearing it usually succeeds.
    apk_ok=0
    for attempt in 1 2 3; do
        if apk add --quiet build-base cmake git curl jq >"$apk_log" 2>&1; then
            apk_ok=1
            break
        fi
        log "provision: apk attempt $attempt failed; clearing failing target and retrying"
        # Parse the failing destination from apk's error and clear it. The
        # `|| true` here is load-bearing: if apk's error doesn't include the
        # word "rename" (a different failure mode entirely), grep exits 1 and
        # `set -o pipefail` would otherwise propagate that and `set -e` would
        # exit the script before we can emit a JSON failure.
        {
            grep -oE "rename [^ ]+ to /?[^ ]+" "$apk_log" 2>/dev/null \
                | awk '{print $NF}' \
                | while read -r p; do
                    case "$p" in
                        /*) rm -f "$p" 2>/dev/null ;;
                        *) rm -f "/$p" 2>/dev/null ;;
                    esac
                done
        } || true
        find /usr -name ".apk.*" -type f -delete 2>/dev/null || true
        find /lib -name ".apk.*" -type f -delete 2>/dev/null || true
        # Also try `apk fix` between retries in case the failure left the db
        # in a state where the next install can't proceed.
        apk fix --quiet >>"$apk_log" 2>&1 || true
        sleep 1
    done
    if [ "$apk_ok" != "1" ]; then
        detail=$(tail -n 1 "$apk_log" 2>/dev/null | tr -d '"\\' | head -c 200)
        manual_extract='cd /tmp && apk fetch binutils gcc g++ && tar -xzf binutils-*.apk -C / && tar -xzf gcc-*.apk -C / && tar -xzf g++-*.apk -C / && g++ --version'
        emit "{\"ok\":false,\"error\":\"apk_install_failed\",\"detail\":\"$detail\",\"log_path\":\"$apk_log\",\"hint\":\"proot rename failed 3x. Bypass with manual tar extract: $manual_extract\",\"attempts\":3}"
        provision_emitted=1
        return 1
    fi

    if [ -x "$LLAMA_SERVER" ]; then
        log "provision: llama-server already built at $LLAMA_SERVER"
        emit "{\"ok\":true,\"already_built\":true,\"path\":\"$LLAMA_SERVER\"}"
        provision_emitted=1
        return 0
    fi

    local src="$BUILD_DIR/llama.cpp"
    if [ ! -d "$src/.git" ]; then
        log "provision: cloning llama.cpp (shallow)"
        rm -rf "$src"
        git clone --depth=1 "$LLAMA_CPP_REPO" "$src" >&2 || {
            emit '{"ok":false,"error":"clone_failed"}'
            provision_emitted=1
            return 1
        }
    else
        log "provision: reusing existing llama.cpp checkout"
    fi

    log "provision: configuring cmake (CPU only, Release)"
    cmake -S "$src" -B "$src/build" \
        -DCMAKE_BUILD_TYPE=Release \
        -DLLAMA_BUILD_TESTS=OFF \
        -DLLAMA_BUILD_EXAMPLES=OFF \
        -DLLAMA_BUILD_SERVER=ON \
        -DGGML_OPENMP=OFF >&2 || {
        emit '{"ok":false,"error":"cmake_configure_failed"}'
        provision_emitted=1
        return 1
    }

    log "provision: building llama-server (this is the slow step)"
    cmake --build "$src/build" --target llama-server -j 2 >&2 || {
        emit '{"ok":false,"error":"cmake_build_failed"}'
        provision_emitted=1
        return 1
    }

    local built=""
    for candidate in \
        "$src/build/bin/llama-server" \
        "$src/build/server/llama-server" \
        "$src/build/llama-server"; do
        if [ -f "$candidate" ]; then built="$candidate"; break; fi
    done
    if [ -z "$built" ]; then
        emit '{"ok":false,"error":"build_artifact_missing"}'
        provision_emitted=1
        return 1
    fi

    cp "$built" "$LLAMA_SERVER"
    chmod +x "$LLAMA_SERVER"
    log "provision: done -> $LLAMA_SERVER"
    emit "{\"ok\":true,\"path\":\"$LLAMA_SERVER\"}"
    provision_emitted=1
}

cmd_list_quants() {
    require_jq
    local repo="${1:-}"
    if [ -z "$repo" ]; then
        emit '{"ok":false,"error":"missing_repo"}'
        return 1
    fi
    local url="https://huggingface.co/api/models/$repo/tree/main"
    log "list-quants: querying $url"
    local body
    body=$(curl -fsSL ${HF_TOKEN:+-H "Authorization: Bearer $HF_TOKEN"} "$url" 2>/dev/null || true)
    if [ -z "$body" ]; then
        emit "{\"ok\":false,\"error\":\"hf_api_failed\",\"repo\":\"$repo\"}"
        return 1
    fi
    echo "$body" | jq --arg repo "$repo" '{
        ok: true,
        repo: $repo,
        files: [.[] | select(.type=="file" and (.path|endswith(".gguf"))) | {
            name: .path,
            size: (.size // 0),
            quant: ((.path | capture("(?<q>[A-Za-z0-9]+(_[A-Za-z0-9]+)+)\\.gguf$")) | .q // null)
        }] | sort_by(.size)
    }'
}

cmd_pull() {
    require_jq
    local target="${1:-}"
    local filter="${2:-}"
    if [ -z "$target" ]; then
        emit '{"ok":false,"error":"missing_target"}'
        return 1
    fi

    local file_url=""
    local filename=""

    if printf '%s' "$target" | grep -q '^https://.*resolve/main/.*\.gguf'; then
        file_url="$target"
        filename=$(basename "${target%%\?*}")
    else
        local quants_json
        quants_json=$(cmd_list_quants "$target" 2>/dev/null) || true
        if [ -z "$quants_json" ] || ! echo "$quants_json" | jq -e '.ok == true' >/dev/null 2>&1; then
            emit "{\"ok\":false,\"error\":\"resolve_failed\",\"repo\":\"$target\"}"
            return 1
        fi
        local chosen=""
        if [ -n "$filter" ]; then
            chosen=$(echo "$quants_json" | jq -r --arg q "$filter" \
                '.files[] | select(.quant == $q or (.name | contains($q))) | .name' | head -1)
        fi
        if [ -z "$chosen" ] || [ "$chosen" = "null" ]; then
            chosen=$(echo "$quants_json" | jq -r --arg pref "$DEFAULT_QUANT_PREFERENCE" '
                (.files | map(select(.quant == $pref)) | .[0].name) //
                (.files | map(select(.name | test("Q4_K_M"))) | .[0].name) //
                (.files | map(select(.name | test("Q4_K"))) | .[0].name) //
                (.files | .[0].name) // empty')
        fi
        if [ -z "$chosen" ] || [ "$chosen" = "null" ]; then
            emit "{\"ok\":false,\"error\":\"no_gguf_in_repo\",\"repo\":\"$target\"}"
            return 1
        fi
        filename="$chosen"
        file_url="https://huggingface.co/$target/resolve/main/$filename"
    fi

    local dest="$MODELS_DIR/$filename"
    mkdir -p "$(dirname "$dest")"
    log "pull: $file_url -> $dest"
    # -C - resumes partials; --fail returns nonzero on HTTP errors instead of
    # writing the error page into the .gguf file.
    if ! curl -L -C - --fail --silent --show-error \
        ${HF_TOKEN:+-H "Authorization: Bearer $HF_TOKEN"} \
        -o "$dest" "$file_url" >&2; then
        log "pull: curl failed"
        # Don't delete on resumable failure — caller can retry and continue.
        emit "{\"ok\":false,\"error\":\"download_failed\",\"file\":\"$filename\"}"
        return 1
    fi
    if ! is_gguf "$dest"; then
        log "pull: bad GGUF magic at $dest (HTML error page or LFS pointer)"
        rm -f "$dest"
        emit '{"ok":false,"error":"not_gguf","hint":"file did not start with GGUF magic bytes; check HF auth or model availability"}'
        return 1
    fi
    local sz
    sz=$(stat -c %s "$dest" 2>/dev/null || echo 0)
    emit "{\"ok\":true,\"file\":\"$filename\",\"path\":\"$dest\",\"size\":$sz}"
}

cmd_list_models() {
    local out="["
    local sep=""
    if [ -d "$MODELS_DIR" ]; then
        for f in "$MODELS_DIR"/*.gguf; do
            [ -e "$f" ] || continue
            local n; n=$(basename "$f")
            local s; s=$(stat -c %s "$f" 2>/dev/null || echo 0)
            out="${out}${sep}{\"name\":\"$n\",\"path\":\"$f\",\"size\":$s}"
            sep=","
        done
    fi
    out="${out}]"
    emit "{\"ok\":true,\"models\":$out}"
}

cmd_serve() {
    local model="${1:-}"
    local port="$DEFAULT_PORT"
    [ $# -gt 0 ] && shift
    while [ $# -gt 0 ]; do
        case "$1" in
            --port) port="$2"; shift 2 ;;
            *) shift ;;
        esac
    done
    if [ -z "$model" ]; then
        emit '{"ok":false,"error":"missing_model"}'
        return 1
    fi
    local model_path="$MODELS_DIR/$model"
    [ -f "$model_path" ] || model_path="$model"
    if [ ! -f "$model_path" ]; then
        emit "{\"ok\":false,\"error\":\"model_not_found\",\"model\":\"$model\"}"
        return 1
    fi
    if [ ! -x "$LLAMA_SERVER" ]; then
        emit '{"ok":false,"error":"not_provisioned","hint":"run: morsllm provision"}'
        return 1
    fi

    # Replace any prior server on this port.
    cmd_stop >/dev/null 2>&1 || true

    log "serve: launching llama-server on 127.0.0.1:$port"
    local log_file="$LOGS_DIR/server.log"
    : > "$log_file"
    nohup "$LLAMA_SERVER" \
        --host 127.0.0.1 \
        --port "$port" \
        -m "$model_path" \
        --n-gpu-layers 0 \
        >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"
    cat > "$META_FILE" <<EOF
{"pid":$pid,"port":$port,"model":"$model","model_path":"$model_path","started":"$(date -u +%FT%TZ)"}
EOF

    log "serve: waiting for /health"
    local elapsed=0
    while [ $elapsed -lt 60 ]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            log "serve: process died early; see $log_file"
            rm -f "$PID_FILE"
            emit "{\"ok\":false,\"error\":\"server_died\",\"log\":\"$log_file\"}"
            return 1
        fi
        if curl -fsS "http://127.0.0.1:$port/health" >/dev/null 2>&1; then
            log "serve: ready"
            emit "{\"ok\":true,\"pid\":$pid,\"port\":$port,\"model\":\"$model\",\"base_url\":\"http://127.0.0.1:$port/v1\"}"
            return 0
        fi
        sleep 1
        elapsed=$((elapsed + 1))
    done
    emit "{\"ok\":false,\"error\":\"health_timeout\",\"pid\":$pid,\"hint\":\"tail $log_file\"}"
    return 1
}

cmd_stop() {
    if [ -f "$PID_FILE" ]; then
        local pid; pid=$(cat "$PID_FILE" 2>/dev/null || echo "")
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
            sleep 1
            kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null || true
            log "stop: killed pid=$pid"
        fi
        rm -f "$PID_FILE" "$META_FILE"
    fi
    emit '{"ok":true}'
}

cmd_status() {
    local running=false
    local pid=""
    local port=""
    local model=""
    local base_url=""
    if [ -f "$PID_FILE" ]; then
        pid=$(cat "$PID_FILE" 2>/dev/null || echo "")
        if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
            running=true
            if [ -f "$META_FILE" ] && command -v jq >/dev/null 2>&1; then
                port=$(jq -r .port "$META_FILE" 2>/dev/null || echo "$DEFAULT_PORT")
                model=$(jq -r .model "$META_FILE" 2>/dev/null || echo "")
                base_url="http://127.0.0.1:$port/v1"
            fi
        else
            rm -f "$PID_FILE"
        fi
    fi
    local provisioned=false
    [ -x "$LLAMA_SERVER" ] && provisioned=true
    emit "{\"ok\":true,\"provisioned\":$provisioned,\"running\":$running,\"pid\":\"$pid\",\"port\":\"$port\",\"model\":\"$model\",\"base_url\":\"$base_url\"}"
}

usage() {
    cat >&2 <<EOF
morsllm — local GGUF runtime for MorsVitaEst

Usage:
  morsllm provision                       Build llama-server (one-time, slow)
  morsllm list-quants <repo>              List .gguf files in a HuggingFace repo
  morsllm pull <repo|url> [quant]         Download a GGUF (resumable). Default quant: $DEFAULT_QUANT_PREFERENCE
  morsllm list-models                     List downloaded GGUF files
  morsllm serve <filename> [--port P]     Start llama-server (default $DEFAULT_PORT)
  morsllm stop                            Stop the running server
  morsllm status                          JSON status

Storage:
  $MODELS_DIR

Server binds 127.0.0.1 only. Set HF_TOKEN for private/gated repos.
EOF
}

main() {
    local cmd="${1:-}"
    [ $# -gt 0 ] && shift
    case "$cmd" in
        provision)     cmd_provision "$@" ;;
        list-quants)   cmd_list_quants "$@" ;;
        pull|pull-url) cmd_pull "$@" ;;
        list-models)   cmd_list_models "$@" ;;
        serve)         cmd_serve "$@" ;;
        stop)          cmd_stop "$@" ;;
        status)        cmd_status "$@" ;;
        -h|--help)     usage; exit 0 ;;
        "")            usage; exit 1 ;;
        *)             usage; exit 1 ;;
    esac
}

main "$@"
