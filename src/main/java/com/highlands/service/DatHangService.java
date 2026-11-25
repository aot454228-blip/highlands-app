package com.highlands.service;

import com.highlands.entity.ChiTietDonHang;
import com.highlands.entity.ChiTietDonHangTopping;
import com.highlands.entity.DonHang;
import com.highlands.entity.DonHangKhach;
import com.highlands.entity.KhachHang;
import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;
import com.highlands.repository.ChiTietDonHangRepository;
import com.highlands.repository.ChiTietDonHangToppingRepository;
import com.highlands.repository.DonHangKhachRepository;
import com.highlands.repository.DonHangRepository;
import com.highlands.repository.KhachHangRepository;
import com.highlands.repository.SanPhamRepository;
import com.highlands.repository.ToppingRepository;
import com.highlands.service.ThanhToanService.Dong;
import com.highlands.service.ThanhToanService.HoaDon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Khách hàng đặt hàng: giỏ hàng -> thanh toán (tiền mặt khi nhận hàng / chuyển khoản) -> xem lại đơn.
//
// Ghi vào các bảng của phân hệ quản lý theo đúng quy ước cũ để trang Thống kê vẫn đúng:
//   don_hang.tong_tien           = tổng thanh toán (đã trừ khuyến mãi)
//   chi_tiet_don_hang.don_gia    = giá của riêng món (không gồm topping)
// Phần riêng của khách (ai đặt, địa chỉ giao, trạng thái...) nằm ở bảng don_hang_khach.
@Service
public class DatHangService {

    public static final String TIEN_MAT = ThanhToanService.TIEN_MAT;
    public static final String CHUYEN_KHOAN = ThanhToanService.CHUYEN_KHOAN;
    private static final int SO_DONG_TOI_DA = 50;

    // Tài khoản cửa hàng nhận tiền khi khách chọn chuyển khoản (cấu hình trong application-local.properties)
    public record TaiKhoanNhanTien(String nganHang, String soTaiKhoan, String chuTaiKhoan) {
        public boolean daCauHinh() {
            return soTaiKhoan != null && !soTaiKhoan.isBlank();
        }
    }

    // Thông tin nhận hàng khách nhập lúc đặt
    public record GiaoHang(String soDienThoai, String diaChi, String ghiChu) {
    }

    public record DonCuaToi(Long donKhachId, Long maDon, LocalDateTime ngay, BigDecimal tong,
                            String phuongThuc, String trangThai) {
    }

    private final DonHangRepository donHangRepo;
    private final ChiTietDonHangRepository chiTietRepo;
    private final ChiTietDonHangToppingRepository chiTietToppingRepo;
    private final DonHangKhachRepository donKhachRepo;
    private final KhachHangRepository khachHangRepo;
    private final SanPhamRepository sanPhamRepo;
    private final ToppingRepository toppingRepo;
    private final BanHangService banHang;
    private final TaiKhoanNhanTien taiKhoanNhanTien;
    private final TransactionTemplate ghi;
    private final TransactionTemplate doc;

    public DatHangService(DonHangRepository donHangRepo, ChiTietDonHangRepository chiTietRepo,
                          ChiTietDonHangToppingRepository chiTietToppingRepo, DonHangKhachRepository donKhachRepo,
                          KhachHangRepository khachHangRepo, SanPhamRepository sanPhamRepo,
                          ToppingRepository toppingRepo, BanHangService banHang, PlatformTransactionManager tm,
                          @Value("${highlands.chuyen-khoan.ngan-hang:}") String nganHang,
                          @Value("${highlands.chuyen-khoan.so-tai-khoan:}") String soTaiKhoan,
                          @Value("${highlands.chuyen-khoan.chu-tai-khoan:}") String chuTaiKhoan) {
        this.donHangRepo = donHangRepo;
        this.chiTietRepo = chiTietRepo;
        this.chiTietToppingRepo = chiTietToppingRepo;
        this.donKhachRepo = donKhachRepo;
        this.khachHangRepo = khachHangRepo;
        this.sanPhamRepo = sanPhamRepo;
        this.toppingRepo = toppingRepo;
        this.banHang = banHang;
        this.taiKhoanNhanTien = new TaiKhoanNhanTien(nganHang.trim(), soTaiKhoan.trim(), chuTaiKhoan.trim());
        this.ghi = new TransactionTemplate(tm);
        this.doc = new TransactionTemplate(tm);
        this.doc.setReadOnly(true);
    }

