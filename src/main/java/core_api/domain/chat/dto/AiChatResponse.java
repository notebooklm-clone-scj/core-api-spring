package core_api.domain.chat.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiChatResponse {
    private String answer;

    private List<ReferenceChunk> reference_chunks;

    // 파이썬이 보내주는 딕셔너리 모양에 맞춘 내부 클래스 생성
    @Getter
    @NoArgsConstructor
    public static class ReferenceChunk {
        private int page_number; // 참고한 페이지
        private String content; //참고한 내용
    }
}
