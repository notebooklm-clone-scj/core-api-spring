package core_api.domain.document;

import core_api.domain.notebook.Notebook;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Document {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private Notebook notebook;

    @Column(nullable = false)
    private String filename;

    @Column(columnDefinition = "TEXT")
    private String summary; // AI가 만들어준 3줄 요약

    private int totalPages; // 총 페이지 수
    private int fullTextLength; // 추출된 텍스트 길이

    @Column(nullable = false)
    private String status; //문서 분석 상태(PROCESSING, COMPLETED, FAILED)

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Document(Notebook notebook, String filename, String summary, int totalPages, int fullTextLength, String status) {
        this.notebook = notebook;
        this.filename = filename;
        this.summary = summary;
        this.totalPages = totalPages;
        this.fullTextLength = fullTextLength;
        this.status = (status != null) ? status : "PROCESSING";
    }

    // 분석 성공 시 호출
    public void completeAnalysis(String summary, int totalPages, int fullTextLength) {
        this.summary = summary;
        this.totalPages = totalPages;
        this.fullTextLength = fullTextLength;
        this.status = "COMPLETED";
    }

    // 분석 실패 시 호출
    public void failAnalysis() {
        this.status = "FAILED";
    }

}
