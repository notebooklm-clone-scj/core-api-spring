package core_api.domain.notebook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotebookCreateRequest {

    @NotBlank(message = "노트북 제목은 필수입니다.")
    private String title;
}
