package in.mapmytour.auth.initializer;

import in.mapmytour.auth.entity.User;
import in.mapmytour.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) {
                createDummyUsers();
        }

        private void createDummyUsers() {
                // Common password for all dummy users
                String commonPassword = passwordEncoder.encode("Dummy@123");

                List<User> dummyUsers = Arrays.asList(
                                // Regular USER
                                User.builder()
                                                .email("user@mapmytour.in")
                                                .firstName("Map My Tour")
                                                .lastName("User")
                                                .password(commonPassword)
                                                .phone("9876543210")
                                                .dateOfBirth(LocalDate.of(1990, 5, 15))
                                                .gender(User.Gender.MALE)
                                                .address(User.Address.builder()
                                                                .street("123 User Street")
                                                                .city("Mumbai")
                                                                .state("Maharashtra")
                                                                .country("India")
                                                                .postalCode("400001")
                                                                .build())
                                                .preferences(getDefaultPreferences())
                                                .role(User.UserRole.USER)
                                                .isVerified(true)
                                                .isActive(true)
                                                .build(),

                                // ADMIN user
                                User.builder()
                                                .email("admin@mapmytour.in")
                                                .firstName("Map My Tour")
                                                .lastName("Admin")
                                                .password(commonPassword)
                                                .phone("9876543211")
                                                .dateOfBirth(LocalDate.of(1985, 8, 20))
                                                .gender(User.Gender.FEMALE)
                                                .address(User.Address.builder()
                                                                .street("456 Admin Avenue")
                                                                .city("Bangalore")
                                                                .state("Karnataka")
                                                                .country("India")
                                                                .postalCode("560001")
                                                                .build())
                                                .preferences(getDefaultPreferences())
                                                .role(User.UserRole.ADMIN)
                                                .isVerified(true)
                                                .isActive(true)
                                                .build(),

                                // B2B user
                                User.builder()
                                                .email("b2b@mapmytour.in")
                                                .firstName("Map My Tour")
                                                .lastName("Partner")
                                                .password(commonPassword)
                                                .phone("9876543212")
                                                .dateOfBirth(LocalDate.of(1988, 3, 10))
                                                .gender(User.Gender.OTHER)
                                                .address(User.Address.builder()
                                                                .street("789 Partner Road")
                                                                .city("Delhi")
                                                                .state("Delhi")
                                                                .country("India")
                                                                .postalCode("110001")
                                                                .build())
                                                .preferences(getDefaultPreferences())
                                                .role(User.UserRole.B2B)
                                                .isVerified(true)
                                                .isActive(true)
                                                .build(),

                                // Map My Tour Primary Super Admin (mapmytourr@gmail.com)
                                User.builder()
                                                .email("mapmytourr@gmail.com")
                                                .firstName("Map My Tour")
                                                .lastName("Super Admin")
                                                .password(passwordEncoder.encode("SuperAdmin123@"))
                                                .phone("9876543214")
                                                .dateOfBirth(LocalDate.of(1980, 1, 1))
                                                .gender(User.Gender.MALE)
                                                .address(User.Address.builder()
                                                                .street("Map My Tour HQ")
                                                                .city("Bangalore")
                                                                .state("Karnataka")
                                                                .country("India")
                                                                .postalCode("560001")
                                                                .build())
                                                .preferences(getAdminPreferences())
                                                .role(User.UserRole.SUPER_ADMIN)
                                                .isVerified(true)
                                                .isActive(true)
                                                .build());

                // Save users if they don't exist
                dummyUsers.forEach(user -> {
                        if (!userRepository.existsByEmail(user.getEmail())) {
                                userRepository.save(user);
                                log.info("Created dummy {} user: {}", user.getRole(), user.getEmail());
                        }
                });

                log.info("✅ Dummy users initialization completed");
        }

        private User.UserPreferences getDefaultPreferences() {
                return User.UserPreferences.builder()
                                .notifications(User.NotificationPreferences.builder()
                                                .email(true)
                                                .sms(false)
                                                .push(true)
                                                .build())
                                .privacy(User.PrivacyPreferences.builder()
                                                .profileVisible(true)
                                                .showBookingHistory(true)
                                                .build())
                                .interests("[\"hiking\", \"photography\", \"cultural experiences\"]")
                                .build();
        }

        private User.UserPreferences getAdminPreferences() {
                return User.UserPreferences.builder()
                                .notifications(User.NotificationPreferences.builder()
                                                .email(true)
                                                .sms(true)
                                                .push(true)
                                                .build())
                                .privacy(User.PrivacyPreferences.builder()
                                                .profileVisible(false)
                                                .showBookingHistory(false)
                                                .build())
                                .interests("[\"system administration\", \"security\", \"user management\"]")
                                .build();
        }
}