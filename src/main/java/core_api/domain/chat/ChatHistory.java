package core_api.domain.chat;

import core_api.domain.notebook.Notebook;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어느 노트북에서 나눈 대화인지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id")
    private Notebook notebook;

    // 누가 말했는지 구분 ("USER" 혹은 "AI")
    @Column(nullable = false)
    private String role;

    // 대화 내용 (text 타입으로 저장)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // 대화가 생성된 시간
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // DB에 저장되기 직전 자동으로 현재 시간 세팅
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
