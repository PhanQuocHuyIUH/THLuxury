package com.thluxury.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thluxury.catalog.domain.Product;
import com.thluxury.catalog.domain.ProductImage;
import com.thluxury.catalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rebuild dòng product_view từ dữ liệu products + product_images.
 * Được gọi bởi consumer khi nhận event ProductChanged.
 */
@Service
public class ProductViewProjector {

    private static final Logger log = LoggerFactory.getLogger(ProductViewProjector.class);

    private final ProductRepository products;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public ProductViewProjector(ProductRepository products, JdbcTemplate jdbc) {
        this.products = products;
        this.jdbc = jdbc;
        this.json = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Transactional
    public void rebuild(UUID productId) {
        var opt = products.findById(productId);
        if (opt.isEmpty()) {
            log.warn("Projection rebuild skipped — product {} not found", productId);
            return;
        }
        Product p = opt.get();
        Map<String, Object> doc = toDoc(p);

        try {
            String docJson = json.writeValueAsString(doc);
            String searchText = buildSearchText(p);

            // Cast tham số String sang jsonb bằng ::jsonb để khỏi cần PGobject.
            jdbc.update("""
                    INSERT INTO product_view (product_id, doc, search_tsv, loai_sp, status, gia_hien_tai, updated_at)
                    VALUES (?, ?::jsonb, to_tsvector('simple', ?), ?, ?, ?, now())
                    ON CONFLICT (product_id) DO UPDATE SET
                        doc          = EXCLUDED.doc,
                        search_tsv   = EXCLUDED.search_tsv,
                        loai_sp      = EXCLUDED.loai_sp,
                        status       = EXCLUDED.status,
                        gia_hien_tai = EXCLUDED.gia_hien_tai,
                        updated_at   = now()
                    """,
                    p.getId(),
                    docJson,
                    searchText,
                    p.getLoaiSp(),
                    p.getStatus().name(),
                    p.currentPrice());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to project product " + productId, e);
        }
    }

    @Transactional
    public void delete(UUID productId) {
        jdbc.update("DELETE FROM product_view WHERE product_id = ?", productId);
    }

    private Map<String, Object> toDoc(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("maSp", p.getMaSp());
        m.put("tenSp", p.getTenSp());
        m.put("loaiSp", p.getLoaiSp());
        m.put("giaBanDau", p.getGiaBanDau());
        m.put("giaGiamGia", p.getGiaGiamGia() == null ? BigDecimal.ZERO : p.getGiaGiamGia());
        m.put("giaHienTai", p.currentPrice());
        m.put("trongLuong", p.getTrongLuong());
        m.put("hamLuong", p.getHamLuong());
        m.put("loaiDa", p.getLoaiDa());
        m.put("mauDa", p.getMauDa());
        m.put("gioiTinh", p.getGioiTinh());
        m.put("thuongHieu", p.getThuongHieu());
        m.put("description", p.getDescription());
        m.put("status", p.getStatus().name());
        m.put("images", p.getImages().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(ProductImage::getImageUrl)
                .toList());
        m.put("createdAt", p.getCreatedAt());
        m.put("updatedAt", p.getUpdatedAt());
        return m;
    }

    private String buildSearchText(Product p) {
        return List.of(
                nonNull(p.getTenSp()), nonNull(p.getLoaiSp()), nonNull(p.getLoaiDa()),
                nonNull(p.getMauDa()), nonNull(p.getThuongHieu()), nonNull(p.getDescription()))
                .stream().filter(s -> !s.isEmpty()).reduce("", (a, b) -> a + " " + b).trim();
    }

    private String nonNull(String s) { return s == null ? "" : s; }
}
