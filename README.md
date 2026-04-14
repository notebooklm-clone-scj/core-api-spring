# core-api-spring

메인 비즈니스 로직과 사용자 API를 담당하는 Spring Boot 서비스입니다.

## 역할

- 사용자 회원가입 / 로그인
- 노트북 생성 및 목록 조회
- 문서 업로드 요청 수신 및 상태 관리
- 문서 기반 채팅 이력 저장
- AI Worker 호출 결과 저장
- 전역 예외 처리와 요청 검증

## 주요 책임

```txt
프론트 요청 수신
  -> 도메인 검증
  -> DB 상태 저장
  -> FastAPI 호출
  -> 결과 영속화
  -> JSON 응답 반환
```

## 포함된 기능

- Spring Security + JWT Filter 기반 인증
- Access / Refresh Token 발급과 재발급
- Redis 기반 refresh token rotation
- 문서 비동기 분석 시작점
- 최근 대화 윈도우 기반 AI 요청
- 대화 요약 메모리 저장
- AI 답변 레퍼런스 저장 및 재조회
- AI 호출 로그 저장 및 관리자 조회 API
- validation + 공통 에러 응답 구조

## 실행

```bash
cd /Users/seochanjin/workspace/notebooklm/core-api-spring
./gradlew bootRun
```

테스트:

```bash
./gradlew test
```

## 관련 문서

- 전체 문서는 `../infra-config/README.md` 와 `../infra-config/docs/architecture.md` 에서 확인할 수 있습니다.
