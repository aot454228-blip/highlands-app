# highlands-app (App desktop Java Swing + SQL Server) - Quản lý + Nhân viên + Khách hàng

Bản app của highlands-be: cùng database `HighlandsDB`, cùng Entity / Repository / Service,
nhưng giao diện là cửa sổ Swing thay cho trang web. Không còn web server, không cần trình duyệt.

## Chạy
1. Cài JDK 17+. Mở thư mục này bằng VS Code (Extension Pack for Java + Spring Boot Extension Pack) hoặc IntelliJ.
2. Đặt mật khẩu SQL Server trong `application-local.properties` hoặc biến môi trường `SPRING_DATASOURCE_PASSWORD`.
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
- **Quản lý**: đăng nhập bằng tài khoản nội bộ có `tai_khoan.vai_tro = QUAN_LY`, rồi mở cửa sổ quản trị (Thống kê / Sản phẩm & Menu / Khuyến mãi). Đăng xuất hoặc đóng cửa sổ quản trị kết thúc phiên và quay về chọn vai trò.
  Trước khi chạy bản có xác thực Quản lý, chạy `sql/migration_quyen_quan_ly.sql` trên HighlandsDB.
  Migration chỉ thêm cột quyền và giữ tài khoản cũ ở `NHAN_VIEN`, không tạo tài khoản hay đổi mật khẩu.
  DBA cấp `QUAN_LY` cho tài khoản nội bộ do chủ hệ thống chỉ định; không suy ra quyền từ tên đăng nhập.
  Khách hàng ở bảng `khach_hang` không được dùng đăng nhập nội bộ.
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
- Sản phẩm và topping đã dùng trong đơn có thể gỡ khỏi thực đơn. Hóa đơn cũ vẫn giữ được dữ liệu. Khuyến mãi đã dùng vẫn được khóa ngoại bảo vệ.
- `KhoiDongNhanVien` (lớp main riêng) không còn dùng: `HighlandsApplication` là điểm khởi chạy duy nhất.


---

# Phân hệ Khách hàng (đã ghép chung vào app này)

Mở app -> hộp thoại chọn vai trò có thêm nút **Khách hàng** -> đăng nhập (hoặc **Đăng ký** tài khoản mới).
Khách hàng có tài khoản RIÊNG (bảng `khach_hang`), không dùng bảng `tai_khoan` của nhân viên/quản lý.

## Cài đặt (làm 1 lần)
1. Chạy `sql/migration_khach_hang.sql` trong SSMS (sau `migration_nhan_vien.sql`). Chỉ THÊM 4 bảng mới:
   `khach_hang`, `don_hang_khach`, `danh_gia_san_pham`, `phan_hoi`; không sửa bảng cũ.
2. (Tuỳ chọn) bật chuyển khoản: thêm vào `application-local.properties` (không đẩy lên Git):
```
highlands.chuyen-khoan.ngan-hang=Tên ngân hàng
highlands.chuyen-khoan.so-tai-khoan=Số tài khoản của cửa hàng
highlands.chuyen-khoan.chu-tai-khoan=TÊN CHỦ TÀI KHOẢN
```
   Chưa cấu hình thì khách chỉ chọn được "Tiền mặt" (nút Chuyển khoản bị khóa và có báo rõ).

## Chức năng (theo sơ đồ use case)
- **Đặt hàng**: Chọn sản phẩm (tìm kiếm, lọc danh mục, topping) -> **Quản lý giỏ hàng** (thay đổi số lượng, xóa món / xóa hết)
  -> **Thanh toán** (Tiền mặt khi nhận hàng, hoặc Chuyển khoản); có áp mã khuyến mãi; xem hóa đơn sau khi đặt.
- **Quản lý thông tin cá nhân**: xem thông tin, cập nhật (họ tên, SĐT, email, địa chỉ), đổi mật khẩu.
- **Đánh giá & phản hồi**: đánh giá sản phẩm 1-5 sao (chỉ sản phẩm đã mua; đánh giá lại = cập nhật), gửi phản hồi cho cửa hàng.
- Thêm ngoài sơ đồ: **Đơn hàng của tôi** (xem lại đơn + hóa đơn) và Đăng ký/Đăng nhập.

## Phần mới
```
entity/      KhachHang, DonHangKhach, DanhGiaSanPham, PhanHoi
repository/  KhachHang, DonHangKhach, DanhGiaSanPham, PhanHoi
service/     KhachHangService, DatHangService, DanhGiaPhanHoiService
ui/          DangNhapKhachHang, FormKhachHang, TrangDatHang, HopThoaiThanhToanKhach,
             TrangDonCuaToi, TrangThongTinCaNhan, TrangDanhGiaPhanHoi
```
Sửa nhỏ file cũ: `ChonVaiTro` (thêm nút), `HighlandsApplication` (thêm nhánh), `HopThoaiHoaDon` (thêm constructor cho hóa đơn khách).

## Lưu ý
- Đơn khách được đưa vào thống kê sau khi nhân viên xác nhận đã thu tiền.
  Đơn mới có trạng thái "Chờ xác nhận" và xuất hiện ở trang **Đơn online** của nhân viên.
- Chuyển khoản chỉ hiện thông tin tài khoản và khách tự tick "đã chuyển"; app không kết nối cổng thanh toán, không tự đối soát.
- Giá, topping, khuyến mãi được tính lại từ CSDL lúc đặt; bấm đặt nhiều lần chỉ tạo một đơn (`ma_giao_dich`).
- Mật khẩu băm PBKDF2 (dùng lại `MatKhau`); sai 5 lần liên tiếp thì khóa 60 giây.

## Đồng bộ thực đơn và đơn online

- Trên **HighlandsDB** đang sử dụng, chạy `sql/migration_dong_bo_nghiep_vu.sql` trước khi chạy bản mới. Migration chỉ thêm cột `dang_ban` và có thể chạy lại. Ba màn hình dùng cùng cấu hình kết nối cơ sở dữ liệu.
- **Làm mới thực đơn** tải lại món, danh mục, giá và topping. Trang đang mở cũng tự tải mỗi 15 giây. Giỏ giữ món, topping và số lượng. Nếu món hoặc topping đã gỡ vẫn nằm trong giỏ, bỏ dòng đó hoặc chọn lại topping trước khi thanh toán.
- **Đơn online** và **Đơn hàng của tôi** tự tải mỗi 15 giây khi đang mở. Nhân viên cập nhật tiến độ giao hàng như trước.
- Trong **Đơn online**, chọn **Xác nhận thu tiền** sau khi đã nhận đủ tiền mặt hoặc kiểm tra tiền chuyển khoản. Thao tác lưu chứng từ cho mã đơn hiện có. Doanh thu và bán chạy tính theo ngày ghi trên chứng từ. Cột tiền theo món tính theo đơn giá món lúc bán nhân số lượng, trước giảm giá và phụ thu topping. Dữ liệu mẫu cũ trong `sql/du-lieu-thong-ke-mau.sql` chưa có chứng từ nên không được tính vào doanh thu hoặc bán chạy.
- Đặt thông tin kết nối riêng trong `application-local.properties` hoặc qua biến môi trường. Tệp cấu hình riêng được Git bỏ qua.
