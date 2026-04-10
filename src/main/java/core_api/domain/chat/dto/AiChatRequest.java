package core_api.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequest {

    @NotBlank(message = "질문은 필수입니다.")
    private String question;
}
