package com.highlands.service;

import com.highlands.entity.DanhGiaSanPham;
import com.highlands.entity.KhachHang;
import com.highlands.entity.PhanHoi;
import com.highlands.entity.SanPham;
import com.highlands.repository.DanhGiaSanPhamRepository;
import com.highlands.repository.DonHangKhachRepository;
import com.highlands.repository.KhachHangRepository;
import com.highlands.repository.PhanHoiRepository;
import com.highlands.repository.SanPhamRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// Khách đánh giá sản phẩm (1-5 sao) và gửi phản hồi cho cửa hàng
@Service
public class DanhGiaPhanHoiService {

    private static final int NHAN_XET_TOI_DA = 1000;
    private static final int TIEU_DE_TOI_THIEU = 3;
    private static final int TIEU_DE_TOI_DA = 150;
    private static final int PHAN_HOI_TOI_THIEU = 10;
    private static final int PHAN_HOI_TOI_DA = 2000;

    private final DanhGiaSanPhamRepository danhGiaRepo;
    private final PhanHoiRepository phanHoiRepo;
    private final DonHangKhachRepository donKhachRepo;
    private final KhachHangRepository khachHangRepo;
    private final SanPhamRepository sanPhamRepo;

    public DanhGiaPhanHoiService(DanhGiaSanPhamRepository danhGiaRepo, PhanHoiRepository phanHoiRepo,
                                 DonHangKhachRepository donKhachRepo, KhachHangRepository khachHangRepo,
                                 SanPhamRepository sanPhamRepo) {
        this.danhGiaRepo = danhGiaRepo;
        this.phanHoiRepo = phanHoiRepo;
        this.donKhachRepo = donKhachRepo;
        this.khachHangRepo = khachHangRepo;
        this.sanPhamRepo = sanPhamRepo;
    }

    // ===== Đánh giá sản phẩm =====

    // Các sản phẩm khách đã mua (chỉ những món này được đánh giá), xếp theo tên
    public List<SanPham> layDanhSachDaMua(Long khachHangId) {
        return donKhachRepo.sanPhamDaMua(khachHangId).stream()
                .sorted(Comparator.comparing((SanPham s) -> TienIch.boDau(s.getTen())))
                .toList();
    }

    public List<DanhGiaSanPham> layDanhGiaCuaToi(Long khachHangId) {
        return danhGiaRepo.timTheoKhachHang(khachHangId);
    }

    // Gửi đánh giá mới; nếu khách đã đánh giá sản phẩm này rồi thì cập nhật đánh giá cũ
    @Transactional
    public DanhGiaSanPham guiDanhGia(Long khachHangId, Long sanPhamId, int soSao, String noiDung) {
        if (soSao < 1 || soSao > 5) throw new LoiNghiepVu("Vui lòng chọn số sao từ 1 đến 5");
        String nx = noiDung == null ? "" : noiDung.trim();
        if (nx.length() > NHAN_XET_TOI_DA) throw new LoiNghiepVu("Nhận xét tối đa " + NHAN_XET_TOI_DA + " ký tự");
        if (sanPhamId == null) throw new LoiNghiepVu("Vui lòng chọn sản phẩm cần đánh giá");

        KhachHang kh = khachHangRepo.findById(khachHangId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy tài khoản khách hàng"));
        SanPham sp = sanPhamRepo.findById(sanPhamId)
                .orElseThrow(() -> new LoiNghiepVu("Sản phẩm không còn trong thực đơn"));
        if (donKhachRepo.demLanMua(khachHangId, sanPhamId) == 0) {
            throw new LoiNghiepVu("Bạn chỉ có thể đánh giá sản phẩm đã mua");
        }

        LocalDateTime bay = LocalDateTime.now();
        DanhGiaSanPham dg = danhGiaRepo.findByKhachHang_IdAndSanPham_Id(khachHangId, sanPhamId).orElse(null);
        if (dg == null) {
            dg = new DanhGiaSanPham();
            dg.setKhachHang(kh);
            dg.setSanPham(sp);
            dg.setNgayTao(bay);
        }
        dg.setSoSao(soSao);
        dg.setNoiDung(nx.isEmpty() ? null : nx);
        dg.setNgayCapNhat(bay);
        try {
            return danhGiaRepo.saveAndFlush(dg);
        } catch (DataIntegrityViolationException e) {
            throw new LoiNghiepVu("Đánh giá đang được lưu ở nơi khác, vui lòng thử lại");
        }
    }

    // ===== Phản hồi =====

    public List<PhanHoi> layPhanHoiCuaToi(Long khachHangId) {
        return phanHoiRepo.findByKhachHang_IdOrderByNgayGuiDescIdDesc(khachHangId);
    }

    public PhanHoi guiPhanHoi(Long khachHangId, String tieuDe, String noiDung) {
        String td = tieuDe == null ? "" : tieuDe.trim().replaceAll("\\s+", " ");
        String nd = noiDung == null ? "" : noiDung.trim();
        if (td.length() < TIEU_DE_TOI_THIEU) throw new LoiNghiepVu("Tiêu đề phải có ít nhất " + TIEU_DE_TOI_THIEU + " ký tự");
        if (td.length() > TIEU_DE_TOI_DA) throw new LoiNghiepVu("Tiêu đề tối đa " + TIEU_DE_TOI_DA + " ký tự");
        if (nd.length() < PHAN_HOI_TOI_THIEU) {
            throw new LoiNghiepVu("Nội dung phản hồi phải có ít nhất " + PHAN_HOI_TOI_THIEU + " ký tự");
        }
        if (nd.length() > PHAN_HOI_TOI_DA) throw new LoiNghiepVu("Nội dung phản hồi tối đa " + PHAN_HOI_TOI_DA + " ký tự");

        KhachHang kh = khachHangRepo.findById(khachHangId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy tài khoản khách hàng"));
        PhanHoi p = new PhanHoi();
        p.setKhachHang(kh);
        p.setTieuDe(td);
        p.setNoiDung(nd);
        p.setNgayGui(LocalDateTime.now());
        p.setTrangThai(PhanHoi.CHUA_XU_LY);
        return phanHoiRepo.save(p);
    }
}
