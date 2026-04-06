package core_api.domain.chat.dto;

import core_api.domain.chat.ChatHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatHistoryResponse {

    private String role;
    private String message;
    private LocalDateTime createdAt;

    public static ChatHistoryResponse from(ChatHistory chatHistory) {
        return ChatHistoryResponse.builder()
                .role(chatHistory.getRole())
                .message(chatHistory.getMessage())
                .createdAt(chatHistory.getCreatedAt())
                .build();
    }
}
