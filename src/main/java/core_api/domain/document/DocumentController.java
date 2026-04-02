package core_api.domain.document;

import core_api.domain.document.dto.AiSummaryResponse;
import core_api.domain.document.dto.DocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notebooks/{notebookId}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // PDF 업로드 및 요약
    @PostMapping
    public ResponseEntity<AiSummaryResponse> uploadDocument(
            @PathVariable("notebookId") Long notebookId,
            @RequestParam("file") MultipartFile file) {

        // 넘어온 PDF 파일을 Service에 전달
        AiSummaryResponse response = documentService.uploadAndSummarizeDocument(notebookId, file);
        return ResponseEntity.ok(response);
    }

    // 노트북 내 문서 목록 조회
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable("notebookId") Long notebookId) {

        List<DocumentResponse> responses = documentService.getDocumentsByNotebook(notebookId);
        return ResponseEntity.ok(responses);
    }
}
