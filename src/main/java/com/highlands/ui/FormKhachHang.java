package com.highlands.ui;

import com.highlands.entity.KhachHang;
import com.highlands.service.BanHangService;
import com.highlands.service.DanhGiaPhanHoiService;
import com.highlands.service.DatHangService;
import com.highlands.service.KhachHangService;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;

/** Giao diện chính của Khách hàng: header đỏ + sidebar + CardLayout (Đặt hàng / Đơn hàng / Thông tin / Đánh giá). */
public class FormKhachHang extends JFrame {
    private static final String CARD_DAT_HANG = "dathang";
    private static final String CARD_DON = "don";
    private static final String CARD_THONG_TIN = "thongtin";
    private static final String CARD_DANH_GIA = "danhgia";

    private final ConfigurableApplicationContext ctx;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel noiDung = new JPanel(cardLayout);
    private final MucMenu mucDatHang = new MucMenu("Đặt hàng");
    private final MucMenu mucDon = new MucMenu("Đơn hàng của tôi");
    private final MucMenu mucThongTin = new MucMenu("Thông tin cá nhân");
    private final MucMenu mucDanhGia = new MucMenu("Đánh giá & phản hồi");
    private final JLabel tieuDeTen = new JLabel();
    private final JLabel tenSidebar = new JLabel();
    private final JLabel taiKhoanSidebar = new JLabel();

    private KhachHang khachHang;
    private final TrangDonCuaToi trangDon;
    private final TrangThongTinCaNhan trangThongTin;
    private final TrangDanhGiaPhanHoi trangDanhGia;