    public TaiKhoanNhanTien layTaiKhoanNhanTien() {
        return taiKhoanNhanTien;
    }

    // ===== Đặt hàng =====

    // Giá, topping, khuyến mãi đều được tính lại từ CSDL; giao diện chỉ gửi món + số lượng.
    // Gọi lại với cùng maGiaoDich (bấm 2 lần, lỗi mạng...) thì trả lại hóa đơn đã lưu, không tạo đơn thứ hai.
    public HoaDon datHang(Long khachHangId, List<DongGio> gio, String maKhuyenMai, String phuongThuc,
                          GiaoHang giaoHang, String maGiaoDich) {
        if (!TIEN_MAT.equals(phuongThuc) && !CHUYEN_KHOAN.equals(phuongThuc)) {
            throw new LoiNghiepVu("Phương thức thanh toán không hợp lệ");
        }
        if (CHUYEN_KHOAN.equals(phuongThuc) && !taiKhoanNhanTien.daCauHinh()) {
            throw new LoiNghiepVu("Cửa hàng chưa mở nhận chuyển khoản. Vui lòng chọn thanh toán tiền mặt");
        }
        if (khachHangId == null) throw new LoiNghiepVu("Chưa đăng nhập");
        if (gio == null || gio.isEmpty()) throw new LoiNghiepVu("Giỏ hàng đang trống");
        if (gio.size() > SO_DONG_TOI_DA) throw new LoiNghiepVu("Giỏ hàng tối đa " + SO_DONG_TOI_DA + " dòng");
        String khoa = maGiaoDich == null ? "" : maGiaoDich.trim();
        if (khoa.isEmpty() || khoa.length() > 64) throw new LoiNghiepVu("Mã giao dịch không hợp lệ");
        if (giaoHang == null) throw new LoiNghiepVu("Vui lòng nhập thông tin nhận hàng");

        final String sdt = KhachHangService.chuanHoaSoDienThoai(giaoHang.soDienThoai());
        final String diaChi = giaoHang.diaChi() == null ? "" : giaoHang.diaChi().trim();
        if (diaChi.length() < 5) throw new LoiNghiepVu("Vui lòng nhập địa chỉ nhận hàng (ít nhất 5 ký tự)");
        if (diaChi.length() > 255) throw new LoiNghiepVu("Địa chỉ tối đa 255 ký tự");
        final String ghiChu = giaoHang.ghiChu() == null ? "" : giaoHang.ghiChu().trim();
        if (ghiChu.length() > 255) throw new LoiNghiepVu("Ghi chú tối đa 255 ký tự");

        List<DongGio> banSao = new ArrayList<>(gio);
        try {
            return ghi.execute(st -> luuDon(khachHangId, banSao, maKhuyenMai, phuongThuc, sdt, diaChi, ghiChu, khoa));
        } catch (DataIntegrityViolationException e) {
            // Hai lần bấm chạy song song: lần kia đã lưu xong với cùng mã giao dịch
            HoaDon daLuu = doc.execute(st -> donKhachRepo.findByMaGiaoDich(khoa)
                    .filter(d -> d.getKhachHang().getId().equals(khachHangId))
                    .map(this::dungHoaDon).orElse(null));
            if (daLuu != null) return daLuu;
            throw e;
        }
    }

