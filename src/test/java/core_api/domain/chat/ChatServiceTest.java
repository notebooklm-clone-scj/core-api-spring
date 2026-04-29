package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.notebook.Notebook;
import core_api.domain.notebook.NotebookRepository;
import core_api.domain.user.Role;
import core_api.domain.user.User;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private NotebookRepository notebookRepository;

    @Mock
    private AiWorkerClient aiWorkerClient;

    @Mock
    private ChatHistoryRepository chatHistoryRepository;

    @Mock
    private ChatReferenceRepository chatReferenceRepository;

    @Mock
    private ChatMemoryRepository chatMemoryRepository;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("AI 답변 생성 시 reference chunks도 함께 저장한다")
    void chatWithNotebook_savesReferences() throws Exception {
        // given: 노트북, 이전 대화, AI 응답 mock 준비
        Long userId = 1L;
        Long notebookId = 1L;
        Notebook notebook = createNotebook("테스트 노트북");
        AiChatRequest request = new AiChatRequest("이 문서의 핵심 내용을 알려줘");

        ChatHistory oldUser = ChatHistory.builder()
                .notebook(notebook)
                .role("USER")
                .message("이전 질문")
                .build();

        ChatHistory oldAi = ChatHistory.builder()
                .notebook(notebook)
                .role("AI")
                .message("이전 답변")
                .build();

        AiChatResponse response = new AiChatResponse();
        setField(response, "answer", "핵심 내용은 세 가지입니다.");

        AiChatResponse.ReferenceChunk chunk1 = new AiChatResponse.ReferenceChunk();
        setField(chunk1, "document_id", 10L);
        setField(chunk1, "document_title", "테스트 문서");
        setField(chunk1, "section_title", "핵심 요약");
        setField(chunk1, "page_number", 3);
        setField(chunk1, "chunk_index", 4);
        setField(chunk1, "page_chunk_index", 1);
        setField(chunk1, "content", "첫 번째 근거 문장");

        AiChatResponse.ReferenceChunk chunk2 = new AiChatResponse.ReferenceChunk();
        setField(chunk2, "document_id", 10L);
        setField(chunk2, "document_title", "테스트 문서");
        setField(chunk2, "section_title", "세부 내용");
        setField(chunk2, "page_number", 7);
        setField(chunk2, "chunk_index", 8);
        setField(chunk2, "page_chunk_index", 0);
        setField(chunk2, "content", "두 번째 근거 문장");

        setField(response, "reference_chunks", List.of(chunk1, chunk2));

        given(notebookRepository.findByIdAndUserId(notebookId, userId)).willReturn(Optional.of(notebook));
        given(chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId))
                .willReturn(new ArrayList<>(List.of(oldAi, oldUser)));
        given(aiWorkerClient.askQuestionWithHistory(any(), any(), any(), any())).willReturn(response);
        given(chatMemoryRepository.findByNotebookId(notebookId)).willReturn(Optional.empty());

        ChatHistory savedAiChat = ChatHistory.builder()
                .id(100L)
                .notebook(notebook)
                .role("AI")
                .message("핵심 내용은 세 가지입니다.")
                .build();

        given(chatHistoryRepository.save(any(ChatHistory.class)))
                .willAnswer(invocation -> {
                    ChatHistory arg = invocation.getArgument(0);
                    if ("AI".equals(arg.getRole())) {
                        return savedAiChat;
                    }
                    return arg;
                });

        // when: 채팅 요청 실행
        chatService.chatWithNotebook(userId, notebookId, request);

        // then: reference chunks가 ChatReference로 저장
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatReference>> captor = ArgumentCaptor.forClass(List.class);

        then(chatReferenceRepository).should().saveAll(captor.capture());

        List<ChatReference> savedReferences = captor.getValue();

        assertThat(savedReferences).hasSize(2);

        assertThat(savedReferences.get(0).getChatHistory()).isEqualTo(savedAiChat);
        assertThat(savedReferences.get(0).getDocumentId()).isEqualTo(10L);
        assertThat(savedReferences.get(0).getDocumentTitle()).isEqualTo("테스트 문서");
        assertThat(savedReferences.get(0).getSectionTitle()).isEqualTo("핵심 요약");
        assertThat(savedReferences.get(0).getPageNumber()).isEqualTo(3);
        assertThat(savedReferences.get(0).getChunkIndex()).isEqualTo(4);
        assertThat(savedReferences.get(0).getPageChunkIndex()).isEqualTo(1);
        assertThat(savedReferences.get(0).getContent()).isEqualTo("첫 번째 근거 문장");
        assertThat(savedReferences.get(0).getSortOrder()).isEqualTo(0);

        assertThat(savedReferences.get(1).getChatHistory()).isEqualTo(savedAiChat);
        assertThat(savedReferences.get(1).getDocumentId()).isEqualTo(10L);
        assertThat(savedReferences.get(1).getDocumentTitle()).isEqualTo("테스트 문서");
        assertThat(savedReferences.get(1).getSectionTitle()).isEqualTo("세부 내용");
        assertThat(savedReferences.get(1).getPageNumber()).isEqualTo(7);
        assertThat(savedReferences.get(1).getChunkIndex()).isEqualTo(8);
        assertThat(savedReferences.get(1).getPageChunkIndex()).isEqualTo(0);
        assertThat(savedReferences.get(1).getContent()).isEqualTo("두 번째 근거 문장");
        assertThat(savedReferences.get(1).getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 노트북으로 채팅하면 예외가 발생한다")
    void chatWithNotebook_fail_notebookNotFound() {
        // given: 존재하지 않는 노트북 ID
        given(notebookRepository.findByIdAndUserId(999L, 1L)).willReturn(Optional.empty());

        // when & then: NOTEBOOK_NOT_FOUND 예외가 발생
        assertThatThrownBy(() -> chatService.chatWithNotebook(1L, 999L, new AiChatRequest("질문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTEBOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅 이력 조회 시 AI 답변의 references도 함께 반환한다")
    void getChatHistory_includesReferences() {
        // given: reference가 포함된 AI 채팅 이력 준비
        Long userId = 1L;
        Notebook notebook = createNotebook("테스트");

        ChatHistory aiChat = ChatHistory.builder()
                .notebook(notebook)
                .role("AI")
                .message("답변입니다.")
                .references(List.of(
                        ChatReference.builder()
                                .documentId(20L)
                                .documentTitle("이력 문서")
                                .sectionTitle("개요")
                                .pageNumber(2)
                                .chunkIndex(3)
                                .pageChunkIndex(0)
                                .content("근거 문장")
                                .sortOrder(0)
                                .build()
                ))
                .build();

        given(notebookRepository.findByIdAndUserId(1L, userId)).willReturn(Optional.of(notebook));
        given(chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(1L))
                .willReturn(List.of(aiChat));

        // when: 채팅 이력을 조회한다
        List<core_api.domain.chat.dto.ChatHistoryResponse> result =
                chatService.getChatHistory(userId, 1L);

        // then: 응답에 reference 정보가 함께 포함된다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReferences()).hasSize(1);
        assertThat(result.get(0).getReferences().get(0).getDocumentId()).isEqualTo(20L);
        assertThat(result.get(0).getReferences().get(0).getDocumentTitle()).isEqualTo("이력 문서");
        assertThat(result.get(0).getReferences().get(0).getSectionTitle()).isEqualTo("개요");
        assertThat(result.get(0).getReferences().get(0).getPageNumber()).isEqualTo(2);
        assertThat(result.get(0).getReferences().get(0).getChunkIndex()).isEqualTo(3);
        assertThat(result.get(0).getReferences().get(0).getPageChunkIndex()).isEqualTo(0);
        assertThat(result.get(0).getReferences().get(0).getContent()).isEqualTo("근거 문장");
    }

    @Test
    @DisplayName("채팅 요청 시 USER와 AI 대화가 모두 저장된다")
    void chatWithNotebook_savesUserAndAiHistories() throws Exception {
        // given
        Long userId = 1L;
        Long notebookId = 1L;
        Notebook notebook = createNotebook("저장 테스트");
        AiChatRequest request = new AiChatRequest("질문 내용");

        AiChatResponse response = new AiChatResponse();
        setField(response, "answer", "AI 답변");
        setField(response, "reference_chunks", List.of());

        given(notebookRepository.findByIdAndUserId(notebookId, userId)).willReturn(Optional.of(notebook));
        given(chatMemoryRepository.findByNotebookId(notebookId)).willReturn(Optional.empty());
        given(chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId)).willReturn(new ArrayList<>());
        given(chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId)).willReturn(List.of());
        doReturn(response).when(aiWorkerClient)
                .askQuestionWithHistory(eq(notebookId), eq("질문 내용"), isNull(), anyList());
        given(chatHistoryRepository.save(any(ChatHistory.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        chatService.chatWithNotebook(userId, notebookId, request);

        // then
        ArgumentCaptor<ChatHistory> historyCaptor = ArgumentCaptor.forClass(ChatHistory.class);
        then(chatHistoryRepository).should(times(2)).save(historyCaptor.capture());

        List<ChatHistory> savedHistories = historyCaptor.getAllValues();
        assertThat(savedHistories).hasSize(2);
        assertThat(savedHistories.get(0).getRole()).isEqualTo("USER");
        assertThat(savedHistories.get(0).getMessage()).isEqualTo("질문 내용");
        assertThat(savedHistories.get(1).getRole()).isEqualTo("AI");
        assertThat(savedHistories.get(1).getMessage()).isEqualTo("AI 답변");
    }

    @Test
    @DisplayName("요약 대상 대화가 충분히 쌓이면 ChatMemory를 새로 저장한다")
    void chatWithNotebook_createsChatMemoryWhenThresholdReached() throws Exception {
        // given
        Long userId = 1L;
        Long notebookId = 1L;
        Notebook notebook = createNotebook("메모리 테스트");
        AiChatRequest request = new AiChatRequest("새 질문");

        AiChatResponse response = new AiChatResponse();
        setField(response, "answer", "새 답변");
        setField(response, "reference_chunks", List.of());

        List<ChatHistory> recentHistory = List.of(
                historyWithId(notebook, 6L, "AI", "답변3"),
                historyWithId(notebook, 5L, "USER", "질문3"),
                historyWithId(notebook, 4L, "AI", "답변2"),
                historyWithId(notebook, 3L, "USER", "질문2"),
                historyWithId(notebook, 2L, "AI", "답변1"),
                historyWithId(notebook, 1L, "USER", "질문1")
        );

        List<ChatHistory> allHistories = List.of(
                historyWithId(notebook, 1L, "USER", "질문1"),
                historyWithId(notebook, 2L, "AI", "답변1"),
                historyWithId(notebook, 3L, "USER", "질문2"),
                historyWithId(notebook, 4L, "AI", "답변2"),
                historyWithId(notebook, 5L, "USER", "질문3"),
                historyWithId(notebook, 6L, "AI", "답변3"),
                historyWithId(notebook, 7L, "USER", "질문4"),
                historyWithId(notebook, 8L, "AI", "답변4"),
                historyWithId(notebook, 9L, "USER", "질문5"),
                historyWithId(notebook, 10L, "AI", "답변5"),
                historyWithId(notebook, 11L, "USER", "질문6"),
                historyWithId(notebook, 12L, "AI", "답변6")
        );

        ChatHistory savedAiChat = historyWithId(notebook, 13L, "AI", "새 답변");

        given(notebookRepository.findByIdAndUserId(notebookId, userId)).willReturn(Optional.of(notebook));
        given(chatMemoryRepository.findByNotebookId(notebookId)).willReturn(Optional.empty());
        given(chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId)).willReturn(new ArrayList<>(recentHistory));
        given(chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(notebookId)).willReturn(allHistories);
        doReturn(response).when(aiWorkerClient)
                .askQuestionWithHistory(eq(notebookId), eq("새 질문"), isNull(), anyList());
        doReturn("압축 요약").when(aiWorkerClient)
                .summarizeConversation(eq(notebookId), isNull(), anyList());
        given(chatHistoryRepository.save(any(ChatHistory.class)))
                .willAnswer(invocation -> {
                    ChatHistory chatHistory = invocation.getArgument(0);
                    if ("AI".equals(chatHistory.getRole())) {
                        return savedAiChat;
                    }
                    return chatHistory;
                });

        // when
        chatService.chatWithNotebook(userId, notebookId, request);

        // then
        ArgumentCaptor<ChatMemory> chatMemoryCaptor = ArgumentCaptor.forClass(ChatMemory.class);
        then(chatMemoryRepository).should().save(chatMemoryCaptor.capture());

        ChatMemory savedMemory = chatMemoryCaptor.getValue();
        assertThat(savedMemory.getSummary()).isEqualTo("압축 요약");
        assertThat(savedMemory.getLastSummarizedChatHistoryId()).isEqualTo(6L);
    }

    @Test
    @DisplayName("AI Worker 장애가 발생하면 채팅 저장 없이 예외를 그대로 전달한다")
    void chatWithNotebook_fail_aiWorkerError() {
        // given
        Long userId = 1L;
        Long notebookId = 1L;
        Notebook notebook = createNotebook("장애 테스트");

        given(notebookRepository.findByIdAndUserId(notebookId, userId)).willReturn(Optional.of(notebook));
        given(chatMemoryRepository.findByNotebookId(notebookId)).willReturn(Optional.empty());
        given(chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId)).willReturn(new ArrayList<>());
        doThrow(new CustomException(ErrorCode.AI_WORKER_ERROR)).when(aiWorkerClient)
                .askQuestionWithHistory(eq(notebookId), eq("질문"), isNull(), anyList());

        // when / then
        assertThatThrownBy(() -> chatService.chatWithNotebook(userId, notebookId, new AiChatRequest("질문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_WORKER_ERROR);

        then(chatHistoryRepository).should(never()).save(any(ChatHistory.class));
        then(chatReferenceRepository).should(never()).saveAll(anyList());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Notebook createNotebook(String title) {
        User user = User.builder()
                .email("test@test.com")
                .password("password123!")
                .nickname("테스터")
                .role(Role.USER)
                .build();

        Notebook notebook = Notebook.builder()
                .title(title)
                .user(user)
                .build();

        try {
            setField(notebook, "id", 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return notebook;
    }

    private ChatHistory historyWithId(Notebook notebook, Long id, String role, String message) throws Exception {
        ChatHistory chatHistory = ChatHistory.builder()
                .notebook(notebook)
                .role(role)
                .message(message)
                .build();
        setField(chatHistory, "id", id);
        return chatHistory;
    }
}
