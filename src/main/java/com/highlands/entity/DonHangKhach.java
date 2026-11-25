package com.highlands.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Thông tin khách của một đơn (mở rộng 1-1 với don_hang, không sửa bảng don_hang của phân hệ quản lý).
// Ràng buộc UNIQUE của don_hang_id và ma_giao_dich nằm ở CSDL (sql/migration_khach_hang.sql).
@Entity
@Table(name = "don_hang_khach",
        uniqueConstraints = @UniqueConstraint(name = "uq_don_hang_khach_ma_giao_dich", columnNames = "ma_giao_dich"))
public class DonHangKhach {

    public static final String CHO_XAC_NHAN = "Chờ xác nhận";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "don_hang_id", nullable = false)
    private DonHang donHang;

    @ManyToOne(optional = false)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne
    @JoinColumn(name = "khuyen_mai_id")
    private KhuyenMai khuyenMai;

    @Column(name = "ma_khuyen_mai", columnDefinition = "NVARCHAR(50)")
    private String maKhuyenMai;

    @Column(name = "tien_hang", nullable = false, precision = 18, scale = 2)
    private BigDecimal tienHang;

    @Column(name = "giam_gia", nullable = false, precision = 18, scale = 2)
    private BigDecimal giamGia = BigDecimal.ZERO;

    @Column(name = "tong_thanh_toan", nullable = false, precision = 18, scale = 2)
    private BigDecimal tongThanhToan;

    // "Tiền mặt" (trả khi nhận hàng) hoặc "Chuyển khoản"
    @Column(name = "phuong_thuc", columnDefinition = "NVARCHAR(30) NOT NULL")
    private String phuongThuc;

    @Column(name = "trang_thai", columnDefinition = "NVARCHAR(30) NOT NULL")
    private String trangThai = CHO_XAC_NHAN;

    // Bản chụp lúc đặt: sửa hồ sơ sau này không làm đổi đơn cũ
    @Column(name = "so_dien_thoai", columnDefinition = "NVARCHAR(20) NOT NULL")
    private String soDienThoai;

    @Column(name = "dia_chi_giao", columnDefinition = "NVARCHAR(255) NOT NULL")
    private String diaChiGiao;

    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(255)")
    private String ghiChu;

    @Column(name = "ngay_dat", nullable = false)
    private LocalDateTime ngayDat;

    // Khóa chống tạo trùng đơn khi bấm đặt hàng nhiều lần
    @Column(name = "ma_giao_dich", columnDefinition = "NVARCHAR(64) NOT NULL")
    private String maGiaoDich;

    public DonHangKhach() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public DonHang getDonHang() { return donHang; }
    public void setDonHang(DonHang donHang) { this.donHang = donHang; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public KhuyenMai getKhuyenMai() { return khuyenMai; }
    public void setKhuyenMai(KhuyenMai khuyenMai) { this.khuyenMai = khuyenMai; }
    public String getMaKhuyenMai() { return maKhuyenMai; }
    public void setMaKhuyenMai(String maKhuyenMai) { this.maKhuyenMai = maKhuyenMai; }
    public BigDecimal getTienHang() { return tienHang; }
    public void setTienHang(BigDecimal tienHang) { this.tienHang = tienHang; }
    public BigDecimal getGiamGia() { return giamGia; }
    public void setGiamGia(BigDecimal giamGia) { this.giamGia = giamGia; }
    public BigDecimal getTongThanhToan() { return tongThanhToan; }
    public void setTongThanhToan(BigDecimal tongThanhToan) { this.tongThanhToan = tongThanhToan; }
    public String getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(String phuongThuc) { this.phuongThuc = phuongThuc; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getDiaChiGiao() { return diaChiGiao; }
    public void setDiaChiGiao(String diaChiGiao) { this.diaChiGiao = diaChiGiao; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }
    public String getMaGiaoDich() { return maGiaoDich; }
    public void setMaGiaoDich(String maGiaoDich) { this.maGiaoDich = maGiaoDich; }
}
