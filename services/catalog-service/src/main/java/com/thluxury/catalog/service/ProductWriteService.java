package com.thluxury.catalog.service;

import com.thluxury.catalog.domain.Product;
import com.thluxury.catalog.domain.ProductImage;
import com.thluxury.catalog.domain.ProductStatus;
import com.thluxury.catalog.messaging.ProductChangedEvent;
import com.thluxury.catalog.repository.ProductRepository;
import com.thluxury.catalog.web.dto.ProductDtos.*;
import com.thluxury.catalog.web.exception.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CQRS Command side — chỉ ghi vào write model + phát event sau commit.
 * KHÔNG đụng product_view trực tiếp — projection được rebuild qua event consumer.
 */
@Service
public class ProductWriteService {

    private final ProductRepository products;
    private final ApplicationEventPublisher events;

    public ProductWriteService(ProductRepository products, ApplicationEventPublisher events) {
        this.products = products;
        this.events = events;
    }

    @Transactional
    public UUID create(CreateProductRequest req, String triggeredBy) {
        Product p = new Product();
        p.setTenSp(req.tenSp());
        p.setLoaiSp(req.loaiSp());
        p.setGiaBanDau(req.giaBanDau());
        p.setGiaGiamGia(req.giaGiamGia() != null ? req.giaGiamGia() : BigDecimal.ZERO);
        p.setTrongLuong(req.trongLuong());
        p.setHamLuong(req.hamLuong());
        p.setLoaiDa(req.loaiDa());
        p.setMauDa(req.mauDa());
        p.setGioiTinh(req.gioiTinh());
        p.setThuongHieu(req.thuongHieu());
        p.setDescription(req.description());
        p.setStatus(ProductStatus.ACTIVE);

        if (req.images() != null) {
            int i = 0;
            for (ImageRequest img : req.images()) {
                p.getImages().add(new ProductImage(p, img.imageUrl(),
                        img.sortOrder() != null ? img.sortOrder() : i));
                i++;
            }
        }

        products.save(p);
        events.publishEvent(new ProductChangedEvent(
                "PRODUCT_CREATED", p.getId(), p.getMaSp(), Instant.now(), triggeredBy));
        return p.getId();
    }

    @Transactional
    public void update(UUID id, UpdateProductRequest req, Long ifMatchVersion, String triggeredBy) {
        Product p = products.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Sản phẩm không tồn tại: " + id));

        if (ifMatchVersion != null && ifMatchVersion != p.getVersion()) {
            throw new ApiException(HttpStatus.PRECONDITION_FAILED, "VERSION_MISMATCH",
                    "Phiên bản đã thay đổi, vui lòng tải lại (current=" + p.getVersion() + ")");
        }

        if (req.tenSp() != null)       p.setTenSp(req.tenSp());
        if (req.loaiSp() != null)      p.setLoaiSp(req.loaiSp());
        if (req.giaBanDau() != null)   p.setGiaBanDau(req.giaBanDau());
        if (req.giaGiamGia() != null)  p.setGiaGiamGia(req.giaGiamGia());
        if (req.trongLuong() != null)  p.setTrongLuong(req.trongLuong());
        if (req.hamLuong() != null)    p.setHamLuong(req.hamLuong());
        if (req.loaiDa() != null)      p.setLoaiDa(req.loaiDa());
        if (req.mauDa() != null)       p.setMauDa(req.mauDa());
        if (req.gioiTinh() != null)    p.setGioiTinh(req.gioiTinh());
        if (req.thuongHieu() != null)  p.setThuongHieu(req.thuongHieu());
        if (req.description() != null) p.setDescription(req.description());

        if (req.images() != null) {
            p.getImages().clear();
            int i = 0;
            for (ImageRequest img : req.images()) {
                p.getImages().add(new ProductImage(p, img.imageUrl(),
                        img.sortOrder() != null ? img.sortOrder() : i));
                i++;
            }
        }

        events.publishEvent(new ProductChangedEvent(
                "PRODUCT_UPDATED", p.getId(), p.getMaSp(), Instant.now(), triggeredBy));
    }

    @Transactional
    public void archive(UUID id, String triggeredBy) {
        Product p = products.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Sản phẩm không tồn tại: " + id));
        if (p.getStatus() == ProductStatus.ARCHIVED) return;
        p.setStatus(ProductStatus.ARCHIVED);
        events.publishEvent(new ProductChangedEvent(
                "PRODUCT_UPDATED", p.getId(), p.getMaSp(), Instant.now(), triggeredBy));
    }

    /** Used only for initial seed bootstrap — phát event để build read model. */
    @Transactional
    public List<UUID> bulkSeedIfEmpty(List<Product> drafts) {
        if (products.count() > 0) return List.of();
        for (Product p : drafts) {
            products.save(p);
            events.publishEvent(new ProductChangedEvent(
                    "PRODUCT_CREATED", p.getId(), p.getMaSp(), Instant.now(), "system"));
        }
        return drafts.stream().map(Product::getId).toList();
    }
}
