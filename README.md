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
- AI 답변 레퍼런스와 출처 메타데이터 저장 및 재조회
- AI 호출 로그 저장 및 관리자 조회 API
- validation + 공통 에러 응답 구조

## RAG 연동 책임

Spring은 직접 임베딩이나 LLM 생성을 수행하지 않고, 도메인 상태와 AI Worker 호출 흐름을 관리합니다.

```txt
채팅 요청
  -> 노트북 소유권 검증
  -> 최근 대화 + summary memory 조회
  -> FastAPI에 notebookId, question, history 전달
  -> answer + reference_chunks 수신
  -> USER/AI ChatHistory 저장
  -> ChatReference 저장
  -> 필요 시 conversation memory 갱신
```

저장하는 reference metadata:

| 필드 | 설명 |
| --- | --- |
| `documentId` | 근거가 나온 문서 ID |
| `documentTitle` | 근거 문서 제목 |
| `sectionTitle` | 근거 chunk 주변 섹션 제목 |
| `pageNumber` | PDF 페이지 번호 |
| `chunkIndex` | 문서 전체 기준 chunk 순서 |
| `pageChunkIndex` | 해당 페이지 안에서의 chunk 순서 |
| `content` | 실제 근거 chunk 본문 |

## 실행

```bash
cd /Users/seochanjin/workspace/notebooklm/core-api-spring
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`application-local.yml`에는 Git에 올리면 안 되는 로컬 비밀값만 넣습니다.

예시:

```yaml
spring:
  datasource:
    password: your-local-db-password

jwt:
  secret: replace-with-a-32-byte-or-longer-local-secret-key
```

테스트:

```bash
./gradlew test
```

IntelliJ에서 직접 실행할 때는 `application-local.yml`이 자동으로 읽히지 않을 수 있습니다.
이 경우 Run Configuration의 Active profiles에 아래 값을 넣습니다.

```txt
local
```

로그에 `No active profile set`이 보이면 local profile이 적용되지 않은 상태입니다.

## 관련 문서

- 전체 문서는 `../infra-config/README.md` 와 `../infra-config/docs/architecture.md` 에서 확인할 수 있습니다.
