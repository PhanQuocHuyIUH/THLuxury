package com.thluxury.catalog.web;

import com.thluxury.catalog.service.ProductQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ProductQueryService query;

    public CategoryController(ProductQueryService query) {
        this.query = query;
    }

    @GetMapping
    public List<String> all() {
        return query.distinctLoaiSp();
    }
}
