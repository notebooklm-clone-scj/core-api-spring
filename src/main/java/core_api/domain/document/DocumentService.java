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

    private static final long MAX_DOCUMENTS_PER_NOTEBOOK = 3L;
    private static final long MAX_DOCUMENTS_PER_USER = 5L;

    private final DocumentRepository documentRepository;
    private final NotebookRepository notebookRepository;
    private final DocumentAsyncWorker documentAsyncWorker;
    private final AiWorkerClient aiWorkerClient;

    // 사용자 요청을 받고 즉시 응답 (비동기 입구)
    @Transactional
    public Long uploadAndSummarizeDocumentAsync(Long userId, Long notebookId, MultipartFile file) {
        Notebook notebook = getOwnedNotebook(userId, notebookId);

        validateDocumentUploadLimit(notebook);

        try{
            // MultipartFile의 데이터를 미리 byte[]로 읽기 (비동기 쓰레드 안전)
            byte[] fileBytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();

            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "document.pdf";
            }

            // DB에 먼저 PROCESSING 상태로 저장 (껍대기 생성)
            Document newDocument = Document.builder()
                    .notebook(notebook)
                    .filename(originalFilename)
                    .status(DocumentStatus.PROCESSING)
                    .build();

            Document savedDoc = documentRepository.save(newDocument);

            // 별도 쓰레드에서 돌아갈 비동기 메서드 호출
            documentAsyncWorker.analyzeDocumentInBackground(
                    savedDoc.getId(),
                    notebook.getId(),
                    fileBytes,
                    originalFilename
            );

            // 분석을 기다리지 않고 생성된 문서 ID를 즉시 반환
            return savedDoc.getId();

        } catch (IOException e) {
            log.error("파일 읽기 실패: ", e);
            throw new CustomException(ErrorCode.FILE_READ_ERROR);
        }
    }




    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByNotebook(Long userId, Long notebookId) {
        getOwnedNotebook(userId, notebookId);

        // 해당 노트북의 문서를 조회
        List<Document> documents = documentRepository.findAllByNotebookId(notebookId);

        // Entity -> DTO, Map으로 반환
        return documents.stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteDocument(Long userId, Long notebookId, Long documentId) {
        getOwnedNotebook(userId, notebookId);

        Document document = documentRepository.findByIdAndNotebookId(documentId, notebookId)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        aiWorkerClient.deleteDocumentVectors(document.getId(), document.getFilename());
        documentRepository.delete(document);
    }

    private void validateDocumentUploadLimit(Notebook notebook) {
        long notebookDocumentCount = documentRepository.countByNotebookId(notebook.getId());
        if (notebookDocumentCount >= MAX_DOCUMENTS_PER_NOTEBOOK) {
            throw new CustomException(ErrorCode.DOCUMENT_LIMIT_PER_NOTEBOOK_EXCEEDED);
        }

        long userDocumentCount = documentRepository.countByNotebookUserId(notebook.getUser().getId());
        if (userDocumentCount >= MAX_DOCUMENTS_PER_USER) {
            throw new CustomException(ErrorCode.DOCUMENT_LIMIT_PER_USER_EXCEEDED);
        }
    }

    private Notebook getOwnedNotebook(Long userId, Long notebookId) {
        return notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));
    }

}
