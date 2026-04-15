package core_api.domain.notebook;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.chat.ChatHistory;
import core_api.domain.chat.ChatHistoryRepository;
import core_api.domain.chat.ChatMemory;
import core_api.domain.chat.ChatMemoryRepository;
import core_api.domain.document.Document;
import core_api.domain.document.DocumentRepository;
import core_api.domain.notebook.dto.NotebookCreateRequest;
import core_api.domain.notebook.dto.NotebookResponse;
import core_api.domain.notebook.dto.NotebookUpdateRequest;
import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private static final long MAX_NOTEBOOKS_PER_USER = 2L;

    private final NotebookRepository notebookRepository;
    private final DocumentRepository documentRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatMemoryRepository chatMemoryRepository;
    private final UserRepository userRepository;
    private final AiWorkerClient aiWorkerClient;

    @Transactional
    public Long createNotebook(NotebookCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        long notebookCount = notebookRepository.countByUserId(user.getId());

        if (notebookCount >= MAX_NOTEBOOKS_PER_USER) {
            throw new CustomException(ErrorCode.NOTEBOOK_LIMIT_EXCEEDED);
        }

        Notebook notebook = Notebook.builder()
                .title(request.getTitle())
                .user(user)
                .build();

        return notebookRepository.save(notebook).getId();
    }

    @Transactional(readOnly = true)
    public List<NotebookResponse> getNotebooks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return notebookRepository.findAllByUser(user).stream()
                .map(NotebookResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateNotebookTitle(Long notebookId, NotebookUpdateRequest request) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));

        notebook.updateTitle(request.getTitle().trim());
    }

    @Transactional
    public void deleteNotebook(Long notebookId) {
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));

        List<Document> documents = documentRepository.findAllByNotebookId(notebookId);

        for (Document document : documents) {
            aiWorkerClient.deleteDocumentVectors(document.getId(), document.getFilename());
        }

        List<ChatHistory> chatHistories = chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId);

        if (!chatHistories.isEmpty()) {
            chatHistoryRepository.deleteAll(chatHistories);
        }

        ChatMemory chatMemory = chatMemoryRepository.findByNotebookId(notebookId).orElse(null);

        if (chatMemory != null) {
            chatMemoryRepository.delete(chatMemory);
        }

        if (!documents.isEmpty()) {
            documentRepository.deleteAll(documents);
        }

        notebookRepository.delete(notebook);
    }

}
