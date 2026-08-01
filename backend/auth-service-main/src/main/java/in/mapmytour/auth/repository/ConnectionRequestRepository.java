package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.ConnectionRequest;
import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, String> {
    Optional<ConnectionRequest> findByIdAndRecipient(String id, User recipient);
    Optional<ConnectionRequest> findByIdAndRequester(String id, User requester);
    Optional<ConnectionRequest> findByRequesterAndRecipient(User requester, User recipient);
    List<ConnectionRequest> findByRecipientAndStatus(User recipient, String status);
    List<ConnectionRequest> findByRequesterAndStatus(User requester, String status);
    boolean existsByRequesterAndRecipient(User requester, User recipient);
}

