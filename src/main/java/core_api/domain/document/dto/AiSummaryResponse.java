package core_api.domain.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryResponse {
    private int total_pages;
    private String text_preview;
    private int full_text_length;
    private String filename;
    private String summary;
}
