# highlands-app (App desktop Java Swing + SQL Server) - Quản lý + Nhân viên

Bản app của highlands-be: cùng database `HighlandsDB`, cùng Entity / Repository / Service,
nhưng giao diện là cửa sổ Swing thay cho trang web. Không còn web server, không cần trình duyệt.

## Chạy
1. Cài JDK 17+. Mở thư mục này bằng VS Code (Extension Pack for Java + Spring Boot Extension Pack) hoặc IntelliJ.
2. Sửa mật khẩu SQL Server trong `src/main/resources/application.properties`.
3. Chạy `HighlandsApplication.java` (nút Run), hoặc `mvn spring-boot:run`.
   - Hiện màn hình "Đang kết nối SQL Server...", vài giây sau mở cửa sổ chính.
   - SQL Server chưa chạy / sai mật khẩu: hiện hộp thoại báo lỗi sau khoảng 5 giây.
4. Đóng gói 1 file chạy được: `mvn package` -> `target/highlands-app-0.0.1.jar`,
   chạy bằng `java -jar target/highlands-app-0.0.1.jar` (hoặc nhấp đúp trên Windows).

## Giao diện (giống bản web)
- Thanh đỏ trên cùng + menu trái (Thống kê / Sản phẩm & Menu / Khuyến mãi).
- **Khuyến mãi**: tìm theo tên, bảng có ảnh, mã, mức giảm, đơn tối thiểu, ngày, trạng thái (Đang áp dụng / Sắp diễn ra / Đã kết thúc).
- **Sản phẩm & Menu**: 3 tab Sản phẩm / Danh mục / Topping, tìm + lọc theo danh mục, 10 dòng mỗi trang, nhãn "Hot" (top 5 bán chạy 30 ngày).
- **Thống kê**: chọn khoảng ngày, tổng doanh thu / chi phí / lợi nhuận, bảng theo ngày, top 5 món bán chạy.
- Thêm / sửa trong hộp thoại; lưu bị lỗi thì hộp thoại vẫn mở, giữ dữ liệu đã nhập.
- Bấm "Sửa" / "Xóa" trong cột Thao tác, hoặc nhấp đúp một dòng để sửa.
- Ngày nhập dạng `30/11/2026` (hoặc `2026-11-30`); số tiền nhập `25000` hoặc `25.000`.

## Cấu trúc
```
src/main/java/com/highlands/
  HighlandsApplication.java   khởi động Spring (không web) rồi mở cửa sổ
  entity/ repository/          giữ nguyên như bản web
  service/                     giữ nguyên quy tắc kiểm tra; báo lỗi bằng LoiNghiepVu
  ui/                          giao diện Swing
    FormChinh                  cửa sổ chính (thanh đỏ, menu, đổi trang)
    TrangKhuyenMai, TrangSanPham, TrangThongKe
    HopThoaiKhuyenMai, HopThoaiMuc (sản phẩm/danh mục/topping), HopThoai (khung chung)
    Bang, O                    bảng dữ liệu và các kiểu ô (ảnh, badge, Sửa/Xóa, thanh tỉ lệ)
    Nen                        chạy truy vấn DB ở luồng nền để cửa sổ không bị treo
    Theme, Nut, The, ONhap, ThongBao, Anh, ManHinhCho
```

## Thay đổi so với bản web (v3)
- Bỏ `spring-boot-starter-web`, các Controller và trang HTML; `spring.main.web-application-type=none`.
- Service ném `LoiNghiepVu` thay cho `ResponseStatusException` (lớp của web).
- Kiểm tra trùng mã khuyến mãi dùng `existsByMaIgnoreCaseAndIdNot` (DB tự đếm, không tải cả bảng).
- Tìm kiếm khuyến mãi trả về theo thứ tự id; gõ nhanh thì chỉ hiện kết quả của lần tìm mới nhất.
- Kết nối SQL Server có `loginTimeout=5`, `hikari.connection-timeout=5000`.

## Ảnh
DB lưu dạng `/images/xxx.jpg`. App tìm lần lượt: link `http(s)://...` -> đường dẫn đầy đủ ->
thư mục `images/` cạnh nơi chạy app -> `src/main/resources/static/images/` -> trong file jar.
Không tìm thấy thì hiện "Chưa có ảnh".

## Lưu ý DB cũ
Nếu bảng `khuyen_mai` trước đây do Hibernate tự tạo (cột `ten` là VARCHAR, mất dấu tiếng Việt), chạy một lần trong SSMS:
```sql
ALTER TABLE khuyen_mai ALTER COLUMN ten NVARCHAR(100) NOT NULL;
```


---

# Phân hệ Nhân viên (đã ghép chung vào app này)

Một app, hai phân hệ dùng chung database `HighlandsDB`. Mở app -> hộp thoại **chọn vai trò**:
- **Quản lý**: vào thẳng cửa sổ quản trị như cũ (Thống kê / Sản phẩm & Menu / Khuyến mãi).
- **Nhân viên**: màn hình đăng nhập -> **Bán hàng** (thực đơn, topping, giỏ hàng, mã khuyến mãi, thanh toán tiền mặt, in hóa đơn)
  và **Lịch sử đơn hàng** (chỉ xem).

## Cài đặt phần nhân viên (làm 1 lần)
1. Chạy `sql/migration_nhan_vien.sql` trong SSMS. File chỉ THÊM 4 bảng mới (`nhan_vien`, `tai_khoan`,
   `thanh_toan_don_hang`, `chi_tiet_don_hang_topping`), không sửa bảng cũ.
2. Tạo mật khẩu băm: chạy hàm `main` của `com.highlands.service.MatKhau`, nhập mật khẩu (>= 8 ký tự), chép chuỗi hash.
3. Tạo nhân viên + tài khoản đầu tiên (mẫu ở cuối file migration). Không có tài khoản mặc định.

## Phần mới (không sửa logic của phân hệ quản lý)
```
entity/      NhanVien, TaiKhoan, ThanhToanDonHang, ChiTietDonHangTopping
repository/  NhanVien, TaiKhoan, ThanhToanDonHang, ChiTietDonHangTopping (+ 1 hàm thêm vào KhuyenMai / ChiTietDonHang)
service/     DangNhapService, BanHangService, ThanhToanService, MatKhau, DongGio, TienIch
ui/          DangNhapNhanVien, FormNhanVien, TrangBanHang, TrangThanhToan, HopThoaiTopping,
             TrangLichSuDonHang, HopThoaiHoaDon, KieuNV, LogoTron, ChayNen, ChonVaiTro
```
- Đơn bán ra ghi vào `don_hang` / `chi_tiet_don_hang` đúng quy ước cũ (`tong_tien` = đã trừ khuyến mãi,
  `don_gia` = giá món chưa gồm topping) nên trang **Thống kê** của quản lý tự cộng doanh thu và top bán chạy của nhân viên.
- Giá, topping, khuyến mãi luôn được tính lại từ CSDL lúc thanh toán; không tin số tiền từ giao diện.
- Mỗi giỏ có `ma_giao_dich`: bấm thanh toán nhiều lần chỉ tạo một đơn.
- Mật khẩu băm PBKDF2-HMAC-SHA256 (600.000 vòng, salt riêng); sai 5 lần liên tiếp thì khóa tên đó 60 giây.
- Khuyến mãi / topping đã dùng trong đơn thì quản lý không xóa được (khóa ngoại), app báo lỗi thay vì mất dữ liệu.
- `KhoiDongNhanVien` (lớp main riêng) không còn dùng: `HighlandsApplication` là điểm khởi chạy duy nhất.
