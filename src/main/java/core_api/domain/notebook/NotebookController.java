package core_api.domain.notebook;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.document.dto.AiSummaryResponse;
import core_api.domain.document.dto.DocumentResponse;
import core_api.domain.notebook.dto.NotebookCreateRequest;
import core_api.domain.notebook.dto.NotebookResponse;
import core_api.domain.notebook.dto.NotebookUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    // 노트북 생성
    @PostMapping
    public ResponseEntity<String> createNotebook(@Valid @RequestBody NotebookCreateRequest request) {
        Long notebookId = notebookService.createNotebook(request);
        return ResponseEntity.ok("노트북 생성 성공 : " + notebookId);
    }

    // 노트북 목록 조회
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotebookResponse>> getNotebooks(@PathVariable Long userId) {
        List<NotebookResponse> responses = notebookService.getNotebooks(userId);
        return ResponseEntity.ok(responses);
    }

    // 노트북 제목 변경
    @PatchMapping("/{notebookId}")
    public ResponseEntity<String> updateNotebookTitle(
            @PathVariable Long notebookId,
            @Valid @RequestBody NotebookUpdateRequest request
    ) {
        notebookService.updateNotebookTitle(notebookId, request);
        return ResponseEntity.ok("노트북 제목이 변경되었습니다.");
    }

    // 노트북 삭제
    @DeleteMapping("/{notebookId}")
    public ResponseEntity<String> deleteNotebook(@PathVariable Long notebookId) {
        notebookService.deleteNotebook(notebookId);
        return ResponseEntity.ok("노트북이 삭제되었습니다.");
    }
}
