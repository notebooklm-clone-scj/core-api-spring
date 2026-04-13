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

    private final AiCallLogRepository aiCallLogRepository;

    // 관리자 인증과 인가는 이제 Spring Security가 먼저 처리
    // 이 서비스는 인증이 끝난 뒤 실제 로그 조회에만 집중
    @Transactional(readOnly = true)
    public AdminAiCallLogPageResponse getAiCallLogs(
            Boolean success,
            AiRequestType requestType,
            Long notebookId,
            Long documentId,
            Integer page,
            Integer size
    ) {
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
