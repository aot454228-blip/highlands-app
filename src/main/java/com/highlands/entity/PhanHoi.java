package com.highlands.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Phản hồi / góp ý khách gửi cho cửa hàng
@Entity
@Table(name = "phan_hoi")
public class PhanHoi {

    public static final String CHUA_XU_LY = "Chưa xử lý";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @Column(name = "tieu_de", columnDefinition = "NVARCHAR(150) NOT NULL")
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(2000) NOT NULL")
    private String noiDung;

    @Column(name = "ngay_gui", nullable = false)
    private LocalDateTime ngayGui;

    @Column(name = "trang_thai", columnDefinition = "NVARCHAR(30) NOT NULL")
    private String trangThai = CHUA_XU_LY;

    public PhanHoi() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }
    public String getTieuDe() { return tieuDe; }
    public void setTieuDe(String tieuDe) { this.tieuDe = tieuDe; }
    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }
    public LocalDateTime getNgayGui() { return ngayGui; }
    public void setNgayGui(LocalDateTime ngayGui) { this.ngayGui = ngayGui; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
}
