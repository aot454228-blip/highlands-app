package com.highlands.repository;

import com.highlands.entity.SanPham;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface SanPhamRepository extends JpaRepository<SanPham, Long> {

    List<SanPham> findByDangBanTrue(Sort sort);
    Optional<SanPham> findByIdAndDangBanTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SanPham> findLockedByIdAndDangBanTrue(Long id);

    List<SanPham> findByDangBanTrueAndTenContainingIgnoreCase(String tuKhoa);

    List<SanPham> findByDangBanTrueAndDanhMuc_Id(Long danhMucId);

    List<SanPham> findByDangBanTrueAndTenContainingIgnoreCaseAndDanhMuc_Id(String tuKhoa, Long danhMucId);

    boolean existsByDanhMuc_IdAndDangBanTrue(Long danhMucId);

    // true nếu trong cùng danh mục đã có sản phẩm khác (id khác) cùng tên
    boolean existsByTenIgnoreCaseAndDanhMuc_IdAndDangBanTrueAndIdNot(String ten, Long danhMucId, Long id);
}
