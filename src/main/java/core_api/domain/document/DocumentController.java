package core_api.domain.document;

import core_api.domain.document.dto.DocumentResponse;
import core_api.global.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
            Authentication authentication,
            @PathVariable("notebookId") Long notebookId,
            @RequestParam("file") MultipartFile file) {

        // 넘어온 PDF 파일을 Service에 전달 -> 요약 결과를 기다리지 않고 문서 ID만 전달
        Long documentId = documentService.uploadAndSummarizeDocumentAsync(
                getAuthenticatedUserId(authentication),
                notebookId,
                file
        );

        return ResponseEntity.ok(documentId);
    }

    // 노트북 내 문서 목록 조회
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            Authentication authentication,
            @PathVariable("notebookId") Long notebookId) {

        List<DocumentResponse> responses = documentService.getDocumentsByNotebook(
                getAuthenticatedUserId(authentication),
                notebookId
        );
        return ResponseEntity.ok(responses);
    }

    // 노트북 내 문서 삭제
    @DeleteMapping("/{documentId}")
    public ResponseEntity<String> deleteDocument(
            Authentication authentication,
            @PathVariable("notebookId") Long notebookId,
            @PathVariable("documentId") Long documentId
    ) {
        documentService.deleteDocument(getAuthenticatedUserId(authentication), notebookId, documentId);
        return ResponseEntity.ok("문서가 삭제되었습니다.");
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
