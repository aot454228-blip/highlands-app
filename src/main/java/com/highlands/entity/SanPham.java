package com.highlands.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "san_pham")
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String ten;

    @Column(name = "gia", nullable = false)
    private long gia;

    // Mỗi sản phẩm thuộc một danh mục
    @ManyToOne
    @JoinColumn(name = "danh_muc_id", nullable = false)
    private DanhMuc danhMuc;

    // Đường dẫn ảnh (VD: /images/phin-sua-da.jpg hoặc link https://...). Để trống = hiện khung ảnh trống
    @Column(name = "hinh_anh", length = 500)
    private String hinhAnh;

    @ColumnDefault("1")
    @Column(name = "dang_ban", nullable = false)
    private boolean dangBan = true;

    public boolean isDangBan() { return dangBan; }
    public void setDangBan(boolean dangBan) { this.dangBan = dangBan; }

    public SanPham() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
    public long getGia() { return gia; }
    public void setGia(long gia) { this.gia = gia; }
    public DanhMuc getDanhMuc() { return danhMuc; }
    public void setDanhMuc(DanhMuc danhMuc) { this.danhMuc = danhMuc; }
    public String getHinhAnh() { return hinhAnh; }
    public void setHinhAnh(String hinhAnh) { this.hinhAnh = hinhAnh; }
}
