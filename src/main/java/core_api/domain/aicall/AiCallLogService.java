package core_api.domain.aicall;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// AI 호출 결과를 기록하는 전용 서비스
// -> 핵심 포인트는 REQUIRES_NEW
// -> 본 요청이 실패/롤백되어도 "실패 로그 자체는" 남길 수 있게 한다.
public class AiCallLogService {

    private final AiCallLogRepository aiCallLogRepository;

    // 성공 호출 기록
    // -> 운영 화면에서 평균 응답 시간, 요청 종류별 성공 건수 등을 볼 때 사용한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            AiRequestType requestType,
            String requestId,
            Long notebookId,
            Long documentId,
            long latencyMs,
            Integer referenceCount
    ) {
        aiCallLogRepository.save(
                AiCallLog.builder()
                        .requestType(requestType)
                        .requestId(requestId)
                        .notebookId(notebookId)
                        .documentId(documentId)
                        .success(true)
                        .latencyMs(latencyMs)
                        .referenceCount(referenceCount)
                        .build()
        );
    }

    // 실패 호출 기록
    // -> 어떤 요청이 왜 실패했는지 관리자 화면과 운영 분석에서 확인하기 위한 용도다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            AiRequestType requestType,
            String requestId,
            Long notebookId,
            Long documentId,
            long latencyMs,
            String errorCode,
            String errorMessage
    ) {
        aiCallLogRepository.save(
                AiCallLog.builder()
                        .requestType(requestType)
                        .requestId(requestId)
                        .notebookId(notebookId)
                        .documentId(documentId)
                        .success(false)
                        .latencyMs(latencyMs)
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .build()
        );
    }
}
