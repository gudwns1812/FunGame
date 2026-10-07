#!/usr/bin/env bash
set -uo pipefail

payload=$(cat)

case "$payload" in
  *commit*) ;;
  *) exit 0 ;;
esac

cmd=$(printf '%s' "$payload" | node -e 'let s="";process.stdin.on("data",d=>s+=d).on("end",()=>{try{process.stdout.write(JSON.parse(s).tool_input?.command??"")}catch{}})' 2>/dev/null)

env_prefix='([A-Za-z_][A-Za-z0-9_]*=[^[:space:]]*[[:space:]]+)*'
git_options='([[:space:]]+(-[a-zA-Z]([[:space:]]+[^[:space:]]+)?|--[a-zA-Z-]+(=[^[:space:]]+)?))*'
git_subcommand="(^|[;&|(]|&&|\\|\\|)[[:space:]]*${env_prefix}git${git_options}[[:space:]]+commit([[:space:]]|$)"

if ! printf '%s' "$cmd" | grep -Eq "$git_subcommand"; then
  exit 0
fi


range=HEAD
case "$cmd" in
  *' -a'*|*'--all'*) ;;
  *) git diff --cached --quiet || range=--cached ;;
esac

fail=0

case "$cmd" in
  *COMMENT_SWEEP=skip*) ;;
  *)
    if ! out=$(bash .claude/skills/comment-sweep/scripts/find-added-comments.sh "$range" 2>&1); then
      printf '%s\n\n' "$out" >&2
      echo "comment-sweep: 이번 변경이 주석을 추가했다. 지우고 다시 커밋한다." >&2
      echo "정말 남겨야 하는 주석이면 사용자에게 이유를 말하고 확인받은 뒤" >&2
      echo "COMMENT_SWEEP=skip 을 앞에 붙여 커밋한다." >&2
      fail=1
    fi
    ;;
esac

case "$cmd" in
  *INLINE_FQN=skip*) ;;
  *)
    if ! out=$(bash .claude/skills/no-inline-fqn/scripts/find-inline-fqn.sh "$range" 2>&1); then
      printf '%s\n\n' "$out" >&2
      echo "no-inline-fqn: 정규화된 이름을 코드 안에 그대로 썼다. import 로 바꾼다." >&2
      echo "같은 이름이 충돌해 어쩔 수 없다면 사용자에게 이유를 말하고 확인받은 뒤" >&2
      echo "INLINE_FQN=skip 을 앞에 붙여 커밋한다." >&2
      fail=1
    fi
    ;;
esac

case "$cmd" in
  *WIDE_CLASS=skip*) ;;
  *)
    if ! out=$(bash .claude/skills/split-wide-class/scripts/find-wide-classes.sh "$range" 2>&1); then
      printf '%s

' "$out" >&2
      echo "split-wide-class: 협력자를 너무 많이 든 클래스가 있다. 쪼갤 수 있는지 본다." >&2
      echo "쪼개지 않는 것이 맞다면 사용자에게 이유를 말하고 확인받은 뒤" >&2
      echo "WIDE_CLASS=skip 을 앞에 붙여 커밋한다." >&2
      fail=1
    fi
    ;;
esac

exit $((fail * 2))