    private HoaDon luuDon(Long khachHangId, List<DongGio> gio, String maKhuyenMai, String phuongThuc,
                          String sdt, String diaChi, String ghiChu, String khoa) {
        // Đã lưu rồi (gửi lại) -> trả đúng hóa đơn đó, nhưng chỉ khi đơn thuộc về chính khách này
        var daCo = donKhachRepo.findByMaGiaoDich(khoa);
        if (daCo.isPresent()) {
            if (!daCo.get().getKhachHang().getId().equals(khachHangId)) throw new LoiNghiepVu("Mã giao dịch không hợp lệ");
            return dungHoaDon(daCo.get());
        }

        KhachHang kh = khachHangRepo.findById(khachHangId)
                .orElseThrow(() -> new LoiNghiepVu("Không tìm thấy tài khoản khách hàng"));
        if (!kh.isHoatDong()) throw new LoiNghiepVu("Tài khoản đã bị khóa. Vui lòng liên hệ cửa hàng");

        // 1. Dựng lại từng dòng từ CSDL (không tin giá từ giao diện)
        record DongDaXacNhan(SanPham sp, int soLuong, List<Topping> topping) {
        }
        List<DongDaXacNhan> dong = new ArrayList<>();
        BigDecimal tienHang = BigDecimal.ZERO;
        for (DongGio g : gio) {
            int sl = g.getSoLuong();
            if (sl < 1 || sl > 99) throw new LoiNghiepVu("Số lượng mỗi món phải từ 1 đến 99");
            SanPham sp = sanPhamRepo.findByIdAndDangBanTrue(g.getSanPham().getId())
                    .filter(p -> p.getDanhMuc().isDangBan()).orElseThrow(() ->
                    new LoiNghiepVu("Món \"" + g.getSanPham().getTen() + "\" không còn trong thực đơn"));
            if (sp.getGia() != g.getSanPham().getGia())
                throw new LoiNghiepVu("Giá món đã đổi. Vui lòng làm mới thực đơn rồi thử lại.");
            Map<Long, Topping> tpDuyNhat = new LinkedHashMap<>();
            for (Topping t : g.getTopping()) {
                Topping tp = toppingRepo.findByIdAndDangBanTrue(t.getId()).orElseThrow(() ->
                        new LoiNghiepVu("Topping \"" + t.getTen() + "\" không còn tồn tại"));
                if (tp.getGia() != t.getGia())
                    throw new LoiNghiepVu("Giá topping đã đổi. Vui lòng làm mới thực đơn rồi thử lại.");
                tpDuyNhat.put(tp.getId(), tp);
            }
            List<Topping> tps = new ArrayList<>(tpDuyNhat.values());
            long giaTp = tps.stream().mapToLong(Topping::getGia).sum();
            dong.add(new DongDaXacNhan(sp, sl, tps));
            tienHang = tienHang.add(BigDecimal.valueOf((sp.getGia() + giaTp) * sl));
        }

        // 2. Khuyến mãi
        BigDecimal giam = BigDecimal.ZERO;
        BanHangService.ApDung km = null;
        if (maKhuyenMai != null && !maKhuyenMai.isBlank()) {
            km = banHang.apDung(maKhuyenMai, tienHang);
            if (!km.hopLe()) throw new LoiNghiepVu(km.thongBao());
            giam = km.giam();
        }
        BigDecimal tong = tienHang.subtract(giam);

        // 3. Ghi đơn hàng (bảng của phân hệ quản lý)
        DonHang dh = new DonHang();
        dh.setNgayDat(LocalDate.now());
        dh.setTongTien(tong.longValueExact());
        dh = donHangRepo.save(dh);

        for (DongDaXacNhan d : dong) {
            ChiTietDonHang ct = new ChiTietDonHang();
            ct.setDonHang(dh);
            ct.setSanPham(d.sp());
            ct.setSoLuong(d.soLuong());
            ct.setDonGia(d.sp().getGia());
            ct = chiTietRepo.save(ct);
            for (Topping t : d.topping()) {
                ChiTietDonHangTopping ctt = new ChiTietDonHangTopping();
                ctt.setChiTietDonHang(ct);
                ctt.setTopping(t);
                ctt.setTenTopping(t.getTen());
                ctt.setGiaTopping(BigDecimal.valueOf(t.getGia()));
                chiTietToppingRepo.save(ctt);
            }
        }

        // 4. Ghi thông tin khách
        DonHangKhach dk = new DonHangKhach();
        dk.setDonHang(dh);
        dk.setKhachHang(kh);
        if (km != null) {
            dk.setKhuyenMai(km.khuyenMai());
            dk.setMaKhuyenMai(km.khuyenMai().getMa());
        }
        dk.setTienHang(tienHang);
        dk.setGiamGia(giam);
        dk.setTongThanhToan(tong);
        dk.setPhuongThuc(phuongThuc);
        dk.setTrangThai(DonHangKhach.CHO_XAC_NHAN);
        dk.setSoDienThoai(sdt);
        dk.setDiaChiGiao(diaChi);
        dk.setGhiChu(ghiChu.isEmpty() ? null : ghiChu);
        dk.setNgayDat(LocalDateTime.now());
        dk.setMaGiaoDich(khoa);
        dk = donKhachRepo.save(dk);
        return dungHoaDon(dk);
    }

