package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@Import(GatewaySecurityConfigTest.TestBackendConfig.class)
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void catalogGet_isPublic() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void categoriesGet_isPublic() {
        webTestClient.get()
                .uri("/api/categories")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void inventoryGet_isPublic() {
        webTestClient.get()
                .uri("/api/inventory")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void orderCreate_withoutCredentials_isUnauthorized() {
        webTestClient.post()
                .uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void orderCreate_withUserCredentials_passesSecurity() {
        webTestClient.post()
                .uri("/api/orders")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void orderByEmail_withUserCredentials_passesSecurity() {
        webTestClient.get()
                .uri("/api/orders/by-email?email=ivan@example.com")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void orderById_withUserCredentials_passesSecurity() {
        webTestClient.get()
                .uri("/api/orders/42")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void productWrite_withUserCredentials_isForbidden() {
        webTestClient.patch()
                .uri("/api/products/10")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void productWrite_withAdminCredentials_passesSecurity() {
        webTestClient.patch()
                .uri("/api/products/10")
                .headers(headers -> headers.setBasicAuth("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ordersList_withUserCredentials_isForbidden() {
        webTestClient.get()
                .uri("/api/orders")
                .headers(headers -> headers.setBasicAuth("ivan", "ivan"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void ordersList_withAdminCredentials_passesSecurity() {
        webTestClient.get()
                .uri("/api/orders")
                .headers(headers -> headers.setBasicAuth("anna", "anna"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void unknownRoute_withAdminCredentials_isForbidden() {
        webTestClient.get()
                .uri("/api/unknown")
                .headers(headers -> headers.setBasicAuth("anna", "anna"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void corsPreflight_isPublic() {
        webTestClient.options()
                .uri("http://localhost/api/orders")
                .header(HttpHeaders.ORIGIN, "http://localhost:8443")
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                        "POST"
                )
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                        "authorization, content-type"
                )
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .valueEquals(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:8443"
                );
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(
                    path("/test-backend"),
                    request -> ServerResponse.ok().build()
            );
        }
    }
}
