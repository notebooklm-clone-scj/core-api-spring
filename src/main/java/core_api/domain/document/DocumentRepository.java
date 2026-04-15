package core_api.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findAllByNotebookId(Long notebookId);

    long countByNotebookId(Long notebookId);

    long countByNotebookUserId(Long userId);

    java.util.Optional<Document> findByIdAndNotebookId(Long documentId, Long notebookId);
}
