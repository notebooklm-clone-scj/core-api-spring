package core_api.domain.aicall;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// AI 호출 결과를 "요약 기록" 형태로 남기는 엔티티
// -> 모든 stacktrace를 DB에 저장하는 용도가 아니라,
// -> 관리자 화면에서 최근 실패, 평균 응답 시간, 요청 종류별 건수를 보기 위한 목적이다.
public class AiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Spring -> FastAPI -> 로그를 하나의 요청으로 묶기 위한 식별자
    @Column(nullable = false, length = 64)
    private String requestId;

    // 어떤 종류의 AI 호출이었는지 저장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AiRequestType requestType;

    // 채팅/요약이 어떤 노트북에서 발생했는지 추적하기 위한 값
    private Long notebookId;

    // PDF 분석이 어떤 문서에서 발생했는지 추적하기 위한 값
    private Long documentId;

    // 성공/실패 여부
    @Column(nullable = false)
    private boolean success;

    // 호출 완료까지 걸린 시간(ms)
    @Column(nullable = false)
    private Long latencyMs;

    // 실패 시 공통 ErrorCode의 코드값 저장
    @Column(length = 32)
    private String errorCode;

    // 실패 원인을 운영 화면에서 빠르게 확인하기 위한 메시지
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // CHAT 요청일 때 참고 근거가 몇 개 반환됐는지 저장
    private Integer referenceCount;

    // 언제 발생한 요청인지 시간순으로 보기 위한 필드
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiCallLog(
            String requestId,
            AiRequestType requestType,
            Long notebookId,
            Long documentId,
            boolean success,
            Long latencyMs,
            String errorCode,
            String errorMessage,
            Integer referenceCount
    ) {
        this.requestId = requestId;
        this.requestType = requestType;
        this.notebookId = notebookId;
        this.documentId = documentId;
        this.success = success;
        this.latencyMs = latencyMs;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.referenceCount = referenceCount;
    }

    // 생성 시각은 엔티티가 처음 저장될 때 한 번만 기록한다.
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
