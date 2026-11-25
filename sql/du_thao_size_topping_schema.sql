/*
 DỰ THẢO ĐỂ DUYỆT — CHƯA CHẠY TRÊN HIGHLANDSDB.
 Chỉ thêm bảng/cột/khóa. Không cập nhật hoặc xóa dữ liệu cũ, không chèn dữ liệu mẫu.
 Giữ san_pham.gia cho món không size; don_gia của dòng đơn là giá món/size lúc bán.
 Topping vẫn dùng bảng topping và các cột snapshot hiện có.
*/
USE [HighlandsDB];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
DECLARE @ApDungSchema BIT = 0;
IF @ApDungSchema <> 1
BEGIN
    PRINT N'CHƯA CHẠY: schema đang tắt. Chỉ bật sau khi duyệt.';
    RETURN;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.san_pham', N'U') IS NULL
       OR OBJECT_ID(N'dbo.topping', N'U') IS NULL
       OR OBJECT_ID(N'dbo.chi_tiet_don_hang', N'U') IS NULL
       OR OBJECT_ID(N'dbo.chi_tiet_don_hang_topping', N'U') IS NULL
        THROW 51000, N'Thiếu bảng nền; dừng, không tạo bảng thay thế.', 1;

    IF EXISTS (
        SELECT 1 FROM (VALUES
            (N'dbo.san_pham', N'id'), (N'dbo.topping', N'id'),
            (N'dbo.chi_tiet_don_hang', N'san_pham_id'),
            (N'dbo.chi_tiet_don_hang', N'don_gia')
        ) AS e(bang, cot)
        LEFT JOIN sys.columns c ON c.object_id = OBJECT_ID(e.bang) AND c.name = e.cot
        WHERE c.column_id IS NULL OR TYPE_NAME(c.user_type_id) <> N'bigint'
    )
        THROW 51001, N'Kiểu khóa/đơn giá khác BIGINT; cần duyệt lại dự thảo.', 1;

    IF OBJECT_ID(N'dbo.san_pham_size') IS NOT NULL
       AND OBJECT_ID(N'dbo.san_pham_size', N'U') IS NULL
        THROW 51002, N'Tên san_pham_size đã được đối tượng khác sử dụng.', 1;

    IF OBJECT_ID(N'dbo.san_pham_size', N'U') IS NULL
        EXEC(N'CREATE TABLE dbo.san_pham_size (
            id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_san_pham_size PRIMARY KEY,
            san_pham_id BIGINT NOT NULL,
            [size] NVARCHAR(10) NOT NULL,
            gia BIGINT NOT NULL,
            hoat_dong BIT NOT NULL CONSTRAINT df_san_pham_size_hoat_dong DEFAULT (1)
        );');

    IF OBJECT_ID(N'dbo.san_pham_topping') IS NOT NULL
       AND OBJECT_ID(N'dbo.san_pham_topping', N'U') IS NULL
        THROW 51003, N'Tên san_pham_topping đã được đối tượng khác sử dụng.', 1;

    IF OBJECT_ID(N'dbo.san_pham_topping', N'U') IS NULL
        EXEC(N'CREATE TABLE dbo.san_pham_topping (
            san_pham_id BIGINT NOT NULL,
            topping_id BIGINT NOT NULL,
            hoat_dong BIT NOT NULL CONSTRAINT df_san_pham_topping_hoat_dong DEFAULT (1),
            CONSTRAINT pk_san_pham_topping PRIMARY KEY (san_pham_id, topping_id)
        );');

    -- Bảng có sẵn nhưng khác cấu trúc: không tự sửa/chuyển đổi.
    IF EXISTS (
        SELECT 1 FROM (VALUES
            (N'dbo.san_pham_size', N'id', N'bigint'),
            (N'dbo.san_pham_size', N'san_pham_id', N'bigint'),
            (N'dbo.san_pham_size', N'size', N'nvarchar'),
            (N'dbo.san_pham_size', N'gia', N'bigint'),
            (N'dbo.san_pham_size', N'hoat_dong', N'bit'),
            (N'dbo.san_pham_topping', N'san_pham_id', N'bigint'),
            (N'dbo.san_pham_topping', N'topping_id', N'bigint'),
            (N'dbo.san_pham_topping', N'hoat_dong', N'bit')
        ) AS e(bang, cot, kieu)
        LEFT JOIN sys.columns c ON c.object_id = OBJECT_ID(e.bang) AND c.name = e.cot
        WHERE c.column_id IS NULL OR TYPE_NAME(c.user_type_id) <> e.kieu OR c.is_nullable <> 0
    )
        THROW 51004, N'Bảng size/topping có cấu trúc khác dự thảo.', 1;

    IF COLUMNPROPERTY(OBJECT_ID(N'dbo.san_pham_size'), N'id', 'IsIdentity') <> 1
       OR COL_LENGTH(N'dbo.san_pham_size', N'size') <> 20
        THROW 51005, N'ID hoặc độ dài size không khớp dự thảo.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_size') AND type = 'PK')
        EXEC(N'ALTER TABLE dbo.san_pham_size ADD CONSTRAINT pk_san_pham_size PRIMARY KEY (id);');
    IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_topping') AND type = 'PK')
        EXEC(N'ALTER TABLE dbo.san_pham_topping ADD CONSTRAINT pk_san_pham_topping PRIMARY KEY (san_pham_id, topping_id);');

    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.san_pham_size') AND name = N'uq_san_pham_size_san_pham_size')
        EXEC(N'ALTER TABLE dbo.san_pham_size ADD CONSTRAINT uq_san_pham_size_san_pham_size UNIQUE (san_pham_id, [size]);');
    -- Khóa đích cho FK ghép đảm bảo size của dòng đơn thuộc đúng sản phẩm.
    IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id = OBJECT_ID(N'dbo.san_pham_size') AND name = N'uq_san_pham_size_id_san_pham')
        EXEC(N'ALTER TABLE dbo.san_pham_size ADD CONSTRAINT uq_san_pham_size_id_san_pham UNIQUE (id, san_pham_id);');

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_size') AND name = N'ck_san_pham_size_ma')
        EXEC(N'ALTER TABLE dbo.san_pham_size WITH CHECK ADD CONSTRAINT ck_san_pham_size_ma CHECK ([size] IN (N''S'', N''M'', N''L''));');
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_size') AND name = N'ck_san_pham_size_gia')
        EXEC(N'ALTER TABLE dbo.san_pham_size WITH CHECK ADD CONSTRAINT ck_san_pham_size_gia CHECK (gia >= 0);');

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_size') AND name = N'fk_san_pham_size_san_pham')
        EXEC(N'ALTER TABLE dbo.san_pham_size WITH CHECK ADD CONSTRAINT fk_san_pham_size_san_pham FOREIGN KEY (san_pham_id) REFERENCES dbo.san_pham(id);');
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_topping') AND name = N'fk_san_pham_topping_san_pham')
        EXEC(N'ALTER TABLE dbo.san_pham_topping WITH CHECK ADD CONSTRAINT fk_san_pham_topping_san_pham FOREIGN KEY (san_pham_id) REFERENCES dbo.san_pham(id);');
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.san_pham_topping') AND name = N'fk_san_pham_topping_topping')
        EXEC(N'ALTER TABLE dbo.san_pham_topping WITH CHECK ADD CONSTRAINT fk_san_pham_topping_topping FOREIGN KEY (topping_id) REFERENCES dbo.topping(id);');

    -- NULL cho dòng cũ, không suy đoán hoặc gán size ngược cho đơn đã có.
    -- Dùng EXEC để tránh lỗi SQL Server biên dịch tham chiếu cột trước khi được thêm.
    IF COL_LENGTH(N'dbo.chi_tiet_don_hang', N'san_pham_size_id') IS NULL
        EXEC(N'ALTER TABLE dbo.chi_tiet_don_hang ADD san_pham_size_id BIGINT NULL;');
    IF COL_LENGTH(N'dbo.chi_tiet_don_hang', N'size') IS NULL
        EXEC(N'ALTER TABLE dbo.chi_tiet_don_hang ADD [size] NVARCHAR(10) NULL;');

    IF EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID(N'dbo.chi_tiet_don_hang') AND name = N'san_pham_size_id'
                 AND (TYPE_NAME(user_type_id) <> N'bigint' OR is_nullable <> 1))
       OR EXISTS (SELECT 1 FROM sys.columns
                  WHERE object_id = OBJECT_ID(N'dbo.chi_tiet_don_hang') AND name = N'size'
                    AND (TYPE_NAME(user_type_id) <> N'nvarchar' OR max_length <> 20 OR is_nullable <> 1))
        THROW 51006, N'Cột size có kiểu/độ dài/nullability khác dự thảo.', 1;

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.chi_tiet_don_hang') AND name = N'fk_ctdh_size_san_pham')
        EXEC(N'ALTER TABLE dbo.chi_tiet_don_hang WITH CHECK ADD CONSTRAINT fk_ctdh_size_san_pham
               FOREIGN KEY (san_pham_size_id, san_pham_id) REFERENCES dbo.san_pham_size(id, san_pham_id);');

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.chi_tiet_don_hang') AND name = N'ck_ctdh_size_snapshot')
        EXEC(N'ALTER TABLE dbo.chi_tiet_don_hang WITH CHECK ADD CONSTRAINT ck_ctdh_size_snapshot CHECK (
            (san_pham_size_id IS NULL AND [size] IS NULL)
            OR (san_pham_size_id IS NOT NULL AND [size] IS NOT NULL AND [size] IN (N''S'', N''M'', N''L''))
        );');

    -- Không có ON DELETE CASCADE. Size đã dùng chỉ ngừng bán (hoat_dong = 0).
    -- Không sửa don_gia / tên, giá topping snapshot của đơn cũ.
    -- Không thay đổi khóa UNIQUE ma_giao_dich/don_hang_id của hai bảng ghi đơn/thanh toán.

    COMMIT TRANSACTION;
    PRINT N'Đã bổ sung cấu trúc. Không cập nhật dữ liệu đơn hàng hoặc bảng giá cũ.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
