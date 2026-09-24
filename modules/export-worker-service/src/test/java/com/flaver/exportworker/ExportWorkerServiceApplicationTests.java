package com.flaver.exportworker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false",
        "services.calendar.url=http://localhost:8082",
        "services.export.url=http://localhost:8083",
        "services.integration.url=http://localhost:8085"
})
class ExportWorkerServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
