/* =====================================================================
   MIGRATION: Phân hệ Khách hàng – HighlandsDB (SQL Server)
   - Chỉ THÊM bảng mới, không sửa/xóa bảng cũ, không mất dữ liệu cũ.
   - Khách hàng có tài khoản RIÊNG (bảng khach_hang), không dùng bảng tai_khoan của nhân viên/quản lý.
   - Đơn khách đặt vẫn ghi vào don_hang / chi_tiet_don_hang / chi_tiet_don_hang_topping đúng quy ước cũ
     (tong_tien = đã trừ khuyến mãi, don_gia = giá món chưa gồm topping) nên trang Thống kê tự cộng doanh thu.
   - GIẢ ĐỊNH: id các bảng cũ kiểu BIGINT (giống migration_nhan_vien.sql).
   - Chạy sau migration_nhan_vien.sql (cần bảng khuyen_mai, don_hang, san_pham).
   ===================================================================== */
USE HighlandsDB;
GO

IF OBJECT_ID('dbo.khach_hang') IS NULL
CREATE TABLE dbo.khach_hang (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    ho_ten         NVARCHAR(100) NOT NULL,
    so_dien_thoai  NVARCHAR(20)  NOT NULL,
    email          NVARCHAR(100) NULL,
    dia_chi        NVARCHAR(255) NULL,
    ten_dang_nhap  NVARCHAR(50)  NOT NULL,
    mat_khau_hash  NVARCHAR(255) NOT NULL,          -- PBKDF2-HMAC-SHA256, KHÔNG lưu mật khẩu thuần
    hoat_dong      BIT           NOT NULL DEFAULT 1,
    ngay_tao       DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_khach_hang_ten_dang_nhap UNIQUE (ten_dang_nhap)
);
GO

/* Thông tin khách của một đơn (mở rộng 1-1 với don_hang, không đụng vào don_hang) */
IF OBJECT_ID('dbo.don_hang_khach') IS NULL
CREATE TABLE dbo.don_hang_khach (
    id               BIGINT IDENTITY(1,1) PRIMARY KEY,
    don_hang_id      BIGINT        NOT NULL UNIQUE REFERENCES dbo.don_hang(id),
    khach_hang_id    BIGINT        NOT NULL REFERENCES dbo.khach_hang(id),
    khuyen_mai_id    BIGINT        NULL     REFERENCES dbo.khuyen_mai(id),
    ma_khuyen_mai    NVARCHAR(50)  NULL,
    tien_hang        DECIMAL(18,2) NOT NULL,
    giam_gia         DECIMAL(18,2) NOT NULL DEFAULT 0,
    tong_thanh_toan  DECIMAL(18,2) NOT NULL,
    phuong_thuc      NVARCHAR(30)  NOT NULL,        -- 'Tiền mặt' (trả khi nhận hàng) | 'Chuyển khoản'
    trang_thai       NVARCHAR(30)  NOT NULL DEFAULT N'Chờ xác nhận',
    so_dien_thoai    NVARCHAR(20)  NOT NULL,        -- bản chụp lúc đặt, sửa hồ sơ sau này không đổi đơn cũ
    dia_chi_giao     NVARCHAR(255) NOT NULL,
    ghi_chu          NVARCHAR(255) NULL,
    ngay_dat         DATETIME2     NOT NULL,
    ma_giao_dich     NVARCHAR(64)  NOT NULL UNIQUE  -- khóa chống tạo trùng đơn khi bấm đặt hàng nhiều lần
);
GO
CREATE INDEX ix_don_hang_khach_khach_hang ON dbo.don_hang_khach (khach_hang_id, ngay_dat DESC);
GO

/* Mỗi khách đánh giá 1 sản phẩm 1 lần (đánh giá lại = cập nhật) */
IF OBJECT_ID('dbo.danh_gia_san_pham') IS NULL
CREATE TABLE dbo.danh_gia_san_pham (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    khach_hang_id  BIGINT        NOT NULL REFERENCES dbo.khach_hang(id),
    san_pham_id    BIGINT        NOT NULL REFERENCES dbo.san_pham(id),
    so_sao         INT           NOT NULL CHECK (so_sao BETWEEN 1 AND 5),
    noi_dung       NVARCHAR(1000) NULL,
    ngay_tao       DATETIME2     NOT NULL,
    ngay_cap_nhat  DATETIME2     NOT NULL,
    CONSTRAINT uq_danh_gia_khach_san_pham UNIQUE (khach_hang_id, san_pham_id)
);
GO

IF OBJECT_ID('dbo.phan_hoi') IS NULL
CREATE TABLE dbo.phan_hoi (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,
    khach_hang_id  BIGINT         NOT NULL REFERENCES dbo.khach_hang(id),
    tieu_de        NVARCHAR(150)  NOT NULL,
    noi_dung       NVARCHAR(2000) NOT NULL,
    ngay_gui       DATETIME2      NOT NULL,
    trang_thai     NVARCHAR(30)   NOT NULL DEFAULT N'Chưa xử lý'
);
GO
