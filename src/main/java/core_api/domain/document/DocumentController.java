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
    public ResponseEntity<Long> uploadDocument(
            @PathVariable("notebookId") Long notebookId,
            @RequestParam("file") MultipartFile file) {

        // 넘어온 PDF 파일을 Service에 전달 -> 요약 결과를 기다리지 않고 문서 ID만 전달
        Long documentId = documentService.uploadAndSummarizeDocumentAsync(notebookId, file);

        return ResponseEntity.ok(documentId);
    }

    // 노트북 내 문서 목록 조회
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable("notebookId") Long notebookId) {

        List<DocumentResponse> responses = documentService.getDocumentsByNotebook(notebookId);
        return ResponseEntity.ok(responses);
    }

    // 노트북 내 문서 삭제
    @DeleteMapping("/{documentId}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable("notebookId") Long notebookId,
            @PathVariable("documentId") Long documentId
    ) {
        documentService.deleteDocument(notebookId, documentId);
        return ResponseEntity.ok("문서가 삭제되었습니다.");
    }
}
