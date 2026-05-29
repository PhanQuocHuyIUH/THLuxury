package com.thluxury.gateway.filter;

import com.nimbusds.jwt.JWTClaimsSet;
import com.thluxury.gateway.jwks.JwksVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

/**
 * Global filter:
 *   1. If path is in PUBLIC_PATHS — let through, strip any incoming X-User-* headers (client cannot spoof).
 *   2. Otherwise — require Authorization: Bearer ...; verify with JWKS; inject X-User-Id, X-User-Role, X-Branch-Id headers.
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGlobalFilter.class);

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/password/forgot",
            "/api/auth/password/reset",
            "/api/auth/oauth2/google",
            "/api/auth/.well-known/",
            "/api/ai/",
            "/api/payments/stripe/webhook",
            "/__fallback/",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    );
    /** GET-only public prefixes (handled in matcher). */
    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/branches",
            "/api/products",
            "/api/categories"
    );

    private static final Set<String> SPOOF_HEADERS = Set.of("X-User-Id", "X-User-Role", "X-Branch-Id");

    private final JwksVerifier verifier;

    public JwtAuthGlobalFilter(JwksVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        boolean publicPath = isPublic(request.getMethod().name(), path);

        // Always strip headers from inbound request to prevent spoofing
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> SPOOF_HEADERS.forEach(h::remove))
                .build();

        String auth = mutated.getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            if (publicPath) {
                return chain.filter(exchange.mutate().request(mutated).build());
            }
            return unauthorized(exchange, "UNAUTHENTICATED", "Missing or invalid Authorization header");
        }

        String token = auth.substring(7);
        return verifier.verify(token)
                .flatMap(claims -> {
                    ServerHttpRequest withIdentity = injectIdentityHeaders(mutated, claims);
                    return chain.filter(exchange.mutate().request(withIdentity).build());
                })
                .onErrorResume(e -> {
                    log.debug("JWT verification failed: {}", e.getMessage());
                    if (publicPath) {
                        return chain.filter(exchange.mutate().request(mutated).build());
                    }
                    return unauthorized(exchange, "INVALID_TOKEN", e.getMessage());
                });
    }

    private boolean isPublic(String method, String path) {
        for (String p : PUBLIC_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        if ("GET".equalsIgnoreCase(method)) {
            for (String p : PUBLIC_GET_PREFIXES) {
                if (path.equals(p) || path.startsWith(p + "/") || path.startsWith(p + "?")) return true;
            }
        }
        return false;
    }

    private ServerHttpRequest injectIdentityHeaders(ServerHttpRequest mutated, JWTClaimsSet claims) {
        try {
            String sub = claims.getSubject();
            String role = (String) claims.getClaim("role");
            String branchId = (String) claims.getClaim("branchId");
            return mutated.mutate()
                    .header("X-User-Id", sub)
                    .header("X-User-Role", role == null ? "" : role)
                    .header("X-Branch-Id", branchId == null ? "" : branchId)
                    .build();
        } catch (Exception e) {
            return mutated;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String body = "{\"code\":\"" + code + "\",\"message\":\""
                + (message == null ? "" : message.replace("\"", "'")) + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
