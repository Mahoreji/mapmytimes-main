package in.mapmytour.blog;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
public class BlogServiceApplication {

	private final Environment environment;

	public BlogServiceApplication(Environment environment) {
		this.environment = environment;
	}

	public static void main(String[] args) {
		// Enable ANSI colors for better console output
		System.setProperty("spring.output.ansi.enabled", "always");

		// 1. Load .env file automatically for production consistency
		try {
			log.info("🔍 Attempting to load .env file for security standardization...");
			Dotenv d = Dotenv.configure()
					.directory("./")
					.ignoreIfMalformed()
					.ignoreIfMissing()
					.load();

			d.entries().forEach(entry -> {
				// Ensure environment variables are committed to JVM system properties early
				if (System.getProperty(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
			log.info("✅ .env file loaded successfully. Keys found: {}", 
				d.entries().stream().map(io.github.cdimascio.dotenv.DotenvEntry::getKey).toList());
		} catch (Exception e) {
			log.error("❌ Failed to load .env file: {}", e.getMessage());
		}

		SpringApplication.run(BlogServiceApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationReady() {
		String serverPort = environment.getProperty("server.port", "8089");
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
				environment.getActiveProfiles().length > 0 ?
						String.join(", ", environment.getActiveProfiles()) : "default"
		);
	}
}