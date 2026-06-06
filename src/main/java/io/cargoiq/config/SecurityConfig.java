package io.cargoiq.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Stateless JWT security + role-based access control.
 *
 * <p><b>Design.</b> The app is its own authorization server <i>and</i> resource
 * server: {@code /api/v1/auth/login} mints an HS256-signed JWT (see
 * {@code JwtTokenIssuer}); every other protected request is validated as a
 * bearer token by Spring Security's OAuth2 resource server. A symmetric secret
 * keeps the dev/demo setup to a single shared key — for a multi-service
 * deployment you would switch to an RS256 key pair and publish a JWKS, which is
 * a change isolated to the {@code JwtEncoder}/{@code JwtDecoder} beans here.
 *
 * <p><b>RBAC.</b> Two roles (see {@link io.cargoiq.domain.model.Role}):
 * <ul>
 *   <li><b>USER</b> — read the corpus, run RAG queries, look up reference data.</li>
 *   <li><b>ADMIN</b> — additionally ingest and delete documents (the
 *       corpus-mutating operations).</li>
 * </ul>
 * Authorities ride in the {@code roles} JWT claim already in
 * {@code ROLE_*} form, so {@code hasRole('ADMIN')} resolves directly.
 *
 * <p><b>Deliberately open paths.</b> Auth endpoints, the OpenAPI docs, the
 * actuator liveness probes, and the embedded MCP endpoint ({@code /mcp}) are
 * permitted without a token so the demo (MCP Inspector, Swagger UI) works out
 * of the box. Locking {@code /mcp} behind the same bearer scheme is a one-line
 * change when this is deployed somewhere reachable.
 *
 * @author Vishal Dogra
 */
@Configuration
public class SecurityConfig {

    private final byte[] secret;

    public SecurityConfig(
            @Value("${cargoiq.security.jwt.secret:change-me-in-prod-this-is-a-dev-only-secret-please}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // stateless API: no cookies, no CSRF surface
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // --- public ---
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Spring Security filters the ERROR dispatch too; permit it so a
                // validation failure on a public endpoint renders as its real
                // status (e.g. 400) instead of being turned into a 401.
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger", "/swagger-ui/**",
                        "/swagger-ui.html").permitAll()
                .requestMatchers("/mcp/**").permitAll() // MCP Inspector / LLM clients
                // --- corpus-mutating operations: ADMIN only ---
                .requestMatchers(HttpMethod.POST, "/api/v1/documents").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").hasRole("ADMIN")
                // --- everything else under the API: any authenticated user ---
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Maps the {@code roles} claim straight to authorities with no prefix — the
     * claim already stores {@code ROLE_USER}/{@code ROLE_ADMIN}, the mirror of
     * what {@code JwtTokenIssuer} writes.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("");
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private SecretKeySpec secretKey() {
        return new SecretKeySpec(secret, "HmacSHA256");
    }
}
