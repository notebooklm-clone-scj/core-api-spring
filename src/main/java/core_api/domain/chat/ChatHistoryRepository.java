package core_api.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // 특정 노트북의 대화 기록을 생성 시간 오름차순(과거->최신)으로 가져오기
    List<ChatHistory> findAllByNotebookIdOrderByCreatedAtAsc(Long notebookId);

    // 특정 노트북의 대화 기록을 생성 시간 내림차순으로 6개 가져오기 (AI 3, 유저 3)
    List<ChatHistory> findTop6ByNotebookIdOrderByCreatedAtDesc(Long notebookId);

    List<ChatHistory> findByNotebookIdAndIdGreaterThanOrderByCreatedAtAsc(Long notebookId, Long ChatHistoryId);
}