package com.gateway.filter;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${jwt.secret-key}")
    private String secretKeyBase64;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyBase64);
        this.secretKey = new SecretKeySpec(decodedKey, "HmacSHA256");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Skip authentication for auth endpoints
            if (request.getPath().toString().startsWith("/auth")) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            try {
                String token = authHeader.substring(7);
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                
                // Validate token expiration
                if (claims.getExpiration().before(new Date())) {
                    return unauthorized(exchange, "Token expired");
                }
                
                // Add claims to request headers for downstream services
                ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", claims.getSubject())
                    .header("X-User-Roles", claims.get("roles", String.class))
                    .build();
                
                return chain.filter(exchange.mutate().request(modifiedRequest).build());
                
            } catch (ExpiredJwtException ex) {
                return unauthorized(exchange, "Token expired");
            } catch (JwtException | IllegalArgumentException ex) {
                return unauthorized(exchange, "Invalid token");
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Failure", message);
        return response.setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }
}