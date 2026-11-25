package com.highlands.repository;

import com.highlands.entity.DanhGiaSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DanhGiaSanPhamRepository extends JpaRepository<DanhGiaSanPham, Long> {

    @Query("select d from DanhGiaSanPham d join fetch d.sanPham where d.khachHang.id = :khachHangId "
            + "order by d.ngayCapNhat desc, d.id desc")
    List<DanhGiaSanPham> timTheoKhachHang(@Param("khachHangId") Long khachHangId);

    Optional<DanhGiaSanPham> findByKhachHang_IdAndSanPham_Id(Long khachHangId, Long sanPhamId);
}
