package com.highlands.repository;

import com.highlands.entity.DanhMuc;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DanhMucRepository extends JpaRepository<DanhMuc, Long> {

    List<DanhMuc> findByDangBanTrue(Sort sort);
    Optional<DanhMuc> findByIdAndDangBanTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DanhMuc> findLockedByIdAndDangBanTrue(Long id);

    // true nếu đã có danh mục khác (id khác) cùng tên, không phân biệt hoa thường
    boolean existsByTenIgnoreCaseAndDangBanTrueAndIdNot(String ten, Long id);
}
