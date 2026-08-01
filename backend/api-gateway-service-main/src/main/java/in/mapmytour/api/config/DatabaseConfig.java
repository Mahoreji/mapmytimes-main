package in.mapmytour.api.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Production-Hardened Database Configuration for API Gateway.
 * 
 * High availability features:
 *   - Connection Retries for Flyway
 *   - HikariCP Pool Optimization
 *   - Non-blocking R2DBC for application logic
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    @Value("${spring.flyway.url}")
    private String flywayUrl;

    @Value("${spring.flyway.user}")
    private String flywayUser;

    @Value("${spring.flyway.password}")
    private String flywayPassword;

    /**
     * Enterprise HikariCP DataSource for Flyway.
     * Tuned for infrequent migration writes.
     */
    @Bean
    public DataSource flywayDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(flywayUrl);
        config.setUsername(flywayUser);
        config.setPassword(flywayPassword);
        config.setDriverClassName("org.postgresql.Driver");
        
        config.setMaximumPoolSize(2); // Only need a small pool for migrations
        config.setConnectionTimeout(30000);
        config.setPoolName("Flyway-Migration-Pool");
        
        return new HikariDataSource(config);
    }

    /**
     * Resilient Flyway Migration.
     * Retries up to 10 times to allow the DB to wake up/start in parallel.
     */
    @Bean
    public Flyway flyway(DataSource flywayDataSource) {
        log.info("Production Security: Initializing Resilient Flyway Migration...");
        
        Flyway flyway = Flyway.configure()
                .dataSource(flywayDataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .connectRetries(10) // Vital for production cloud/docker environments
                .load();
        
        try {
            flyway.migrate();
        } catch (Exception e) {
            log.error("CRITICAL: Gateway Security Database Migration Failed!", e);
            // In production, we fail-fast here because security tables MUST exist
            throw e;
        }
        
        return flyway;
    }
}
