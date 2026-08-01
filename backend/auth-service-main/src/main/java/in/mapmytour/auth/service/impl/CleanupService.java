package in.mapmytour.auth.service.impl;

import in.mapmytour.auth.repository.OtpTokenRepository;
import in.mapmytour.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpTokenRepository otpTokenRepository;

    /**
     * Clean up expired refresh tokens every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    @Transactional
    public void cleanupExpiredRefreshTokens() {
        try {
            refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
            log.info("Cleaned up expired refresh tokens");
        } catch (Exception e) {
            log.error("Error cleaning up refresh tokens", e);
        }
    }

    /**
     * Clean up expired OTP tokens every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    @Transactional
    public void cleanupExpiredOtpTokens() {
        try {
            otpTokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
            log.info("Cleaned up expired OTP tokens");
        } catch (Exception e) {
            log.error("Error cleaning up OTP tokens", e);
        }
    }
}