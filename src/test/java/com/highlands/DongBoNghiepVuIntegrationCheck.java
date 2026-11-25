package com.highlands;

import com.highlands.entity.DanhMuc;
import com.highlands.entity.KhachHang;
import com.highlands.entity.KhuyenMai;
import com.highlands.entity.NhanVien;
import com.highlands.entity.SanPham;
import com.highlands.entity.Topping;
import com.highlands.repository.DonHangRepository;
import com.highlands.repository.KhachHangRepository;
import com.highlands.repository.KhuyenMaiRepository;
import com.highlands.repository.NhanVienRepository;
import com.highlands.repository.ThanhToanDonHangRepository;
import com.highlands.service.BanHangService;
import com.highlands.service.DanhMucService;
import com.highlands.service.DatHangService;
import com.highlands.service.DonOnlineService;
import com.highlands.service.DongGio;
import com.highlands.service.LoiNghiepVu;
import com.highlands.service.SanPhamService;
import com.highlands.service.ThanhToanService;
import com.highlands.service.ThongKeService;
import com.highlands.service.ToppingService;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Chạy với HighlandsDB kiểm thử mới, đã áp dụng các migration và chưa có đơn. */
public class DongBoNghiepVuIntegrationCheck {
    public static void main(String[] args) throws Exception {
        if (!"yes".equals(System.getenv("HIGHLANDS_TEST_DB")))
            throw new IllegalArgumentException("Chỉ chạy trên HighlandsDB kiểm thử (HIGHLANDS_TEST_DB=yes)");
        try (var ctx = new SpringApplicationBuilder(HighlandsApplication.class).web(WebApplicationType.NONE)
                .run("--spring.jpa.hibernate.ddl-auto=validate", "--highlands.chuyen-khoan.so-tai-khoan=TEST")) {
            var orders = ctx.getBean(DonHangRepository.class);
            var receipts = ctx.getBean(ThanhToanDonHangRepository.class);
            check(orders.count() == 0 && receipts.count() == 0, "Cần cơ sở dữ liệu kiểm thử chưa có đơn");
            var menu = ctx.getBean(BanHangService.class);
            var products = ctx.getBean(SanPhamService.class);
            var categories = ctx.getBean(DanhMucService.class);
            var toppings = ctx.getBean(ToppingService.class);

            var category = new DanhMuc();
            category.setTen("Kiểm thử");
            category = categories.them(category);
            var product = new SanPham();
            product.setTen("Cà phê");
            product.setGia(30000);
            product.setDanhMuc(category);
            product = products.them(product);
            final Long productId = product.getId();
            var topping = new Topping();
            topping.setTen("Thạch");
            topping.setGia(5000);
            topping = toppings.them(topping);
            final Long toppingId = topping.getId();

            var cart = new ArrayList<>(List.of(new DongGio(product, List.of(topping), 2)));
            var oldCart = List.copyOf(cart);
            category.setTen("Cà phê pha máy");
            categories.sua(category.getId(), category);
            product = products.layTheoId(productId);
            product.setGia(35000);
            products.sua(productId, product);
            topping = toppings.layTheoId(toppingId);
            topping.setGia(7000);
            toppings.sua(toppingId, topping);
            var updated = menu.taiThucDon();
            check(updated.mon().stream().anyMatch(m -> m.sanPham().getId().equals(productId)
                    && m.tenDanhMuc().equals("Cà phê pha máy")), "Thực đơn không cập nhật danh mục");
            check(updated.capNhatGio(cart) && cart.get(0).getSoLuong() == 2
                    && cart.get(0).thanhTien().longValueExact() == 84000, "Giỏ không giữ số lượng hoặc sai giá");

            var staff = new NhanVien();
            staff.setMaNhanVien("CHECK");
            staff.setHoTen("Nhân viên kiểm thử");
            final Long staffId = ctx.getBean(NhanVienRepository.class).save(staff).getId();
            var customer = new KhachHang();
            customer.setHoTen("Khách kiểm thử");
            customer.setTenDangNhap("dongbo");
            customer.setSoDienThoai("0912345678");
            customer.setMatKhauHash("fixture");
            final Long customerId = ctx.getBean(KhachHangRepository.class).save(customer).getId();
            var promo = new KhuyenMai();
            promo.setTen("Giảm khi kiểm thử");
            promo.setMa("TEST10K");
            promo.setLoai("GIAM_TIEN");
            promo.setSoTienGiam(10000L);
            ctx.getBean(KhuyenMaiRepository.class).save(promo);

            var checkout = ctx.getBean(DatHangService.class);
            var online = ctx.getBean(DonOnlineService.class);
            var counter = ctx.getBean(ThanhToanService.class);
            var stats = ctx.getBean(ThongKeService.class);
            var address = new DatHangService.GiaoHang("0912345678", "123 Đường kiểm thử", "");
            expectBusinessError(() -> checkout.datHang(customerId, oldCart, null, DatHangService.TIEN_MAT, address, "old-price"));
            expectBusinessError(() -> counter.thanhToan(staffId, oldCart, null, BigDecimal.valueOf(100000), "counter-old-price"));
            var cash = checkout.datHang(customerId, cart, "TEST10K", DatHangService.TIEN_MAT, address, "cash");
            var cashRetry = checkout.datHang(customerId, cart, "TEST10K", DatHangService.TIEN_MAT, address, "cash");
            check(cash.maDon().equals(cashRetry.maDon()), "Đặt lại tạo trùng đơn");
            var transfer = checkout.datHang(customerId, cart, null, DatHangService.CHUYEN_KHOAN, address, "transfer");
            var cashRow = online.danhSach().stream().filter(d -> d.maDon().equals(cash.maDon())).findFirst().orElseThrow();
            var transferRow = online.danhSach().stream().filter(d -> d.maDon().equals(transfer.maDon())).findFirst().orElseThrow();
            online.capNhat(cashRow.id(), cashRow.trangThai(), "Đã xác nhận");
            check(checkout.layDonCuaToi(customerId).stream().anyMatch(d -> d.maDon().equals(cash.maDon())
                    && d.trangThai().equals("Đã xác nhận")), "Khách chưa thấy trạng thái mới");
            expectBusinessError(() -> online.capNhat(cashRow.id(), cashRow.trangThai(), "Đã xác nhận"));

            var today = LocalDate.now();
            var yesterday = today.minusDays(1);
            ctx.getBean(JdbcTemplate.class).update("update don_hang set ngay_dat=? where id=?", yesterday, cash.maDon());
            check(revenue(stats, today) == 0 && stats.banChay(today, today, 5).isEmpty(), "Tính đơn chưa thu tiền");

            var executor = Executors.newFixedThreadPool(2);
            var start = new CountDownLatch(1);
            try {
                var first = executor.submit(() -> { start.await(); online.xacNhanThuTien(cashRow.id(), staffId); return true; });
                var second = executor.submit(() -> { start.await(); online.xacNhanThuTien(cashRow.id(), staffId); return true; });
                start.countDown();
                first.get(20, TimeUnit.SECONDS);
                second.get(20, TimeUnit.SECONDS);
            } finally {
                executor.shutdownNow();
            }
            online.xacNhanThuTien(cashRow.id(), staffId);
            check(receipts.count() == 1 && orders.count() == 2, "Tạo trùng đơn hoặc chứng từ");
            check(revenue(stats, today) == 74000 && revenue(stats, yesterday) == 0, "Sai doanh thu theo ngày thu");
            check(online.chiTiet(cashRow.id()).don().daThuTien() && !online.chiTiet(transferRow.id()).don().daThuTien(),
                    "Hiển thị sai trạng thái thu tiền");
            online.xacNhanThuTien(transferRow.id(), staffId);
            check(revenue(stats, today) == 158000, "Thiếu doanh thu tiền mặt hoặc chuyển khoản");
            check(((Number) stats.banChay(today, today, 5).get(0).get("soLuong")).longValue() == 4,
                    "Sai số lượng bán chạy");

            var counterBill = counter.thanhToan(staffId, cart, null, BigDecimal.valueOf(100000), "counter");
            check(counterBill.tienThua().longValueExact() == 16000 && revenue(stats, today) == 242000,
                    "Thanh toán tại quầy bị ảnh hưởng");

            products.xoa(productId);
            toppings.xoa(toppingId);
            categories.xoa(category.getId());
            var deleted = menu.taiThucDon();
            check(deleted.mon().stream().noneMatch(m -> m.sanPham().getId().equals(productId))
                    && deleted.topping().stream().noneMatch(t -> t.getId().equals(toppingId))
                    && deleted.danhMuc().stream().noneMatch(d -> d.getTen().equals("Cà phê pha máy")),
                    "Thực đơn vẫn còn món, danh mục hoặc topping đã gỡ");
            deleted.capNhatGio(cart);
            check(cart.size() == 1 && cart.get(0).getSoLuong() == 2, "Gỡ món làm mất giỏ");
            expectBusinessError(() -> checkout.datHang(customerId, cart, null, DatHangService.TIEN_MAT, address, "removed"));
            check(checkout.layHoaDon(customerId, cashRow.id()).tongThanhToan().longValueExact() == 74000
                    && revenue(stats, today) == 242000 && orders.count() == 3 && receipts.count() == 3,
                    "Gỡ món ảnh hưởng hóa đơn hoặc doanh thu cũ");
            System.out.println("PASS: SQL Server schema, menu edits/removal, cart, order retry, status, concurrent collection and reports");
        }
    }

    private static long revenue(ThongKeService stats, LocalDate day) {
        return ((Number) stats.doanhThu(day, day).get("tong")).longValue();
    }

    private static void expectBusinessError(Runnable action) {
        try {
            action.run();
        } catch (LoiNghiepVu expected) {
            return;
        }
        throw new AssertionError("Cần báo lỗi nghiệp vụ");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
