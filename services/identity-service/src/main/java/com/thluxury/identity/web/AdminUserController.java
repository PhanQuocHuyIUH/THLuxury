package com.thluxury.identity.web;

import com.thluxury.identity.domain.UserRole;
import com.thluxury.identity.service.UserAdminService;
import com.thluxury.identity.web.dto.AdminDtos.*;
import com.thluxury.identity.web.dto.AuthDtos.UserView;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserAdminService service;

    public AdminUserController(UserAdminService service) {
        this.service = service;
    }

    @GetMapping
    public Page<UserView> list(@RequestParam(required = false) UserRole role,
                               @RequestParam(required = false) UUID branchId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return service.search(role, branchId, page, size);
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PostMapping("/branch-managers")
    public ResponseEntity<UserView> createBranchManager(@RequestBody @Valid CreateBranchManagerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBranchManager(req));
    }

    @PutMapping("/{id}")
    public UserView update(@PathVariable UUID id, @RequestBody @Valid UpdateUserRequest req) {
        return service.update(id, req);
    }
}
