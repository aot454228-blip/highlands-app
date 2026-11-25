-- Chạy trên HighlandsDB sau các migration nhân viên và khách hàng.
-- Các bản ghi hiện có tiếp tục xuất hiện trong thực đơn sau khi nâng cấp.
USE HighlandsDB;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.san_pham', 'dang_ban') IS NULL
    ALTER TABLE dbo.san_pham ADD dang_ban BIT NOT NULL CONSTRAINT df_san_pham_dang_ban DEFAULT (1);
IF COL_LENGTH('dbo.topping', 'dang_ban') IS NULL
    ALTER TABLE dbo.topping ADD dang_ban BIT NOT NULL CONSTRAINT df_topping_dang_ban DEFAULT (1);
IF COL_LENGTH('dbo.danh_muc', 'dang_ban') IS NULL
    ALTER TABLE dbo.danh_muc ADD dang_ban BIT NOT NULL CONSTRAINT df_danh_muc_dang_ban DEFAULT (1);
COMMIT;
GO
