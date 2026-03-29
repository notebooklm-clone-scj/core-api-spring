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

    @PostMapping("/analyze")
    public ResponseEntity<AiSummaryResponse> analyzePdf(@RequestParam("file") MultipartFile file) {
        // 넘어온 PDF 파일을 Service에 전달
        AiSummaryResponse response = notebookService.analyzePdf(file);

        // 2. 파이썬 서버에서 무사히 받아온 요약 결과를 포스트맨에 띄워줍니다!
        return ResponseEntity.ok(response);
    }
}
