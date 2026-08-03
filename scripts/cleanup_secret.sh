#!/usr/bin/env bash
set -euo pipefail

# cleanup_secret.sh
# Safely removes a leaked secret from the repository history and force-pushes the cleaned history.
# IMPORTANT: Do NOT hardcode secrets in this script. Provide the secret via the SECRET environment variable
# or as the first argument when running the script.

# Usage examples:
# SECRET="sk-..." ./scripts/cleanup_secret.sh
# or
# ./scripts/cleanup_secret.sh "sk-..."

SECRET="${1:-${SECRET:-}}"
if [[ -z "$SECRET" ]]; then
  echo "ERROR: Provide the secret as first arg or set SECRET environment variable."
  exit 1
fi

REPO_SSH="git@github.com:SwetamKumar/Ai_Resume_Analyzer.git"
TMP_DIR="$(mktemp -d)"
echo "Working in $TMP_DIR"
cd "$TMP_DIR"

# 1) Mirror clone
echo "Cloning mirror..."
git clone --mirror "$REPO_SSH" repo.git
cd repo.git

# 2) Prepare replacements.txt using exact secret (left) => replacement (right)
REPLACEMENT="REDACTED_OPENROUTER_KEY"
printf '%s\n' "${SECRET}==>${REPLACEMENT}" > ../replacements.txt
echo "replacements.txt prepared (not containing the secret file in the repo)."

# 3) Ensure git-filter-repo is installed
if ! command -v git-filter-repo >/dev/null 2>&1; then
  echo "git-filter-repo not found. Install: pip install git-filter-repo"
  exit 1
fi

# 4) Run git-filter-repo (this rewrites history)
echo "Running git-filter-repo (this rewrites history)..."
git filter-repo --replace-text ../replacements.txt

# 5) Force push cleaned history
echo "Force-pushing cleaned history to origin..."
git push --force --mirror origin

# 6) Cleanup
cd ..
rm -rf repo.git ../replacements.txt

echo "Done. Repository history has been rewritten and force-pushed."

echo "IMPORTANT: After running this script, do the following:"
echo "  - Revoke/rotate the exposed OpenRouter key immediately (if you haven't already)."
echo "  - Ask all collaborators to re-clone the repository (recommended)."
echo "  - Update deployments/CI with the new key stored in environment/secret store."
