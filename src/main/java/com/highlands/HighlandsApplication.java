package com.highlands;

import com.highlands.service.DanhMucService;
import com.highlands.service.KhuyenMaiService;
import com.highlands.service.SanPhamService;
import com.highlands.service.ThongKeService;
import com.highlands.service.ToppingService;
import com.highlands.ui.ChonVaiTro;
import com.highlands.ui.DangNhapKhachHang;
import com.highlands.ui.DangNhapNhanVien;
import com.highlands.ui.DichVu;
import com.highlands.ui.ManHinhCho;
import com.highlands.ui.FormChinh;
import com.highlands.ui.Theme;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

// Điểm khởi động của app desktop:
// 1. Hiện màn hình chờ  2. Khởi động Spring (kết nối SQL Server, JPA)  3. Chọn phân hệ Quản lý / Nhân viên / Khách hàng
@SpringBootApplication
public class HighlandsApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        Theme.caiDat();

        ManHinhCho cho = ManHinhCho.hien("Đang kết nối SQL Server...");

        ConfigurableApplicationContext ctx;
        try {
            ctx = new SpringApplicationBuilder(HighlandsApplication.class)
                    .headless(false)                  // cho phép mở cửa sổ Swing
                    .web(WebApplicationType.NONE)     // không chạy web server
                    .run(args);
        } catch (Throwable e) {
            cho.dong();
            JOptionPane.showMessageDialog(null,
                    "Không kết nối được SQL Server.\n"
                            + "Hãy kiểm tra SQL Server đã chạy và thông tin trong application.properties.\n\n"
                            + nguyenNhan(e),
                    "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        DichVu dv = new DichVu(
                ctx.getBean(KhuyenMaiService.class),
                ctx.getBean(SanPhamService.class),
                ctx.getBean(DanhMucService.class),
                ctx.getBean(ToppingService.class),
                ctx.getBean(ThongKeService.class));

        dvChung = dv;
        ctxChung = ctx;
        SwingUtilities.invokeLater(() -> {
            cho.dong();
            hienChonVaiTro();
        });
    }

    private static ConfigurableApplicationContext ctxChung;
    private static DichVu dvChung;

    /** Mở hộp thoại chọn vai trò. Gọi lại hàm này khi đăng xuất để đổi vai trò. */
    public static void hienChonVaiTro() {
        ConfigurableApplicationContext ctx = ctxChung;
        DichVu dv = dvChung;
        // Mọi phân hệ đều xác thực tài khoản; chọn vai trò không tự cấp quyền.
        ChonVaiTro.VaiTro vaiTro = ChonVaiTro.hien();
        if (vaiTro == null) {
            ctx.close();
            System.exit(0);
            return;
        }
        if (vaiTro == ChonVaiTro.VaiTro.NHAN_VIEN) {
            new DangNhapNhanVien(ctx).setVisible(true);
            return;
        }
        if (vaiTro == ChonVaiTro.VaiTro.KHACH_HANG) {
            new DangNhapKhachHang(ctx).setVisible(true);
            return;
        }
        DangNhapNhanVien.choQuanLy(ctx, phien -> {
            FormChinh f = new FormChinh(dv, phien, HighlandsApplication::hienChonVaiTro);
            f.setVisible(true);
        }).setVisible(true);
    }

    // Lấy thông báo gốc (lớp trong cùng) để người dùng biết lỗi gì
    private static String nguyenNhan(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) t = t.getCause();
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
