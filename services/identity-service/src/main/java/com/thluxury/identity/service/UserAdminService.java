package com.thluxury.identity.service;

import com.thluxury.identity.domain.User;
import com.thluxury.identity.domain.UserRole;
import com.thluxury.identity.repository.UserRepository;
import com.thluxury.identity.web.dto.AdminDtos.*;
import com.thluxury.identity.web.dto.AuthDtos.UserView;
import com.thluxury.identity.web.exception.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserAdminService {

    private final UserRepository users;
    private final BranchService branches;
    private final PasswordEncoder encoder;

    public UserAdminService(UserRepository users,
                            BranchService branches,
                            PasswordEncoder encoder) {
        this.users = users;
        this.branches = branches;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public Page<UserView> search(UserRole role, UUID branchId, int page, int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return users.search(role, branchId, pageable).map(AuthService::toView);
    }

    @Transactional(readOnly = true)
    public UserView get(UUID id) {
        return AuthService.toView(users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Người dùng không tồn tại: " + id)));
    }

    @Transactional
    public UserView createBranchManager(CreateBranchManagerRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email đã được sử dụng");
        }
        branches.load(req.branchId()); // validate branch exists

        User u = new User();
        u.setEmail(req.email().toLowerCase());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName());
        u.setPhone(req.phone());
        u.setRole(UserRole.BRANCH_MANAGER);
        u.setBranchId(req.branchId());
        u.setEnabled(true);
        users.save(u);
        return AuthService.toView(u);
    }

    @Transactional
    public UserView update(UUID id, UpdateUserRequest req) {
        User u = users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Người dùng không tồn tại: " + id));
        if (req.fullName() != null) u.setFullName(req.fullName());
        if (req.phone() != null)    u.setPhone(req.phone());
        if (req.enabled() != null)  u.setEnabled(req.enabled());
        if (req.role() != null)     u.setRole(req.role());
        if (req.branchId() != null) {
            branches.load(req.branchId());
            u.setBranchId(req.branchId());
        }
        // Enforce: BRANCH_MANAGER must have branch_id
        if (u.getRole() == UserRole.BRANCH_MANAGER && u.getBranchId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_BRANCH",
                    "BRANCH_MANAGER bắt buộc phải gán branchId");
        }
        return AuthService.toView(u);
    }
}
