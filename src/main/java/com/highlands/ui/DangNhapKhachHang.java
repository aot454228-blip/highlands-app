package com.highlands.ui;

import com.highlands.service.KhachHangService;
import com.highlands.service.KhachHangService.HoSo;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** Đăng nhập / đăng ký cho Khách hàng. Đăng ký xong thì quay về đăng nhập với tên vừa tạo. */
public class DangNhapKhachHang extends JFrame {
    private static final String THE_DANG_NHAP = "dangnhap";
    private static final String THE_DANG_KY = "dangky";

    private final ConfigurableApplicationContext ctx;
    private final KhachHangService khachHangService;
    private final CardLayout theChuyen = new CardLayout();
    private final JPanel khuPhai = new JPanel(theChuyen);

    // --- đăng nhập ---
    private final JTextField oTen = new JTextField();
    private final JPasswordField oMatKhau = new JPasswordField();
    private final JLabel nhanThongBao = new JLabel(" ");
    private final JButton nutDangNhap = KieuNV.nut("Đăng nhập");

    // --- đăng ký ---
    private final JTextField dkHoTen = new JTextField();
    private final JTextField dkSdt = new JTextField();
    private final JTextField dkEmail = new JTextField();
    private final JTextField dkDiaChi = new JTextField();
    private final JTextField dkTen = new JTextField();
    private final JPasswordField dkMatKhau = new JPasswordField();
    private final JPasswordField dkNhapLai = new JPasswordField();
    private final JLabel dkLoi = new JLabel(" ");
    private final JButton nutDangKy = KieuNV.nut("Đăng ký");
    private boolean dangXuLy;

