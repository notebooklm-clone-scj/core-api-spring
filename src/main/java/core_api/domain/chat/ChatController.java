package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notebooks/{notebookId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // AI와 대화
    @PostMapping
    public ResponseEntity<AiChatResponse> chat(
            @PathVariable("notebookId") Long notebookId,
            @RequestBody AiChatRequest request) {

        AiChatResponse response = chatService.chatWithNotebook(notebookId, request);
        return ResponseEntity.ok(response);
    }
}
