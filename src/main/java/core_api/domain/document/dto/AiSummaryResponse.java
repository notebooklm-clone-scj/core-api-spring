package core_api.domain.document.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// FastAPI의 PDF 요약 응답을 그대로 받기 위한 DTO
public class AiSummaryResponse {
    private int total_pages;
    private String text_preview;
    private int full_text_length;
    private String filename;
    private String summary;
    private int chunks_saved; // 운영 로그에서 chunk 개수도 쓰기 때문에 chunks_saved 필드를 추가
}