    public DangNhapKhachHang(ConfigurableApplicationContext ctx) {
        super("Highlands Coffee – Khách hàng");
        this.ctx = ctx;
        this.khachHangService = ctx.getBean(KhachHangService.class);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { thoat(); }
        });
        setSize(900, 740);
        setResizable(false);
        setLocationRelativeTo(null);

        JPanel goc = new JPanel(new BorderLayout());
        goc.add(taoCotTrai(), BorderLayout.WEST);
        khuPhai.setBackground(Color.WHITE);
        khuPhai.add(taoDangNhap(), THE_DANG_NHAP);
        khuPhai.add(taoDangKy(), THE_DANG_KY);
        goc.add(khuPhai, BorderLayout.CENTER);
        GiaoDienBanHang.hoanThien(goc);
        setContentPane(goc);
        getRootPane().setDefaultButton(nutDangNhap);
    }

    // ================= CỘT TRÁI =================
    private JComponent taoCotTrai() {
        JPanel trai = new JPanel();
        trai.setBackground(KieuNV.DO);
        trai.setPreferredSize(new Dimension(340, 0));
        trai.setLayout(new BoxLayout(trai, BoxLayout.Y_AXIS));
        trai.add(Box.createVerticalGlue());
        LogoTron logo = new LogoTron(120);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        trai.add(logo);
        trai.add(Box.createVerticalStrut(18));
        trai.add(canGiua(new JLabel("HIGHLANDS COFFEE"), new Font("Segoe UI", Font.BOLD, 24), Color.WHITE));
        trai.add(Box.createVerticalStrut(6));
        trai.add(canGiua(new JLabel("Dành cho KHÁCH HÀNG"), KieuNV.FONT, Theme.SIDEBAR_TEXT));
        trai.add(Box.createVerticalGlue());
        return trai;
    }

    // ================= ĐĂNG NHẬP =================
    private JComponent taoDangNhap() {
        JPanel p = khungPhai();
        p.add(tieuDe("Đăng nhập"));
        p.add(Box.createVerticalStrut(24));
        p.add(nhan("Tên đăng nhập"));
        p.add(Box.createVerticalStrut(4));
        p.add(truong(oTen));
        p.add(Box.createVerticalStrut(14));
        p.add(nhan("Mật khẩu"));
        p.add(Box.createVerticalStrut(4));
        p.add(truong(oMatKhau));
        p.add(Box.createVerticalStrut(8));
        nhanThongBao.setFont(KieuNV.FONT);
        nhanThongBao.setAlignmentX(LEFT_ALIGNMENT);
        p.add(nhanThongBao);
        p.add(Box.createVerticalStrut(14));

        JPanel hang = new JPanel(new GridLayout(1, 2, 12, 0));
        hang.setOpaque(false);
        hang.setAlignmentX(LEFT_ALIGNMENT);
        hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton nutThoat = KieuNV.nutPhu("Thoát");
        nutThoat.addActionListener(e -> thoat());
        nutDangNhap.addActionListener(e -> dangNhap());
        hang.add(nutDangNhap);
        hang.add(nutThoat);
        p.add(hang);
        p.add(Box.createVerticalStrut(18));

        JButton sangDangKy = lienKet("Chưa có tài khoản? Đăng ký ngay");
        sangDangKy.addActionListener(e -> {
            if (dangXuLy) return;
            dkLoi.setText(" ");
            theChuyen.show(khuPhai, THE_DANG_KY);
            getRootPane().setDefaultButton(nutDangKy);
            dkHoTen.requestFocusInWindow();
        });
        p.add(sangDangKy);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void dangNhap() {
        if (dangXuLy) return;
        String ten = oTen.getText();
        char[] mk = oMatKhau.getPassword();
        baoLoi(nhanThongBao, " ");
        datDangXuLy(true);
        ChayNen.chay(() -> {
            try { return khachHangService.dangNhap(ten, mk); }
            finally { Arrays.fill(mk, '\0'); }
        }, kh -> {
            datDangXuLy(false);
            oMatKhau.setText("");
            dispose();
            new FormKhachHang(ctx, kh).setVisible(true);
        }, ex -> {
            Arrays.fill(mk, '\0');
            oMatKhau.setText("");
            baoLoi(nhanThongBao, ChayNen.thongDiep(ex));
            datDangXuLy(false);
            oMatKhau.requestFocusInWindow();
        });
    }

    // ================= ĐĂNG KÝ =================
    private JComponent taoDangKy() {
        JPanel p = khungPhai();
        p.setBorder(BorderFactory.createEmptyBorder(28, 50, 24, 50));
        p.add(tieuDe("Đăng ký tài khoản"));
        p.add(Box.createVerticalStrut(14));
        p.add(nhan("Họ và tên *"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkHoTen));
        p.add(Box.createVerticalStrut(9));
        p.add(nhan("Số điện thoại *"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkSdt));
        p.add(Box.createVerticalStrut(9));
        p.add(nhan("Email (không bắt buộc)"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkEmail));
        p.add(Box.createVerticalStrut(9));
        p.add(nhan("Địa chỉ nhận hàng (không bắt buộc)"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkDiaChi));
        p.add(Box.createVerticalStrut(9));
        p.add(nhan("Tên đăng nhập * (4-30 ký tự: chữ không dấu, số, . hoặc _)"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkTen));
        p.add(Box.createVerticalStrut(9));
        p.add(nhan("Mật khẩu * (ít nhất 8 ký tự)"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkMatKhau));
        p.add(Box.createVerticalStrut(9));
        p.add(nhan("Nhập lại mật khẩu *"));
        p.add(Box.createVerticalStrut(3));
        p.add(truong(dkNhapLai));
        p.add(Box.createVerticalStrut(6));
        dkLoi.setFont(KieuNV.FONT);
        dkLoi.setAlignmentX(LEFT_ALIGNMENT);
        p.add(dkLoi);
        p.add(Box.createVerticalStrut(8));

        JPanel hang = new JPanel(new GridLayout(1, 2, 12, 0));
        hang.setOpaque(false);
        hang.setAlignmentX(LEFT_ALIGNMENT);
        hang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JButton nutQuayLai = KieuNV.nutPhu("Quay lại đăng nhập");
        nutQuayLai.addActionListener(e -> veDangNhap());
        nutDangKy.addActionListener(e -> dangKy());
        hang.add(nutDangKy);
        hang.add(nutQuayLai);
        p.add(hang);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private void dangKy() {
        if (dangXuLy) return;
        HoSo hoSo = new HoSo(dkHoTen.getText(), dkSdt.getText(), dkEmail.getText(), dkDiaChi.getText());
        String ten = dkTen.getText();
        char[] mk = dkMatKhau.getPassword();
        char[] lai = dkNhapLai.getPassword();
        baoLoi(dkLoi, " ");
        datDangXuLy(true);
        ChayNen.chay(() -> {
            try { return khachHangService.dangKy(hoSo, ten, mk, lai); }
            finally {
                Arrays.fill(mk, '\0');
                Arrays.fill(lai, '\0');
            }
        }, kh -> {
            datDangXuLy(false);
            xoaFormDangKy();
            oTen.setText(kh.getTenDangNhap());
            oMatKhau.setText("");
            veDangNhap();
            nhanThongBao.setForeground(KieuNV.XANH);
            nhanThongBao.setText("<html>Đăng ký thành công! Hãy đăng nhập bằng tài khoản vừa tạo.</html>");
            oMatKhau.requestFocusInWindow();
        }, ex -> {
            Arrays.fill(mk, '\0');
            Arrays.fill(lai, '\0');
            baoLoi(dkLoi, ex instanceof com.highlands.service.LoiNghiepVu
                    ? ChayNen.thongDiep(ex) : "Không thể đăng ký lúc này. Vui lòng thử lại hoặc liên hệ cửa hàng.");
            datDangXuLy(false);
        });
    }

    private void datDangXuLy(boolean dangXuLy) {
        this.dangXuLy = dangXuLy;
        nutDangNhap.setEnabled(!dangXuLy);
        nutDangKy.setEnabled(!dangXuLy);
    }

    private void xoaFormDangKy() {
        for (JTextField t : new JTextField[]{dkHoTen, dkSdt, dkEmail, dkDiaChi, dkTen, dkMatKhau, dkNhapLai}) t.setText("");
        baoLoi(dkLoi, " ");
    }

    private void veDangNhap() {
        if (dangXuLy) return;
        theChuyen.show(khuPhai, THE_DANG_NHAP);
        getRootPane().setDefaultButton(nutDangNhap);
    }

    private void thoat() {
        if (dangXuLy) return;
        ctx.close();
        System.exit(0);
    }

    // ================= TIỆN ÍCH GIAO DIỆN =================
    private static JPanel khungPhai() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        return p;
    }

    private static JLabel tieuDe(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(new Font("Segoe UI", Font.BOLD, 26));
        l.setForeground(KieuNV.DO);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel canGiua(JLabel l, Font f, Color c) {
        l.setFont(f); l.setForeground(c); l.setAlignmentX(CENTER_ALIGNMENT);
        return l;
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

    private static JButton lienKet(String chu) {
        JButton b = new JButton("<html><u>" + KieuNV.escape(chu) + "</u></html>");
        b.setFont(KieuNV.DAM);
        b.setForeground(KieuNV.DO_SANG);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    private static void baoLoi(JLabel nhan, String noiDung) {
        nhan.setForeground(KieuNV.DO_SANG);
        nhan.setText(" ".equals(noiDung) ? " " : "<html>" + KieuNV.escape(noiDung) + "</html>");
    }
}
