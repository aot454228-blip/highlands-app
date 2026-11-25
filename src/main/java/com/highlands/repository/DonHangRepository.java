package com.highlands.repository;

import com.highlands.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DonHangRepository extends JpaRepository<DonHang, Long> {

    // Mỗi chứng từ ứng với một đơn; doanh thu ghi nhận theo ngày thu tiền.
    @Query("select cast(t.ngayThanhToan as LocalDate), sum(t.tongThanhToan) from ThanhToanDonHang t "
            + "where t.ngayThanhToan >= :tu and t.ngayThanhToan < :den "
            + "group by cast(t.ngayThanhToan as LocalDate) order by 1")
    List<Object[]> doanhThuTheoNgay(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);
}
