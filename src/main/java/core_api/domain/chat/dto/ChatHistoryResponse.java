package core_api.domain.chat.dto;

import core_api.domain.chat.ChatHistory;
import core_api.domain.chat.ChatReference;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ChatHistoryResponse {

    private String role;
    private String message;
    private LocalDateTime createdAt;
    private List<ReferenceResponse> references;

    public static ChatHistoryResponse from(ChatHistory chatHistory) {
        return ChatHistoryResponse.builder()
                .role(chatHistory.getRole())
                .message(chatHistory.getMessage())
                .createdAt(chatHistory.getCreatedAt())
                .references(chatHistory.getReferences().stream()
                        .map(ReferenceResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    public static class ReferenceResponse {
        private Long documentId;
        private String documentTitle;
        private String sectionTitle;
        private int pageNumber;
        private Integer chunkIndex;
        private Integer pageChunkIndex;
        private String content;

        public static ReferenceResponse from(ChatReference chatReference) {
            return ReferenceResponse.builder()
                    .documentId(chatReference.getDocumentId())
                    .documentTitle(chatReference.getDocumentTitle())
                    .sectionTitle(chatReference.getSectionTitle())
                    .pageNumber(chatReference.getPageNumber())
                    .chunkIndex(chatReference.getChunkIndex())
                    .pageChunkIndex(chatReference.getPageChunkIndex())
                    .content(chatReference.getContent())
                    .build();
        }
    }
}
