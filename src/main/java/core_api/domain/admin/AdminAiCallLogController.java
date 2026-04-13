package core_api.domain.admin;

import core_api.domain.admin.dto.AdminAiCallLogPageResponse;
import core_api.domain.aicall.AiRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai-call-logs")
@RequiredArgsConstructor
public class AdminAiCallLogController {

    private final AdminAiCallLogService adminAiCallLogService;

    // 관리자 전용 AI 호출 로그 목록 조회 API
    // 예)
    // - /api/v1/admin/ai-call-logs
    // - /api/v1/admin/ai-call-logs?success=false
    // - /api/v1/admin/ai-call-logs?requestType=CHAT
    @GetMapping
    public ResponseEntity<AdminAiCallLogPageResponse> getAiCallLogs(
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) AiRequestType requestType,
            @RequestParam(required = false) Long notebookId,
            @RequestParam(required = false) Long documentId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        AdminAiCallLogPageResponse response = adminAiCallLogService.getAiCallLogs(
                success,
                requestType,
                notebookId,
                documentId,
                page,
                size
        );

        return ResponseEntity.ok(response);
    }
}
