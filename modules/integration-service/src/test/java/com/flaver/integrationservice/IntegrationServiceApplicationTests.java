package com.flaver.integrationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:integration_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "google.oauth.client-id=test-client",
        "google.oauth.client-secret=test-secret",
        "integration.token-encryption-secret=test-secret-change-me-32-bytes-min"
})
class IntegrationServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
