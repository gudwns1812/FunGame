#!/usr/bin/env bash
set -uo pipefail

RANGE="${1:-HEAD}"

{
  git diff "$RANGE" -U0 -- .

  git ls-files --others --exclude-standard -z | while IFS= read -r -d '' new_file; do
    git diff --no-index -U0 --no-color -- /dev/null "$new_file" || true
  done
} | LC_ALL=C awk '
function trimmed(s) {
  sub(/^[ \t]+/, "", s)
  return s
}
function is_comment(t) {
  return t ~ /^\/\// || t ~ /^\*/ || t ~ /^\/\*/
}
function report(reason) {
  printf "%s:%d: %s\n    -> %s\n", call_file, call_line, call_first, reason
  found++
}
function inspect(   message, args, lowered) {
  if (match(call, /"[^"]*"/)) {
    message = substr(call, RSTART, RLENGTH)
    if (message ~ /[A-Za-z]/ && message !~ /[\352-\355][\200-\277][\200-\277]/) {
      report("메시지에 한글이 없다. 한국어로 쓴다")
    }
  }

  args = call
  gsub(/"[^"]*"/, "", args)
  lowered = tolower(args)
  if (match(lowered, /e-?mail|password|token|nickname|loginid/)) {
    report("인자에 개인정보로 보이는 값(" substr(lowered, RSTART, RLENGTH) ")이 있다. 내부 id 로 바꾼다")
  }
}
function finish_call() {
  if (collecting) {
    inspect()
    collecting = 0
  }
}

/^\+\+\+ / {
  finish_call()
  file = substr($0, 7)
  skip = file !~ /\.java$/
  next
}
/^@@/ {
  finish_call()
  match($0, /\+[0-9]+/)
  line = substr($0, RSTART + 1, RLENGTH - 1) + 0
  next
}
/^\+/ {
  if (!skip) {
    raw = substr($0, 2)
    t = trimmed(raw)

    if (!collecting && !is_comment(t) && match(raw, /(^|[^A-Za-z0-9_])log\.(trace|debug|info|warn|error)\(/)) {
      collecting = 1
      call = substr(raw, RSTART)
      call_file = file
      call_line = line
      call_first = t
    } else if (collecting) {
      call = call " " t
    }

    if (collecting && call ~ /\);/) {
      finish_call()
    }
  }
  line++
  next
}
{
  finish_call()
}

END {
  finish_call()
  if (found > 0) {
    printf "\n로그 메시지 문제 %d 건. 메시지를 한국어로 쓰고, 개인정보 대신 내부 id 를 남긴다.\n", found
    exit 1
  }
  print "로그 메시지 문제 없음."
  exit 0
}
'
