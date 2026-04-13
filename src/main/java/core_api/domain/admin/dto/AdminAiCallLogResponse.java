package core_api.domain.admin.dto;

import core_api.domain.aicall.AiCallLog;
import core_api.domain.aicall.AiRequestType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminAiCallLogResponse {

    private Long id;
    private String requestId;
    private AiRequestType requestType;
    private Long notebookId;
    private Long documentId;
    private boolean success;
    private Long latencyMs;
    private String errorCode;
    private String errorMessage;
    private Integer referenceCount;
    private LocalDateTime createdAt;

    public static AdminAiCallLogResponse from(AiCallLog aiCallLog) {
        return AdminAiCallLogResponse.builder()
                .id(aiCallLog.getId())
                .requestId(aiCallLog.getRequestId())
                .requestType(aiCallLog.getRequestType())
                .notebookId(aiCallLog.getNotebookId())
                .documentId(aiCallLog.getDocumentId())
                .success(aiCallLog.isSuccess())
                .latencyMs(aiCallLog.getLatencyMs())
                .errorCode(aiCallLog.getErrorCode())
                .errorMessage(aiCallLog.getErrorMessage())
                .referenceCount(aiCallLog.getReferenceCount())
                .createdAt(aiCallLog.getCreatedAt())
                .build();
    }
}
