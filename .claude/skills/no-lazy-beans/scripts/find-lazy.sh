#!/usr/bin/env bash
set -uo pipefail

RANGE="${1:-HEAD}"

git diff "$RANGE" -U0 -- . | awk '
function trimmed(s) {
  sub(/^[ \t]+/, "", s)
  return s
}
function offends(f, raw,   t) {
  t = trimmed(raw)
  if (t ~ /^\/\// || t ~ /^\*/ || t ~ /^#/) return 0

  if (f ~ /\.(java|kt|kts|gradle)$/ && t ~ /(^|[^A-Za-z])@Lazy([^A-Za-z]|$)/) return 1
  if (f ~ /\.(yml|yaml|properties)$/ && t ~ /lazy-initialization|lazy_initialization|lazyInitialization/) return 1
  return 0
}

/^\+\+\+ b\// { file = substr($0, 7); next }
/^@@/ {
  match($0, /\+[0-9]+/)
  line = substr($0, RSTART + 1, RLENGTH - 1) + 0
  next
}
/^\+\+\+/ { next }
/^\+/ {
  text = substr($0, 2)
  if (offends(file, text)) {
    printf "%s:%d: %s\n", file, line, text
    found++
  }
  line++
}

END {
  if (found > 0) {
    printf "\n지연 생성 %d 건. 기동 시점에 없는 빈은 없는 빈이다.\n", found
    exit 1
  }
  print "지연 생성 없음."
  exit 0
}
'
