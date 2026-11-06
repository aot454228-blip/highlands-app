/* =====================================================================
   MIGRATION: Đăng nhập cho Quản lý - HighlandsDB (SQL Server)
   - Thêm cột vai_tro vào tai_khoan (tài khoản cũ mặc định là NHAN_VIEN).
   - Tạo tài khoản quản lý: tên đăng nhập ql001, mật khẩu tạm Nhanvien@123 (nên đổi bằng service/MatKhau.java).
   - Chạy lại nhiều lần vẫn an toàn.
   ===================================================================== */
USE HighlandsDB;
GO

IF COL_LENGTH('dbo.tai_khoan', 'vai_tro') IS NULL
    ALTER TABLE dbo.tai_khoan ADD vai_tro NVARCHAR(20) NOT NULL CONSTRAINT DF_tai_khoan_vai_tro DEFAULT N'NHAN_VIEN';
GO

IF NOT EXISTS (SELECT 1 FROM dbo.nhan_vien WHERE ma_nhan_vien = N'QL001')
    INSERT INTO dbo.nhan_vien (ma_nhan_vien, ho_ten) VALUES (N'QL001', N'Quản lý');

IF NOT EXISTS (SELECT 1 FROM dbo.tai_khoan WHERE ten_dang_nhap = N'ql001')
    INSERT INTO dbo.tai_khoan (ten_dang_nhap, mat_khau_hash, nhan_vien_id, vai_tro)
    VALUES (N'ql001',
            N'pbkdf2_sha256$600000$bHd/edHM82XncAaSL7omiA==$iep585DQLyPe8fjGJPcADD4sBqvkZoFTc6UXqIM4LQs=',
            (SELECT id FROM dbo.nhan_vien WHERE ma_nhan_vien = N'QL001'),
            N'QUAN_LY');
ELSE
    UPDATE dbo.tai_khoan SET vai_tro = N'QUAN_LY', hoat_dong = 1 WHERE ten_dang_nhap = N'ql001';
GO

-- Đảm bảo tài khoản nhân viên nv001 (nếu có) là NHAN_VIEN
UPDATE dbo.tai_khoan SET vai_tro = N'NHAN_VIEN' WHERE ten_dang_nhap = N'nv001';
GO

SELECT id, ten_dang_nhap, vai_tro, hoat_dong FROM dbo.tai_khoan;
GO
