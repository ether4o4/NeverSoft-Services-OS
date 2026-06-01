#!/bin/sh
# morsllm-setup: one-shot recovery for the sandbox build toolchain.
#
# Run this in the in-app terminal if `Set up engine` fails because
# binutils / gcc / g++ couldn't be installed via apk (proot's hardlink
# and atomic-rename emulation refuses certain operations the Alpine apk
# installer relies on).
#
# What it does:
#   1. Fetches binutils, gcc, and g++ as .apk files (apk fetch, not add).
#   2. Extracts each with tar to /, then post-processes every tar
#      "can't create hardlink" error into a symlink — same target, no
#      hardlink syscall, sidesteps proot's failure mode.
#   3. Walks every triplet-prefixed binary in /usr/bin and /usr/<triplet>/bin
#      to populate the standard short shortcuts (gcc, g++, c++, ar, as,
#      ld, nm, ranlib, strip, etc.) so cmake's link step can find them.
#   4. Creates gcc-ar / gcc-ranlib / gcc-nm pointing at the plain binutils
#      tools. cmake with gcc as the C++ compiler defaults its archiver
#      to gcc-ar (for LTO); the gcc apk ships these as native names that
#      apk failed to install, so they need to be re-pointed manually.
#   5. Verifies g++, gcc, ar, and gcc-ar all execute.
#
# Idempotent — safe to re-run.

set -e

echo "morsllm-setup: fetching binutils gcc g++..."
cd /tmp
apk fetch binutils gcc g++

echo "morsllm-setup: extracting + auto-symlinking failed hardlinks..."
for apk in binutils-*.apk gcc-*.apk g++-*.apk; do
    tar -xzf "$apk" -C / 2>&1 | grep "can't create hardlink" | \
        sed "s/.*hardlink '\([^']*\)' to '\([^']*\)'.*/\2 \1/" | \
        while read -r tgt lnk; do
            [ -n "$tgt" ] && [ -n "$lnk" ] && \
                ln -sf "/$tgt" "/$lnk" 2>/dev/null || true
        done
done

echo "morsllm-setup: creating tool shortcuts from triplet binaries..."
# Try the triplet-prefixed copy in /usr/bin first, then the real binary
# under /usr/<triplet>/bin/. Whichever exists becomes the link target.
for s in g++ gcc c++ cpp ar as ld ld.bfd nm objcopy objdump ranlib strip readelf addr2line; do
    [ -e "/usr/bin/$s" ] && continue
    for src in /usr/bin/*-alpine-linux-musl-$s /usr/*-alpine-linux-musl/bin/$s; do
        if [ -e "$src" ]; then
            ln -sf "$src" "/usr/bin/$s" 2>/dev/null && break
        fi
    done
done

echo "morsllm-setup: creating gcc-* archiver wrappers (cmake LTO support)..."
for s in ar nm ranlib; do
    if [ ! -e "/usr/bin/gcc-$s" ] && [ -e "/usr/bin/$s" ]; then
        ln -sf "$s" "/usr/bin/gcc-$s"
    fi
done

echo ""
echo "morsllm-setup: verification:"
g++ --version 2>&1 | head -1
gcc --version 2>&1 | head -1
ar --version 2>&1 | head -1
gcc-ar --version 2>&1 | head -1

echo ""
echo "morsllm-setup: done. Tap Set up engine in the app, or run 'morsllm provision' to continue."
