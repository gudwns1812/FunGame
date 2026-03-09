# 작업 계획: `.gitignore` 최적화 및 Git 반영

## 배경 및 목표
프로젝트 구조가 변경됨에 따라 불필요한 파일(빌드 결과물, IDE 설정, OS 임시 파일 등)이 Git에 포함되지 않도록 `.gitignore`를 상세히 작성하고, 이를 원격 저장소에 반영합니다.

## 작업 상세

### 1. `.gitignore` 파일 작성 및 수정
- **루트 (`FunGame/.gitignore`)**:
  - IDE 설정 (`.idea/`, `.vscode/`)
  - OS 파일 (`.DS_Store`, `Thumbs.db`)
  - Gradle 캐시 및 래퍼 (`.gradle/`, `!gradle/wrapper/gradle-wrapper.jar`)
- **백엔드 (`backend/.gitignore`)**:
  - 빌드 결과물 (`build/`, `out/`, `bin/`)
  - 환경 설정 파일 중 민감 정보 (`application-*.yml`, `application-*.properties`)
- **프론트엔드 (`frontend/.gitignore`)**:
  - 의존성 패키지 (`node_modules/`)
  - 빌드 결과물 (`dist/`)
  - 환경 변수 파일 (`.env`, `.env.local`, `.env.*.local`)

### 2. 기존 추적 파일 정리
- 만약 이미 `.idea/`나 `node_modules/` 등이 Git에 인덱싱되어 있다면, `git rm -r --cached` 명령을 통해 인덱스에서 제거합니다.

### 3. Git 커밋 및 푸시
- **커밋 메시지 규칙 준수**: `chore: [common] .gitignore 최적화 및 프로젝트 구조 반영`
- 원격 저장소(`origin`)로 푸시합니다.

## 예상 결과
- 불필요한 파일이 제외된 깔끔한 저장소 상태 유지.
- 향후 빌드 시 생성되는 파일들이 Git에 올라가지 않음.

---
위 계획에 대해 리뷰 부탁드립니다. 승인해 주시면 구현을 시작하겠습니다.
