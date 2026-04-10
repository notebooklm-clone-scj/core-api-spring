package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.chat.dto.ChatHistoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notebooks/{notebookId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // AI와 대화
    @PostMapping
    public ResponseEntity<AiChatResponse> chat(
            @PathVariable("notebookId") Long notebookId,
            @Valid @RequestBody AiChatRequest request) {

        AiChatResponse response = chatService.chatWithNotebook(notebookId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ChatHistoryResponse>> getChatHistory(
            @PathVariable("notebookId") Long notebookId){
        List<ChatHistoryResponse> responses = chatService.getChatHistory(notebookId);
        return ResponseEntity.ok(responses);
    }
}
