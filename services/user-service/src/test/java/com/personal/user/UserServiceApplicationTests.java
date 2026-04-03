package com.personal.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.kafka.bootstrap-servers=localhost:9092"
})
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
		return;
	}

}
