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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final NotebookRepository notebookRepository;
    private final AiWorkerClient aiWorkerClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatReferenceRepository chatReferenceRepository;

    @Transactional
    public AiChatResponse chatWithNotebook(Long notebookId, AiChatRequest request) {
        // 노트북 존재 유무 확인
        Notebook notebook = notebookRepository.findById(notebookId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));

        // 이 방의 최근 6개의 과거 대화 내역 꺼내오기
        List<ChatHistory> recentHistory = chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId);

        Collections.reverse(recentHistory);

        // 파이썬에게 '질문' + '과거 대화 내역'을 같이 전송
        AiChatResponse response = aiWorkerClient.askQuestionWithHistory(request.getQuestion(), recentHistory);

        // 유저의 질문을 DB에 저장 (역할: USER)
        ChatHistory userChat = ChatHistory.builder()
                .notebook(notebook)
                .role("USER")
                .message(request.getQuestion())
                .build();

        // 파이썬이 만들어준 AI의 답변을 DB에 저장 (역할: AI)
        ChatHistory aiChat = ChatHistory.builder()
                .notebook(notebook)
                .role("AI")
                .message(response.getAnswer())
                .build();

        chatHistoryRepository.save(userChat);
        ChatHistory savedAiChat = chatHistoryRepository.save(aiChat);

        List<AiChatResponse.ReferenceChunk> referenceChunks = response.getReference_chunks();

        if (referenceChunks != null && !referenceChunks.isEmpty()) {
            List<ChatReference> references = new ArrayList<>();

            for (int index = 0; index < referenceChunks.size(); index++) {
                AiChatResponse.ReferenceChunk chunk = referenceChunks.get(index);

                references.add(ChatReference.builder()
                        .chatHistory(savedAiChat)
                        .pageNumber(chunk.getPage_number())
                        .content(chunk.getContent())
                        .sortOrder(index)
                        .build());
            }

            chatReferenceRepository.saveAll(references);
        }

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
