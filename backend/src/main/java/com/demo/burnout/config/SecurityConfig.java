package com.demo.burnout.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${security.enabled:true}")
    private boolean securityEnabled;

    // Cache validated tokens for 5 minutes to avoid hitting GitHub API on every request
    private final Map<String, CachedAuth> tokenCache = new ConcurrentHashMap<>();
    private static final Duration CACHE_DURATION = Duration.ofMinutes(5);

    record CachedAuth(String username, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Health endpoints are public (for Azure probes)
                .requestMatchers("/actuator/**").permitAll()
                // OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // All API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll()
            )
            .addFilterBefore(githubTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public OncePerRequestFilter githubTokenFilter() {
        return new OncePerRequestFilter() {
            private final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

            @Override
            protected void doFilterInternal(HttpServletRequest request, 
                                          HttpServletResponse response, 
                                          FilterChain filterChain) throws ServletException, IOException {
                
                // Skip auth for health endpoints
                String path = request.getRequestURI();
                if (path.startsWith("/actuator")) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Skip auth if security is disabled (for local development)
                if (!securityEnabled) {
                    log.debug("Security disabled, allowing request");
                    var auth = new UsernamePasswordAuthenticationToken(
                        "anonymous", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Extract Bearer token
                String authHeader = request.getHeader("Authorization");
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    log.warn("Missing or invalid Authorization header from {}", request.getRemoteAddr());
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Missing Authorization header. Use: Authorization: Bearer <github-token>\"}");
                    return;
                }

                String token = authHeader.substring(7);

                // Check cache first
                CachedAuth cached = tokenCache.get(token);
                if (cached != null && !cached.isExpired()) {
                    log.debug("Using cached auth for user: {}", cached.username());
                    var auth = new UsernamePasswordAuthenticationToken(
                        cached.username(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Validate token against GitHub API
                try {
                    HttpRequest ghRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.github.com/user"))
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "burnout-backend")
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                    HttpResponse<String> ghResponse = httpClient.send(ghRequest, HttpResponse.BodyHandlers.ofString());

                    if (ghResponse.statusCode() == 200) {
                        // Extract username from response (simple JSON parsing)
                        String body = ghResponse.body();
                        String username = extractJsonField(body, "login");
                        
                        log.info("Authenticated GitHub user: {}", username);
                        
                        // Cache the validated token
                        tokenCache.put(token, new CachedAuth(username, 
                            System.currentTimeMillis() + CACHE_DURATION.toMillis()));

                        var auth = new UsernamePasswordAuthenticationToken(
                            username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        filterChain.doFilter(request, response);
                    } else {
                        log.warn("GitHub token validation failed: {} {}", ghResponse.statusCode(), ghResponse.body());
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Invalid GitHub token\"}");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("{\"error\":\"Token validation interrupted\"}");
                } catch (Exception e) {
                    log.error("Error validating GitHub token", e);
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token validation failed: " + e.getMessage() + "\"}");
                }
            }

            private String extractJsonField(String json, String field) {
                // Simple extraction - for production use a proper JSON library
                String pattern = "\"" + field + "\":\"";
                int start = json.indexOf(pattern);
                if (start == -1) return "unknown";
                start += pattern.length();
                int end = json.indexOf("\"", start);
                if (end == -1) return "unknown";
                return json.substring(start, end);
            }
        };
    }
}
