# 회원가입 및 로그인 시스템 구축 TODO 리스트

## Phase 1: 백엔드 인프라 및 DB 설계
- [x] `backend/build.gradle`에 Spring Security 및 Session JDBC 의존성 추가
- [x] `Member` 엔티티 설계 (loginId, password, nickname, role 등)
- [x] `Role` Enum 클래스 생성 (MASTER, ADMIN, USER)
- [x] `MemberRepository` 인터페이스 및 JPA 설정
- [x] Spring Session JDBC 테이블 생성을 위한 설정 및 스키마 확인 (`application.yml`)

## Phase 2: Spring Security 및 인증 비즈니스 로직
- [x] `SecurityConfig` 클래스 구현 (BCrypt 설정, 세션 관리, API 접근 제어)
- [x] `UserDetailsService` 구현 (DB 연동 사용자 정보 로드)
- [x] `AuthService` 구현 (회원가입 로직, 로그인 처리, 비밀번호 암호화)
- [x] `AuthController` 구현 (Signup, Login, Logout, Me API)
- [x] 인증 관련 커스텀 예외 정의 (`ErrorType`, `ErrorCode` 추가)

## Phase 3: 기존 인증 로직 리팩토링 (백엔드)
- [x] `PlayerInterceptor` 클래스 제거 및 `WebConfig`에서 등록 해제
- [x] `NickNameDecodeResolver` 수정 또는 제거 (SecurityContext 활용 방식으로 변경)
- [x] 기존 컨트롤러(방 생성, 입장 등)에서 `playerName` 헤더 의존성 제거

## Phase 4: 프론트엔드 인증 페이지 및 로직
- [x] `SignupPage.tsx` 구현 (닉네임, 아이디, 비밀번호 필드 및 유효성 검사)
- [x] `LoginPage.tsx` 구현 (로그인 연동 및 에러 처리)
- [x] `useAuth` 훅 또는 Auth Context 구현 (로그인 상태 전역 관리)
- [x] `App.tsx` 라우팅 수정 및 Route Guard(인증 가드) 적용

## Phase 5: 통합 및 검증
- [x] Axios Interceptor 수정 (401 에러 발생 시 로그인 페이지 리다이렉트)
- [x] 기존 API 호출 시 `playerName` 헤더 전송 로직 제거
- [x] 회원가입 -> 로그인 -> 로비 -> 게임 진행 전체 플로우 통합 테스트
- [x] 권한(Role)별 접근 제어 동작 검증 (MASTER, ADMIN, USER)
