package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndIsActiveTrue(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByFacebookId(String facebookId);

    Optional<User> findByPasswordResetToken(String token);

    Optional<User> findByEmailVerificationToken(String token);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isLocked = false AND u.isActive = true")
    Optional<User> findActiveUserByEmail(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.loginAttempts = 0, u.isLocked = false, u.lockedUntil = null, u.lastLoginAt = :loginTime WHERE u.id = :userId")
    void resetLoginAttempts(@Param("userId") String userId, @Param("loginTime") LocalDateTime loginTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.loginAttempts = u.loginAttempts + 1 WHERE u.id = :userId")
    void incrementLoginAttempts(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.isLocked = true, u.lockedUntil = :lockUntil WHERE u.id = :userId")
    void lockAccount(@Param("userId") String userId, @Param("lockUntil") LocalDateTime lockUntil);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.isVerified = true, u.emailVerificationToken = null, u.emailVerificationExpiresAt = null WHERE u.id = :userId")
    void verifyUser(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.password = :password, u.passwordResetToken = null, u.passwordResetExpiresAt = null WHERE u.id = :userId")
    void updatePasswordAndClearResetToken(@Param("userId") String userId, @Param("password") String password);

    long countByIsActive(boolean isActive);

    @Query("SELECT TO_CHAR(u.createdAt, 'YYYY-MM'), COUNT(u) FROM User u GROUP BY TO_CHAR(u.createdAt, 'YYYY-MM') ORDER BY 1 ASC")
    List<Object[]> countRegistrationsByMonth();
}