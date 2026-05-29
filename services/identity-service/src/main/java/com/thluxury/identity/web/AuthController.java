package com.thluxury.identity.web;

import com.thluxury.identity.service.AuthService;
import com.thluxury.identity.service.GoogleOAuthService;
import com.thluxury.identity.service.PasswordResetService;
import com.thluxury.identity.web.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;
    private final PasswordResetService passwordReset;
    private final GoogleOAuthService googleOAuth;

    public AuthController(AuthService auth,
                          PasswordResetService passwordReset,
                          GoogleOAuthService googleOAuth) {
        this.auth = auth;
        this.passwordReset = passwordReset;
        this.googleOAuth = googleOAuth;
    }

    @PostMapping("/register")
    public ResponseEntity<UserView> register(@RequestBody @Valid RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    public TokenPair login(@RequestBody @Valid LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@RequestBody @Valid RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutRequest req) {
        auth.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/forgot")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
        passwordReset.forgot(req.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
        passwordReset.reset(req.token(), req.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/google")
    public TokenPair googleLogin(@RequestBody @Valid GoogleLoginRequest req) {
        return googleOAuth.loginOrRegister(req.idToken());
    }
}
