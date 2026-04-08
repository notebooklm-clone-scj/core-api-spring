package core_api.domain.document;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.document.dto.AiSummaryResponse;
import core_api.domain.document.dto.DocumentResponse;
import core_api.domain.notebook.Notebook;
import core_api.domain.notebook.NotebookRepository;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final NotebookRepository notebookRepository;
    private final DocumentAsyncWorker documentAsyncWorker;

    // 사용자 요청을 받고 즉시 응답 (비동기 입구)
    @Transactional
    public Long uploadAndSummarizeDocumentAsync(Long notebookId, MultipartFile file) {
        // 노트북 존재 확인
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(()->new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));

        try{
            // MultipartFile의 데이터를 미리 byte[]로 읽기 (비동기 쓰레드 안전)
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            // DB에 먼저 PROCESSING 상태로 저장 (껍대기 생성)
            Document newDocument = Document.builder()
                    .notebook(notebook)
                    .filename(originalFilename)
                    .status(DocumentStatus.PROCESSING)
                    .build();

            Document savedDoc = documentRepository.save(newDocument);

            // 별도 쓰레드에서 돌아갈 비동기 메서드 호출
            documentAsyncWorker.analyzeDocumentInBackground(savedDoc.getId(), fileBytes, originalFilename);

            // 분석을 기다리지 않고 생성된 문서 ID를 즉시 반환
            return savedDoc.getId();

        } catch (IOException e) {
            log.error("파일 읽기 실패: ", e);
            throw new CustomException(ErrorCode.FILE_READ_ERROR);
        }
    }




    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByNotebook(Long notebookId) {
        // 노트북이 있는지 확인
        if (!notebookRepository.existsById(notebookId)) {
            throw new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND);
        }

        // 해당 노트북의 문서를 조회
        List<Document> documents = documentRepository.findAllByNotebookId(notebookId);

        // Entity -> DTO, Map으로 반환
        return documents.stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

}
