package core_api.domain.notebook;

import core_api.domain.notebook.dto.NotebookCreateRequest;
import core_api.domain.notebook.dto.NotebookResponse;
import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class NotebookServiceTest {

    @Mock
    private NotebookRepository notebookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotebookService notebookService;

    @Test
    @DisplayName("유저가 새로운 노트북을 성공적으로 생성")
    void createNotebook_success() {
        //given
        Long userId = 1L;
        NotebookCreateRequest request = new NotebookCreateRequest("첫번째");

        User fakeUser = User.builder().email("test@gmail.com").nickname("찬진").role(core_api.domain.user.Role.USER).build();

        given(userRepository.findById(userId)).willReturn(Optional.of(fakeUser));

        Notebook fakeSavedNotebook = Notebook.builder()
                .title(request.getTitle())
                .user(fakeUser)
                .build();
        given(notebookRepository.save(any(Notebook.class))).willReturn(fakeSavedNotebook);

        //when
        notebookService.createNotebook(userId, request);
        //then

    }

    @Test
    @DisplayName("내 노트북 목록을 조회하면 리스트 형태로 반환")
    void getNotebooks_success() {
        //given
        Long userId = 1L;
        User fakeUser = User.builder().email("test@gmail.com").role(core_api.domain.user.Role.USER).build();

        Notebook notebook1 = Notebook.builder().title("프로젝트 A").user(fakeUser).build();
        Notebook notebook2 = Notebook.builder().title("프로젝트 B").user(fakeUser).build();

        given(notebookRepository.findAllByUserId(userId)).willReturn(List.of(notebook1, notebook2));

        //when
        List<NotebookResponse> responses = notebookService.getNotebooks(userId);

        //then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("프로젝트 A");
    }

    @Test
    @DisplayName("존재하지 않는 유저로 노트북을 만들려 하면 에러 발생")
    void createNotebook_fail_userNotFound() {
        // given
        NotebookCreateRequest request = new NotebookCreateRequest("유령 프로젝트");

        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when
        // then
        assertThatThrownBy(() -> notebookService.createNotebook(999L, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("내 소유가 아닌 노트북은 제목을 변경할 수 없다")
    void updateNotebookTitle_fail_notOwnedNotebook() {
        given(notebookRepository.findByIdAndUserId(10L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> notebookService.updateNotebookTitle(1L, 10L, createNotebookUpdateRequest("새 제목")))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.NOTEBOOK_NOT_FOUND);
    }

    private core_api.domain.notebook.dto.NotebookUpdateRequest createNotebookUpdateRequest(String title) {
        core_api.domain.notebook.dto.NotebookUpdateRequest request =
                new core_api.domain.notebook.dto.NotebookUpdateRequest();

        try {
            java.lang.reflect.Field titleField =
                    core_api.domain.notebook.dto.NotebookUpdateRequest.class.getDeclaredField("title");
            titleField.setAccessible(true);
            titleField.set(request, title);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return request;
    }
}
