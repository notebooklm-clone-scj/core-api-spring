package core_api.domain.notebook;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotebookCreateRequest {

    private Long userId;
    private String title;
}
