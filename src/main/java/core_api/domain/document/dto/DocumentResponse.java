package core_api.domain.document.dto;

import core_api.domain.document.Document;
import core_api.domain.document.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String filename;
    private String summary;
    private int totalPages;
    private DocumentStatus status;
    private LocalDateTime createdAt;

    // 엔티티를 DTO로 변경
    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .summary(document.getSummary())
                .totalPages(document.getTotalPages())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
