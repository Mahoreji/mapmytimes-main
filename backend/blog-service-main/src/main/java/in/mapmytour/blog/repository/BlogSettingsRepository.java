// BlogSettingsRepository.java
package in.mapmytour.blog.repository;

import in.mapmytour.blog.entity.BlogSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogSettingsRepository extends JpaRepository<BlogSettings, String> {

    Optional<BlogSettings> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);
}