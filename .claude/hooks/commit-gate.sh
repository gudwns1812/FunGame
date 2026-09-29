#!/usr/bin/env bash
set -uo pipefail

payload=$(cat)

case "$payload" in
  *'git commit'*) ;;
  *) exit 0 ;;
esac

cmd=$(printf '%s' "$payload" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{process.stdout.write(JSON.parse(s).tool_input?.command??"")}catch{}})' 2>/dev/null)

case "$cmd" in
  *'git commit'*) ;;
  *) exit 0 ;;
esac

case "$cmd" in
  *COMMENT_SWEEP=skip*) exit 0 ;;
esac

range=HEAD
case "$cmd" in
  *' -a'*|*'--all'*) ;;
  *) git diff --cached --quiet || range=--cached ;;
esac

if out=$(bash .claude/skills/comment-sweep/scripts/find-added-comments.sh "$range" 2>&1); then
  exit 0
fi

printf '%s\n\n' "$out" >&2
echo "comment-sweep: 이번 변경이 주석을 추가했다. 지우고 다시 커밋한다." >&2
echo "정말 남겨야 하는 주석이면 사용자에게 이유를 말하고 확인받은 뒤" >&2
echo "COMMENT_SWEEP=skip 을 앞에 붙여 커밋한다." >&2
exit 2
