package core_api.domain.notebook;

import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public Long createNotebook(NotebookCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다"));

        Notebook notebook = Notebook.builder()
                .title(request.getTitle())
                .user(user)
                .build();

        return notebookRepository.save(notebook).getId();
    }

    @Transactional(readOnly = true)
    public List<NotebookResponse> getNotebooks(Long userId)
    {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다"));

        return notebookRepository.findAllByUser(user).stream()
                .map(NotebookResponse::from)
                .collect(Collectors.toList());
    }

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
}
