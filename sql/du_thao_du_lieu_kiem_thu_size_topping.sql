/*
 DỰ THẢO DỮ LIỆU KIỂM THỬ — KHÔNG PHẢI BẢNG GIÁ CHÍNH THỨC.
 CHƯA CHẠY. Chỉ dùng sau khi duyệt và áp dụng du_thao_size_topping_schema.sql.
 Mặc định KHÔNG chèn dữ liệu. Phải tự đổi @ApDungDuLieuKiemThu thành 1 sau khi duyệt.
 Chỉ INSERT dòng thiếu. Gặp dữ liệu cùng khóa/tên nhưng khác giá thì ROLLBACK,
 không UPDATE giá, không tạo lại sản phẩm, không sửa giá san_pham.gia hoặc đơn cũ.
 Không tạo tài khoản, mật khẩu, hóa đơn hay thanh toán thử trong HighlandsDB.
*/
USE [HighlandsDB];
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @ApDungDuLieuKiemThu BIT = 0;
IF @ApDungDuLieuKiemThu <> 1
BEGIN
    PRINT N'CHƯA CHẠY: dữ liệu minh họa đang tắt. Chỉ bật sau khi được duyệt.';
    RETURN;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.san_pham_size', N'U') IS NULL
       OR OBJECT_ID(N'dbo.san_pham_topping', N'U') IS NULL
       OR COL_LENGTH(N'dbo.chi_tiet_don_hang', N'san_pham_size_id') IS NULL
       OR COL_LENGTH(N'dbo.chi_tiet_don_hang', N'size') IS NULL
        THROW 51100, N'Chưa có schema size/topping đã duyệt. Dừng.', 1;

    DECLARE @GiaSize TABLE (
        ten_san_pham NVARCHAR(100) NOT NULL,
        [size] NVARCHAR(10) NOT NULL,
        gia BIGINT NOT NULL,
        PRIMARY KEY (ten_san_pham, [size])
    );
    INSERT INTO @GiaSize (ten_san_pham, [size], gia) VALUES
        (N'Phin Sữa Đá', N'S', 29000),
        (N'Phin Sữa Đá', N'M', 35000),
        (N'Phin Sữa Đá', N'L', 41000),
        (N'Trà Sen Vàng', N'S', 39000),
        (N'Trà Sen Vàng', N'M', 45000),
        (N'Trà Sen Vàng', N'L', 51000),
        (N'Freeze Trà Xanh', N'S', 49000),
        (N'Freeze Trà Xanh', N'M', 55000),
        (N'Freeze Trà Xanh', N'L', 61000);

    -- Không đoán ID hoặc tạo sản phẩm cùng tên. Nếu tên không khớp hoặc trùng tên,
    -- cần kiểm tra ID/danh mục và chỉnh bản dự thảo trước khi thực thi.
    IF EXISTS (
        SELECT n.ten_san_pham
        FROM (SELECT DISTINCT ten_san_pham FROM @GiaSize) n
        LEFT JOIN dbo.san_pham s WITH (UPDLOCK, HOLDLOCK) ON s.ten = n.ten_san_pham
        GROUP BY n.ten_san_pham
        HAVING COUNT(s.id) <> 1
    )
        THROW 51101, N'Mỗi tên món kiểm thử phải khớp đúng một sản phẩm có sẵn; không tự tạo sản phẩm.', 1;

    DECLARE @SanPham TABLE (
        id BIGINT NOT NULL PRIMARY KEY,
        ten NVARCHAR(100) NOT NULL UNIQUE
    );
    INSERT INTO @SanPham (id, ten)
    SELECT s.id, n.ten_san_pham
    FROM (SELECT DISTINCT ten_san_pham FROM @GiaSize) n
    JOIN dbo.san_pham s WITH (UPDLOCK, HOLDLOCK) ON s.ten = n.ten_san_pham;

    IF EXISTS (
        SELECT 1 FROM @GiaSize g
        JOIN @SanPham p ON p.ten = g.ten_san_pham
        JOIN dbo.san_pham_size z WITH (UPDLOCK, HOLDLOCK)
            ON z.san_pham_id = p.id AND z.[size] = g.[size]
        WHERE z.gia <> g.gia OR z.hoat_dong <> 1
    )
        THROW 51102, N'Size có sẵn khác giá/trạng thái minh họa. Không ghi đè, rollback toàn bộ dữ liệu thử.', 1;

    INSERT INTO dbo.san_pham_size (san_pham_id, [size], gia, hoat_dong)
    SELECT p.id, g.[size], g.gia, 1
    FROM @GiaSize g JOIN @SanPham p ON p.ten = g.ten_san_pham
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.san_pham_size z WITH (UPDLOCK, HOLDLOCK)
        WHERE z.san_pham_id = p.id AND z.[size] = g.[size]
    );

    DECLARE @Topping TABLE (ten NVARCHAR(100) NOT NULL PRIMARY KEY, gia BIGINT NOT NULL);
    INSERT INTO @Topping (ten, gia) VALUES
        (N'Trân châu trắng', 10000),
        (N'Thạch cà phê', 8000),
        (N'Kem cheese', 12000);

    IF EXISTS (
        SELECT m.ten FROM @Topping m
        JOIN dbo.topping t WITH (UPDLOCK, HOLDLOCK) ON t.ten = m.ten
        GROUP BY m.ten HAVING COUNT(t.id) > 1
    )
        THROW 51103, N'Tên topping bị trùng trong dữ liệu hiện có; cần xác định ID trước.', 1;

    IF EXISTS (
        SELECT 1 FROM @Topping m
        JOIN dbo.topping t WITH (UPDLOCK, HOLDLOCK) ON t.ten = m.ten
        WHERE t.gia <> m.gia
    )
        THROW 51104, N'Topping có sẵn khác giá minh họa. Không ghi đè giá hiện có.', 1;

    INSERT INTO dbo.topping (ten, gia, hinh_anh)
    SELECT m.ten, m.gia, NULL FROM @Topping m
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.topping t WITH (UPDLOCK, HOLDLOCK) WHERE t.ten = m.ten
    );

    -- CHƯA CÓ MA TRẬN TOPPING THEO MÓN ĐƯỢC DUYỆT: để trống, không tự gán.
    -- Sau khi duyệt, thêm các dòng được chấp nhận vào biến bảng dưới đây.
    DECLARE @LienKet TABLE (
        ten_san_pham NVARCHAR(100) NOT NULL,
        ten_topping NVARCHAR(100) NOT NULL,
        PRIMARY KEY (ten_san_pham, ten_topping)
    );
    -- INSERT INTO @LienKet (ten_san_pham, ten_topping) VALUES (...các liên kết đã duyệt...);

    IF EXISTS (
        SELECT 1 FROM @LienKet l
        LEFT JOIN @SanPham p ON p.ten = l.ten_san_pham
        LEFT JOIN @Topping m ON m.ten = l.ten_topping
        WHERE p.id IS NULL OR m.ten IS NULL
    )
        THROW 51105, N'Liên kết topping phải thuộc các món/topping kiểm thử đã duyệt.', 1;

    IF EXISTS (
        SELECT 1 FROM @LienKet l
        JOIN @SanPham p ON p.ten = l.ten_san_pham
        JOIN dbo.topping t ON t.ten = l.ten_topping
        JOIN dbo.san_pham_topping pt WITH (UPDLOCK, HOLDLOCK)
            ON pt.san_pham_id = p.id AND pt.topping_id = t.id
        WHERE pt.hoat_dong <> 1
    )
        THROW 51106, N'Liên kết có sẵn đã tắt. Không tự bật lại.', 1;

    INSERT INTO dbo.san_pham_topping (san_pham_id, topping_id, hoat_dong)
    SELECT p.id, t.id, 1
    FROM @LienKet l JOIN @SanPham p ON p.ten = l.ten_san_pham
    JOIN dbo.topping t ON t.ten = l.ten_topping
    WHERE NOT EXISTS (
        SELECT 1 FROM dbo.san_pham_topping pt WITH (UPDLOCK, HOLDLOCK)
        WHERE pt.san_pham_id = p.id AND pt.topping_id = t.id
    );

    COMMIT TRANSACTION;
    PRINT N'Đã chèn những dòng kiểm thử còn thiếu; không cập nhật dữ liệu có sẵn.';
    IF NOT EXISTS (SELECT 1 FROM @LienKet)
        PRINT N'Chưa gán topping theo món: @LienKet đang trống, cần duyệt ma trận áp dụng.';
END TRY
BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
