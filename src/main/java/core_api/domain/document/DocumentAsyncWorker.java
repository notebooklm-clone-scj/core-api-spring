package core_api.domain.document;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.document.dto.AiSummaryResponse;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
// 문서 업로드 직후 오래 걸리는 AI 분석을 백그라운드로 넘기는 워커
// 업로드 API 응답과 AI 분석 시간을 분리하기 위한 목적
public class DocumentAsyncWorker {

    private final DocumentRepository documentRepository;
    private final AiWorkerClient aiWorkerClient;

    // 실제 파이썬 서버와 통신하는 무거운 작업
    // 외부 API 호출 동안 DB 커넥션을 붙잡지 않기 위해 @Transactional은 메서드에 두지 않았다
    // 대신 필요한 순간에만 DB를 짧게 읽고 저장
    @Async("documentTaskExecutor") // 비동기 실행
    public void analyzeDocumentInBackground(Long documentId, Long notebookId, byte[] fileBytes, String filename) {
        log.info("비동기 분석 시작: {}", filename);

        try{
            // documentId를 같이 넘겨 PDF 요약 호출 실패 시에도 "어느 문서에서 실패했는지" 운영 로그에 남길 수 있게 한다.
            AiSummaryResponse aiResponse = aiWorkerClient.extractPdfSummary(notebookId, documentId, fileBytes, filename);

            // DB에서 저장했던 문서를 다시 찾음, DB 업데이트 로직 (이 순간에만 짧게 DB 사용)
            Document document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

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
