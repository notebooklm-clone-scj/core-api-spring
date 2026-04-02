package core_api.domain.document;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.document.dto.AiSummaryResponse;
import core_api.domain.document.dto.DocumentResponse;
import core_api.domain.notebook.Notebook;
import core_api.domain.notebook.NotebookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final NotebookRepository notebookRepository;
    private final AiWorkerClient aiWorkerClient;

    // pdf 분석(요약)을 지시
    public AiSummaryResponse uploadAndSummarizeDocument(Long notebookId, MultipartFile file) {
        // 어떤 노트북에 올릴지 확인
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new IllegalArgumentException("해당 노트북을 찾을 수 없습니다."));

        try {
            // 파이썬에게 pdf 요약 지시
            AiSummaryResponse aiResponse = aiWorkerClient.extractPdfSummary(file);

            // 파이썬에서 제공한 결과를 바탕으로 Document 엔티티(DB 저장용 객체) 조립
            Document newDocument = Document.builder()
                    .notebook(notebook)
                    .filename(aiResponse.getFilename())
                    .summary(aiResponse.getSummary())
                    .totalPages(aiResponse.getTotal_pages())
                    .fullTextLength(aiResponse.getFull_text_length())
                    .build();

            // DB에 저장
            documentRepository.save(newDocument);

            // 파이썬이 준 결과를 프론트에 그대로 전달
            return aiResponse;

        } catch (IOException e) {
            throw new RuntimeException("PDF AI 분석 중 통신 에러가 발생했습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByNotebook(Long notebookId) {
        // 노트북이 있는지 확인
        if (!notebookRepository.existsById(notebookId)) {
            throw new IllegalArgumentException("해당 노트북을 찾을 수 없습니다.");
        }

        // 해당 노트북의 문서를 조회
        List<Document> documents = documentRepository.findAllByNotebookId(notebookId);

        // Entity -> DTO, Map으로 반환
        return documents.stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

}
