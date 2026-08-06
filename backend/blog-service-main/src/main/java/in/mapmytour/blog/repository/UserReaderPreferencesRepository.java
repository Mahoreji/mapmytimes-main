package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.UserReaderPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReaderPreferencesRepository extends JpaRepository<UserReaderPreferences, String> {

    Optional<UserReaderPreferences> findByUserId(String userId);
}
