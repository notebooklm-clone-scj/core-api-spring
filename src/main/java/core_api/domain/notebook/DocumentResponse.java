package core_api.domain.notebook;

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
    private LocalDateTime createdAt;

    // 엔티티를 DTO로 변경
    public static DocumentResponse from(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .summary(document.getSummary())
                .totalPages(document.getTotalPages())
                .createdAt(document.getCreatedAt())
                .build();
    }
}
