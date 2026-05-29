package com.thluxury.identity.service;

import com.thluxury.identity.config.AuthRabbitConfig;
import com.thluxury.identity.domain.PasswordResetToken;
import com.thluxury.identity.domain.User;
import com.thluxury.identity.repository.PasswordResetTokenRepository;
import com.thluxury.identity.repository.UserRepository;
import com.thluxury.identity.web.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_BYTES = 32;
    private static final long EXPIRES_MINUTES = 30;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final RabbitTemplate rabbit;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users,
                                PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder,
                                RabbitTemplate rabbit) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.rabbit = rabbit;
    }

    /** Tạo token + publish event auth.password.reset. Idempotent: nếu email không tồn tại vẫn trả về OK. */
    @Transactional
    public void forgot(String email) {
        Optional<User> userOpt = users.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for unknown email {} — silent success", email);
            return;
        }
        User user = userOpt.get();
        if (!user.isEnabled()) {
            log.info("Password reset requested for disabled user {} — silent success", email);
            return;
        }

        tokens.deleteByUserId(user.getId());

        byte[] raw = new byte[TOKEN_BYTES];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = sha256(token);

        PasswordResetToken entity = new PasswordResetToken(hash, user.getId(),
                OffsetDateTime.now().plusMinutes(EXPIRES_MINUTES));
        tokens.save(entity);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("fullName", user.getFullName());
        payload.put("token", token);
        payload.put("expiresAt", entity.getExpiresAt().toString());

        rabbit.convertAndSend(AuthRabbitConfig.EXCHANGE,
                AuthRabbitConfig.RK_PASSWORD_RESET, payload);
        log.info("Published auth.password.reset for {}", user.getEmail());
    }

    @Transactional
    public void reset(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "Liên kết không hợp lệ");
        }
        String hash = sha256(token);
        PasswordResetToken entity = tokens.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN",
                        "Liên kết không hợp lệ"));
        if (entity.getUsedAt() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_ALREADY_USED",
                    "Liên kết này đã được sử dụng");
        }
        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED",
                    "Liên kết đã hết hạn, vui lòng yêu cầu lại");
        }

        User user = users.findById(entity.getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "USER_NOT_FOUND",
                        "Không tìm thấy tài khoản"));
        user.setPasswordHash(encoder.encode(newPassword));
        users.save(user);

        entity.setUsedAt(OffsetDateTime.now());
        tokens.save(entity);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
