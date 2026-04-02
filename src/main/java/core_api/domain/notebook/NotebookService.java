package core_api.domain.notebook;

import core_api.domain.chat.AiWorkerClient;
import core_api.domain.document.DocumentRepository;
import core_api.domain.notebook.dto.NotebookCreateRequest;
import core_api.domain.notebook.dto.NotebookResponse;
import core_api.domain.user.User;
import core_api.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotebookService {

    private final NotebookRepository notebookRepository;
    private final UserRepository userRepository;

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
    public List<NotebookResponse> getNotebooks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다"));

        return notebookRepository.findAllByUser(user).stream()
                .map(NotebookResponse::from)
                .collect(Collectors.toList());
    }

}
