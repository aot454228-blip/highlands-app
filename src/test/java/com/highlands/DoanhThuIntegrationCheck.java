package com.highlands;

import com.highlands.entity.*;
import com.highlands.repository.*;
import com.highlands.service.ThongKeService;
import jakarta.persistence.EntityManager;
import org.hibernate.cfg.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Standalone integration check. Requires runtime classpath plus H2; uses only an in-memory DB. */
public class DoanhThuIntegrationCheck {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 20);

    public static void main(String[] args) throws Exception {
        Configuration cfg = new Configuration();
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:revenue_check;MODE=MSSQLServer");
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var files = Files.list(Path.of("src/main/java/com/highlands/entity"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                cfg.addAnnotatedClass(Class.forName("com.highlands.entity."
                        + file.getFileName().toString().replace(".java", "")));
            }
        }
        try (var factory = cfg.buildSessionFactory(); EntityManager em = factory.createEntityManager()) {
            JpaRepositoryFactory repos = new JpaRepositoryFactory(em);
            DonHangRepository orders = repos.getRepository(DonHangRepository.class);
            ThongKeService stats = new ThongKeService(orders,
                    repos.getRepository(PhieuNhapRepository.class),
                    repos.getRepository(ChiTietDonHangRepository.class));
            em.getTransaction().begin();
            NhanVien staff = new NhanVien();
            staff.setMaNhanVien("CHECK"); staff.setHoTen("Check"); em.persist(staff);
            DanhMuc category = new DanhMuc(); category.setTen("Check"); em.persist(category);
            SanPham product = new SanPham(); product.setTen("Coffee");
            product.setGia(100000); product.setDanhMuc(category); em.persist(product);
            KhachHang customer = new KhachHang(); customer.setHoTen("Check");
            customer.setSoDienThoai("0901234567"); customer.setTenDangNhap("check");
            customer.setMatKhauHash("fixture"); em.persist(customer);

            // Two distinct paid orders with the same amount must both count (no SUM DISTINCT).
            for (int i = 0; i < 2; i++) {
                DonHang order = order(em, DAY, 80000, product);
                payment(em, order, staff, "paid-" + i);
            }
            DonHang outside = order(em, DAY.minusDays(1), 80000, product);
            payment(em, outside, staff, "outside");
            order(em, DAY, 999999, product); // Legacy order with no proof of payment.
            DonHang onlineOrder = order(em, DAY, 100000, product);
            DonHangKhach online = new DonHangKhach();
            online.setDonHang(onlineOrder); online.setKhachHang(customer);
            online.setTienHang(new BigDecimal("100000")); online.setGiamGia(BigDecimal.ZERO);
            online.setTongThanhToan(new BigDecimal("100000")); online.setPhuongThuc("Chuyển khoản");
            online.setSoDienThoai("0901234567"); online.setDiaChiGiao("Fixture address");
            online.setNgayDat(DAY.atStartOfDay()); online.setMaGiaoDich("online"); em.persist(online);
            em.flush();

            // Delivery states, including completed and a cancellation fixture, never establish payment.
            for (String state : List.of(DonHangKhach.CHO_XAC_NHAN, "Đã xác nhận", "Đang pha chế",
                    "Đang giao hàng", "Hoàn thành", "Hủy")) {
                online.setTrangThai(state); em.flush();
                check(((Number) stats.doanhThu(DAY, DAY).get("tong")).longValue() == 160000,
                        "Unpaid online state changed revenue: " + state);
            }
            check(((Number) stats.doanhThu(DAY.minusDays(1), DAY).get("tong")).longValue() == 240000,
                    "Date range / inclusive boundaries");
            Map<String, Object> empty = stats.doanhThu(DAY.plusDays(1), DAY.plusDays(1));
            check(((Number) empty.get("tong")).longValue() == 0, "Empty revenue");
            check(((List<?>) empty.get("theoNgay")).isEmpty(), "Empty daily rows");
            var top = stats.banChay(DAY, DAY, 5);
            check(top.size() == 1 && ((Number) top.get(0).get("soLuong")).longValue() == 2,
                    "Best sellers must exclude unpaid orders");
            check(((Number) top.get(0).get("doanhThu")).longValue() == 200000,
                    "Product revenue keeps existing pre-discount semantics");
            // Read repeatedly after status changes: total is calculated, never incrementally added.
            check(((Number) stats.doanhThu(DAY, DAY).get("tong")).longValue() == 160000,
                    "Repeated reads double counted revenue");
            em.getTransaction().rollback();
            System.out.println("PASS: real repository JPQL + ThongKeService; paid/unpaid, status changes, "
                    + "same-value orders, date boundaries, empty results, discounts, best sellers, repeated reads");
        }
    }
    private static DonHang order(EntityManager em, LocalDate date, long total, SanPham product) {
        DonHang d = new DonHang(); d.setNgayDat(date); d.setTongTien(total); em.persist(d);
        ChiTietDonHang line = new ChiTietDonHang(); line.setDonHang(d); line.setSanPham(product);
        line.setSoLuong(1); line.setDonGia(100000); em.persist(line);
        return d;
    }
    private static void payment(EntityManager em, DonHang d, NhanVien staff, String key) {
        ThanhToanDonHang t = new ThanhToanDonHang(); t.setDonHang(d); t.setNhanVien(staff);
        t.setTienHang(new BigDecimal("100000")); t.setGiamGia(new BigDecimal("20000"));
        t.setTongThanhToan(new BigDecimal("80000")); t.setPhuongThuc("Tiền mặt");
        t.setTienKhachDua(new BigDecimal("100000")); t.setTienThua(new BigDecimal("20000"));
        t.setNgayThanhToan(LocalDateTime.of(d.getNgayDat(), java.time.LocalTime.NOON));
        t.setMaGiaoDich(key); em.persist(t);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
