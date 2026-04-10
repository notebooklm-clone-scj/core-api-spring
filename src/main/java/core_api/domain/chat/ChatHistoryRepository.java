package core_api.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // 특정 노트북의 대화 기록을 생성 시간 오름차순(과거->최신)으로 가져오기
    List<ChatHistory> findAllByNotebookIdOrderByCreatedAtAsc(Long notebookId);

    List<ChatHistory> findTop6ByNotebookIdOrderByCreatedAtDesc(Long notebookId);
}