package core_api.domain.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_history_id", nullable = false)
    private ChatHistory chatHistory;

    @Column
    private Long documentId;

    @Column
    private String documentTitle;

    @Column
    private String sectionTitle;

    @Column(nullable = false)
    private int pageNumber;

    @Column
    private Integer chunkIndex;

    @Column
    private Integer pageChunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int sortOrder;
}
