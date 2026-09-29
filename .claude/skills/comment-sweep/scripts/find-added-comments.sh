#!/usr/bin/env bash
set -uo pipefail

RANGE="${1:-HEAD}"

git diff "$RANGE" -U0 -- . | awk '
function trimmed(s) {
  sub(/^[ \t]+/, "", s)
  return s
}
function is_test(f) {
  if (f ~ /(^|\/)src\/test\//) return 1
  if (f ~ /Tests?\.(java|kt)$/) return 1
  if (f ~ /\.(test|spec)\.(js|jsx|mjs|cjs|ts|tsx)$/) return 1
  if (f ~ /(^|\/)__tests__\//) return 1
  return 0
}
function is_comment(f, raw,   t) {
  if (f !~ /\.(java|js|jsx|mjs|cjs|ts|tsx|gradle|kt|kts)$/) return 0

  t = trimmed(raw)
  if (t == "") return 0

  if (t ~ /^\/\//) return 1
  if (t ~ /^\/\*/) return 1
  if (t ~ /^\*/) return 1
  if (t ~ /^\{\/\*/) return 1
  if (t ~ /\/\// && t !~ /:\/\//) return 1
  return 0
}

/^\+\+\+ b\// {
  file = substr($0, 7)
  skip = is_test(file)
  if (skip && !(file in seen)) {
    seen[file] = 1
    skipped++
  }
  next
}
/^@@/ {
  match($0, /\+[0-9]+/)
  line = substr($0, RSTART + 1, RLENGTH - 1) + 0
  next
}
/^\+\+\+/ { next }
/^\+/ {
  if (!skip) {
    text = substr($0, 2)
    if (is_comment(file, text)) {
      printf "%s:%d: %s\n", file, line, text
      found++
    }
  }
  line++
}

END {
  if (skipped > 0) printf "테스트 파일 %d 개는 검사에서 제외했다.\n", skipped
  if (found > 0) {
    printf "\n주석 후보 %d 건. 지우고 커밋한다.\n", found
    exit 1
  }
  print "새로 추가된 주석 없음."
  exit 0
}
'
