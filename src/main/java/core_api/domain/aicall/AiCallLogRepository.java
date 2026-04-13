package core_api.domain.aicall;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AiCallLogRepository extends JpaRepository<AiCallLog, Long>, JpaSpecificationExecutor<AiCallLog> {
}
