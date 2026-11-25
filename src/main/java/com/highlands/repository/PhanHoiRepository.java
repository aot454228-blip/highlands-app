package com.highlands.repository;

import com.highlands.entity.PhanHoi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhanHoiRepository extends JpaRepository<PhanHoi, Long> {

    List<PhanHoi> findByKhachHang_IdOrderByNgayGuiDescIdDesc(Long khachHangId);
}
