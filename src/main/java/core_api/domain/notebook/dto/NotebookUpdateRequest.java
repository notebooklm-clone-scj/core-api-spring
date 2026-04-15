package core_api.domain.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotebookUpdateRequest {

    @NotBlank(message = "노트북 제목은 필수입니다.")
    @Size(max = 100, message = "노트북 제목은 100자 이하여야 합니다.")
    private String title;
}
