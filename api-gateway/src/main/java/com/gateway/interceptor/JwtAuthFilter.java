package com.gateway.interceptor;

//import java.nio.charset.StandardCharsets;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.cloud.gateway.filter.GlobalFilter;
//import org.springframework.cloud.gateway.filter.GatewayFilterChain;
//import org.springframework.core.Ordered;
//import org.springframework.core.io.buffer.DataBuffer;
//import org.springframework.core.io.buffer.DataBufferFactory;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import reactor.core.publisher.Mono;
//import java.util.Base64;
//
//@Component
//public class JwtAuthFilter implements GlobalFilter, Ordered {
//
//    @Value("${jwt.secret-key}")
//    private String secretKey;
//
//    private static final String SWAGGER_USERNAME = "swagger";
//    private static final String SWAGGER_PASSWORD = "secret"; // 🔐 use strong password
//
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//        String path = exchange.getRequest().getPath().toString();
//
//        if (isSwaggerPath(path)) {
//            // Handle basic auth for Swagger
//            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//            if (authHeader != null && authHeader.startsWith("Basic ")) {
//                String base64Credentials = authHeader.substring("Basic ".length());
//                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
//                String[] values = credentials.split(":", 2);
//
//                if (values.length == 2 &&
//                        SWAGGER_USERNAME.equals(values[0]) &&
//                        SWAGGER_PASSWORD.equals(values[1])) {
//                    return chain.filter(exchange); // ✅ Authenticated
//                }
//            }
//            return unauthorizedResponse(exchange, "Unauthorized Swagger Access. Provide Basic Auth.");
//        }
//
//        if (path.contains("/login") || path.contains("/actuator")) {
//            return chain.filter(exchange); // Allow login and actuator unauthenticated
//        }
//
//        // JWT Auth for all other paths
//        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//            try {
//                Jwts.parserBuilder()
//                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
//                        .build()
//                        .parseClaimsJws(token);
//
//                return chain.filter(exchange); // ✅ Valid JWT
//            } catch (Exception e) {
//                return unauthorizedResponse(exchange, "Invalid or expired JWT token.");
//            }
//        }
//
//        return unauthorizedResponse(exchange, "Missing Authorization header.");
//    }
//
//    private boolean isSwaggerPath(String path) {
//        return path.contains("/swagger-ui") ||
//                path.contains("/v3/api-docs") ||
//                path.contains("/swagger-resources") ||
//                path.contains("/webjars") ||
//                path.contains("/internal-api-docs");
//    }
//
//    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
//        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//        exchange.getResponse().getHeaders().add("WWW-Authenticate", "Basic realm=\"Swagger\"");
//        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
//        String responseBody = "{\"error\":\"" + message + "\"}";
//        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
//        DataBuffer dataBuffer = bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8));
//        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
//    }
//
//    @Override
//    public int getOrder() {
//        return -1;
//    }
//}


import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private static final String SWAGGER_USERNAME = "swagger";
    private static final String SWAGGER_PASSWORD = "secret";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 1) Swagger UI and related assets → Basic auth only
        if (isSwaggerUiResource(path)) {
            return handleSwaggerBasic(exchange, chain);
        }

        // 2) Allow login & actuator without auth
        if (path.contains("/login") || path.contains("/refresh") || path.contains("/getTokenForAdminIdWithInvestorId") || path.contains("/actuator") || path.contains("/nse/images")) {
            return chain.filter(exchange);
        }

        // 3) All other endpoints → require Bearer JWT
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token);
                return chain.filter(exchange); // valid token
            } catch (Exception ex) {
                return unauthorizedJwt(exchange, "Invalid or expired JWT token.");
            }
        }

        return unauthorizedJwt(exchange, "Missing or invalid Authorization header.");
    }

    private Mono<Void> handleSwaggerBasic(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            // decode credentials
            String base64Creds = authHeader.substring(6);
            String creds = new String(Base64.getDecoder().decode(base64Creds), StandardCharsets.UTF_8);
            String[] parts = creds.split(":", 2);
            if (parts.length == 2
                    && SWAGGER_USERNAME.equals(parts[0])
                    && SWAGGER_PASSWORD.equals(parts[1])) {
                return chain.filter(exchange); // basic auth OK
            }
        }
        return unauthorizedSwagger(exchange, "Unauthorized Swagger access: Basic auth required.");
    }

    private Mono<Void> unauthorizedSwagger(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Swagger\"");
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(("{\"error\":\"" + msg + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Mono<Void> unauthorizedJwt(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        // remove any WWW-Authenticate header so browser won't pop up
        exchange.getResponse().getHeaders().remove(HttpHeaders.WWW_AUTHENTICATE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(("{\"error\":\"" + msg + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private boolean isSwaggerUiResource(String path) {
        return path.contains("/swagger-ui")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-resources")
                || path.contains("/webjars")
                || path.contains("/internal-api-docs");
    }

    @Override
    public int getOrder() {
        // Just before Netty writes response
        return -1;
    }
}
