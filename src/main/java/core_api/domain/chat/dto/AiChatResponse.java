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
        private Long document_id; // 참고한 문서 ID
        private String document_title; // 참고한 문서 제목
        private String section_title; // 참고한 섹션 제목
        private int page_number; // 참고한 페이지
        private Integer chunk_index; // 문서 전체 기준 chunk 순서
        private Integer page_chunk_index; // 해당 페이지 안에서의 chunk 순서
        private String content; //참고한 내용
    }
}
