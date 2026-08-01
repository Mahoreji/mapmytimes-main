package in.mapmytour.customer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = CustomerSupportServiceApplication.class)
@EnableAutoConfiguration(exclude = {KafkaAutoConfiguration.class})
@TestPropertySource(properties = {
	"spring.kafka.bootstrap-servers=localhost:9092",
	"REDIS_HOST=localhost",
	"REDIS_PORT=6379",
	"DATABASE_URL=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
	"spring.datasource.driver-class-name=org.h2.Driver",
	"DB_USERNAME=sa",
	"DB_PASSWORD=",
	"spring.jpa.hibernate.ddl-auto=update",
	"JWT_SECRET_KEY=mytestsecretkeymytestsecretkeymytestsecretkey",
	"AWS_ACCESS_KEY=mock",
	"AWS_SECRET_KEY=mock",
	"AWS_REGION=ap-south-1",
	"AWS_S3_BUCKET=mock-bucket"
})
class CustomerSupportServiceApplicationTests {

	@Test
	void contextLoads() {
		// Test that the application context loads successfully
		// Note: Kafka is excluded from this test to avoid connection issues
	}

}
