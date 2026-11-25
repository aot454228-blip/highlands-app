package com.highlands.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Khách hàng đặt hàng trên app (bảng khach_hang, tạo bởi sql/migration_khach_hang.sql).
// Có tài khoản riêng: mật khẩu chỉ lưu dạng băm PBKDF2 (xem service/MatKhau)
@Entity
@Table(name = "khach_hang",
        uniqueConstraints = @UniqueConstraint(name = "uq_khach_hang_ten_dang_nhap", columnNames = "ten_dang_nhap"))
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ho_ten", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String hoTen;

    @Column(name = "so_dien_thoai", columnDefinition = "NVARCHAR(20) NOT NULL")
    private String soDienThoai;

    @Column(name = "email", columnDefinition = "NVARCHAR(100)")
    private String email;

    @Column(name = "dia_chi", columnDefinition = "NVARCHAR(255)")
    private String diaChi;

    @Column(name = "ten_dang_nhap", columnDefinition = "NVARCHAR(50) NOT NULL")
    private String tenDangNhap;

    @Column(name = "mat_khau_hash", columnDefinition = "NVARCHAR(255) NOT NULL")
    private String matKhauHash;

    @Column(name = "hoat_dong", nullable = false)
    private boolean hoatDong = true;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    public KhachHang() {
    }

    @PrePersist
    void truocKhiLuu() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDiaChi() { return diaChi; }
    public void setDiaChi(String diaChi) { this.diaChi = diaChi; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getMatKhauHash() { return matKhauHash; }
    public void setMatKhauHash(String matKhauHash) { this.matKhauHash = matKhauHash; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
}
