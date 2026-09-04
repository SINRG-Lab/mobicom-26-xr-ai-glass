#!/usr/bin/env bash
# Execute the analysis notebooks. Committed notebooks are left untouched; each is
# executed into a mirrored copy under _executed/. Figures land in the usual
# Plots/ directories.
#
#   ./run_all_notebooks.sh              # all
#   ./run_all_notebooks.sh live_ai      # only paths matching "live_ai"

set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
FILTER="${1:-}"
TIMEOUT="${NB_TIMEOUT:-1200}"

while IFS= read -r -d '' nb; do
  rel="${nb#./}"
  [[ -n "$FILTER" && "$rel" != *"$FILTER"* ]] && continue

  dest="_executed/$(dirname "$rel")"
  mkdir -p "$dest"
  echo ">>> $rel"
  jupyter nbconvert \
    --to notebook \
    --execute \
    --ExecutePreprocessor.timeout="$TIMEOUT" \
    --output-dir "$dest" \
    --output "$(basename "$rel")" \
    "$nb"
done < <(find . \
    -path ./_executed -prune -o \
    -path './.*' -prune -o \
    -name '.ipynb_checkpoints' -prune -o \
    -name '*.ipynb' -print0 | sort -z)

echo
echo "Figures written to */Plots/; executed notebooks written to _executed/."
