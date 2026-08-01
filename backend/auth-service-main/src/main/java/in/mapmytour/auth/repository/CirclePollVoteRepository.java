package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.CirclePoll;
import in.mapmytour.auth.entity.CirclePollOption;
import in.mapmytour.auth.entity.CirclePollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CirclePollVoteRepository extends JpaRepository<CirclePollVote, String> {

    Optional<CirclePollVote> findByPollAndUserId(CirclePoll poll, String userId);

    long countByPollAndOption(CirclePoll poll, CirclePollOption option);

    List<CirclePollVote> findByPoll(CirclePoll poll);
}
