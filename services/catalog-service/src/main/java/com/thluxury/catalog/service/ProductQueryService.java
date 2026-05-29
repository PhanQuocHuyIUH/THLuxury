package com.thluxury.catalog.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thluxury.catalog.repository.ProductRepository;
import com.thluxury.catalog.web.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * CQRS Query side — đọc thẳng từ product_view, có cache-aside qua {@link ProductCache}.
 * KHÔNG đụng bảng products để giảm tải write model.
 */
@Service
public class ProductQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryService.class);

    private final JdbcTemplate jdbc;
    private final ProductCache cache;
    private final ProductRepository products;
    private final ObjectMapper json;

    public ProductQueryService(JdbcTemplate jdbc, ProductCache cache, ProductRepository products) {
        this.jdbc = jdbc;
        this.cache = cache;
        this.products = products;
        this.json = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /** Query params hợp lệ — chuyển thành key cache + WHERE SQL. */
    public record ListQuery(
            String keyword,
            String loaiSp,
            String mauDa,
            String gioiTinh,
            BigDecimal giaMin,
            BigDecimal giaMax,
            String sort,        // price-asc | price-desc | newest
            int page,
            int size
    ) {}

    public Map<String, Object> list(ListQuery q) {
        String cacheKey = ProductCache.LIST_PREFIX + hash(q);
        String cached = cache.get(cacheKey);
        if (cached != null) {
            log.debug("cache HIT  {}", cacheKey);
            return parse(cached);
        }
        log.debug("cache MISS {}", cacheKey);

        StringBuilder where = new StringBuilder(" WHERE status = 'ACTIVE'");
        List<Object> args = new ArrayList<>();
        if (q.loaiSp() != null && !q.loaiSp().isBlank()) {
            where.append(" AND loai_sp = ?"); args.add(q.loaiSp());
        }
        if (q.mauDa() != null && !q.mauDa().isBlank()) {
            where.append(" AND doc->>'mauDa' = ?"); args.add(q.mauDa());
        }
        if (q.gioiTinh() != null && !q.gioiTinh().isBlank()) {
            where.append(" AND doc->>'gioiTinh' = ?"); args.add(q.gioiTinh());
        }
        if (q.giaMin() != null) { where.append(" AND gia_hien_tai >= ?"); args.add(q.giaMin()); }
        if (q.giaMax() != null) { where.append(" AND gia_hien_tai <= ?"); args.add(q.giaMax()); }
        if (q.keyword() != null && !q.keyword().isBlank()) {
            where.append(" AND search_tsv @@ plainto_tsquery('simple', ?)");
            args.add(q.keyword());
        }

        String orderBy = switch (q.sort() == null ? "" : q.sort()) {
            case "price-asc"  -> " ORDER BY gia_hien_tai ASC";
            case "price-desc" -> " ORDER BY gia_hien_tai DESC";
            default           -> " ORDER BY updated_at DESC";
        };

        int size = Math.max(1, Math.min(q.size(), 100));
        int offset = Math.max(0, q.page()) * size;

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM product_view" + where, Long.class, args.toArray());

        List<Object> argsWithLimit = new ArrayList<>(args);
        argsWithLimit.add(size);
        argsWithLimit.add(offset);
        List<JsonNode> items = jdbc.query(
                "SELECT doc FROM product_view" + where + orderBy + " LIMIT ? OFFSET ?",
                (rs, i) -> readJson(rs.getString(1)),
                argsWithLimit.toArray());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("content", items);
        resp.put("page", q.page());
        resp.put("size", size);
        resp.put("total", total == null ? 0 : total);

        try {
            cache.put(cacheKey, json.writeValueAsString(resp));
        } catch (Exception e) {
            log.warn("Failed to cache list result: {}", e.getMessage());
        }
        return resp;
    }

    public JsonNode getDetail(UUID id) {
        String cacheKey = ProductCache.DETAIL_PREFIX + id;
        String cached = cache.get(cacheKey);
        if (cached != null) return readJson(cached);

        List<JsonNode> rows = jdbc.query(
                "SELECT doc FROM product_view WHERE product_id = ?",
                (rs, i) -> readJson(rs.getString(1)),
                id);
        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                    "Sản phẩm không tồn tại: " + id);
        }
        JsonNode doc = rows.get(0);
        try {
            cache.put(cacheKey, json.writeValueAsString(doc));
        } catch (Exception ignored) {}
        return doc;
    }

    public JsonNode getByMaSp(Long maSp) {
        return products.findByMaSp(maSp)
                .map(p -> getDetail(p.getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                        "Sản phẩm không tồn tại: maSp=" + maSp));
    }

    public List<String> distinctLoaiSp() {
        return products.findDistinctLoaiSp();
    }

    // --------------------------------------------------------

    private JsonNode readJson(String s) {
        try { return json.readTree(s); }
        catch (Exception e) { throw new IllegalStateException("Bad JSONB in product_view: " + e.getMessage(), e); }
    }

    private Map<String, Object> parse(String s) {
        try { return json.readValue(s, Map.class); }
        catch (Exception e) { throw new IllegalStateException("Bad cached JSON: " + e.getMessage(), e); }
    }

    private String hash(ListQuery q) {
        String raw = String.join("|",
                nz(q.keyword()), nz(q.loaiSp()), nz(q.mauDa()), nz(q.gioiTinh()),
                String.valueOf(q.giaMin()), String.valueOf(q.giaMax()),
                nz(q.sort()), String.valueOf(q.page()), String.valueOf(q.size()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
