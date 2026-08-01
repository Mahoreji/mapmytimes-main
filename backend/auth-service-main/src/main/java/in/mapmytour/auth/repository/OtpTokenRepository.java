package in.mapmytour.auth.repository;

import in.mapmytour.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, String> {

    Optional<OtpToken> findByEmailAndOtpAndType(String email, String otp, OtpToken.OtpType type);

    Optional<OtpToken> findByEmailAndTypeAndIsUsedFalse(String email, OtpToken.OtpType type);

    Optional<OtpToken> findByVerificationToken(String verificationToken);

    @Modifying
    @Query("UPDATE OtpToken ot SET ot.isUsed = true WHERE ot.id = :id")
    void markAsUsed(@Param("id") String id);

    @Modifying
    @Query("UPDATE OtpToken ot SET ot.attempts = ot.attempts + 1 WHERE ot.id = :id")
    void incrementAttempts(@Param("id") String id);

    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.expiresAt < :now OR ot.isUsed = true")
    void deleteExpiredAndUsedTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(ot) FROM OtpToken ot WHERE ot.email = :email AND ot.type = :type AND ot.createdAt > :since")
    long countRecentOtpsByEmailAndType(@Param("email") String email, @Param("type") OtpToken.OtpType type, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE OtpToken ot SET ot.isUsed = true WHERE ot.email = :email AND ot.type = :type")
    void invalidateAllOtpsByEmailAndType(@Param("email") String email, @Param("type") OtpToken.OtpType type);
}