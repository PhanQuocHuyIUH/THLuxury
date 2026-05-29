package com.thluxury.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {
    private ProductDtos() {}

    public record CreateProductRequest(
            @NotBlank @Size(max = 300) String tenSp,
            @NotBlank @Size(max = 50)  String loaiSp,
            @NotNull @DecimalMin("0")  BigDecimal giaBanDau,
            @DecimalMin("0")           BigDecimal giaGiamGia,
            @DecimalMin("0")           BigDecimal trongLuong,
            @Size(max = 20)            String hamLuong,
            @Size(max = 50)            String loaiDa,
            @Size(max = 50)            String mauDa,
            @Size(max = 20)            String gioiTinh,
            @Size(max = 100)           String thuongHieu,
            String description,
            @Valid List<ImageRequest> images
    ) {}

    public record UpdateProductRequest(
            @Size(max = 300) String tenSp,
            @Size(max = 50)  String loaiSp,
            @DecimalMin("0") BigDecimal giaBanDau,
            @DecimalMin("0") BigDecimal giaGiamGia,
            @DecimalMin("0") BigDecimal trongLuong,
            @Size(max = 20)  String hamLuong,
            @Size(max = 50)  String loaiDa,
            @Size(max = 50)  String mauDa,
            @Size(max = 20)  String gioiTinh,
            @Size(max = 100) String thuongHieu,
            String description,
            @Valid List<ImageRequest> images
    ) {}

    public record ImageRequest(
            @NotBlank @Size(max = 500) String imageUrl,
            @Min(0) Integer sortOrder
    ) {}

    public record ProductSummary(
            String id,
            Long maSp,
            String tenSp,
            String loaiSp,
            BigDecimal giaBanDau,
            BigDecimal giaGiamGia,
            BigDecimal giaHienTai,
            String thumbnail,
            String status
    ) {}
}
