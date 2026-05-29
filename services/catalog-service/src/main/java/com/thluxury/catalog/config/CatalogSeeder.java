package com.thluxury.catalog.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thluxury.catalog.domain.Product;
import com.thluxury.catalog.domain.ProductImage;
import com.thluxury.catalog.repository.ProductRepository;
import com.thluxury.catalog.service.ProductViewProjector;
import com.thluxury.catalog.service.ProductCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Đọc src/main/resources/seed/products.json (chính là 5TLuxury/data/trangSuc.json),
 * insert vào bảng products + product_images, rebuild luôn product_view (không qua
 * RabbitMQ vì lúc startup AMQP có thể chưa sẵn sàng).
 *
 * Idempotent: nếu DB đã có sản phẩm thì bỏ qua.
 */
@Component
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private final ProductRepository products;
    private final ProductViewProjector projector;
    private final ProductCache cache;

    public CatalogSeeder(ProductRepository products,
                         ProductViewProjector projector,
                         ProductCache cache) {
        this.products = products;
        this.projector = projector;
        this.cache = cache;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (products.count() > 0) {
            log.info("CatalogSeeder: skipped — products table not empty");
            return;
        }

        ObjectMapper json = new ObjectMapper();
        JsonNode arr;
        try (var in = new ClassPathResource("seed/products.json").getInputStream()) {
            arr = json.readTree(in);
        }

        int created = 0;
        for (JsonNode n : arr) {
            Product p = new Product();
            p.setTenSp(text(n, "tenSP"));
            p.setLoaiSp(text(n, "loaiSP"));
            p.setGiaBanDau(bd(n, "giaBanDau"));
            p.setGiaGiamGia(bd(n, "giaGiamGia"));
            p.setTrongLuong(bd(n, "trongLuong"));
            p.setHamLuong(text(n, "hamLuong"));
            p.setLoaiDa(text(n, "loaiDa"));
            p.setMauDa(text(n, "mauDa"));
            p.setGioiTinh(text(n, "gioiTinh"));
            p.setThuongHieu(text(n, "thuongHieu"));
            p.setDescription("Sản phẩm " + p.getTenSp() + " thương hiệu " + p.getThuongHieu()
                    + ", chất liệu " + p.getHamLuong() + ", đá " + p.getLoaiDa() + " màu " + p.getMauDa() + ".");

            int order = 0;
            for (String key : new String[]{"hinh1", "hinh2", "hinh3"}) {
                String src = text(n, key);
                if (src != null && !src.isBlank()) {
                    // /src/image/1.1.png → /products/1.1.png  (FE Next.js serve from /public/products)
                    String url = src.replace("/src/image/", "/products/");
                    p.getImages().add(new ProductImage(p, url, order++));
                }
            }

            // saveAndFlush để @Generated(maSp) được fetch về trước khi projection chạy.
            products.saveAndFlush(p);
            projector.rebuild(p.getId());
            created++;
        }
        cache.invalidateAllLists();
        log.info("CatalogSeeder: imported {} products from seed/products.json", created);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static BigDecimal bd(JsonNode n, String field) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) return null;
        return new BigDecimal(v.asText());
    }
}
