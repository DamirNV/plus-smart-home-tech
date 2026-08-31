package ru.yandex.practicum.gateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class GatewaySecurityConfig {

    private static final String[] CATALOG_PATHS = {
            "/api/products",
            "/api/products/**",
            "/api/categories",
            "/api/categories/**",
            "/api/inventory",
            "/api/inventory/**"
    };

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .httpBasic(Customizer.withDefaults())
                .authorizeExchange(exchange -> exchange
                        // CORS preflight не выполняет бизнес-действие.
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Стандартные Swagger/OpenAPI пути оставляем публичными.
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Публичное чтение каталога, категорий и остатков.
                        .pathMatchers(HttpMethod.GET, CATALOG_PATHS).permitAll()

                        // Пользовательские сценарии заказов.
                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/orders",
                                "/api/orders/**"
                        ).hasRole("USER")
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/by-email"
                        ).hasRole("USER")
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders/{id}"
                        ).hasRole("USER")

                        // Полный список заказов доступен только администратору.
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/orders"
                        ).hasRole("ADMIN")

                        // Изменение каталога, категорий и остатков.
                        .pathMatchers(HttpMethod.POST, CATALOG_PATHS).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PUT, CATALOG_PATHS).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, CATALOG_PATHS).hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE, CATALOG_PATHS).hasRole("ADMIN")

                        // Всё, что явно не опубликовано выше, закрыто.
                        .anyExchange().denyAll()
                )
                .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            SecurityProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        UserDetails[] users = properties.users().stream()
                .map(user -> User.withUsername(user.username())
                        .password(passwordEncoder.encode(user.password()))
                        .roles(user.roles().toArray(String[]::new))
                        .build())
                .toArray(UserDetails[]::new);

        return new MapReactiveUserDetailsService(users);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}