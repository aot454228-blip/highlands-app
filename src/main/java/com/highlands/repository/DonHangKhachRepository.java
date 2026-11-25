package com.highlands.repository;

import com.highlands.entity.DonHangKhach;
import com.highlands.entity.SanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DonHangKhachRepository extends JpaRepository<DonHangKhach, Long> {

    @Query("select k from DonHangKhach k join fetch k.donHang join fetch k.khachHang order by k.ngayDat desc, k.id desc")
    List<DonHangKhach> timDonOnline();

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update DonHangKhach k set k.trangThai = :moi where k.id = :id and k.trangThai = :cu")
    int doiTrangThai(@Param("id") Long id, @Param("cu") String cu, @Param("moi") String moi);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select k from DonHangKhach k where k.id = :id")
    Optional<DonHangKhach> khoaDon(@Param("id") Long id);

    Optional<DonHangKhach> findByMaGiaoDich(String maGiaoDich);

    // Các đơn của một khách, mới nhất trước
    @Query("select k from DonHangKhach k join fetch k.donHang where k.khachHang.id = :khachHangId "
            + "order by k.ngayDat desc, k.id desc")
    List<DonHangKhach> timTheoKhachHang(@Param("khachHangId") Long khachHangId);

    // Các sản phẩm khách đã từng mua (để cho phép đánh giá)
    @Query("select distinct c.sanPham from ChiTietDonHang c, DonHangKhach k "
            + "where k.donHang = c.donHang and k.khachHang.id = :khachHangId")
    List<SanPham> sanPhamDaMua(@Param("khachHangId") Long khachHangId);

    // Khách đã mua sản phẩm này bao nhiêu dòng đơn (0 = chưa mua)
    @Query("select count(c) from ChiTietDonHang c, DonHangKhach k "
            + "where k.donHang = c.donHang and k.khachHang.id = :khachHangId and c.sanPham.id = :sanPhamId")
    long demLanMua(@Param("khachHangId") Long khachHangId, @Param("sanPhamId") Long sanPhamId);
}
