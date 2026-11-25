package com.highlands.repository;

import com.highlands.entity.ChiTietDonHang;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Long> {

    // Mỗi dòng: [id sản phẩm, tên, tổng số lượng bán, tổng tiền], bán nhiều nhất xếp trước
    @Query("select s.id, s.ten, sum(cast(c.soLuong as Long)), sum(c.soLuong * c.donGia) "
            + "from ChiTietDonHang c join c.sanPham s join c.donHang d "
            + "where exists (select t.id from ThanhToanDonHang t where t.donHang = d "
            + "and t.ngayThanhToan >= :tu and t.ngayThanhToan < :den) "
            + "group by s.id, s.ten order by sum(cast(c.soLuong as Long)) desc")
    List<Object[]> banChay(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den, Pageable pageable);

    // Phân hệ nhân viên: các dòng của một đơn (để dựng hóa đơn)
    List<ChiTietDonHang> findByDonHang_IdOrderByIdAsc(Long donHangId);
}
