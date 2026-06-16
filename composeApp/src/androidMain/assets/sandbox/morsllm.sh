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

# Walk every triplet-prefixed binary in /usr/bin and create the short
# shortcut (e.g. /usr/bin/ar -> aarch64-alpine-linux-musl-ar) if the
# short name is missing. Idempotent. Called after every install path
# (apk add success, tar fallback, preflight) so cmake can find `ar`,
# `as`, `ld`, etc. regardless of how the binaries got there.
ensure_binutils_shortcuts() {
    # All loop vars are function-local so this never clobbers a caller's
    # variable — notably `src`, the llama.cpp source dir used by the cmake
    # step. A leaked `src` here pointed `cmake -S` at a readelf binary and
    # failed configure with "source directory ... is a file, not a directory".
    local triplet_bin base short s cand tool
    # Pass 1: walk triplet-prefixed binaries in /usr/bin and create the
    # short shortcut (e.g. /usr/bin/ar -> aarch64-alpine-linux-musl-ar).
    for triplet_bin in /usr/bin/*-alpine-linux-musl-*; do
        [ -e "$triplet_bin" ] || continue
        local base
        base=$(basename "$triplet_bin")
        local short="${base##*-alpine-linux-musl-}"
        case "$short" in
            *-[0-9]*) continue ;;
        esac
        if [ ! -e "/usr/bin/$short" ]; then
            ln -sf "$base" "/usr/bin/$short" 2>/dev/null
        fi
    done

    # Pass 2: for any standard binutils tool still missing, point it at
    # the real binary under /usr/<triplet>/bin/. Handles the case where
    # the triplet-prefixed copy in /usr/bin didn't get extracted either
    # (both names ship as hardlinks in the apk and both can fail).
    for s in ar as ld ld.bfd nm objcopy objdump ranlib strip readelf addr2line; do
        [ -e "/usr/bin/$s" ] && continue
        for cand in /usr/*-alpine-linux-musl/bin/$s; do
            if [ -e "$cand" ]; then
                ln -sf "$cand" "/usr/bin/$s" 2>/dev/null
                break
            fi
        done
    done

    # Pass 3: gcc-* archiver wrappers. cmake with gcc as the C++ compiler
    # defaults `CMAKE_<LANG>_COMPILER_AR` to `gcc-ar` (for LTO). The gcc
    # apk ships these as native names (no triplet prefix), so if the apk
    # install failed they need to be pointed at the plain binutils tools.
    # Fine for non-LTO builds, which is what we configure.
    for tool in ar ranlib nm; do
        if [ -e "/usr/bin/$tool" ] && [ ! -e "/usr/bin/gcc-$tool" ]; then
            ln -sf "$tool" "/usr/bin/gcc-$tool" 2>/dev/null
        fi
    done
}

# Fall-back installer for apk packages whose `apk add` fails under proot
# due to hardlink-translation issues (binutils, gcc, g++ etc.). Fetches the
# .apk files, extracts each with tar to /, then post-processes failed
# hardlinks into symlinks AND explicitly creates the standard short-name
# shortcuts (gcc, g++, c++, ar, as, ld, etc.) pointing at the triplet-
# prefixed binaries when the package shipped them as hardlinks that tar
# couldn't materialize.
#
# Usage: tar_extract_with_symlinks pkg1 pkg2 ...
tar_extract_with_symlinks() {
    local pkgs="$*"
    [ -z "$pkgs" ] && return 1
    cd /tmp || return 1
    rm -f *.apk
    apk fetch $pkgs || return 1
    for apk_file in $(echo "$pkgs" | tr ' ' '\n' | while read p; do ls "${p}"-*.apk 2>/dev/null | head -1; done); do
        [ -z "$apk_file" ] && continue
        echo "tar: extracting $apk_file"
        # Capture stderr separately so we can post-process the hardlink errors
        # into symlinks. Many secondary names (e.g. *-gcc-14.2.0) ship as
        # hardlinks in the apk that proot's link emulation refuses.
        tar -xzf "$apk_file" -C / 2>&1 | grep "can't create hardlink" \
            | sed "s/.*hardlink '\([^']*\)' to '\([^']*\)'.*/\2 \1/" \
            | while read -r tgt lnk; do
                [ -n "$tgt" ] && [ -n "$lnk" ] && ln -sf "/$tgt" "/$lnk" 2>/dev/null \
                    && echo "  linked /$lnk -> /$tgt"
            done
    done
    # Explicitly recreate the standard binary shortcuts. These ship as
    # hardlinks in the apk too — to the triplet-prefixed versions — but
    # because BOTH names are missing from disk at extraction time, the
    # link-target ordering means tar may silently drop the shortcut without
    # an error line we can parse. Walk every triplet-prefixed binary and
    # create the short symlink if it's missing.
    for triplet_bin in /usr/bin/*-alpine-linux-musl*-*; do
        [ -e "$triplet_bin" ] || continue
        local base
        base=$(basename "$triplet_bin")
        # Strip the leading triplet to get the short name (e.g. "g++").
        local short="${base##*-alpine-linux-musl-}"
        # Skip versioned suffixes like gcc-14.2.0 — keep only bare names.
        case "$short" in
            *-[0-9]*) continue ;;
        esac
        if [ ! -e "/usr/bin/$short" ]; then
            ln -sf "$base" "/usr/bin/$short" 2>/dev/null \
                && echo "  shortcut /usr/bin/$short -> $base"
        fi
    done
    return 0
}

cmd_provision() {
    # Catch-all so an unexpected exit (set -e + pipefail tripping on something
    # we didn't anticipate, OOM kill, etc.) doesn't leave the UI staring at
    # `provision_unparseable`. provision_emitted is flipped to 1 immediately
    # after every explicit emit below; the trap only fires when no emit ran.
    provision_emitted=0
    trap '[ "${provision_emitted:-0}" = "1" ] || emit "{\"ok\":false,\"error\":\"provision_crashed\",\"detail\":\"line ${LINENO}\"}"' EXIT

    # Fast path: try downloading the pre-built llama-server binary from our
    # release artifact. Cuts provision time from 10-30 min compile-in-proot
    # to <1 minute download + chmod. Same aarch64-linux-musl static build
    # we'd produce in-sandbox, just cross-compiled in CI and published as a
    # release asset. Falls through to source compile if download or exec
    # check fails (network down, release not built yet, arch mismatch).
    if [ ! -x "$LLAMA_SERVER" ]; then
        prebuilt_url="https://github.com/ether4o4/MorsVitaEst/releases/download/llama-server-prebuilt-latest/llama-server-aarch64-musl"
        mkdir -p "$BIN_DIR"
        log "provision: trying pre-built binary at $prebuilt_url"
        if curl -fsSL --max-time 120 -o "$LLAMA_SERVER.tmp" "$prebuilt_url" 2>/dev/null \
            && [ -s "$LLAMA_SERVER.tmp" ]; then
            chmod 755 "$LLAMA_SERVER.tmp"
            if "$LLAMA_SERVER.tmp" --version >/dev/null 2>&1; then
                mv "$LLAMA_SERVER.tmp" "$LLAMA_SERVER"
                log "provision: pre-built binary installed -> $LLAMA_SERVER"
                emit "{\"ok\":true,\"prebuilt\":true,\"path\":\"$LLAMA_SERVER\"}"
                provision_emitted=1
                return 0
            else
                log "provision: pre-built binary downloaded but exec check failed, falling back to source compile"
                rm -f "$LLAMA_SERVER.tmp"
            fi
        else
            log "provision: pre-built download failed or empty, falling back to source compile"
            rm -f "$LLAMA_SERVER.tmp"
        fi
    fi

    apk_log="$LOGS_DIR/apk.log"
    mkdir -p "$LOGS_DIR"

    # Skip the entire apk install pipeline if the build deps are already
    # present — e.g. the user (or a prior provision attempt) manually
    # extracted them with tar+symlink to bypass proot's hardlink failures.
    # Without this short-circuit, the pre-clean step below would delete
    # the user's hand-crafted symlinks and apk would loop forever trying
    # to reinstall packages that already exist on disk.
    # Try to fill in any missing short-name shortcuts (ar, as, ld, etc.)
    # from triplet-prefixed binaries before checking — handles the case
    # where a user manually installed the compilers but didn't link the
    # binutils tools.
    ensure_binutils_shortcuts

    if command -v g++ >/dev/null 2>&1 && \
       command -v gcc >/dev/null 2>&1 && \
       command -v cmake >/dev/null 2>&1 && \
       command -v git >/dev/null 2>&1 && \
       command -v curl >/dev/null 2>&1 && \
       command -v jq >/dev/null 2>&1; then
        log "provision: build deps already present (g++, cmake, git, curl, jq); skipping apk add"
        apk_ok=1
    else
        apk_ok=0
    fi

    # Everything below is gated on the preflight: if the build deps are
    # already present (apk_ok=1), skip every step that could disturb the
    # user's working install (the pre-clean rm would nuke their symlinks).
    if [ "$apk_ok" != "1" ]; then
        log "provision: refreshing alpine package index"
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
    fi

    # Up to 3 attempts: proot's renameat() emulation is occasionally flaky
    # and a fresh attempt after parsing the failing path from the error and
    # clearing it usually succeeds. Skipped entirely if the preflight passed.
    [ "$apk_ok" = "1" ] && goto_build=1 || goto_build=0
    for attempt in 1 2 3; do
        [ "$goto_build" = "1" ] && break
        if apk add --quiet build-base cmake git curl jq linux-headers musl-dev >"$apk_log" 2>&1; then
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
        log "provision: apk install failed 3x; falling back to manual tar extract + symlink"
        if ! tar_extract_with_symlinks "binutils" "gcc" "g++" >>"$apk_log" 2>&1; then
            detail=$(tail -n 1 "$apk_log" 2>/dev/null | tr -d '"\\' | head -c 200)
            manual_extract='cd /tmp && apk fetch binutils gcc g++ && tar -xzf binutils-*.apk -C / && tar -xzf gcc-*.apk -C / && tar -xzf g++-*.apk -C /'
            emit "{\"ok\":false,\"error\":\"apk_install_failed\",\"detail\":\"$detail\",\"log_path\":\"$apk_log\",\"hint\":\"apk add failed 3x AND tar fallback failed. Try in terminal: $manual_extract\",\"attempts\":3}"
            provision_emitted=1
            return 1
        fi
        # Make the script visible to the caller's PATH for the rest of provision.
        export PATH="/usr/bin:/bin:/usr/sbin:/sbin:$PATH"
        if ! command -v g++ >/dev/null 2>&1; then
            detail="manual tar extract succeeded but g++ still not on PATH after creating standard shortcuts"
            emit "{\"ok\":false,\"error\":\"manual_extract_incomplete\",\"detail\":\"$detail\",\"log_path\":\"$apk_log\"}"
            provision_emitted=1
            return 1
        fi
        log "provision: tar fallback succeeded; g++ now functional"
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

    # Surgically remove llama.cpp's web-UI build step. The `llama-ui-assets`
    # target invokes `llama-ui-embed` mid-build to bundle the web UI's
    # HTML/CSS/JS into a C++ source. Under proot the exec call fails with
    # "permission denied" regardless of chmod state — proot's exec
    # emulation rejects freshly-linked binaries in the build dir. MVE
    # never serves the web UI (it talks to llama-server via /v1), so the
    # entire UI subtree is dead weight; cutting it out skips the wall.
    if [ -f "$src/tools/CMakeLists.txt" ]; then
        sed -i.morsbak '/add_subdirectory(ui)/d' "$src/tools/CMakeLists.txt" 2>/dev/null || true
    fi
    if [ -f "$src/tools/server/CMakeLists.txt" ]; then
        # Drop any reference to the ui-assets target from server (deps,
        # link libs, includes). Server itself doesn't need UI assets at
        # runtime; it just had a build-time dep on the embed step.
        sed -i.morsbak '/llama-ui-assets/d' "$src/tools/server/CMakeLists.txt" 2>/dev/null || true
        # Also drop the `llama-ui` library token from any
        # target_link_libraries line. Even with the UI subdirectory gone,
        # tools/server/CMakeLists.txt still tells the linker to bind
        # `-lllama-ui`, which fails with "cannot find -lllama-ui" at the
        # final link step. Sed removes just that token (note the leading
        # space) so the rest of the link line stays intact.
        sed -i 's/ llama-ui\b//g' "$src/tools/server/CMakeLists.txt" 2>/dev/null || true
    fi
    # server-http.cpp unconditionally `#include "ui.h"` which used to be
    # generated by the UI build step we just deleted. Stub it out with a
    # no-op header so the include compiles and any UI-asset lookups return
    # nullptr (server just 404s on UI routes — fine, MVE never hits them).
    # Credit to whichever in-app AI session worked this out live; baking
    # it in so the next fresh sandbox doesn't need to do it manually.
    if [ -d "$src/tools/server" ] && [ ! -f "$src/tools/server/ui.h" ]; then
        cat > "$src/tools/server/ui.h" <<'UI_H_EOF'
#pragma once
#include <string>
#include <vector>
// Stub matching current llama.cpp master: server-http.cpp iterates
// llama_ui_get_assets() and reads a.name / a->type, so the struct needs
// those fields and the vector accessor. MVE never serves the web UI, so
// every accessor returns empty / nullptr (UI routes just 404).
struct llama_ui_asset {
    std::string name;
    const unsigned char * data;
    unsigned int size;
    std::string type;
    std::string etag;
};
inline std::vector<llama_ui_asset> llama_ui_get_assets() { return {}; }
inline const llama_ui_asset * llama_ui_find_asset(const char *) { return nullptr; }
UI_H_EOF
    fi

    # Ensure binutils shortcuts (ar, as, ld, nm, etc.) exist regardless
    # of how the compilers were installed — cmake's link step calls `ar`
    # by short name and silently fails with "no such file or directory"
    # if only the triplet-prefixed version exists.
    ensure_binutils_shortcuts

    cmake_log="$LOGS_DIR/cmake.log"
    # Stale build dir from a prior FAILED attempt gets wiped so cmake
    # re-runs configure cleanly with the latest UI/linker patches. If a
    # successful build already produced llama-server, leave the tree
    # intact — the cached object files mean the next provision can short-
    # circuit to "already built" without a 30-min recompile.
    if [ -d "$src/build" ] && [ ! -f "$src/build/bin/llama-server" ]; then
        log "provision: removing stale build dir from prior failed attempt"
        rm -rf "$src/build"
    fi

    log "provision: configuring cmake (CPU only, Release, static libs, no UI)"
    # BUILD_SHARED_LIBS=OFF: proot's symlink emulation rejects the SO
    # version chain (.so -> .so.0 -> .so.0.13.1). Static archives sidestep
    # it. Explicit CMAKE_AR/RANLIB/NM so cmake doesn't try to auto-detect
    # gcc-ar (which can fail-and-cache under partial installs).
    if ! cmake -S "$src" -B "$src/build" \
        -DCMAKE_BUILD_TYPE=Release \
        -DLLAMA_BUILD_TESTS=OFF \
        -DLLAMA_BUILD_EXAMPLES=OFF \
        -DLLAMA_BUILD_SERVER=ON \
        -DGGML_OPENMP=OFF \
        -DGGML_NATIVE=ON \
        -DBUILD_SHARED_LIBS=OFF \
        -DCMAKE_AR=/usr/bin/ar \
        -DCMAKE_RANLIB=/usr/bin/ranlib \
        -DCMAKE_NM=/usr/bin/nm \
        -DCMAKE_C_COMPILER_AR=/usr/bin/gcc-ar \
        -DCMAKE_CXX_COMPILER_AR=/usr/bin/gcc-ar \
        -DCMAKE_C_COMPILER_RANLIB=/usr/bin/gcc-ranlib \
        -DCMAKE_CXX_COMPILER_RANLIB=/usr/bin/gcc-ranlib >"$cmake_log" 2>&1; then
        detail=$(tail -n 1 "$cmake_log" 2>/dev/null | tr -d '"\\' | head -c 200)
        emit "{\"ok\":false,\"error\":\"cmake_configure_failed\",\"detail\":\"$detail\",\"log_path\":\"$cmake_log\",\"hint\":\"Check compiler is on PATH: g++ --version. If missing, paste the workaround command from your prior chat.\"}"
        provision_emitted=1
        return 1
    fi

    # Pre-build llama-ui-embed first and chmod +x it. llama.cpp's server
    # build target depends on the llama-ui-assets target which runs
    # llama-ui-embed as part of the build (to bundle web-UI files into a
    # C++ source). Under proot the linker output sometimes lands without
    # the executable bit set, so when cmake tries to exec the binary it
    # fails with "permission denied" mid-build. Building the binary
    # separately and force-chmodding it before the server target runs
    # avoids that race.
    log "provision: pre-building llama-ui-embed and forcing +x on build artifacts"
    cmake --build "$src/build" --target llama-ui-embed -j 2 >>"$cmake_log" 2>&1 || true
    find "$src/build" -type f \( -name "llama-*" -o -name "*-embed" \) -exec chmod 755 {} \; 2>/dev/null || true

    log "provision: building llama-server (this is the slow step, real 10-30 min)"
    if ! cmake --build "$src/build" --target llama-server -j 2 >"$cmake_log" 2>&1; then
        # Second-chance: sweep the build dir for any newly-built binary
        # that proot might have stripped +x from, then retry. If the
        # llama-server build itself produces a binary that needs to be
        # invoked downstream, this saves us another full provision cycle.
        log "provision: cmake build failed once, chmodding +x build dir and retrying"
        find "$src/build" -type f -exec sh -c 'head -c 4 "$1" 2>/dev/null | grep -q ELF && chmod 755 "$1"' _ {} \; 2>/dev/null || true
        if ! cmake --build "$src/build" --target llama-server -j 2 >>"$cmake_log" 2>&1; then
            detail=$(tail -n 1 "$cmake_log" 2>/dev/null | tr -d '"\\' | head -c 200)
            emit "{\"ok\":false,\"error\":\"cmake_build_failed\",\"detail\":\"$detail\",\"log_path\":\"$cmake_log\",\"hint\":\"Common: OOM (-j 1 instead of -j 2), missing header, or proot symlink issue. Last 8KB of cmake output is in the log.\"}"
            provision_emitted=1
            return 1
        fi
    fi

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
            emit "{\"ok\":false,\"error\":\"no_gguf_in_repo\",\"repo\":\"$target\",\"detail\":\"The HuggingFace repo '$target' does not contain any .gguf files. llama.cpp only loads GGUF format — try a repo like bartowski/Qwen2.5-0.5B-Instruct-GGUF, or convert your model to GGUF first.\",\"hint\":\"Tap 'Tiny' in the Quick install row for a known-working model, OR paste a repo whose name ends in -GGUF.\"}"
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
    # Phone CPUs are big.LITTLE: handing llama.cpp every core (including the slow
    # efficiency cluster) makes it SLOWER, not faster, because the fast cores
    # stall each layer waiting on the slow ones. Drop ~2 cores on >4-core chips,
    # floor 2. Also cap context at 4096 so the KV cache stays small on a phone.
    local ncpu threads
    ncpu=$(nproc 2>/dev/null || echo 4)
    if [ "$ncpu" -gt 4 ]; then
        threads=$((ncpu - 2))
    else
        threads="$ncpu"
    fi
    [ "$threads" -lt 2 ] && threads=2
    log "serve: using $threads of $ncpu cores, ctx 4096"
    nohup "$LLAMA_SERVER" \
        --host 127.0.0.1 \
        --port "$port" \
        -m "$model_path" \
        --n-gpu-layers 0 \
        --threads "$threads" \
        --ctx-size 4096 \
        >"$log_file" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"
    cat > "$META_FILE" <<EOF
{"pid":$pid,"port":$port,"model":"$model","model_path":"$model_path","started":"$(date -u +%FT%TZ)"}
EOF

    log "serve: waiting for /health (large models can take a few minutes to load)"
    local elapsed=0
    while [ $elapsed -lt 300 ]; do
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
    emit "{\"ok\":false,\"error\":\"health_timeout\",\"pid\":$pid,\"hint\":\"Model not ready after 5 min — it may be too large for this device's free RAM. Try a smaller model or quant. Log: $log_file\"}"
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
