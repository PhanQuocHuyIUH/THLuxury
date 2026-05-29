package com.thluxury.catalog.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.thluxury.catalog.service.ProductQueryService;
import com.thluxury.catalog.service.ProductQueryService.ListQuery;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductQueryService query;

    public ProductController(ProductQueryService query) {
        this.query = query;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiSp,
            @RequestParam(required = false) String mauDa,
            @RequestParam(required = false) String gioiTinh,
            @RequestParam(required = false) BigDecimal giaMin,
            @RequestParam(required = false) BigDecimal giaMax,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return query.list(new ListQuery(keyword, loaiSp, mauDa, gioiTinh,
                giaMin, giaMax, sort, page, size));
    }

    @GetMapping("/{id}")
    public JsonNode detail(@PathVariable UUID id) {
        return query.getDetail(id);
    }

    @GetMapping("/by-ma-sp/{maSp}")
    public JsonNode detailByMaSp(@PathVariable Long maSp) {
        return query.getByMaSp(maSp);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return query.distinctLoaiSp();
    }
}
