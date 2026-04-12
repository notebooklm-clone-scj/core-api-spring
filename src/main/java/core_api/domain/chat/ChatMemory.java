package core_api.domain.chat;

import core_api.domain.notebook.Notebook;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false, unique = true)
    private Notebook notebook;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "last_summarized_chat_history_id")
    private Long lastSummarizedChatHistoryId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public ChatMemory(Notebook notebook, String summary, Long lastSummarizedChatHistoryId) {
        this.notebook = notebook;
        this.summary = summary;
        this.lastSummarizedChatHistoryId = lastSummarizedChatHistoryId;
    }

    public void updateSummary(String summary, Long lastSummarizedChatHistoryId) {
        this.summary = summary;
        this.lastSummarizedChatHistoryId = lastSummarizedChatHistoryId;
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
}
