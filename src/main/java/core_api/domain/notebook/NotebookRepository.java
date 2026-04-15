package core_api.domain.notebook;

import core_api.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotebookRepository extends JpaRepository<Notebook, Long> {

    List<Notebook> findAllByUser(User user);

    long countByUserId(Long userId);
}
