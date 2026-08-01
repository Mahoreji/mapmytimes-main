package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.TravelGroup;
import in.mapmytour.auth.entity.TravelGroupMember;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelGroupMemberRepository extends JpaRepository<TravelGroupMember, String> {
    List<TravelGroupMember> findByGroup(TravelGroup group);
    List<TravelGroupMember> findByUser(User user);
    Optional<TravelGroupMember> findByGroupAndUser(TravelGroup group, User user);
    boolean existsByGroupAndUser(TravelGroup group, User user);
    long countByGroupAndStatus(TravelGroup group, String status);
}

