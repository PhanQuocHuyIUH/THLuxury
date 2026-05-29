package com.thluxury.identity.web;

import com.thluxury.identity.service.BranchService;
import com.thluxury.identity.web.dto.AdminDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branches;

    public BranchController(BranchService branches) {
        this.branches = branches;
    }

    /** PUBLIC list — storefront cần để hiển thị chi nhánh khi STORE_PICKUP. */
    @GetMapping
    @PreAuthorize("permitAll()")
    public List<BranchView> list() {
        return branches.listAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public BranchView get(@PathVariable UUID id) {
        return branches.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchView> create(@RequestBody @Valid CreateBranchRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branches.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BranchView update(@PathVariable UUID id, @RequestBody @Valid UpdateBranchRequest req) {
        return branches.update(id, req);
    }
}
