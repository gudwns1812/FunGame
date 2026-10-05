#!/usr/bin/env bash
set -uo pipefail

RANGE="${1:-HEAD}"

MAX_FIELDS="${MAX_FIELDS:-7}"
MAX_CTOR_ARGS="${MAX_CTOR_ARGS:-4}"

# 클래스의 폭은 파일 전체의 성질이라 바뀐 줄이 아니라 건드린 파일을 통째로 본다.
# 한 줄만 고쳐도 그 클래스가 이미 넓으면 그때가 볼 때다.
changed_files() {
  git diff --name-only "$RANGE" -- '*.java'
  git ls-files --others --exclude-standard -- '*.java'
}

report=$(
  changed_files | sort -u | while IFS= read -r file; do
    [ -f "$file" ] || continue
    case "$file" in
      */src/test/*) continue ;;
    esac

    # 스프링이 만들어 주는 빈만 본다. 엔티티와 값 객체의 필드는 협력자가 아니라 데이터라
    # 개수가 많아도 쪼갤 대상이 아니다.
    grep -qE '^@(Component|Service|Repository|Configuration|RestController|Controller|ControllerAdvice)' \
      "$file" || continue

    awk -v cls="$(basename "$file" .java)" -v file="$file" \
        -v max_fields="$MAX_FIELDS" -v max_args="$MAX_CTOR_ARGS" '
      { src = src "\n" $0 }

      END {
        lines = split(src, line, "\n")
        for (i = 1; i <= lines; i++) {
          l = line[i]

          # 인스턴스 필드: 들여쓴 private 선언 중 static 이 아니고 괄호가 없는 것.
          # 괄호를 거르면 메서드와 중첩 record 가 함께 빠진다.
          if (l ~ /^[ \t]+private[ \t]/ && l !~ /(^|[ \t])static[ \t]/ && l ~ /;[ \t]*$/ && l !~ /\(/) {
            fields++
          }
        }

        args = constructor_args(src, cls)

        if (fields > max_fields || args > max_args) {
          printf "%s: 인스턴스 필드 %d, 생성자 인자 %d\n", file, fields, args
          exit 1
        }
      }

      # 선언만 센다. new Cls( 같은 호출은 접근 제어자가 앞에 없어 걸리지 않는다.
      function constructor_args(text, name,   pattern, at, depth, j, c, count, seen) {
        pattern = "(public|protected|private)[ \t]+" name "[ \t]*\\("

        if (!match(text, pattern)) {
          return 0
        }

        at = RSTART + RLENGTH - 1
        depth = 0
        count = 0
        seen = 0

        for (j = at; j <= length(text); j++) {
          c = substr(text, j, 1)

          if (c == "(") { depth++; continue }
          if (c == ")") { depth--; if (depth == 0) break; continue }
          if (depth == 1) {
            if (c == ",") count++
            if (c ~ /[A-Za-z0-9_]/) seen = 1
          }
        }

        return seen ? count + 1 : 0
      }
    ' "$file"
  done
)

if [ -n "$report" ]; then
  printf '%s\n' "$report"
  printf '\n기준: 인스턴스 필드 %d 개 초과 또는 생성자 인자 %d 개 초과.\n' "$MAX_FIELDS" "$MAX_CTOR_ARGS"
  echo "넓은 클래스다. 쪼갤 수 있는지 보고, 쪼개지 않기로 했으면 그 이유를 사용자에게 말한다."
  exit 1
fi

echo "넓은 클래스 없음."
exit 0
