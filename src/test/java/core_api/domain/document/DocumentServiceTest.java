package core_api.domain.document;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.notebook.Notebook;
import core_api.domain.notebook.NotebookRepository;
import core_api.domain.user.Role;
import core_api.domain.user.User;
import core_api.global.exception.CustomException;
import core_api.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private NotebookRepository notebookRepository;

    @Mock
    private DocumentAsyncWorker documentAsyncWorker;

    @Mock
    private AiWorkerClient aiWorkerClient;

    @InjectMocks
    private DocumentService documentService;

    @Test
    @DisplayName("내 소유 노트북의 문서 목록만 조회할 수 있다")
    void getDocumentsByNotebook_success() {
        Long userId = 1L;
        Long notebookId = 2L;
        Notebook notebook = createNotebook(notebookId);
        Document document = Document.builder()
                .notebook(notebook)
                .filename("sample.pdf")
                .summary("요약")
                .totalPages(3)
                .status(DocumentStatus.COMPLETED)
                .build();

        given(notebookRepository.findByIdAndUserId(notebookId, userId)).willReturn(Optional.of(notebook));
        given(documentRepository.findAllByNotebookId(notebookId)).willReturn(List.of(document));

        List<core_api.domain.document.dto.DocumentResponse> responses =
                documentService.getDocumentsByNotebook(userId, notebookId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getFilename()).isEqualTo("sample.pdf");
    }

    @Test
    @DisplayName("내 소유가 아닌 노트북의 문서는 조회할 수 없다")
    void getDocumentsByNotebook_fail_notOwnedNotebook() {
        given(notebookRepository.findByIdAndUserId(2L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentsByNotebook(1L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTEBOOK_NOT_FOUND);
    }

    private Notebook createNotebook(Long notebookId) {
        User user = User.builder()
                .email("test@test.com")
                .password("password123!")
                .nickname("테스터")
                .role(Role.USER)
                .build();

        Notebook notebook = Notebook.builder()
                .title("테스트 노트북")
                .user(user)
                .build();

        try {
            java.lang.reflect.Field idField = Notebook.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notebook, notebookId);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return notebook;
    }
}
