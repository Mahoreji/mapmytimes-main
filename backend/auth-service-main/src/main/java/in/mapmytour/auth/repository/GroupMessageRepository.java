package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.GroupMessage;
import in.mapmytour.auth.entity.TravelGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, String> {
    Page<GroupMessage> findByGroupOrderByCreatedAtDesc(TravelGroup group, Pageable pageable);
    List<GroupMessage> findByGroupOrderByCreatedAtAsc(TravelGroup group);
    long countByGroup(TravelGroup group);
}

