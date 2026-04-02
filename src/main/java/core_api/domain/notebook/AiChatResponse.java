package core_api.domain.notebook;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiChatResponse {
    private String answer;
    private List<String> reference_chunks;
}
