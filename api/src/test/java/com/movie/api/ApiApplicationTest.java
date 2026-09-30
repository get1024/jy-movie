package com.movie.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.*;

/** Full Boot/Security/MyBatis/Swagger startup smoke test without external services. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:startup;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "jwt.secret=unit-test-only-signing-key-32-bytes-long",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:startup-schema.sql",
        "movie.order.expiration-check-delay-ms=3600000"
})
@DirtiesContext
class ApiApplicationTest {
    @Autowired private TestRestTemplate http;

    @Test
    void servesPublicCatalogUsingExistingEnvelope() {
        ResponseEntity<String> response = http.getForEntity("/api/film", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":200"));
        assertTrue(response.getBody().contains("\"data\":[]"));
    }

    @Test
    void protectedOrdersRejectAnonymousRequests() {
        ResponseEntity<String> response = http.getForEntity("/api/order", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":401"));
    }

    @Test
    void swaggerStillStartsAfterBootUpgrade() {
        assertEquals(HttpStatus.OK, http.getForEntity("/v2/api-docs", String.class).getStatusCode());
    }
}
