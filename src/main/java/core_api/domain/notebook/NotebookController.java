package core_api.domain.notebook;

import core_api.domain.notebook.dto.NotebookCreateRequest;
import core_api.domain.notebook.dto.NotebookResponse;
import core_api.domain.notebook.dto.NotebookUpdateRequest;
import core_api.global.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    // 노트북 생성
    @PostMapping
    public ResponseEntity<String> createNotebook(
            Authentication authentication,
            @Valid @RequestBody NotebookCreateRequest request
    ) {
        Long notebookId = notebookService.createNotebook(getAuthenticatedUserId(authentication), request);
        return ResponseEntity.ok("노트북 생성 성공 : " + notebookId);
    }

    // 노트북 목록 조회
    @GetMapping
    public ResponseEntity<List<NotebookResponse>> getNotebooks(Authentication authentication) {
        List<NotebookResponse> responses = notebookService.getNotebooks(getAuthenticatedUserId(authentication));
        return ResponseEntity.ok(responses);
    }

    // 노트북 제목 변경
    @PatchMapping("/{notebookId}")
    public ResponseEntity<String> updateNotebookTitle(
            Authentication authentication,
            @PathVariable Long notebookId,
            @Valid @RequestBody NotebookUpdateRequest request
    ) {
        notebookService.updateNotebookTitle(getAuthenticatedUserId(authentication), notebookId, request);
        return ResponseEntity.ok("노트북 제목이 변경되었습니다.");
    }

    // 노트북 삭제
    @DeleteMapping("/{notebookId}")
    public ResponseEntity<String> deleteNotebook(Authentication authentication, @PathVariable Long notebookId) {
        notebookService.deleteNotebook(getAuthenticatedUserId(authentication), notebookId);
        return ResponseEntity.ok("노트북이 삭제되었습니다.");
    }

    private Long getAuthenticatedUserId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
