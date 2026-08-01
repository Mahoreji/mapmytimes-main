package in.mapmytour;

import in.mapmytour.auth.AuthServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = AuthServiceApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
		// Verifies that the Spring application context loads successfully
	}

}
