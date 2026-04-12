package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.notebook.Notebook;
import core_api.domain.notebook.NotebookRepository;
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
        Long notebookId = 1L;
        Notebook notebook = Notebook.builder().title("테스트 노트북").build();
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
        setField(chunk1, "page_number", 3);
        setField(chunk1, "content", "첫 번째 근거 문장");

        AiChatResponse.ReferenceChunk chunk2 = new AiChatResponse.ReferenceChunk();
        setField(chunk2, "page_number", 7);
        setField(chunk2, "content", "두 번째 근거 문장");

        setField(response, "reference_chunks", List.of(chunk1, chunk2));

        given(notebookRepository.findById(notebookId)).willReturn(Optional.of(notebook));
        given(chatHistoryRepository.findTop6ByNotebookIdOrderByCreatedAtDesc(notebookId))
                .willReturn(new ArrayList<>(List.of(oldAi, oldUser)));
        given(aiWorkerClient.askQuestionWithHistory(any(), any(), any())).willReturn(response);
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
        chatService.chatWithNotebook(notebookId, request);

        // then: reference chunks가 ChatReference로 저장
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatReference>> captor = ArgumentCaptor.forClass(List.class);

        then(chatReferenceRepository).should().saveAll(captor.capture());

        List<ChatReference> savedReferences = captor.getValue();

        assertThat(savedReferences).hasSize(2);

        assertThat(savedReferences.get(0).getChatHistory()).isEqualTo(savedAiChat);
        assertThat(savedReferences.get(0).getPageNumber()).isEqualTo(3);
        assertThat(savedReferences.get(0).getContent()).isEqualTo("첫 번째 근거 문장");
        assertThat(savedReferences.get(0).getSortOrder()).isEqualTo(0);

        assertThat(savedReferences.get(1).getChatHistory()).isEqualTo(savedAiChat);
        assertThat(savedReferences.get(1).getPageNumber()).isEqualTo(7);
        assertThat(savedReferences.get(1).getContent()).isEqualTo("두 번째 근거 문장");
        assertThat(savedReferences.get(1).getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 노트북으로 채팅하면 예외가 발생한다")
    void chatWithNotebook_fail_notebookNotFound() {
        // given: 존재하지 않는 노트북 ID
        given(notebookRepository.findById(999L)).willReturn(Optional.empty());

        // when & then: NOTEBOOK_NOT_FOUND 예외가 발생
        assertThatThrownBy(() -> chatService.chatWithNotebook(999L, new AiChatRequest("질문")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTEBOOK_NOT_FOUND);
    }

    @Test
    @DisplayName("채팅 이력 조회 시 AI 답변의 references도 함께 반환한다")
    void getChatHistory_includesReferences() {
        // given: reference가 포함된 AI 채팅 이력 준비
        Notebook notebook = Notebook.builder().title("테스트").build();

        ChatHistory aiChat = ChatHistory.builder()
                .notebook(notebook)
                .role("AI")
                .message("답변입니다.")
                .references(List.of(
                        ChatReference.builder()
                                .pageNumber(2)
                                .content("근거 문장")
                                .sortOrder(0)
                                .build()
                ))
                .build();

        given(chatHistoryRepository.findAllByNotebookIdOrderByCreatedAtAsc(1L))
                .willReturn(List.of(aiChat));

        // when: 채팅 이력을 조회한다
        List<core_api.domain.chat.dto.ChatHistoryResponse> result =
                chatService.getChatHistory(1L);

        // then: 응답에 reference 정보가 함께 포함된다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReferences()).hasSize(1);
        assertThat(result.get(0).getReferences().get(0).getPageNumber()).isEqualTo(2);
        assertThat(result.get(0).getReferences().get(0).getContent()).isEqualTo("근거 문장");
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
