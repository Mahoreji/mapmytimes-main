package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.CirclePoll;
import in.mapmytour.auth.entity.CirclePollOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CirclePollOptionRepository extends JpaRepository<CirclePollOption, String> {

    List<CirclePollOption> findByPollOrderBySortOrderAsc(CirclePoll poll);
}
