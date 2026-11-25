package com.highlands.service;

import com.highlands.entity.DanhMuc;
import com.highlands.repository.DanhMucRepository;
import com.highlands.repository.SanPhamRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DanhMucService {

    private final DanhMucRepository repo;
    private final SanPhamRepository sanPhamRepo;

    public DanhMucService(DanhMucRepository repo, SanPhamRepository sanPhamRepo) {
        this.repo = repo;
        this.sanPhamRepo = sanPhamRepo;
    }

    public List<DanhMuc> layTatCa() {
        return repo.findByDangBanTrue(Sort.by("id"));
    }

    public DanhMuc layTheoId(Long id) {
        return repo.findByIdAndDangBanTrue(id).orElseThrow(() ->
                new LoiNghiepVu("Không tìm thấy danh mục id = " + id));
    }

    public DanhMuc them(DanhMuc d) {
        kiemTra(d, 0L);
        d.setId(null);
        return repo.save(d);
    }

    @Transactional
    public DanhMuc sua(Long id, DanhMuc d) {
        khoaDangBan(id);
        kiemTra(d, id);
        d.setId(id);
        return repo.save(d);
    }

    @Transactional
    public void xoa(Long id) {
        DanhMuc d = khoaDangBan(id);
        if (sanPhamRepo.existsByDanhMuc_IdAndDangBanTrue(id)) {
            throw new LoiNghiepVu(
                    "Danh mục đang có sản phẩm, hãy xóa hoặc chuyển sản phẩm trước");
        }
        d.setDangBan(false);
        repo.save(d);
    }

    private DanhMuc khoaDangBan(Long id) {
        return repo.findLockedByIdAndDangBanTrue(id).orElseThrow(() ->
                new LoiNghiepVu("Không tìm thấy danh mục id = " + id));
    }

    // idHienTai = id đang sửa (0 khi thêm mới), dùng để kiểm tra trùng tên
    private void kiemTra(DanhMuc d, Long idHienTai) {
        if (d.getTen() == null || d.getTen().isBlank()) {
            throw new LoiNghiepVu("Tên danh mục không được để trống");
        }
        d.setTen(d.getTen().trim());
        if (repo.existsByTenIgnoreCaseAndDangBanTrueAndIdNot(d.getTen(), idHienTai)) {
            throw new LoiNghiepVu("Danh mục \"" + d.getTen() + "\" đã tồn tại");
        }
    }
}
