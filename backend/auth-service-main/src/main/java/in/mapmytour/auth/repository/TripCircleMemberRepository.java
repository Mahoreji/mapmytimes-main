package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.TripCircle;
import in.mapmytour.auth.entity.TripCircleMember;
import in.mapmytour.auth.entity.TripCircleMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripCircleMemberRepository extends JpaRepository<TripCircleMember, String> {

    List<TripCircleMember> findByUserId(String userId);

    List<TripCircleMember> findByCircleAndLeftAtIsNull(TripCircle circle);

    Optional<TripCircleMember> findByCircleAndUserIdAndLeftAtIsNull(TripCircle circle, String userId);

    boolean existsByCircleAndUserIdAndLeftAtIsNull(TripCircle circle, String userId);

    List<TripCircleMember> findByCircleAndRoleAndLeftAtIsNull(TripCircle circle, TripCircleMemberRole role);
}
