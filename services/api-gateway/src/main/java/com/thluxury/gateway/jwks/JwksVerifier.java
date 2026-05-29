package com.thluxury.gateway.jwks;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Periodically refreshes the Identity Service JWKS and verifies access tokens.
 * Cache TTL: 10 minutes.
 */
@Component
public class JwksVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwksVerifier.class);
    private static final Duration TTL = Duration.ofMinutes(10);

    private final String jwksUri;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private final AtomicReference<CachedKeys> cache = new AtomicReference<>(new CachedKeys(Map.of(), Instant.EPOCH));

    public JwksVerifier(@Value("${thluxury.gateway.jwks-uri}") String jwksUri) {
        this.jwksUri = jwksUri;
        log.info("JwksVerifier configured with uri={}", jwksUri);
    }

    public Mono<JWTClaimsSet> verify(String token) {
        return Mono.fromCallable(() -> verifySync(token));
    }

    private JWTClaimsSet verifySync(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        String kid = jwt.getHeader().getKeyID();
        RSAKey key = getKey(kid);
        if (key == null) {
            throw new IllegalArgumentException("Unknown kid: " + kid);
        }
        if (!jwt.verify(new RSASSAVerifier(key.toRSAPublicKey()))) {
            throw new IllegalArgumentException("Invalid signature");
        }
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token expired");
        }
        if (!"access".equals(claims.getClaim("typ"))) {
            throw new IllegalArgumentException("Not an access token");
        }
        return claims;
    }

    private RSAKey getKey(String kid) throws Exception {
        CachedKeys c = cache.get();
        RSAKey hit = c.keys().get(kid);
        if (hit != null && Instant.now().isBefore(c.expiresAt())) {
            return hit;
        }
        return refresh().get(kid);
    }

    private synchronized Map<String, RSAKey> refresh() throws Exception {
        CachedKeys c = cache.get();
        if (Instant.now().isBefore(c.expiresAt()) && !c.keys().isEmpty()) {
            return c.keys();
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(jwksUri))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException("JWKS fetch failed: HTTP " + resp.statusCode());
        }
        JWKSet set = JWKSet.parse(resp.body());
        Map<String, RSAKey> map = new HashMap<>();
        for (JWK jwk : set.getKeys()) {
            if (jwk instanceof RSAKey rsa) {
                map.put(rsa.getKeyID(), rsa);
            }
        }
        cache.set(new CachedKeys(map, Instant.now().plus(TTL)));
        log.info("JWKS refreshed: {} key(s)", map.size());
        return map;
    }

    private record CachedKeys(Map<String, RSAKey> keys, Instant expiresAt) {}
}
