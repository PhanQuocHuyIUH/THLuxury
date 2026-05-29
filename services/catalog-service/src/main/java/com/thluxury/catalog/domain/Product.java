package com.thluxury.catalog.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "ma_sp", insertable = false, updatable = false)
    @Generated(event = EventType.INSERT)
    private Long maSp;

    @Column(name = "ten_sp", nullable = false, length = 300)
    private String tenSp;

    @Column(name = "loai_sp", nullable = false, length = 50)
    private String loaiSp;

    @Column(name = "gia_ban_dau", nullable = false, precision = 15, scale = 0)
    private BigDecimal giaBanDau;

    @Column(name = "gia_giam_gia", nullable = false, precision = 15, scale = 0)
    private BigDecimal giaGiamGia = BigDecimal.ZERO;

    @Column(name = "trong_luong", precision = 7, scale = 2)
    private BigDecimal trongLuong;

    @Column(name = "ham_luong", length = 20)
    private String hamLuong;

    @Column(name = "loai_da", length = 50)  private String loaiDa;
    @Column(name = "mau_da",  length = 50)  private String mauDa;
    @Column(name = "gioi_tinh", length = 20) private String gioiTinh;
    @Column(name = "thuong_hieu", length = 100) private String thuongHieu;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public Product() {}

    public UUID getId() { return id; }
    public Long getMaSp() { return maSp; }
    public String getTenSp() { return tenSp; }
    public void setTenSp(String tenSp) { this.tenSp = tenSp; }
    public String getLoaiSp() { return loaiSp; }
    public void setLoaiSp(String loaiSp) { this.loaiSp = loaiSp; }
    public BigDecimal getGiaBanDau() { return giaBanDau; }
    public void setGiaBanDau(BigDecimal giaBanDau) { this.giaBanDau = giaBanDau; }
    public BigDecimal getGiaGiamGia() { return giaGiamGia; }
    public void setGiaGiamGia(BigDecimal giaGiamGia) { this.giaGiamGia = giaGiamGia; }
    public BigDecimal getTrongLuong() { return trongLuong; }
    public void setTrongLuong(BigDecimal trongLuong) { this.trongLuong = trongLuong; }
    public String getHamLuong() { return hamLuong; }
    public void setHamLuong(String hamLuong) { this.hamLuong = hamLuong; }
    public String getLoaiDa() { return loaiDa; }
    public void setLoaiDa(String loaiDa) { this.loaiDa = loaiDa; }
    public String getMauDa() { return mauDa; }
    public void setMauDa(String mauDa) { this.mauDa = mauDa; }
    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }
    public String getThuongHieu() { return thuongHieu; }
    public void setThuongHieu(String thuongHieu) { this.thuongHieu = thuongHieu; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public List<ProductImage> getImages() { return images; }

    public BigDecimal currentPrice() {
        return giaGiamGia != null && giaGiamGia.signum() > 0 ? giaGiamGia : giaBanDau;
    }
}
