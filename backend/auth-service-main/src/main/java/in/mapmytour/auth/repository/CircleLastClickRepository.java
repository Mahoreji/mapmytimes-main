package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.CircleLastClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CircleLastClickRepository extends JpaRepository<CircleLastClick, String> {

    Optional<CircleLastClick> findByUserId(String userId);
}
