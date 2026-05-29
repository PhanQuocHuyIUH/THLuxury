package com.thluxury.identity.web;

import com.thluxury.identity.domain.User;
import com.thluxury.identity.repository.UserRepository;
import com.thluxury.identity.service.AuthService;
import com.thluxury.identity.web.dto.AuthDtos.UpdateMeRequest;
import com.thluxury.identity.web.dto.AuthDtos.UserView;
import com.thluxury.identity.web.exception.ApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserView me(@AuthenticationPrincipal String userId) {
        return AuthService.toView(loadUser(userId));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public UserView updateMe(@AuthenticationPrincipal String userId,
                             @RequestBody @Valid UpdateMeRequest req) {
        User u = loadUser(userId);
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.phone() != null)    u.setPhone(req.phone());
        return AuthService.toView(u);
    }

    private User loadUser(String userId) {
        return users.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_GONE", "User không tồn tại"));
    }
}
