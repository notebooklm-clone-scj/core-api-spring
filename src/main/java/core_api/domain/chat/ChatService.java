package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.chat.dto.ChatHistoryResponse;
import core_api.domain.notebook.Notebook;
import core_api.domain.notebook.NotebookRepository;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final NotebookRepository notebookRepository;
    private final AiWorkerClient aiWorkerClient;
    private final ChatHistoryRepository chatHistoryRepository;

    @Transactional
    public AiChatResponse chatWithNotebook(Long notebookId, AiChatRequest request) {
        // 노트북 존재 유무 확인
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));

        // 유저의 질문을 DB에 저장 (역할: USER)
        ChatHistory userChat = ChatHistory.builder()
                .notebook(notebook)
                .role("USER")
                .message(request.getQuestion())
                .build();
        chatHistoryRepository.save(userChat);

        // 이 방의 모든 과거 대화 내역 꺼내오기 (방금 저장한 내 질문 포함!)
        List<ChatHistory> historyList = chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId);

        // 파이썬에게 '질문' + '과거 대화 내역'을 같이 전송
        AiChatResponse response = aiWorkerClient.askQuestionWithHistory(request.getQuestion(), historyList);

        // 파이썬이 만들어준 AI의 답변을 DB에 저장 (역할: AI)
        ChatHistory aiChat = ChatHistory.builder()
                .notebook(notebook)
                .role("AI")
                .message(response.getAnswer())
                .build();
        chatHistoryRepository.save(aiChat);

        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Long notebookId) {
        List<ChatHistory> histories = chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId);
        return histories.stream()
                .map(ChatHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
