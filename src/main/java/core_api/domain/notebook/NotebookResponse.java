package core_api.domain.notebook;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotebookResponse {
    private Long id;
    private String title;
    private LocalDateTime createdAt;

    public static NotebookResponse from(Notebook notebook) {
        return new NotebookResponse(
                notebook.getId(),
                notebook.getTitle(),
                notebook.getCreatedAt()
        );
    }
}
