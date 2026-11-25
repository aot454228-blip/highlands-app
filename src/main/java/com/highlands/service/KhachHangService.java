package com.highlands.service;

import com.highlands.entity.KhachHang;
import com.highlands.repository.KhachHangRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

// Tài khoản khách hàng: đăng ký, đăng nhập, xem / cập nhật thông tin cá nhân, đổi mật khẩu.
// Giống DangNhapService: mọi lỗi sai tên / sai mật khẩu trả CÙNG một thông báo và có khóa tạm khi sai liên tiếp.
@Service
public class KhachHangService {

    private static final String SAI = "Tên đăng nhập hoặc mật khẩu không đúng";
    private static final int SO_LAN_SAI_TOI_DA = 5;
    private static final Duration THOI_GIAN_KHOA = Duration.ofSeconds(60);
    private static final int MAT_KHAU_TOI_THIEU = 8;
    private static final int MAT_KHAU_TOI_DA = 100;

    private static final Pattern TEN_DANG_NHAP = Pattern.compile("^[A-Za-z0-9._]{4,30}$");
    private static final Pattern SO_DIEN_THOAI = Pattern.compile("^0\\d{9,10}$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private record LanSai(int soLan, Instant khoaDen) {
    }

    // Dữ liệu đăng ký / cập nhật hồ sơ do giao diện gửi lên
    public record HoSo(String hoTen, String soDienThoai, String email, String diaChi) {
    }

    private final KhachHangRepository repo;
    private final Map<String, LanSai> saiLienTiep = new ConcurrentHashMap<>();

    public KhachHangService(KhachHangRepository repo) {
        this.repo = repo;
    }

    // ===== Đăng ký / đăng nhập =====

