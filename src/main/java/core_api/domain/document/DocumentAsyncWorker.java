package core_api.domain.document;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.document.dto.AiSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAsyncWorker {

    private final DocumentRepository documentRepository;
    private final AiWorkerClient aiWorkerClient;

    // 실제 파이썬 서버와 통신하고 요약하는 백그라운드 작업
    @Async // 비동기 실행
    @Transactional // 별도 쓰레드에서 트랜잭션 새로 시작
    public void analyzeDocumentInBackground(Long documentId, byte[] fileBytes, String filename) {
        log.info("비동기 분석 시작: {}", filename);

        try{
            // 파이썬 서버로 파일 전송 및 요약 결과 수신
            AiSummaryResponse aiResponse = aiWorkerClient.extractPdfSummary(fileBytes, filename);

            // DB에서 저장했던 문서를 다시 찾음
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다. ID: " + documentId));

            // 엔티티 내부 메서드를 호출하여 상태를 'COMPLETED'로 변경하고 결과 저장
            document.completeAnalysis(
                    aiResponse.getSummary(),
                    aiResponse.getTotal_pages(),
                    aiResponse.getFull_text_length()
            );

            log.info("비동기 분석 완료: {}", filename);

        } catch (Exception e) {
            log.error("비동기 분석 중 실패: {}", filename, e);
            // 실패 시 상태를 FAILED로 변경
            documentRepository.findById(documentId).ifPresent(Document::failAnalysis);
        }
    }
}
