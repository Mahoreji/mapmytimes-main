package in.mapmytour.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SpringBootApplication
public class ApiGatewayServiceApplication {

	private static final Logger log = LoggerFactory.getLogger(ApiGatewayServiceApplication.class);
	private final Environment environment;
	private final org.springframework.data.redis.core.ReactiveRedisTemplate<String, String> redisTemplate;

	public ApiGatewayServiceApplication(Environment environment, 
                                        org.springframework.data.redis.core.ReactiveRedisTemplate<String, String> redisTemplate) {
		this.environment = environment;
		this.redisTemplate = redisTemplate;
	}

	public static void main(String[] args) {
		// Enable ANSI colors for better console output
		System.setProperty("spring.output.ansi.enabled", "always");

		// Fix IPv6/IPv4 connectivity issues - prefer IPv4 for localhost connections
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.net.preferIPv6Addresses", "false");

		// Load .env file automatically if it exists
		try {
			File envFile = new File(".env");
			if (envFile.exists()) {
				Dotenv dotenv = Dotenv.configure()
						.directory(".")
						.ignoreIfMissing()
						.load();

				dotenv.entries().forEach(entry -> {
					String key = entry.getKey();
					String value = entry.getValue();
					// Only set if not already set as system property or environment variable
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, value);
					}
				});
				log.info("✅ Successfully loaded environment variables from .env file");
			} else {
				log.warn("⚠️  No .env file found at: {} - relying on environment variables or defaults", envFile.getAbsolutePath());
			}
		} catch (Exception e) {
			log.warn("⚠️  Could not load .env file: {}", e.getMessage());
		}

		log.info("🚀 Starting MapMyTour API Gateway Service...");
		log.info("⏰ Startup Time: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

		SpringApplication.run(ApiGatewayServiceApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void applicationReady() {
		String serverPort = environment.getProperty("server.port", "8080");
		String contextPath = environment.getProperty("server.servlet.context-path", "");
		String hostAddress = "localhost";
		String applicationName = environment.getProperty("spring.application.name", "api-gateway-service");

		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("Unable to determine host address", e);
		}

		// Get configuration values
		boolean rateLimitEnabled = Boolean.parseBoolean(environment.getProperty("rate-limit.enabled", "false"));
		boolean eurekaEnabled = Boolean.parseBoolean(environment.getProperty("eureka.client.enabled", "false"));
		String activeProfiles = environment.getActiveProfiles().length > 0
				? String.join(", ", environment.getActiveProfiles())
				: "default";

		// Service URLs
		String authServiceUrl = environment.getProperty("AUTH_USER_SERVICE_URL", "http://localhost:8081");
		String paymentServiceUrl = environment.getProperty("PAYMENT_SERVICE_URL", "http://localhost:8088");
		String bookingServiceUrl = environment.getProperty("BOOKING_SERVICE_URL", "http://localhost:8089");
		String coreServiceUrl = environment.getProperty("CORE_SERVICE_URL", "http://localhost:8083");
		String chatServiceUrl = environment.getProperty("CHAT_SERVICE_URL", "https://localhost:8084");
		// Use HTTP and localhost by default for hotel-service in local dev
		String hotelServiceUrl = environment.getProperty("HOTEL_SERVICE_URL", "http://localhost:8092");

		log.info("\n" +
				"╔══════════════════════════════════════════════════════════════════════════════════╗\n" +
				"║                    🗺️  MAPMYTOUR API GATEWAY READY  🎒                          ║\n" +
				"╚══════════════════════════════════════════════════════════════════════════════════╝\n" +
				"\n" +
				"🏠 Application: {}\n" +
				"📍 Local URL:   http://localhost:{}{}\n" +
				"🌐 Network URL: http://{}:{}{}\n" +
				"🔧 Profile(s):  {}\n" +
				"⏰ Started At:  {}\n" +
				"\n" +
				"╔══════════════════════════════════════════════════════════════════════════════════╗\n" +
				"║                             🛠️  MANAGEMENT ENDPOINTS                             ║\n" +
				"╚══════════════════════════════════════════════════════════════════════════════════╝\n" +
				"📊 Health Check:      http://localhost:{}/actuator/health\n" +
				"📈 Metrics:           http://localhost:{}/actuator/metrics\n" +
				"🛣️  Gateway Routes:    http://localhost:{}/actuator/gateway/routes\n" +
				"⚡ Circuit Breakers:  http://localhost:{}/actuator/circuitbreakers\n" +
				"🔧 All Endpoints:     http://localhost:{}/actuator\n" +
				"\n" +
				"╔══════════════════════════════════════════════════════════════════════════════════╗\n" +
				"║                              🎯  SERVICE ROUTES                                  ║\n" +
				"╚══════════════════════════════════════════════════════════════════════════════════╝\n" +
				"🔐 Auth & User:       /api/v1/auth/**     → {}\n" +
				"🔐 User Management:   /api/v1/user/**     → {}\n" +
				"💳 Payments:          /api/v1/payments/** → {}\n" +
				"📅 Bookings:          /api/v1/booking/**  → {}\n" +
				"✈️ Travel Info:       /api/v1/travel/**   → Travel Service\n" +
				"⭐ Reviews:           /api/v1/reviews/**  → Reviews Service\n" +
				"📝 Blog:              /api/v1/blog/**     → Blog Service\n" +
				"🎧 Support:           /api/v1/customer/** → Customer Support\n" +
				"🔧 Utils:             /api/v1/utils/**    → Utils Service\n" +
				"🏛️ Core Services:\n" +
				"   • Tours:           /api/v1/tours/**     → {}\n" +
				"   • Destinations:    /api/v1/destinations/** → {}\n" +
				"   • Activities:      /api/v1/activities/** → {}\n" +
				"   • Adventures:      /api/v1/adventures/** → {}\n" +
				"🤖 AI Chat:           /api/v1/chat/**     → {}\n" +
				"\n" +
				"╔══════════════════════════════════════════════════════════════════════════════════╗\n" +
				"║                             🛡️  SECURITY PLATFORM                               ║\n" +
				"╚══════════════════════════════════════════════════════════════════════════════════╝\n" +
				"🔑 JWT Authentication: ✅ ENABLED (HS256)\n" +
				"🌐 CORS Support:       ✅ ENABLED (Enterprise Strict Pattern)\n" +
				"⚡ Rate Limiting:      {} {}\n" +
				"🛡️ Threat Scoring:    ✅ ACTIVE (Behavioral Analytics Enabled)\n" +
				"🚫 Bot Filtering:      ✅ ACTIVE (Automatic Scanner Banning)\n" +
				"📊 Security Telemetry: http://localhost:{}/api/v1/admin/security/telemetry/stats\n" +
				"📜 Security Docs:     http://localhost:{}/swagger-ui.html\n" +
				"🔄 Circuit Breaker:    ✅ ENABLED (Resilience4j)\n" +
				"📊 Service Discovery:  {} {}\n" +
				"🛡️ Global Exception:   ✅ ENABLED\n" +
				"🚧 Safety Valve:       ✅ LOCALHOST BYPASS ACTIVE\n" +
				"\n" +
				"╔══════════════════════════════════════════════════════════════════════════════════╗\n" +
				"║                            🚀  GATEWAY STATUS: READY                            ║\n" +
				"╚══════════════════════════════════════════════════════════════════════════════════╝\n",
				applicationName,
				serverPort, contextPath,
				hostAddress, serverPort, contextPath,
				activeProfiles,
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
				serverPort,
				serverPort,
				serverPort,
				serverPort,
				serverPort,
				authServiceUrl,
				authServiceUrl,
				paymentServiceUrl,
				bookingServiceUrl,
				coreServiceUrl,
				coreServiceUrl,
				coreServiceUrl,
				coreServiceUrl,
				chatServiceUrl,
				rateLimitEnabled ? "✅" : "❌",
				rateLimitEnabled ? "ENABLED" : "DISABLED",
				serverPort,
				serverPort,
				eurekaEnabled ? "✅" : "❌",
				eurekaEnabled ? "ENABLED" : "DISABLED");

		// Log additional configuration details
		logConfigurationDetails();

		// Test Redis Connection
		testRedisConnection();

		// Log any warnings or important notes
		logImportantNotes();
	}

	private void testRedisConnection() {
		log.info("🧪 Testing Redis Connection...");
		redisTemplate.opsForValue().get("health-check")
				.timeout(java.time.Duration.ofSeconds(5))
				.subscribe(
						success -> log.info("✅ Redis Connection: SUCCESS (Handshake OK)"),
						error -> log.error("❌ Redis Connection: FAILED - {}", error.getMessage()),
						() -> log.info("✅ Redis Connection: SUCCESS (Server Reachable)")
				);
	}

	private void logConfigurationDetails() {
		log.info("🔧 Configuration Details Diagnostics:");
		String host = environment.getProperty("spring.data.redis.host", "150.241.245.162");
		String port = environment.getProperty("spring.data.redis.port", "6379");
		log.info("   • Redis Host: {}", host);
		log.info("   • Redis Port: {}", port);
		
		String jwtSecret = environment.getProperty("jwt.secret", "NOT_SET");
		if (jwtSecret.length() > 20) {
			log.info("   • JWT Secret: {}...", jwtSecret.substring(0, 20));
		}
		log.info("   • JWT Expiration: {} ms", environment.getProperty("jwt.expiration", "604800000"));

		String redisPwd = "";
		String source = "Environment";
		
		try {
			// Try various property names
			redisPwd = environment.getProperty("spring.data.redis.password");
			if (redisPwd == null || redisPwd.isEmpty()) {
				redisPwd = environment.getProperty("spring.redis.password");
			}
			if (redisPwd == null || redisPwd.isEmpty()) {
				redisPwd = System.getenv("REDIS_PASSWORD");
				source = "Env Var (REDIS_PASSWORD)";
			} else {
				source = "Spring Prop (spring.data.redis.password)";
			}
			
			if (redisPwd != null && !redisPwd.isEmpty()) {
				log.info("   • Redis Password Source: {}", source);
				int len = redisPwd.length();
				String first2 = len >= 2 ? redisPwd.substring(0, 2) : "??";
				String last2 = len >= 2 ? redisPwd.substring(len - 2) : "??";
				
				// SHA-256 for verification
				java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
				byte[] hashBytes = digest.digest(redisPwd.getBytes(java.nio.charset.StandardCharsets.UTF_8));
				StringBuilder hexString = new StringBuilder();
				for (int i = 0; i < 4; i++) {
					String hex = Integer.toHexString(0xff & hashBytes[i]);
					if (hex.length() == 1) hexString.append('0');
					hexString.append(hex);
				}
				
				log.info("   • Redis Password Hash: {} (SHA-256 prefix)", hexString.toString());
				log.info("   • Redis Password Details: Len={}, Start={}, End={}", len, first2, last2);
			} else {
				log.warn("   • Redis Password: NOT SET!");
			}
		} catch (Exception e) {
			log.error("   ❌ Error analyzing Redis password: {}", e.getMessage());
		}

		if (Boolean.parseBoolean(environment.getProperty("rate-limit.enabled", "false"))) {
			log.info("   • Rate Limits Enabled");
		}
	}

	private void logImportantNotes() {
		log.info("\n📝 Important Notes:");

		if (!Boolean.parseBoolean(environment.getProperty("rate-limit.enabled", "false"))) {
			log.warn("   ⚠️  Rate limiting is DISABLED - Enable in production!");
		}

		if (!Boolean.parseBoolean(environment.getProperty("eureka.client.enabled", "false"))) {
			log.info("   ℹ️  Service discovery is disabled (development mode)");
		}

		String jwtSecret = environment.getProperty("jwt.secret", "");
		if (jwtSecret.contains("mapMyTourSecretKey2024")) {
			log.warn("   ⚠️  Using default JWT secret - Change in production!");
		}

		log.info("   ✅ Gateway is ready to handle requests!");
		log.info("   📧 For support: support@mapmytimes.com");
		log.info("   🌐 Website: https://mapmytimes.com");

		log.info("\n🎉 MapMyTour API Gateway successfully started and ready for adventure! 🗺️✈️");
	}
}