#!/usr/bin/env bash
# 供应链门禁：扫描版本库内（git ls-files）的明文密钥材料。
#
# 覆盖：私钥块、keystore 密码字面量、云厂商 access key、常见 token 前缀、
# 以及 `api_key = "..."` 这类通用赋值。
#
# 误报处理：把路径写进 scripts/secrets-allowlist.txt（一行一个，支持前缀）。
# 用法：bash scripts/check-secrets.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ALLOWLIST="$ROOT/scripts/secrets-allowlist.txt"
ALLOWED=()
if [[ -f "$ALLOWLIST" ]]; then
  while IFS= read -r allowed_line; do
    [[ -z "$allowed_line" || "$allowed_line" == \#* ]] && continue
    ALLOWED+=("$allowed_line")
  done < "$ALLOWLIST"
fi

is_allowed() {
  local path="$1" allowed
  for allowed in "${ALLOWED[@]:-}"; do
    [[ -n "$allowed" && "$path" == "$allowed"* ]] && return 0
  done
  return 1
}

violations=0
pattern_ids=(
  "private-key"
  "keystore-password"
  "aws-access-key"
  "github-token"
  "google-api-key"
  "slack-token"
  "generic-secret"
)
patterns=(
  '-----BEGIN (RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----'
  "(storePassword|keyPassword)[[:space:]]*=[[:space:]]*[\"'][^\"'\$<]{6,}[\"']"
  'AKIA[0-9A-Z]{16}'
  '(ghp_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{20,})'
  'AIza[0-9A-Za-z_-]{35}'
  'xox[baprs]-[0-9A-Za-z-]{10,}'
  "(api[_-]?key|secret[_-]?key|access[_-]?token)[[:space:]]*[:=][[:space:]]*[\"'][A-Za-z0-9/+_-]{16,}[\"']"
)

while IFS= read -r file; do
  [[ -z "$file" || ! -f "$file" ]] && continue
  is_allowed "$file" && continue
  for i in "${!patterns[@]}"; do
    hits="$(grep -nEI -- "${patterns[$i]}" "$file" 2>/dev/null || true)"
    if [[ -n "$hits" ]]; then
      while IFS= read -r hit; do
        echo "VIOLATION [${pattern_ids[$i]}] $file:$hit"
        violations=$((violations + 1))
      done <<< "$hits"
    fi
  done
done < <(git ls-files)

if (( violations > 0 )); then
  echo "FAILED: $violations potential secret(s) found. Move material to local.properties / Actions Secrets, or allowlist a proven false positive."
  exit 1
fi
echo "OK: no plaintext secrets in tracked files."
