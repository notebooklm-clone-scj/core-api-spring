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

    private static final int RECENT_HISTORY_LIMIT = 6;
    private static final int SUMMARY_TRIGGER_SIZE = 6;

    private final NotebookRepository notebookRepository;
    private final AiWorkerClient aiWorkerClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ChatReferenceRepository chatReferenceRepository;
    private final ChatMemoryRepository chatMemoryRepository;

    @Transactional
    public AiChatResponse chatWithNotebook(Long userId, Long notebookId, AiChatRequest request) {
        Notebook notebook = getOwnedNotebook(userId, notebookId);

        // 기존 summary memory가 있으면 같이 꺼내 오래된 대화 요약 + 최근 대화 구조로 AI에게 전달
        ChatMemory chatMemory = chatMemoryRepository.findByNotebookId(notebookId).orElse(null);
        String conversationSummary = chatMemory != null ? chatMemory.getSummary() :  null;

        // 이 방의 최근 6개의 과거 대화 내역 꺼내오기
        List<ChatHistory> recentHistory =
                new ArrayList<>(chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId));

        // 최신순으로 가져왔기 때문에, AI에게 보내기 전에는 다시 과거 -> 최신 순서로 뒤집는다.
        Collections.reverse(recentHistory);

        // notebookId도 함께 넘겨 AiWorkerClient가 운영 로그에 "어느 노트북 요청인지" 남길 수 있게 한다.
        AiChatResponse response = aiWorkerClient.askQuestionWithHistory(
                notebookId,
                request.getQuestion(),
                conversationSummary,
                recentHistory
        );

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

        saveReferences(savedAiChat, response.getReference_chunks());

        // 이번 턴이 끝난 후, 오래된 대화가 충분히 쌓였으면 summary memory를 다시 압축 갱신
        refreshConversationMemory(notebook, chatMemory);

        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getChatHistory(Long userId, Long notebookId) {
        getOwnedNotebook(userId, notebookId);

        List<ChatHistory> histories = chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId);
        return histories.stream()
                .map(ChatHistoryResponse::from)
                .collect(Collectors.toList());
    }

    private Notebook getOwnedNotebook(Long userId, Long notebookId) {
        return notebookRepository.findByIdAndUserId(notebookId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTEBOOK_NOT_FOUND));
    }

    private void saveReferences(ChatHistory savedAiChat, List<AiChatResponse.ReferenceChunk> referenceChunks) {
        if (referenceChunks == null || referenceChunks.isEmpty()) {
            return;
        }

        // AI가 내려준 reference 순서를 그대로 유지하기 위해 sortOrder를 index로 저장한다.
        List<ChatReference> references = new ArrayList<>();

        for (int index = 0; index < referenceChunks.size(); index++) {
            AiChatResponse.ReferenceChunk chunk = referenceChunks.get(index);

            references.add(ChatReference.builder()
                    .chatHistory(savedAiChat)
                    .documentId(chunk.getDocument_id())
                    .documentTitle(chunk.getDocument_title())
                    .sectionTitle(chunk.getSection_title())
                    .pageNumber(chunk.getPage_number())
                    .chunkIndex(chunk.getChunk_index())
                    .pageChunkIndex(chunk.getPage_chunk_index())
                    .content(chunk.getContent())
                    .sortOrder(index)
                    .build());
        }

        chatReferenceRepository.saveAll(references);
    }

    private void refreshConversationMemory(Notebook notebook, ChatMemory existingMemory) {
        List<ChatHistory> unsummarizedHistories = getUnsummarizedHistories(notebook.getId(), existingMemory);

        // 최근 대화 6개는 raw history로 바로 보내고 오래된 대화만 summary memory 후보로 본다.
        if (unsummarizedHistories.size() <= RECENT_HISTORY_LIMIT) {
            return;
        }

        int summarizeEndIndex = unsummarizedHistories.size() - RECENT_HISTORY_LIMIT;
        List<ChatHistory> historiesToSummarize = new ArrayList<>(unsummarizedHistories.subList(0, summarizeEndIndex));

        // 너무 조금 쌓였을 때는 매번 요약하지 않고 일정량 이상 쌓였을 때만 다시 요약
        if (historiesToSummarize.size() < SUMMARY_TRIGGER_SIZE) {
            return;
        }

        String previousSummary = existingMemory != null ? existingMemory.getSummary() : null;

        // notebookId를 같이 넘겨 summary 호출도 운영 로그에 남김
        String mergedSummary = aiWorkerClient.summarizeConversation(
                notebook.getId(),
                previousSummary,
                historiesToSummarize
        );

        Long lastSummarizedId = historiesToSummarize.get(historiesToSummarize.size() - 1).getId();

        if (existingMemory == null) {
            // 아직 summary memory가 없으면 새로 만든다.
            ChatMemory newMemory = ChatMemory.builder()
                    .notebook(notebook)
                    .summary(mergedSummary)
                    .lastSummarizedChatHistoryId(lastSummarizedId)
                    .build();

            chatMemoryRepository.save(newMemory);
            return;
        }

        // 기존 summary memory가 있으면 마지막 반영 지점과 함께 갱신한다.
        existingMemory.updateSummary(mergedSummary, lastSummarizedId);
    }

    private List<ChatHistory> getUnsummarizedHistories(Long notebookId, ChatMemory existingMemory) {
        // summary memory가 한 번도 없었던 노트북이면 전체 대화를 대상 삼는다.
        if (existingMemory == null || existingMemory.getLastSummarizedChatHistoryId() == null) {
            return chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId);
        }

        // 이미 요약에 반영한 마지막 chatHistoryId 이후 데이터만 다시 가져온다.
        return chatHistoryRepository.findByNotebookIdAndIdGreaterThanOrderByCreatedAtAsc(
                notebookId,
                existingMemory.getLastSummarizedChatHistoryId()
        );
    }
}
