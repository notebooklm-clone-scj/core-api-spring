package core_api.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일 중복 기입 방지
    boolean existsByEmail(String email);
}
