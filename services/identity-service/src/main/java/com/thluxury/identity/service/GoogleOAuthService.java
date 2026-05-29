package com.thluxury.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thluxury.identity.config.JwtProperties;
import com.thluxury.identity.domain.User;
import com.thluxury.identity.domain.UserRole;
import com.thluxury.identity.repository.UserRepository;
import com.thluxury.identity.web.dto.AuthDtos.TokenPair;
import com.thluxury.identity.web.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verify Google id_token bằng cách gọi Google tokeninfo endpoint
 * (đơn giản, không cần manage public keys). Production thì nên cache keys + verify offline.
 */
@Service
public class GoogleOAuthService {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthService.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";
    private static final String EXPECTED_ISS_1 = "https://accounts.google.com";
    private static final String EXPECTED_ISS_2 = "accounts.google.com";

    private final UserRepository users;
    private final JwtService jwt;
    private final RefreshTokenService refreshStore;
    private final ObjectMapper mapper;
    private final long accessTtlSeconds;
    private final HttpClient http;

    @Value("${thluxury.oauth.google.client-id:}")
    private String configuredClientId;

    public GoogleOAuthService(UserRepository users,
                              JwtService jwt,
                              RefreshTokenService refreshStore,
                              ObjectMapper mapper,
                              JwtProperties props) {
        this.users = users;
        this.jwt = jwt;
        this.refreshStore = refreshStore;
        this.mapper = mapper;
        this.accessTtlSeconds = props.accessTtlMinutes() * 60;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Transactional
    public TokenPair loginOrRegister(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_ID_TOKEN", "Thiếu Google id_token");
        }
        JsonNode info = fetchTokenInfo(idToken);

        String iss = info.path("iss").asText("");
        if (!EXPECTED_ISS_1.equals(iss) && !EXPECTED_ISS_2.equals(iss)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ISSUER", "id_token không phải từ Google");
        }
        if (!configuredClientId.isBlank()) {
            String aud = info.path("aud").asText("");
            if (!configuredClientId.equals(aud)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUDIENCE",
                        "id_token không thuộc app này (aud mismatch)");
            }
        }

        String sub = info.path("sub").asText();
        String email = info.path("email").asText();
        String name = info.path("name").asText(email);
        boolean emailVerified = info.path("email_verified").asText("false").equalsIgnoreCase("true");
        if (sub.isBlank() || email.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token thiếu sub/email");
        }
        if (!emailVerified) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "EMAIL_UNVERIFIED",
                    "Email Google chưa được xác minh");
        }

        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setEmail(email.toLowerCase());
            user.setFullName(name);
            user.setRole(UserRole.CUSTOMER);
            user.setEnabled(true);
        }
        user.setOauthProvider("google");
        user.setOauthSubject(sub);
        user = users.save(user);

        var access = jwt.issueAccessToken(user);
        var refresh = jwt.issueRefreshToken(user);
        refreshStore.store(refresh.jti(), user.getId(), refresh.expiresAt());
        return new TokenPair(access.token(), refresh.token(), accessTtlSeconds, AuthService.toView(user));
    }

    private JsonNode fetchTokenInfo(String idToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TOKENINFO_URL + idToken))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("Google tokeninfo HTTP {} → {}", resp.statusCode(), resp.body());
                throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN",
                        "Google không xác thực id_token");
            }
            return mapper.readTree(resp.body());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google tokeninfo call failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "GOOGLE_UNREACHABLE",
                    "Không gọi được Google tokeninfo");
        }
    }
}
