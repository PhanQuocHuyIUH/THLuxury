package com.thluxury.identity.web;

import com.thluxury.identity.service.JwtKeyManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Expose the public JWK Set so other services (Gateway) can verify JWTs.
 * Path: GET /api/auth/.well-known/jwks.json (kept under /api/auth/** so Gateway can route it).
 */
@RestController
@RequestMapping("/api/auth/.well-known")
public class JwksController {

    private final JwtKeyManager keys;

    public JwksController(JwtKeyManager keys) {
        this.keys = keys;
    }

    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return keys.getJwkSet().toJSONObject();
    }
}
