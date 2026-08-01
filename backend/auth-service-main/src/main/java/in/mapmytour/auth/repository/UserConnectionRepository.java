package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.entity.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, String> {
    List<UserConnection> findByUserAndIsActiveTrue(User user);

    List<UserConnection> findByConnectedUserAndIsActiveTrue(User connectedUser);

    Optional<UserConnection> findByUserAndConnectedUser(User user, User connectedUser);

    boolean existsByUserAndConnectedUser(User user, User connectedUser);

    /**
     * Count active connections for a user
     */
    long countByUserAndIsActiveTrue(User user);

    /**
     * Batch fetch active connections for multiple users (avoids N+1 queries)
     */
    @Query("SELECT uc FROM UserConnection uc WHERE uc.user.id IN :userIds AND uc.isActive = true")
    List<UserConnection> findByUserIdInAndIsActiveTrue(@Param("userIds") Set<String> userIds);

    /**
     * Optimized queries to get only emails as strings to avoid Hibernate session issues
     */
    @Query("SELECT uc.connectedUser.email FROM UserConnection uc WHERE uc.user.email = :email AND uc.isActive = true")
    Set<String> findAllConnectedEmailsByUserEmail(@Param("email") String email);

    @Query("SELECT uc.user.email FROM UserConnection uc WHERE uc.connectedUser.email = :email AND uc.isActive = true")
    Set<String> findAllUserEmailsByConnectedUserEmail(@Param("email") String email);

    /**
     * Efficiently count mutual connections between two users using a join query
     */
    @Query("SELECT COUNT(uc1) FROM UserConnection uc1 " +
           "JOIN UserConnection uc2 ON uc1.connectedUser = uc2.connectedUser " +
           "WHERE uc1.user = :user1 AND uc2.user = :user2 " +
           "AND uc1.isActive = true AND uc2.isActive = true")
    long countMutualConnections(@Param("user1") User user1, @Param("user2") User user2);
}
