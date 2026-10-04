#!/usr/bin/env bash
set -uo pipefail

RANGE="${1:-HEAD}"

# 추적 중인 파일은 diff 로, 아직 추적되지 않은 새 파일은 통째로 본다.
{
  git diff "$RANGE" -U0 -- .

  git ls-files --others --exclude-standard -z | while IFS= read -r -d '' new_file; do
    git diff --no-index -U0 --no-color -- /dev/null "$new_file" || true
  done
} | awk '
function trimmed(s) {
  sub(/^[ \t]+/, "", s)
  return s
}
function is_java(f) {
  return f ~ /\.java$/
}
function strip_noise(raw,   t) {
  t = raw
  gsub(/"[^"]*"/, "", t)          # 문자열 리터럴 안의 패키지 이름은 진짜 데이터다
  sub(/\/\/.*$/, "", t)           # 줄 끝 주석
  return t
}
function is_declaration(t) {
  return t ~ /^import / || t ~ /^package /
}
function is_comment(t) {
  return t ~ /^\/\// || t ~ /^\*/ || t ~ /^\/\*/
}

/^\+\+\+ b\// {
  file = substr($0, 7)
  skip = !is_java(file)
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
    raw = substr($0, 2)
    t = trimmed(raw)

    if (!is_declaration(t) && !is_comment(t)) {
      code = strip_noise(raw)

      # 소문자 묶음 두 개 이상을 지나 대문자로 "시작하는" 조각에 닿으면 정규화된 이름이다.
      # 마지막 조각이 대문자로 시작해야 한다. 안 그러면 this.answer.equalsIgnoreCase 처럼
      # 메서드 이름 가운데 대문자를 잡는다.
      if (match(code, /[a-z][a-zA-Z0-9_]*(\.[a-z][a-zA-Z0-9_]*)+\.[A-Z][A-Za-z0-9_]*/)) {
        printf "%s:%d: %s\n", file, line, t
        found++
      }
    }
  }
  line++
}

END {
  if (found > 0) {
    printf "\n정규화된 이름 %d 건. import 로 바꾸고 커밋한다.\n", found
    exit 1
  }
  print "인라인으로 쓴 정규화된 이름 없음."
  exit 0
}
'
