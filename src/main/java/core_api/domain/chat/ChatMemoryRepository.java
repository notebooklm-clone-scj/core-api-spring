package core_api.domain.chat;

import core_api.domain.notebook.Notebook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMemoryRepository extends JpaRepository<ChatMemory, Long> {
    Optional<ChatMemory> findByNotebookId(Long notebookId);
}