    public KhachHang dangKy(HoSo hoSo, String tenDangNhap, char[] matKhau, char[] nhapLai) {
        String ten = tenDangNhap == null ? "" : tenDangNhap.trim();
        if (!TEN_DANG_NHAP.matcher(ten).matches()) {
            throw new LoiNghiepVu("Tên đăng nhập gồm 4-30 ký tự: chữ không dấu, số, dấu chấm hoặc gạch dưới");
        }
        kiemTraMatKhauMoi(matKhau, nhapLai, ten);

        KhachHang k = new KhachHang();
        ganHoSo(k, hoSo);
        k.setTenDangNhap(ten);
        if (repo.existsByTenDangNhapIgnoreCase(ten)) throw new LoiNghiepVu("Tên đăng nhập đã được sử dụng");
        k.setMatKhauHash(MatKhau.bam(matKhau));
        try {
            return repo.saveAndFlush(k);
        } catch (DataIntegrityViolationException e) {
            // saveAndFlush tự mở transaction ở repository; đến đây transaction lỗi đã rollback.
            // Kiểm tra lại bằng truy vấn mới để nhận diện đăng ký đồng thời cùng tên.
            // Không gán nhầm mọi lỗi NOT NULL / độ dài / ràng buộc khác thành trùng tên.
            if (repo.existsByTenDangNhapIgnoreCase(ten))
                throw new LoiNghiepVu("Tên đăng nhập đã được sử dụng");
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public KhachHang dangNhap(String tenDangNhap, char[] matKhau) {
        String ten = tenDangNhap == null ? "" : tenDangNhap.trim();
        if (ten.isEmpty() || matKhau == null || matKhau.length == 0) {
            throw new LoiNghiepVu("Vui lòng nhập tên đăng nhập và mật khẩu");
        }
        if (ten.length() > 50 || matKhau.length > 200) throw new LoiNghiepVu(SAI);

        String khoa = "dn:" + ten.toLowerCase(Locale.ROOT);
        kiemTraKhoa(khoa);

        KhachHang k = repo.findByTenDangNhapIgnoreCase(ten).orElse(null);
        boolean dung;
        if (k == null) {
            MatKhau.baoGia(matKhau); // thời gian trả lời không tiết lộ tên có tồn tại hay không
            dung = false;
        } else {
            dung = MatKhau.kiemTra(matKhau, k.getMatKhauHash());
        }
        if (!dung) {
            ghiNhanSai(khoa);
            throw new LoiNghiepVu(SAI);
        }
        saiLienTiep.remove(khoa);
        if (!k.isHoatDong()) throw new LoiNghiepVu("Tài khoản đã bị khóa. Vui lòng liên hệ cửa hàng");
        return k;
    }

    // ===== Thông tin cá nhân =====

    public KhachHang layTheoId(Long id) {
        return repo.findById(id).orElseThrow(() -> new LoiNghiepVu("Không tìm thấy tài khoản khách hàng"));
    }

    @Transactional
    public KhachHang capNhatThongTin(Long id, HoSo hoSo) {
        KhachHang k = layTheoId(id);
        ganHoSo(k, hoSo);
        return repo.save(k);
    }

    // Đổi mật khẩu: phải nhập đúng mật khẩu hiện tại (sai quá nhiều lần thì khóa tạm như đăng nhập)
    @Transactional
    public void doiMatKhau(Long id, char[] matKhauCu, char[] matKhauMoi, char[] nhapLai) {
        if (matKhauCu == null || matKhauCu.length == 0) throw new LoiNghiepVu("Vui lòng nhập mật khẩu hiện tại");
        KhachHang k = layTheoId(id);

        String khoa = "doi:" + id;
        kiemTraKhoa(khoa);
        if (matKhauCu.length > 200 || !MatKhau.kiemTra(matKhauCu, k.getMatKhauHash())) {
            ghiNhanSai(khoa);
            throw new LoiNghiepVu("Mật khẩu hiện tại không đúng");
        }
        saiLienTiep.remove(khoa);

        kiemTraMatKhauMoi(matKhauMoi, nhapLai, k.getTenDangNhap());
        if (Arrays.equals(matKhauCu, matKhauMoi)) throw new LoiNghiepVu("Mật khẩu mới phải khác mật khẩu hiện tại");
        k.setMatKhauHash(MatKhau.bam(matKhauMoi));
        repo.save(k);
    }

    // ===== Kiểm tra dữ liệu =====

    // "0912 345 678" / "+84912345678" -> "0912345678"; không hợp lệ -> ném lỗi
    public static String chuanHoaSoDienThoai(String s) {
        String t = s == null ? "" : s.trim().replaceAll("[\\s.\\-]", "");
        if (t.startsWith("+84")) t = "0" + t.substring(3);
        if (!SO_DIEN_THOAI.matcher(t).matches()) {
            throw new LoiNghiepVu("Số điện thoại không hợp lệ (VD: 0912345678)");
        }
        return t;
    }

    private static void ganHoSo(KhachHang k, HoSo h) {
        if (h == null) throw new LoiNghiepVu("Thiếu thông tin khách hàng");
        String ten = h.hoTen() == null ? "" : h.hoTen().trim().replaceAll("\\s+", " ");
        if (ten.length() < 2) throw new LoiNghiepVu("Họ tên phải có ít nhất 2 ký tự");
        if (ten.length() > 100) throw new LoiNghiepVu("Họ tên tối đa 100 ký tự");

        String email = h.email() == null ? "" : h.email().trim();
        if (!email.isEmpty() && (email.length() > 100 || !EMAIL.matcher(email).matches())) {
            throw new LoiNghiepVu("Email không hợp lệ");
        }
        String diaChi = h.diaChi() == null ? "" : h.diaChi().trim();
        if (diaChi.length() > 255) throw new LoiNghiepVu("Địa chỉ tối đa 255 ký tự");

        k.setHoTen(ten);
        k.setSoDienThoai(chuanHoaSoDienThoai(h.soDienThoai()));
        k.setEmail(email.isEmpty() ? null : email);
        k.setDiaChi(diaChi.isEmpty() ? null : diaChi);
    }

    private static void kiemTraMatKhauMoi(char[] moi, char[] nhapLai, String tenDangNhap) {
        if (moi == null || moi.length < MAT_KHAU_TOI_THIEU) {
            throw new LoiNghiepVu("Mật khẩu phải có ít nhất " + MAT_KHAU_TOI_THIEU + " ký tự");
        }
        if (moi.length > MAT_KHAU_TOI_DA) throw new LoiNghiepVu("Mật khẩu tối đa " + MAT_KHAU_TOI_DA + " ký tự");
        if (nhapLai == null || !Arrays.equals(moi, nhapLai)) throw new LoiNghiepVu("Hai lần nhập mật khẩu không khớp");
        if (tenDangNhap != null && trungKhongPhanBietHoa(moi, tenDangNhap)) {
            throw new LoiNghiepVu("Mật khẩu không được trùng tên đăng nhập");
        }
    }

    // So sánh mật khẩu với tên đăng nhập mà không tạo thêm bản sao mật khẩu dạng String
    private static boolean trungKhongPhanBietHoa(char[] mk, String ten) {
        if (mk.length != ten.length()) return false;
        for (int i = 0; i < mk.length; i++) {
            if (Character.toLowerCase(mk[i]) != Character.toLowerCase(ten.charAt(i))) return false;
        }
        return true;
    }

    // ===== Khóa tạm khi sai liên tiếp =====

    private void kiemTraKhoa(String khoa) {
        LanSai l = saiLienTiep.get(khoa);
        if (l != null && l.khoaDen() != null) {
            long con = Duration.between(Instant.now(), l.khoaDen()).toSeconds() + 1;
            if (con > 0) throw new LoiNghiepVu("Nhập sai quá nhiều lần. Vui lòng thử lại sau " + con + " giây");
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
