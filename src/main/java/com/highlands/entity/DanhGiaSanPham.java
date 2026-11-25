package com.highlands.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Đánh giá sản phẩm của khách (1-5 sao + nhận xét). Mỗi khách chỉ có một đánh giá cho mỗi sản phẩm;
// đánh giá lại thì cập nhật. Ràng buộc UNIQUE (khach_hang_id, san_pham_id) nằm ở CSDL.
@Entity
@Table(name = "danh_gia_san_pham",
        uniqueConstraints = @UniqueConstraint(name = "uq_danh_gia_khach_san_pham", columnNames = {"khach_hang_id", "san_pham_id"}))
public class DanhGiaSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private SanPham sanPham;

    @Column(name = "so_sao", nullable = false)
    private int soSao;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(1000)")
    private String noiDung;

    @Column(name = "ngay_tao", nullable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime ngayCapNhat;

    public DanhGiaSanPham() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public SanPham getSanPham() { return sanPham; }
    public void setSanPham(SanPham sanPham) { this.sanPham = sanPham; }
    public int getSoSao() { return soSao; }
    public void setSoSao(int soSao) { this.soSao = soSao; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
}
