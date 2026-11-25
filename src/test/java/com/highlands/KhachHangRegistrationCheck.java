package com.highlands;

import com.highlands.entity.KhachHang;
import com.highlands.repository.KhachHangRepository;
import com.highlands.service.*;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Standalone integration check with Spring-managed repository transactions and H2 only. */
public class KhachHangRegistrationCheck {
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = KhachHangRepository.class)
    static class TestConfig {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:registration_check;MODE=MSSQLServer;DB_CLOSE_DELAY=-1;IGNORECASE=TRUE", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var bean = new LocalContainerEntityManagerFactoryBean();
            bean.setDataSource(source);
            bean.setPackagesToScan("com.highlands.entity");
            bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            bean.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return bean;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean KhachHangService khachHangService(KhachHangRepository repo) { return new KhachHangService(repo); }
        @Bean DangNhapService dangNhapService(com.highlands.repository.TaiKhoanRepository repo) {
            return new DangNhapService(repo);
        }
    }
    public static void main(String[] args) throws Exception {
        char[] password = UUID.randomUUID().toString().toCharArray();
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            var service = context.getBean(KhachHangService.class);
            var repo = context.getBean(KhachHangRepository.class);
            var profile = new KhachHangService.HoSo("  Nguyễn   Văn An ", "+84 912 345 678", "", "");
            KhachHang registered = service.dangKy(profile, "  customer.check  ", password, password);
            check(registered.getId() != null && registered.getNgayTao() != null, "ID/time must be persisted");
            check(registered.getHoTen().equals("Nguyễn Văn An"), "Name normalization");
            check(registered.getSoDienThoai().equals("0912345678"), "Phone normalization");
            check(registered.getEmail() == null && registered.getDiaChi() == null, "Optional fields");
            check(MatKhau.kiemTra(password, registered.getMatKhauHash()), "Persisted password hash");
            check(service.dangNhap("CUSTOMER.CHECK", password).getId().equals(registered.getId()), "Login after registration");
            business(() -> service.dangKy(profile, "CUSTOMER.CHECK", password, password), "đã được sử dụng");
            business(() -> service.dangKy(profile, "bad name", password, password), "Tên đăng nhập");
            business(() -> service.dangKy(profile, "mismatch", password, new char[]{'x'}), "không khớp");
            business(() -> service.dangKy(null, "missing.profile", password, password), "Thiếu thông tin");
            business(() -> context.getBean(DangNhapService.class).dangNhapQuanLy("customer.check", password), "không đúng");

            // Force two successful availability checks before either insert.
            CyclicBarrier ready = new CyclicBarrier(2);
            AtomicInteger reads = new AtomicInteger();
            KhachHangRepository concurrentRepo = proxy((method, values) -> {
                Object result = invoke(repo, method, values);
                if (method.getName().equals("existsByTenDangNhapIgnoreCase") && reads.incrementAndGet() <= 2)
                    ready.await(15, TimeUnit.SECONDS);
                return result;
            });
            var concurrent = new KhachHangService(concurrentRepo);
            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Callable<Boolean> register = () -> {
                    try { concurrent.dangKy(profile, "concurrent.check", password, password); return true; }
                    catch (LoiNghiepVu e) {
                        check(e.getMessage().contains("đã được sử dụng"), "Correct concurrent error"); return false;
                    }
                };
                var first = pool.submit(register); var second = pool.submit(register);
                check(first.get(30, TimeUnit.SECONDS) != second.get(30, TimeUnit.SECONDS), "Exactly one concurrent registration succeeds");
            } finally { pool.shutdownNow(); }
            check(repo.count() == 2, "No duplicate customers");

            // Unrelated data errors must retain their cause, not become username conflicts.
            DataIntegrityViolationException unrelated = new DataIntegrityViolationException("fixture unrelated constraint");
            var failing = new KhachHangService(proxy((method, values) -> {
                if (method.getName().equals("existsByTenDangNhapIgnoreCase")) return false;
                if (method.getName().equals("saveAndFlush")) throw unrelated;
                throw new AssertionError(method.getName());
            }));
            try {
                failing.dangKy(profile, "constraint.check", password, password);
                throw new AssertionError("Expected original integrity error");
            } catch (DataIntegrityViolationException e) { check(e == unrelated, "Original error preserved"); }
            System.out.println("PASS: registration/login, validation, normalized profile, PBKDF2, duplicate/case-insensitive names, "
                    + "concurrent duplicate rollback, unrelated integrity errors, customer/manager isolation");
        } finally { Arrays.fill(password, '\0'); }
    }
    interface Call { Object run(Method method, Object[] values) throws Throwable; }
    static KhachHangRepository proxy(Call call) {
        return (KhachHangRepository) Proxy.newProxyInstance(KhachHangRepository.class.getClassLoader(),
                new Class<?>[]{KhachHangRepository.class}, (p, m, a) -> call.run(m, a));
    }
    static Object invoke(Object target, Method method, Object[] values) throws Throwable {
        try { return method.invoke(target, values); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }
    static void business(Runnable action, String message) {
        try { action.run(); throw new AssertionError("Expected business error"); }
        catch (LoiNghiepVu e) { check(e.getMessage().contains(message), "Unexpected error: " + e.getMessage()); }
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
