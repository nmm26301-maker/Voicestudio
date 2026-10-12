#!/usr/bin/env bash
set -euo pipefail
# 1.5.8 intentionally changes reference prompt/duration. Old PCM parity tests remain in Git history.
# Validate the corrected native flow instead; sampler/token parity is still in run_regression.sh.
exec bash "$(dirname "$0")/run_upstream_engine.sh" "$@"
