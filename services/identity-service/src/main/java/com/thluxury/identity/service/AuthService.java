package com.thluxury.identity.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.thluxury.identity.domain.User;
import com.thluxury.identity.domain.UserRole;
import com.thluxury.identity.repository.UserRepository;
import com.thluxury.identity.web.dto.AuthDtos.*;
import com.thluxury.identity.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final RefreshTokenService refreshStore;
    private final long accessTtlSeconds;

    public AuthService(UserRepository users,
                       PasswordEncoder encoder,
                       JwtService jwt,
                       RefreshTokenService refreshStore,
                       com.thluxury.identity.config.JwtProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshStore = refreshStore;
        this.accessTtlSeconds = props.accessTtlMinutes() * 60;
    }

    @Transactional
    public UserView register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email đã được sử dụng");
        }
        User u = new User();
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        u.setRole(UserRole.CUSTOMER);
        u.setEnabled(true);
        users.save(u);
        return toView(u);
    }

    @Transactional
    public TokenPair login(LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "BAD_CREDENTIALS", "Email hoặc mật khẩu không đúng"));
        if (u.getPasswordHash() == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED,
                    "BAD_CREDENTIALS", "Email hoặc mật khẩu không đúng");
        }
        if (!u.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Tài khoản đã bị vô hiệu hoá");
        }
        return issueTokens(u);
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        JWTClaimsSet claims;
        try {
            claims = jwt.parseAndVerify(refreshToken);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH", "Refresh token không hợp lệ");
        }
        if (!"refresh".equals(claims.getClaim("typ"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH", "Token không phải refresh");
        }
        String jti = claims.getJWTID();
        if (!refreshStore.isActive(jti)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REVOKED", "Phiên đăng nhập đã bị thu hồi");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        User u = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_GONE", "Người dùng không còn tồn tại"));
        refreshStore.revoke(jti);   // rotate
        return issueTokens(u);
    }

    public void logout(String refreshToken) {
        try {
            JWTClaimsSet claims = jwt.parseAndVerify(refreshToken);
            refreshStore.revoke(claims.getJWTID());
        } catch (Exception ignored) {
            // best-effort; ignore invalid tokens
        }
    }

    private TokenPair issueTokens(User u) {
        var access  = jwt.issueAccessToken(u);
        var refresh = jwt.issueRefreshToken(u);
        refreshStore.store(refresh.jti(), u.getId(), refresh.expiresAt());
        return new TokenPair(access.token(), refresh.token(), accessTtlSeconds, toView(u));
    }

    public static UserView toView(User u) {
        return new UserView(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getRole(),
                u.getBranchId(),
                u.isEnabled(),
                u.getCreatedAt());
    }
}
