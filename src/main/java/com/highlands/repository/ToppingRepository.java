package com.highlands.repository;

import com.highlands.entity.Topping;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ToppingRepository extends JpaRepository<Topping, Long> {

    List<Topping> findByDangBanTrue(Sort sort);
    Optional<Topping> findByIdAndDangBanTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Topping> findLockedByIdAndDangBanTrue(Long id);

    // true nếu đã có topping khác (id khác) cùng tên, không phân biệt hoa thường
    boolean existsByTenIgnoreCaseAndDangBanTrueAndIdNot(String ten, Long id);
}
