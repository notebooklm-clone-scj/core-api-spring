package core_api.domain.chat;

import core_api.domain.chat.dto.AiChatRequest;
import core_api.domain.chat.dto.AiChatResponse;
import core_api.domain.notebook.NotebookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final NotebookRepository notebookRepository;
    private final AiWorkerClient aiWorkerClient;

    @Transactional(readOnly = true)
    public AiChatResponse chatWithNotebook(Long notebookId, AiChatRequest request) {
        // 노트북 존재 유무 확인
        if (!notebookRepository.existsById(notebookId)) {
            throw new IllegalArgumentException("해당 노트북을 찾을 수 없습니다.");
        }

        // 파이썬 서버에 질문 후 결과 반환
        // 추후 채팅 내역 DB 저장 로직 추가할 예정
        return aiWorkerClient.askQuestion(request.getQuestion());
    }
}
