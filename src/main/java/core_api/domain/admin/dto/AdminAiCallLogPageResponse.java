package core_api.domain.admin.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class AdminAiCallLogPageResponse {

    private List<AdminAiCallLogResponse> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
    private boolean hasNext;

    public static AdminAiCallLogPageResponse from(Page<AdminAiCallLogResponse> pageResult) {
        return AdminAiCallLogPageResponse.builder()
                .content(pageResult.getContent())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .hasNext(pageResult.hasNext())
                .build();
    }
}
