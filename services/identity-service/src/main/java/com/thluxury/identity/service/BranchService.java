package com.thluxury.identity.service;

import com.thluxury.identity.domain.Branch;
import com.thluxury.identity.repository.BranchRepository;
import com.thluxury.identity.web.dto.AdminDtos.*;
import com.thluxury.identity.web.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BranchService {

    private final BranchRepository branches;

    public BranchService(BranchRepository branches) {
        this.branches = branches;
    }

    @Transactional(readOnly = true)
    public List<BranchView> listAll() {
        return branches.findAll().stream().map(BranchService::toView).toList();
    }

    @Transactional(readOnly = true)
    public BranchView get(UUID id) {
        return toView(load(id));
    }

    @Transactional
    public BranchView create(CreateBranchRequest req) {
        if (branches.existsByCode(req.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "BRANCH_CODE_TAKEN",
                    "Mã chi nhánh đã tồn tại: " + req.code());
        }
        Branch b = new Branch();
        b.setCode(req.code().toUpperCase());
        b.setName(req.name());
        b.setAddress(req.address());
        b.setCity(req.city());
        b.setDistrict(req.district());
        b.setWard(req.ward());
        b.setPhone(req.phone());
        b.setLat(req.lat());
        b.setLng(req.lng());
        b.setEnabled(true);
        branches.save(b);
        return toView(b);
    }

    @Transactional
    public BranchView update(UUID id, UpdateBranchRequest req) {
        Branch b = load(id);
        if (req.name() != null)     b.setName(req.name());
        if (req.address() != null)  b.setAddress(req.address());
        if (req.city() != null)     b.setCity(req.city());
        if (req.district() != null) b.setDistrict(req.district());
        if (req.ward() != null)     b.setWard(req.ward());
        if (req.phone() != null)    b.setPhone(req.phone());
        if (req.lat() != null)      b.setLat(req.lat());
        if (req.lng() != null)      b.setLng(req.lng());
        if (req.enabled() != null)  b.setEnabled(req.enabled());
        return toView(b);
    }

    public Branch load(UUID id) {
        return branches.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BRANCH_NOT_FOUND",
                        "Chi nhánh không tồn tại: " + id));
    }

    public static BranchView toView(Branch b) {
        return new BranchView(
                b.getId(), b.getCode(), b.getName(), b.getAddress(),
                b.getCity(), b.getDistrict(), b.getWard(), b.getPhone(),
                b.getLat(), b.getLng(), b.isEnabled());
    }
}
