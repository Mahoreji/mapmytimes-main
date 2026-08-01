package in.mapmytour.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String phone;

    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio; // User biography/description

    private String coverImageUrl; // Profile cover image

    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Embedded
    private Address address;

    @Embedded
    private UserPreferences preferences;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer loginAttempts = 0;

    private LocalDateTime lastLoginAt;

    /**
     * Last time this user was seen active in the system
     * (HTTP request, WebSocket connect, messaging activity, etc.).
     */
    private LocalDateTime lastSeenAt;

    private LocalDateTime lockedUntil;

    // OAuth fields
    @Column(name = "google_id")
    private String googleId;

    @Column(name = "facebook_id")
    private String facebookId;

    @Column(name = "provider")
    private String provider;

    // Email verification
    @JsonIgnore
    private String emailVerificationToken;
    private LocalDateTime emailVerificationExpiresAt;

    // Password reset
    @JsonIgnore
    private String passwordResetToken;
    private LocalDateTime passwordResetExpiresAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private java.util.Set<Role> roles = new java.util.HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return isActive;
    }

    public String getProfileImageUrl() {
        if (avatarUrl != null) {
            return avatarUrl;
        }
        return "https://via.placeholder.com/150";
    }

    public boolean isVerified() {
        return isVerified;
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }

    public enum UserRole {
        USER, ADMIN, B2B, SUPER_ADMIN, EMPLOYEE
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String country;
        private String postalCode;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserPreferences {

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "email", column = @Column(name = "notification_email")),
                @AttributeOverride(name = "sms", column = @Column(name = "notification_sms")),
                @AttributeOverride(name = "push", column = @Column(name = "notification_push"))
        })
        @Builder.Default
        private NotificationPreferences notifications = NotificationPreferences.builder()
                .email(true)
                .sms(false)
                .push(true)
                .build();

        @Embedded
        @AttributeOverrides({
                @AttributeOverride(name = "profileVisible", column = @Column(name = "privacy_profile_visible")),
                @AttributeOverride(name = "showBookingHistory", column = @Column(name = "privacy_show_booking_history")),
                @AttributeOverride(name = "showEmail", column = @Column(name = "privacy_show_email")),
                @AttributeOverride(name = "showPhone", column = @Column(name = "privacy_show_phone")),
                @AttributeOverride(name = "showDateOfBirth", column = @Column(name = "privacy_show_dob")),
                @AttributeOverride(name = "showAddress", column = @Column(name = "privacy_show_address")),
                @AttributeOverride(name = "showStreet", column = @Column(name = "privacy_show_street")),
                @AttributeOverride(name = "showCity", column = @Column(name = "privacy_show_city")),
                @AttributeOverride(name = "showState", column = @Column(name = "privacy_show_state")),
                @AttributeOverride(name = "showPostalCode", column = @Column(name = "privacy_show_postal_code"))
        })
        @Builder.Default
        private PrivacyPreferences privacy = PrivacyPreferences.builder()
                .profileVisible(true)
                .showBookingHistory(true)
                .showEmail(true)
                .showPhone(true)
                .showDateOfBirth(true)
                .showAddress(true)
                .showStreet(true)
                .showCity(true)
                .showState(true)
                .showPostalCode(true)
                .build();

        @Column(length = 1000)
        private String interests; // JSON string for interests array
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NotificationPreferences {
        @Builder.Default
        private Boolean email = true;

        @Builder.Default
        private Boolean sms = false;

        @Builder.Default
        private Boolean push = true;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PrivacyPreferences {
        @Builder.Default
        private Boolean profileVisible = true;

        @Builder.Default
        private Boolean showBookingHistory = true;

        // Granular field-level privacy settings (Instagram-like)
        // Only applicable when profileVisible = true
        @Builder.Default
        private Boolean showEmail = true;

        @Builder.Default
        private Boolean showPhone = true;

        @Builder.Default
        private Boolean showDateOfBirth = true;

        @Builder.Default
        private Boolean showAddress = true; // Controls all address fields

        // Individual address field controls (if showAddress = true)
        @Builder.Default
        private Boolean showStreet = true;

        @Builder.Default
        private Boolean showCity = true;

        @Builder.Default
        private Boolean showState = true;

        @Builder.Default
        private Boolean showPostalCode = true;

        // Country is always visible (for discovery purposes)
    }

    // Helper methods
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isAccountNonLocked() {
        return !isLocked || (lockedUntil != null && lockedUntil.isBefore(LocalDateTime.now()));
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean isEnabled() {
        return isActive && isVerified;
    }

    public void incrementLoginAttempts() {
        this.loginAttempts++;
        if (this.loginAttempts >= 5) {
            this.isLocked = true;
            this.lockedUntil = LocalDateTime.now().plusHours(1); // Lock for 1 hour
        }
    }

    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.isLocked = false;
        this.lockedUntil = null;
        LocalDateTime now = LocalDateTime.now();
        this.lastLoginAt = now;
        this.lastSeenAt = now;
    }
}