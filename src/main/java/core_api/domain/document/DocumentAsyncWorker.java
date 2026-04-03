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
    // @Transactional 제거 (외부 API 통신 중 DB 커넥션 점유 방지)
    // 직접 만든 스레드 풀 지정
    @Async("documentTaskExecutor") // 비동기 실행
    public void analyzeDocumentInBackground(Long documentId, byte[] fileBytes, String filename) {
        log.info("비동기 분석 시작: {}", filename);

        try{
            // 파이썬 서버로 파일 전송 및 요약 결과 수신, 외부 API 호출 (시간이 오래 걸림 - DB 커넥션 없음, 안전)
            AiSummaryResponse aiResponse = aiWorkerClient.extractPdfSummary(fileBytes, filename);

            // DB에서 저장했던 문서를 다시 찾음, DB 업데이트 로직 (이 순간에만 짧게 DB 사용)
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("문서를 찾을 수 없습니다. ID: " + documentId));

            // 엔티티 내부 메서드를 호출하여 상태를 'COMPLETED'로 변경하고 결과 저장
            document.completeAnalysis(
                    aiResponse.getSummary(),
                    aiResponse.getTotal_pages(),
                    aiResponse.getFull_text_length()
            );

            documentRepository.save(document); // 명시적 저장
            log.info("비동기 분석 완료: {}", filename);

        } catch (Exception e) {
            log.error("비동기 분석 중 실패: {}", filename, e);
            // 실패 시 상태를 FAILED로 변경
            // 실패 시에도 DB에 저장되도록 보장
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.failAnalysis();
                documentRepository.save(doc); // 명시적 저장
            });
        }
    }
}