    // ===== Xem lại đơn =====

    public List<DonCuaToi> layDonCuaToi(Long khachHangId) {
        return doc.execute(st -> {
            List<DonCuaToi> kq = new ArrayList<>();
            for (DonHangKhach d : donKhachRepo.timTheoKhachHang(khachHangId)) {
                kq.add(new DonCuaToi(d.getId(), d.getDonHang().getId(), d.getNgayDat(), d.getTongThanhToan(),
                        d.getPhuongThuc(), d.getTrangThai()));
            }
            return kq;
        });
    }

    // Chỉ xem được đơn của chính mình
    public HoaDon layHoaDon(Long khachHangId, Long donKhachId) {
        return doc.execute(st -> {
            DonHangKhach d = donKhachRepo.findById(donKhachId).orElse(null);
            if (d == null || !d.getKhachHang().getId().equals(khachHangId)) throw new LoiNghiepVu("Không tìm thấy đơn hàng");
            return dungHoaDon(d);
        });
    }

    // Dựng hóa đơn từ dữ liệu đã lưu (dùng cho cả lúc vừa đặt và xem lại).
    // Trường "nhanVien" của HoaDon ở đây là tên khách (hóa đơn khách hiện nhãn "Khách hàng")
    HoaDon dungHoaDon(DonHangKhach dk) {
        List<ChiTietDonHang> cts = chiTietRepo.findByDonHang_IdOrderByIdAsc(dk.getDonHang().getId());
        Map<Long, List<ChiTietDonHangTopping>> tpTheoDong = new HashMap<>();
        if (!cts.isEmpty()) {
            List<Long> ids = cts.stream().map(ChiTietDonHang::getId).toList();
            for (ChiTietDonHangTopping t : chiTietToppingRepo.findByChiTietDonHang_IdIn(ids)) {
                tpTheoDong.computeIfAbsent(t.getChiTietDonHang().getId(), k -> new ArrayList<>()).add(t);
            }
        }
        List<Dong> dong = new ArrayList<>();
        for (ChiTietDonHang ct : cts) {
            List<ChiTietDonHangTopping> tps = tpTheoDong.getOrDefault(ct.getId(), List.of());
            BigDecimal giaTp = tps.stream().map(ChiTietDonHangTopping::getGiaTopping)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal donGia = BigDecimal.valueOf(ct.getDonGia());
            dong.add(new Dong(ct.getSoLuong(), ct.getSanPham().getTen(), donGia,
                    tps.stream().map(ChiTietDonHangTopping::getTenTopping).toList(), giaTp,
                    donGia.add(giaTp).multiply(BigDecimal.valueOf(ct.getSoLuong()))));
        }
        return new HoaDon(dk.getDonHang().getId(), dk.getNgayDat(), dk.getKhachHang().getHoTen(), dong,
                dk.getTienHang(), dk.getMaKhuyenMai(), dk.getGiamGia(), dk.getTongThanhToan(),
                dk.getPhuongThuc(), dk.getTongThanhToan(), BigDecimal.ZERO);
    }
}
