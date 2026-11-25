package com.highlands;

import com.highlands.entity.*;
import com.highlands.repository.TaiKhoanRepository;
import com.highlands.service.*;
import org.hibernate.cfg.Configuration;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import java.nio.file.*;
import java.util.*;

/** Standalone integration check: real PBKDF2 and repository, H2 in memory only. */
public class QuanLyAuthenticationCheck {
    public static void main(String[] args) throws Exception {
        Configuration cfg = new Configuration();
        cfg.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        cfg.setProperty("hibernate.connection.url", "jdbc:h2:mem:auth_check;MODE=MSSQLServer");
        cfg.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var files = Files.list(Path.of("src/main/java/com/highlands/entity"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList())
                cfg.addAnnotatedClass(Class.forName("com.highlands.entity."
                        + file.getFileName().toString().replace(".java", "")));
        }
        char[] password = UUID.randomUUID().toString().toCharArray();
        try (var factory = cfg.buildSessionFactory(); var em = factory.createEntityManager()) {
            em.getTransaction().begin();
            String hash = MatKhau.bam(password);
            NhanVien employee = new NhanVien(); employee.setHoTen("Fixture");
            employee.setMaNhanVien("AUTH-CHECK"); em.persist(employee);
            TaiKhoan staff = account("staff-check", hash, employee, TaiKhoan.NHAN_VIEN);
            TaiKhoan manager = account("manager-check", hash, employee, TaiKhoan.QUAN_LY);
            TaiKhoan disabled = account("disabled-check", hash, employee, TaiKhoan.QUAN_LY);
            disabled.setHoatDong(false);
            em.persist(staff); em.persist(manager); em.persist(disabled);
            KhachHang customer = new KhachHang(); customer.setHoTen("Customer fixture");
            customer.setTenDangNhap("customer-check"); customer.setMatKhauHash(hash);
            customer.setSoDienThoai("0901234567"); em.persist(customer);
            em.flush();
            TaiKhoanRepository repo = new JpaRepositoryFactory(em).getRepository(TaiKhoanRepository.class);
            DangNhapService auth = new DangNhapService(repo);
            check(TaiKhoan.NHAN_VIEN.equals(new TaiKhoan().getVaiTro()), "Safe default role");
            check(auth.dangNhap("staff-check", password).getId().equals(staff.getId()), "Staff login regression");
            reject(() -> auth.dangNhapQuanLy("staff-check", password));
            reject(() -> auth.dangNhapQuanLy("customer-check", password));
            reject(() -> auth.dangNhapQuanLy("disabled-check", password));
            reject(() -> auth.dangNhapQuanLy("manager-check", new char[]{'x'}));
            reject(() -> auth.dangNhapQuanLy("", password));
            reject(() -> auth.dangNhapQuanLy("manager-check", null));
            reject(() -> auth.dangNhap("manager-check", password));
            var session = auth.dangNhapQuanLy("  MANAGER-CHECK  ", password);
            auth.kiemTraQuanLy(session);
            session.dangXuat();
            reject(session::kiemTra);
            reject(() -> auth.kiemTraQuanLy(session));
            reject(() -> auth.kiemTraQuanLy(null));
            var revoked = auth.dangNhapQuanLy("manager-check", password);
            manager.setVaiTro(TaiKhoan.NHAN_VIEN); em.flush();
            reject(() -> auth.kiemTraQuanLy(revoked));
            reject(() -> auth.dangNhapQuanLy("manager-check", password));
            manager.setVaiTro("UNKNOWN"); em.flush();
            reject(() -> auth.dangNhapQuanLy("manager-check", password));
            manager.setVaiTro(TaiKhoan.QUAN_LY); employee.setDangLam(false); em.flush();
            reject(() -> auth.dangNhapQuanLy("manager-check", password));
            employee.setDangLam(true); em.flush();
            DangNhapService limited = new DangNhapService(repo);
            for (int i = 0; i < 5; i++) reject(() -> limited.dangNhapQuanLy("manager-check", new char[]{'x'}));
            reject(() -> limited.dangNhapQuanLy("manager-check", password));
            em.getTransaction().rollback();
            System.out.println("PASS: manager authentication, employee/customer isolation, wrong password, "
                    + "disabled/inactive, unknown role, logout/revocation, brute-force limit, staff regression");
        } finally { Arrays.fill(password, '\0'); }
    }
    private static TaiKhoan account(String name, String hash, NhanVien employee, String role) {
        TaiKhoan t = new TaiKhoan(); t.setTenDangNhap(name); t.setMatKhauHash(hash);
        t.setNhanVien(employee); t.setVaiTro(role); return t;
    }
    private static void reject(Runnable action) {
        try { action.run(); throw new AssertionError("Expected access rejection"); }
        catch (LoiNghiepVu expected) { }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
