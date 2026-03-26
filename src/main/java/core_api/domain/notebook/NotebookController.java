package core_api.domain.notebook;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
