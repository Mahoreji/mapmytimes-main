package in.mapmytour.api;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;

@SpringBootTest
class ApiGatewayServiceApplicationTests {

	static {
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
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, value);
					}
				});
			}
		} catch (Exception e) {
			// ignore
		}
	}

	@Test
	void contextLoads() {
	}

}
