package com.highlands.service;

import com.highlands.entity.TaiKhoan;
import com.highlands.repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Đăng nhập nhân viên. Mọi lỗi sai tên / sai mật khẩu đều trả CÙNG một thông báo
// (không cho biết tên đăng nhập có tồn tại hay không) và có khóa tạm khi sai liên tiếp.
@Service
public class DangNhapService {

    private static final String SAI = "Tên đăng nhập hoặc mật khẩu không đúng";
    private static final int SO_LAN_SAI_TOI_DA = 5;
    private static final Duration THOI_GIAN_KHOA = Duration.ofSeconds(60);

    private record LanSai(int soLan, Instant khoaDen) {
    }

    private final TaiKhoanRepository repo;
    private final Map<String, LanSai> saiLienTiep = new ConcurrentHashMap<>();

    public DangNhapService(TaiKhoanRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public TaiKhoan dangNhap(String tenDangNhap, char[] matKhau) {
        TaiKhoan tk = xacThuc(tenDangNhap, matKhau);
        if (!TaiKhoan.NHAN_VIEN.equals(tk.getVaiTro()))
            throw new LoiNghiepVu("Tài khoản không có quyền vào phân hệ Nhân viên");
        return tk;
    }

    /** Chỉ service được tạo phiên sau khi đã kiểm tra mật khẩu và quyền từ CSDL. */
    public static final class PhienQuanLy {
        private final Long taiKhoanId;
        private final String tenDangNhap;
        private volatile boolean hieuLuc = true;

        private PhienQuanLy(TaiKhoan tk) {
            taiKhoanId = tk.getId();
            tenDangNhap = tk.getTenDangNhap();
        }
        public String tenDangNhap() { return tenDangNhap; }
        public void kiemTra() {
            if (!hieuLuc) throw new LoiNghiepVu("Phiên Quản lý đã kết thúc. Vui lòng đăng nhập lại");
        }
        public void dangXuat() { hieuLuc = false; }
    }

    @Transactional(readOnly = true)
    public PhienQuanLy dangNhapQuanLy(String tenDangNhap, char[] matKhau) {
        TaiKhoan tk = xacThuc(tenDangNhap, matKhau);
        if (!TaiKhoan.QUAN_LY.equals(tk.getVaiTro()))
            throw new LoiNghiepVu("Tài khoản không có quyền Quản lý");
        return new PhienQuanLy(tk);
    }

    @Transactional(readOnly = true)
    public void kiemTraQuanLy(PhienQuanLy phien) {
        if (phien == null) throw new LoiNghiepVu("Vui lòng đăng nhập Quản lý");
        phien.kiemTra();
        TaiKhoan tk = repo.findById(phien.taiKhoanId).orElse(null);
        if (tk == null || !tk.isHoatDong() || tk.getNhanVien() == null
                || !tk.getNhanVien().isDangLam() || !TaiKhoan.QUAN_LY.equals(tk.getVaiTro())) {
            phien.dangXuat();
            throw new LoiNghiepVu("Tài khoản không còn quyền Quản lý");
        }
    }

    private TaiKhoan xacThuc(String tenDangNhap, char[] matKhau) {
        String ten = tenDangNhap == null ? "" : tenDangNhap.trim();
        if (ten.isEmpty() || matKhau == null || matKhau.length == 0) {
            throw new LoiNghiepVu("Vui lòng nhập tên đăng nhập và mật khẩu");
        }
        if (ten.length() > 50 || matKhau.length > 200) throw new LoiNghiepVu(SAI);

        String khoa = ten.toLowerCase(Locale.ROOT);
        kiemTraKhoa(khoa);

        TaiKhoan tk = repo.findByTenDangNhapIgnoreCase(ten).orElse(null);
        boolean dung;
        if (tk == null) {
            MatKhau.baoGia(matKhau);
            dung = false;
        } else {
            dung = MatKhau.kiemTra(matKhau, tk.getMatKhauHash());
        }
        if (!dung) {
            ghiNhanSai(khoa);
            throw new LoiNghiepVu(SAI);
        }
        saiLienTiep.remove(khoa);

        if (!tk.isHoatDong()) throw new LoiNghiepVu("Tài khoản đã bị khóa. Vui lòng liên hệ quản lý");
        if (tk.getNhanVien() == null || !tk.getNhanVien().isDangLam()) throw new LoiNghiepVu("Nhân viên không còn làm việc. Vui lòng liên hệ quản lý");
        return tk;
    }

    private void kiemTraKhoa(String khoa) {
        LanSai l = saiLienTiep.get(khoa);
        if (l != null && l.khoaDen() != null) {
            long con = Duration.between(Instant.now(), l.khoaDen()).toSeconds() + 1;
            if (con > 0) throw new LoiNghiepVu("Đăng nhập sai quá nhiều lần. Vui lòng thử lại sau " + con + " giây");
            saiLienTiep.remove(khoa);
        }
    }

    private void ghiNhanSai(String khoa) {
        saiLienTiep.merge(khoa, new LanSai(1, null), (cu, moi) -> {
            int n = cu.soLan() + 1;
            return n >= SO_LAN_SAI_TOI_DA ? new LanSai(0, Instant.now().plus(THOI_GIAN_KHOA)) : new LanSai(n, null);
        });
    }
}
