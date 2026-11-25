package com.highlands.repository;

import com.highlands.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    Optional<KhachHang> findByTenDangNhapIgnoreCase(String tenDangNhap);

    boolean existsByTenDangNhapIgnoreCase(String tenDangNhap);
}
