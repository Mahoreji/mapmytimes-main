package in.mapmytour.auth;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableFeignClients
@Slf4j
@EnableRetry
public class AuthServiceApplication {

	private final Environment environment;

	public AuthServiceApplication(Environment environment) {
		this.environment = environment;
	}

	public static void main(String[] args) {
		System.setProperty("spring.output.ansi.enabled", "always");

		// Load .env file automatically if it exists
		try {
			Dotenv dotenv = Dotenv.configure()
					.directory(".")
					.ignoreIfMissing()
					.load();

			// Set environment variables from .env file
			dotenv.entries().forEach(entry -> {
				String key = entry.getKey();
				String value = entry.getValue();
				// Only set if not already set as system property or environment variable
				if (System.getProperty(key) == null && System.getenv(key) == null) {
					System.setProperty(key, value);
				}
			});

			log.info("✅ Loaded environment variables from .env file");
		} catch (Exception e) {
			log.warn("⚠️  Could not load .env file (this is okay if using system environment variables): {}",
					e.getMessage());
		}

		SpringApplication.run(AuthServiceApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationReady() {
		String serverPort = environment.getProperty("server.port", "8081");
		String contextPath = environment.getProperty("server.servlet.context-path", "");
		String hostAddress = "localhost";

		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("Unable to determine host address", e);
		}

		log.info("\n----------------------------------------------------------\n" +
				"🚀 MapMyTour Application is running! Access URLs:\n" +
				"🏠 Local:      http://localhost:{}{}\n" +
				"🌐 External:   http://{}:{}{}\n" +
				"📖 API Docs:   http://localhost:{}{}/swagger-ui.html\n" +
				"🏥 Health:     http://localhost:{}{}/actuator/health\n" +
				"📊 Metrics:    http://localhost:{}{}/actuator/info\n" +
				"🔧 Profile:    {}\n" +
				"----------------------------------------------------------",
				serverPort, contextPath,
				hostAddress, serverPort, contextPath,
				serverPort, contextPath,
				serverPort, contextPath,
				serverPort, contextPath,
				environment.getActiveProfiles().length > 0 ? String.join(", ", environment.getActiveProfiles())
						: "default");
	}
}