    public FormKhachHang(ConfigurableApplicationContext ctx, KhachHang khachHang) {
        super("Highlands Coffee – Khách hàng");
        this.ctx = ctx;
        this.khachHang = khachHang;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { thoatUngDung(); }
        });
        setSize(1360, 800);
        setMinimumSize(new Dimension(1180, 720));
        setLocationRelativeTo(null);

        // Các trang luôn lấy khách hàng hiện tại qua hàm này (hồ sơ có thể được cập nhật khi đang dùng)
        java.util.function.Supplier<KhachHang> hienTai = () -> this.khachHang;
        trangDon = new TrangDonCuaToi(ctx.getBean(DatHangService.class), hienTai);
        trangThongTin = new TrangThongTinCaNhan(ctx.getBean(KhachHangService.class), hienTai, this::capNhatKhach);
        trangDanhGia = new TrangDanhGiaPhanHoi(ctx.getBean(DanhGiaPhanHoiService.class), hienTai);

        JPanel goc = new JPanel(new BorderLayout());
        goc.add(taoTieuDe(), BorderLayout.NORTH);
        goc.add(taoSidebar(), BorderLayout.WEST);

        noiDung.setBackground(KieuNV.NEN);
        noiDung.add(new TrangDatHang(ctx.getBean(BanHangService.class), ctx.getBean(DatHangService.class),
                hienTai, trangDon::lamMoi), CARD_DAT_HANG);
        noiDung.add(trangDon, CARD_DON);
        noiDung.add(trangThongTin, CARD_THONG_TIN);
        noiDung.add(trangDanhGia, CARD_DANH_GIA);
        goc.add(noiDung, BorderLayout.CENTER);
        GiaoDienBanHang.hoanThien(noiDung);
        setContentPane(goc);

        mucDatHang.addActionListener(e -> chuyen(CARD_DAT_HANG));
        mucDon.addActionListener(e -> chuyen(CARD_DON));
        mucThongTin.addActionListener(e -> chuyen(CARD_THONG_TIN));
        mucDanhGia.addActionListener(e -> chuyen(CARD_DANH_GIA));
        hienTen();
        chuyen(CARD_DAT_HANG);
    }

    // Hồ sơ đổi (hoặc vừa tải lại) thì cập nhật tên hiển thị
    private void capNhatKhach(KhachHang moi) {
        this.khachHang = moi;
        hienTen();
    }

    private void hienTen() {
        tieuDeTen.setText("KHÁCH HÀNG  |  " + khachHang.getHoTen());
        tenSidebar.setText(khachHang.getHoTen());
        taiKhoanSidebar.setText("Tài khoản: " + khachHang.getTenDangNhap());
    }

    private JComponent taoTieuDe() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(KieuNV.DO);
        p.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));
        JPanel trai = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        trai.setOpaque(false);
        trai.add(new LogoTron(38));
        JLabel ten = new JLabel("HIGHLANDS COFFEE");
        ten.setFont(new Font("Segoe UI", Font.BOLD, 20));
        ten.setForeground(Color.WHITE);
        trai.add(ten);
        tieuDeTen.setFont(KieuNV.DAM);
        tieuDeTen.setForeground(Color.WHITE);
        p.add(trai, BorderLayout.WEST);
        p.add(tieuDeTen, BorderLayout.EAST);
        return p;
    }

    private JComponent taoSidebar() {
        JPanel p = new JPanel();
        p.setBackground(Theme.TOPBAR);
        p.setPreferredSize(new Dimension(224, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, KieuNV.VIEN), BorderFactory.createEmptyBorder(20, 14, 16, 14)));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        LogoTron logo = new LogoTron(76);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        p.add(logo);
        p.add(Box.createVerticalStrut(8));
        p.add(dong(new JLabel("Highlands"), new Font("Segoe UI", Font.BOLD, 18), Color.WHITE, CENTER_ALIGNMENT));
        p.add(dong(new JLabel("KHÁCH HÀNG"), KieuNV.NHO, Theme.SIDEBAR_TEXT, CENTER_ALIGNMENT));
        p.add(Box.createVerticalStrut(24));
        p.add(mucDatHang);
        p.add(Box.createVerticalStrut(6));
        p.add(mucDon);
        p.add(Box.createVerticalStrut(6));
        p.add(mucThongTin);
        p.add(Box.createVerticalStrut(6));
        p.add(mucDanhGia);
        p.add(Box.createVerticalGlue());

        p.add(dong(new JLabel("Đang đăng nhập"), KieuNV.NHO, Theme.SIDEBAR_TEXT, CENTER_ALIGNMENT));
        p.add(dong(tenSidebar, KieuNV.DAM, Color.WHITE, CENTER_ALIGNMENT));
        p.add(dong(taiKhoanSidebar, KieuNV.NHO, Theme.SIDEBAR_TEXT, CENTER_ALIGNMENT));
        p.add(Box.createVerticalStrut(12));
        JButton dangXuat = KieuNV.nutPhu("Đăng xuất / Đổi vai trò");
        dangXuat.setAlignmentX(CENTER_ALIGNMENT);
        dangXuat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        dangXuat.addActionListener(e -> dangXuat());
        p.add(dangXuat);
        return p;
    }

    private static JLabel dong(JLabel l, Font f, Color c, float canh) {
        l.setFont(f); l.setForeground(c); l.setAlignmentX(canh);
        return l;
    }

    private void chuyen(String the) {
        cardLayout.show(noiDung, the);
        mucDatHang.setChon(the.equals(CARD_DAT_HANG));
        mucDon.setChon(the.equals(CARD_DON));
        mucThongTin.setChon(the.equals(CARD_THONG_TIN));
        mucDanhGia.setChon(the.equals(CARD_DANH_GIA));
        if (the.equals(CARD_DON)) trangDon.lamMoi();
        if (the.equals(CARD_THONG_TIN)) trangThongTin.lamMoi();
        if (the.equals(CARD_DANH_GIA)) trangDanhGia.lamMoi();
    }

    private void dangXuat() {
        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn đăng xuất?\nGiỏ hàng chưa đặt sẽ bị hủy.",
                "Đăng xuất", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        dispose();
        com.highlands.HighlandsApplication.hienChonVaiTro();
    }

    private void thoatUngDung() {
        if (JOptionPane.showConfirmDialog(this, "Thoát ứng dụng?\nGiỏ hàng chưa đặt sẽ bị hủy.",
                "Thoát", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ctx.close();
        System.exit(0);
    }

    /** Nút menu bên trái: đang chọn thì nền đỏ chữ trắng. */
    private static final class MucMenu extends JButton {
        private boolean chon;

        MucMenu(String chu) {
            super(chu);
            setIcon(GiaoDienBanHang.bieuTuongMenu(chu));
            setIconTextGap(10);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setHorizontalAlignment(SwingConstants.LEFT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 8));
            setAlignmentX(CENTER_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        }

        void setChon(boolean chon) { this.chon = chon; repaint(); }

        @Override public Color getForeground() { return Color.WHITE; }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (chon) g.setColor(KieuNV.DO_SANG);
            else if (getModel().isRollover()) g.setColor(Theme.SIDEBAR_HOVER);
            else g.setColor(Theme.TOPBAR);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.dispose();
            super.paintComponent(g0);
        }
    }
}
