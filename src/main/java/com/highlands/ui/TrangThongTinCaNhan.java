package com.highlands.ui;

import com.highlands.entity.KhachHang;
import com.highlands.service.KhachHangService;
import com.highlands.service.KhachHangService.HoSo;
import com.highlands.service.TienIch;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Quản lý thông tin cá nhân: xem thông tin, cập nhật thông tin, đổi mật khẩu. */
public class TrangThongTinCaNhan extends JPanel {
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final KhachHangService dich;
    private final Supplier<KhachHang> khachHang;
    private final Consumer<KhachHang> khiCapNhat;

    // --- xem + cập nhật thông tin ---
    private final JLabel giaTriTenDangNhap = new JLabel("-");
    private final JLabel giaTriNgayTao = new JLabel("-");
    private final JTextField oHoTen = new JTextField();
    private final JTextField oSdt = new JTextField();
    private final JTextField oEmail = new JTextField();
    private final JTextField oDiaChi = new JTextField();
    private final JLabel nhanThongTin = new JLabel(" ");
    private final JButton nutLuu = KieuNV.nut("Cập nhật thông tin");
    private final JButton nutHoan = KieuNV.nutPhu("Hủy thay đổi");

    // --- đổi mật khẩu ---
    private final JPasswordField oMkCu = new JPasswordField();
    private final JPasswordField oMkMoi = new JPasswordField();
    private final JPasswordField oMkLai = new JPasswordField();
    private final JLabel nhanMatKhau = new JLabel(" ");
    private final JButton nutDoiMk = KieuNV.nut("Đổi mật khẩu");

    /** khiCapNhat: báo cho form chính khi hồ sơ đổi (để cập nhật tên hiển thị ở thanh trên và menu). */
    public TrangThongTinCaNhan(KhachHangService dich, Supplier<KhachHang> khachHang, Consumer<KhachHang> khiCapNhat) {
        this.dich = dich;
        this.khachHang = khachHang;
        this.khiCapNhat = khiCapNhat;
        setLayout(new BorderLayout(0, 10));
        setBackground(KieuNV.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel tren = new JPanel(new BorderLayout());
        tren.setBackground(Color.WHITE);
        tren.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel tieuDe = new JLabel("Thông tin cá nhân");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        tren.add(tieuDe, BorderLayout.WEST);
        add(tren, BorderLayout.NORTH);

        JPanel hai = new JPanel(new GridLayout(1, 2, 14, 0));
        hai.setOpaque(false);
        hai.add(boCuc(taoTheThongTin()));
        hai.add(boCuc(taoTheMatKhau()));
        add(hai, BorderLayout.CENTER);

        dienForm(khachHang.get());
    }

    // Đặt thẻ ở sát phía trên (không bị kéo giãn theo chiều cao)
    private static JComponent boCuc(JComponent the) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(the, BorderLayout.NORTH);
        return p;
    }

    // ================= XEM + CẬP NHẬT =================
    private JComponent taoTheThongTin() {
        JPanel p = khungThe("Hồ sơ của bạn");
        p.add(hangChiXem("Tên đăng nhập", giaTriTenDangNhap));
        p.add(Box.createVerticalStrut(6));
        p.add(hangChiXem("Ngày tạo tài khoản", giaTriNgayTao));
        p.add(Box.createVerticalStrut(12));
        p.add(nhan("Họ và tên *"));
        p.add(truong(oHoTen));
        p.add(Box.createVerticalStrut(8));
        p.add(nhan("Số điện thoại *"));
        p.add(truong(oSdt));
        p.add(Box.createVerticalStrut(8));
        p.add(nhan("Email"));
        p.add(truong(oEmail));
        p.add(Box.createVerticalStrut(8));
        p.add(nhan("Địa chỉ nhận hàng"));
        p.add(truong(oDiaChi));
        p.add(Box.createVerticalStrut(8));
        nhanThongTin.setFont(KieuNV.FONT);
        nhanThongTin.setAlignmentX(LEFT_ALIGNMENT);
        p.add(nhanThongTin);
        p.add(Box.createVerticalStrut(8));

        JPanel hang = new JPanel(new GridLayout(1, 2, 10, 0));
        hang.setOpaque(false);
        hang.setAlignmentX(LEFT_ALIGNMENT);
        hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nutLuu.addActionListener(e -> capNhatThongTin());
        nutHoan.addActionListener(e -> {
            dienForm(khachHang.get());
            baoThongBao(nhanThongTin, " ", false);
        });
        hang.add(nutLuu);
        hang.add(nutHoan);
        p.add(hang);
        return p;
    }

    private void dienForm(KhachHang kh) {
        giaTriTenDangNhap.setText(TienIch.str(kh.getTenDangNhap()));
        giaTriNgayTao.setText(kh.getNgayTao() == null ? "-" : kh.getNgayTao().format(NGAY));
        oHoTen.setText(TienIch.str(kh.getHoTen()));
        oSdt.setText(TienIch.str(kh.getSoDienThoai()));
        oEmail.setText(TienIch.str(kh.getEmail()));
        oDiaChi.setText(TienIch.str(kh.getDiaChi()));
    }

