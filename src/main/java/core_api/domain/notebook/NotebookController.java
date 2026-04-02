package core_api.domain.notebook;

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

    @PostMapping
    public ResponseEntity<String> createNotebook(@RequestBody NotebookCreateRequest request) {
        Long notebookId = notebookService.createNotebook(request);
        return ResponseEntity.ok("노트북 생성 성공 : " + notebookId);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotebookResponse>> getNotebooks(@PathVariable Long userId) {
        List<NotebookResponse> responses = notebookService.getNotebooks(userId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{notebookId}/documents")
    public ResponseEntity<AiSummaryResponse> uploadDocument(
            @PathVariable("notebookId") Long notebookId,
            @RequestParam("file") MultipartFile file) {

        // 넘어온 PDF 파일을 Service에 전달
        AiSummaryResponse response = notebookService.uploadAndSummarizeDocument(notebookId, file);

        // 파이썬 서버에서 받아온 요약 결과 리턴
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{notebookId}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable("notebookId") Long notebookId) {

        List<DocumentResponse> responses = notebookService.getDocumentsByNotebook(notebookId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{notebookId}/chat")
    public ResponseEntity<AiChatResponse> chat(
            @PathVariable("notebookId") Long notebookId,
            @RequestBody AiChatRequest request) {

        AiChatResponse response = notebookService.chatWithNotebook(notebookId, request);
        return ResponseEntity.ok(response);
    }
}
