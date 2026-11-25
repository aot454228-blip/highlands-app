package com.highlands.service;

import com.highlands.entity.DonHangKhach;
import com.highlands.repository.DonHangKhachRepository;
import com.highlands.repository.ThanhToanDonHangRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Xử lý đơn online trên các bảng hiện có, riêng với thanh toán tại quầy. */
@Service
public class DonOnlineService {
    public record DonOnline(Long id, Long maDon, LocalDateTime ngay, String khach,
                            String sdt, String diaChi, String ghiChu, BigDecimal tong,
                            String phuongThuc, String trangThai, boolean daThuTien) {}
    public record ChiTiet(DonOnline don, ThanhToanService.HoaDon hoaDon) {}
    private final DonHangKhachRepository repo;
    private final DatHangService datHang;

    private final ThanhToanDonHangRepository thanhToanRepo;
    private final ThanhToanService thanhToan;

    public DonOnlineService(DonHangKhachRepository repo, DatHangService datHang,
                            ThanhToanDonHangRepository thanhToanRepo, ThanhToanService thanhToan) {
        this.repo = repo;
        this.datHang = datHang;
        this.thanhToanRepo = thanhToanRepo;
        this.thanhToan = thanhToan;
    }
    private DonOnline tomTat(DonHangKhach d, boolean daThuTien) {
        return new DonOnline(d.getId(), d.getDonHang().getId(), d.getNgayDat(),
                d.getKhachHang().getHoTen(), d.getSoDienThoai(), d.getDiaChiGiao(),
                d.getGhiChu(), d.getTongThanhToan(), d.getPhuongThuc(), d.getTrangThai(), daThuTien);
    }
    @Transactional(readOnly = true)
    public List<DonOnline> danhSach() {
        var daThu = new java.util.HashSet<>(thanhToanRepo.timMaDonOnlineDaThu());
        return repo.timDonOnline().stream()
                .map(d -> tomTat(d, daThu.contains(d.getDonHang().getId()))).toList();
    }
    @Transactional(readOnly = true)
    public ChiTiet chiTiet(Long id) {
        DonHangKhach d = repo.findById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn online"));
        return new ChiTiet(tomTat(d, thanhToanRepo.existsByDonHang_Id(d.getDonHang().getId())), datHang.dungHoaDon(d));
    }
    @Transactional
    public void xacNhanThuTien(Long id, Long nhanVienId) {
        if (nhanVienId == null) throw new LoiNghiepVu("Chưa xác định nhân viên");
        // Hai nhân viên xác nhận cùng lúc sẽ kiểm tra chứng từ lần lượt trên cùng đơn.
        DonHangKhach don = repo.khoaDon(id)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy đơn online"));
        if ("Hủy".equals(don.getTrangThai()) || "Đã hủy".equals(don.getTrangThai()))
            throw new LoiNghiepVu("Không thể thu tiền đơn đã hủy");
        thanhToan.ghiNhanThuTienOnline(don, nhanVienId);
    }

    public static String trangThaiTiep(String cu) {
        if (cu == null) return null;
        return switch (cu) {
            case DonHangKhach.CHO_XAC_NHAN -> "Đã xác nhận";
            case "Đã xác nhận" -> "Đang pha chế";
            case "Đang pha chế" -> "Đang giao hàng";
            case "Đang giao hàng" -> "Hoàn thành";
            default -> null;
        };
    }
    @Transactional
    public void capNhat(Long id, String cu, String moi) {
        if (moi == null || !moi.equals(trangThaiTiep(cu)))
            throw new LoiNghiepVu("Chuyển trạng thái không hợp lệ");
        // Cập nhật nguyên tử, tránh hai nhân viên ghi đè trạng thái của nhau.
        if (repo.doiTrangThai(id, cu, moi) != 1)
            throw new LoiNghiepVu("Đơn đã thay đổi hoặc không còn tồn tại. Vui lòng làm mới.");
    }
}
