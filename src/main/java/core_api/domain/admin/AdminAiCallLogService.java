package core_api.domain.admin;

import core_api.domain.admin.dto.AdminAiCallLogPageResponse;
import core_api.domain.admin.dto.AdminAiCallLogResponse;
import core_api.domain.aicall.AiCallLog;
import core_api.domain.aicall.AiCallLogRepository;
import core_api.domain.aicall.AiRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAiCallLogService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AdminAuthService adminAuthService;
    private final AiCallLogRepository aiCallLogRepository;

    // 관리자 인증을 통과한 사용자만 AI 호출 로그 목록을 조회할 수 있습니다.
    // 처음 버전은 필터(success, requestType, notebookId, documentId)와 페이지네이션만 지원합니다.
    @Transactional(readOnly = true)
    public AdminAiCallLogPageResponse getAiCallLogs(
            String authorizationHeader,
            Boolean success,
            AiRequestType requestType,
            Long notebookId,
            Long documentId,
            Integer page,
            Integer size
    ) {
        adminAuthService.authenticateAdmin(authorizationHeader);

        PageRequest pageRequest = PageRequest.of(
                normalizePage(page),
                normalizeSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<AiCallLog> specification = buildSpecification(success, requestType, notebookId, documentId);

        Page<AdminAiCallLogResponse> resultPage = aiCallLogRepository.findAll(specification, pageRequest)
                .map(AdminAiCallLogResponse::from);

        return AdminAiCallLogPageResponse.from(resultPage);
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private Specification<AiCallLog> buildSpecification(
            Boolean success,
            AiRequestType requestType,
            Long notebookId,
            Long documentId
    ) {
        Specification<AiCallLog> specification = (root, query, cb) -> cb.conjunction();

        if (success != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("success"), success));
        }

        if (requestType != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("requestType"), requestType));
        }

        if (notebookId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("notebookId"), notebookId));
        }

        if (documentId != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("documentId"), documentId));
        }

        return specification;
    }
}
