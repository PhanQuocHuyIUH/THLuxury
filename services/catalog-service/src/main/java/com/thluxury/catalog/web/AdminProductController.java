package com.thluxury.catalog.web;

import com.thluxury.catalog.service.ProductWriteService;
import com.thluxury.catalog.web.dto.ProductDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductWriteService write;

    public AdminProductController(ProductWriteService write) {
        this.write = write;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody @Valid CreateProductRequest req,
                                                       @AuthenticationPrincipal Jwt jwt) {
        UUID id = write.create(req, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id,
                                        @RequestBody @Valid UpdateProductRequest req,
                                        @AuthenticationPrincipal Jwt jwt,
                                        HttpServletRequest http) {
        String ifMatch = http.getHeader("If-Match");
        Long version = null;
        if (ifMatch != null) {
            try { version = Long.parseLong(ifMatch.replace("\"", "").trim()); }
            catch (NumberFormatException ignored) {}
        }
        write.update(id, req, version, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        write.archive(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
