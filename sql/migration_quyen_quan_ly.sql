/* HighlandsDB hiện chưa có phân quyền trong tai_khoan.
   Chỉ thêm cột; tài khoản cũ giữ quyền NHAN_VIEN. Không tạo tài khoản/mật khẩu,
   không tự cấp QUAN_LY theo tên đăng nhập hoặc thứ tự bản ghi.
   Chạy trước khi mở phiên bản ứng dụng có xác thực Quản lý. */
USE HighlandsDB;
GO
IF OBJECT_ID(N'dbo.tai_khoan', N'U') IS NULL
    THROW 50001, N'Chưa có bảng tai_khoan. Cần migration_nhan_vien.sql trước.', 1;
GO
IF COL_LENGTH(N'dbo.tai_khoan', N'vai_tro') IS NULL
BEGIN
    ALTER TABLE dbo.tai_khoan ADD vai_tro NVARCHAR(20) NOT NULL
        CONSTRAINT df_tai_khoan_vai_tro DEFAULT N'NHAN_VIEN' WITH VALUES;
END;
GO
-- Tách batch để SQL Server nhận cột mới trước khi biên dịch CHECK.
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints
               WHERE parent_object_id = OBJECT_ID(N'dbo.tai_khoan')
                 AND name = N'ck_tai_khoan_vai_tro')
    ALTER TABLE dbo.tai_khoan ADD CONSTRAINT ck_tai_khoan_vai_tro
        CHECK (vai_tro IN (N'NHAN_VIEN', N'QUAN_LY'));
GO
/* DBA chỉ cấp QUAN_LY cho tài khoản nội bộ đã được chủ hệ thống chỉ định.
   Không có tài khoản quản lý mặc định. Mật khẩu và dữ liệu hiện có giữ nguyên. */