    /** Tải lại hồ sơ mới nhất từ CSDL (gọi mỗi khi mở trang này). */
    public void lamMoi() {
        Long id = khachHang.get().getId();
        ChayNen.chay(() -> dich.layTheoId(id), kh -> {
            dienForm(kh);
            khiCapNhat.accept(kh);
        }, ex -> baoThongBao(nhanThongTin, ChayNen.thongDiep(ex), false));
    }

    private void capNhatThongTin() {
        Long id = khachHang.get().getId();
        HoSo hoSo = new HoSo(oHoTen.getText(), oSdt.getText(), oEmail.getText(), oDiaChi.getText());
        nutLuu.setEnabled(false);
        baoThongBao(nhanThongTin, " ", false);
        ChayNen.chay(() -> dich.capNhatThongTin(id, hoSo), kh -> {
            dienForm(kh);
            khiCapNhat.accept(kh);
            nutLuu.setEnabled(true);
            baoThongBao(nhanThongTin, "Đã cập nhật thông tin.", true);
        }, ex -> {
            nutLuu.setEnabled(true);
            baoThongBao(nhanThongTin, ChayNen.thongDiep(ex), false);
        });
    }

    // ================= ĐỔI MẬT KHẨU =================
    private JComponent taoTheMatKhau() {
        JPanel p = khungThe("Đổi mật khẩu");
        p.add(nhan("Mật khẩu hiện tại *"));
        p.add(truong(oMkCu));
        p.add(Box.createVerticalStrut(8));
        p.add(nhan("Mật khẩu mới * (ít nhất 8 ký tự)"));
        p.add(truong(oMkMoi));
        p.add(Box.createVerticalStrut(8));
        p.add(nhan("Nhập lại mật khẩu mới *"));
        p.add(truong(oMkLai));
        p.add(Box.createVerticalStrut(8));
        nhanMatKhau.setFont(KieuNV.FONT);
        nhanMatKhau.setAlignmentX(LEFT_ALIGNMENT);
        p.add(nhanMatKhau);
        p.add(Box.createVerticalStrut(8));
        nutDoiMk.setAlignmentX(LEFT_ALIGNMENT);
        nutDoiMk.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        nutDoiMk.addActionListener(e -> doiMatKhau());
        p.add(nutDoiMk);
        return p;
    }

    private void doiMatKhau() {
        Long id = khachHang.get().getId();
        char[] cu = oMkCu.getPassword();
        char[] moi = oMkMoi.getPassword();
        char[] lai = oMkLai.getPassword();
        nutDoiMk.setEnabled(false);
        baoThongBao(nhanMatKhau, " ", false);
        ChayNen.chay(() -> {
            dich.doiMatKhau(id, cu, moi, lai);
            return Boolean.TRUE;
        }, ok -> {
            xoaMang(cu, moi, lai);
            oMkCu.setText("");
            oMkMoi.setText("");
            oMkLai.setText("");
            nutDoiMk.setEnabled(true);
            baoThongBao(nhanMatKhau, "Đã đổi mật khẩu.", true);
        }, ex -> {
            xoaMang(cu, moi, lai);
            nutDoiMk.setEnabled(true);
            baoThongBao(nhanMatKhau, ChayNen.thongDiep(ex), false);
        });
    }

    private static void xoaMang(char[]... ds) {
        for (char[] a : ds) Arrays.fill(a, '\0');
    }

    // ================= TIỆN ÍCH GIAO DIỆN =================
    private static JPanel khungThe(String tieuDe) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(16, 18, 18, 18)));
        JLabel t = new JLabel(tieuDe);
        t.setFont(new Font("Segoe UI", Font.BOLD, 17));
        t.setForeground(KieuNV.DO);
        t.setAlignmentX(LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(12));
        return p;
    }

    private static JComponent hangChiXem(String ten, JLabel giaTri) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel a = new JLabel(ten);
        a.setFont(KieuNV.FONT);
        a.setForeground(KieuNV.CHU_PHU);
        giaTri.setFont(KieuNV.DAM);
        h.add(a, BorderLayout.WEST);
        h.add(giaTri, BorderLayout.EAST);
        h.setAlignmentX(LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return h;
    }

    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(KieuNV.DAM);
        l.setForeground(KieuNV.CHU);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent truong(JTextField t) {
        t.setFont(KieuNV.FONT);
        t.setAlignmentX(LEFT_ALIGNMENT);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        t.setPreferredSize(new Dimension(300, 38));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        return t;
    }

    private static void baoThongBao(JLabel nhan, String noiDung, boolean thanhCong) {
        nhan.setForeground(thanhCong ? KieuNV.XANH : KieuNV.DO_SANG);
        nhan.setText(" ".equals(noiDung) ? " " : "<html>" + KieuNV.escape(noiDung) + "</html>");
    }
}
