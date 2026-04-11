package core_api.domain.chat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatReferenceRepository extends JpaRepository<ChatReference, Long> {
}
