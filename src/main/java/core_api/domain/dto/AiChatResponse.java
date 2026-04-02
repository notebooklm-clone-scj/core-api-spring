package core_api.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiChatResponse {
    private String answer;
    private List<String> reference_chunks;
}
