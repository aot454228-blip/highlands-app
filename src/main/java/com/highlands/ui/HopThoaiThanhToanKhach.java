package com.highlands.ui;

import com.highlands.entity.KhachHang;
import com.highlands.service.DatHangService;
import com.highlands.service.DatHangService.GiaoHang;
import com.highlands.service.DatHangService.TaiKhoanNhanTien;
import com.highlands.service.DongGio;
import com.highlands.service.ThanhToanService.HoaDon;
import com.highlands.service.TienIch;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Hộp thoại thanh toán của khách: nhập thông tin nhận hàng, chọn Tiền mặt (trả khi nhận hàng)
 * hoặc Chuyển khoản. Chỉ đóng với kết quả khác null khi đơn đã được lưu thành công.
 */
class HopThoaiThanhToanKhach extends JDialog {
    private static final String THE_TIEN_MAT = "tienmat";
    private static final String THE_CHUYEN_KHOAN = "chuyenkhoan";

    private final DatHangService dich;
    private final Long khachHangId;
    private final List<DongGio> gio;
    private final String maKhuyenMai;
    private final BigDecimal tong;
    private final String maGiaoDich;
    private final TaiKhoanNhanTien taiKhoan;

    private final JTextField oSdt = new JTextField();
    private final JTextField oDiaChi = new JTextField();
    private final JTextField oGhiChu = new JTextField();
    private final JRadioButton chonTienMat = new JRadioButton("Tiền mặt (trả khi nhận hàng)", true);
    private final JRadioButton chonChuyenKhoan = new JRadioButton("Chuyển khoản ngân hàng");
    private final CardLayout theKhuVuc = new CardLayout();
    private final JPanel khuVuc = new JPanel(theKhuVuc);
    private final JLabel nhanNoiDungCk = new JLabel();
    private final JCheckBox daChuyen = new JCheckBox("Tôi đã chuyển khoản đúng số tiền trên");
    private final JButton nutXacNhan = KieuNV.nut("Đặt hàng");
    private final JButton nutHuy = KieuNV.nutPhu("Hủy");
    private HoaDon ketQua;
    private boolean dangXuLy;

    HopThoaiThanhToanKhach(Window cha, DatHangService dich, KhachHang kh, List<DongGio> gio, String maKhuyenMai,
                           BigDecimal tienHang, BigDecimal giam, String maGiaoDich) {
        super(cha, "Đặt hàng", ModalityType.APPLICATION_MODAL);
        this.dich = dich;
        this.khachHangId = kh.getId();
        this.gio = gio;
        this.maKhuyenMai = maKhuyenMai;
        this.maGiaoDich = maGiaoDich;
        this.tong = tienHang.subtract(giam);
        this.taiKhoan = dich.layTaiKhoanNhanTien();

        JPanel goc = new JPanel();
        goc.setLayout(new BoxLayout(goc, BoxLayout.Y_AXIS));
        goc.setBackground(Color.WHITE);
        goc.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel tieuDe = new JLabel("Xác nhận đặt hàng");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        tieuDe.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(tieuDe);
        goc.add(Box.createVerticalStrut(10));
        goc.add(hang("Tổng tiền hàng", KieuNV.tien(tienHang), false));
        goc.add(hang("Tiền giảm giá" + (maKhuyenMai == null ? "" : " (" + maKhuyenMai + ")"),
                giam.signum() > 0 ? "-" + KieuNV.tien(giam) : KieuNV.tien(BigDecimal.ZERO), false));
        goc.add(hang("TỔNG THANH TOÁN", KieuNV.tien(tong), true));
        goc.add(Box.createVerticalStrut(12));

        // --- thông tin nhận hàng ---
        goc.add(tieuMuc("Thông tin nhận hàng"));
        goc.add(Box.createVerticalStrut(4));
        goc.add(nhan("Số điện thoại *"));
        oSdt.setText(TienIch.str(kh.getSoDienThoai()));
        goc.add(truong(oSdt));
        goc.add(Box.createVerticalStrut(6));
        goc.add(nhan("Địa chỉ nhận hàng *"));
        oDiaChi.setText(TienIch.str(kh.getDiaChi()));
        goc.add(truong(oDiaChi));
        goc.add(Box.createVerticalStrut(6));
        goc.add(nhan("Ghi chú (không bắt buộc)"));
        goc.add(truong(oGhiChu));
        goc.add(Box.createVerticalStrut(12));

        // --- phương thức thanh toán ---
        goc.add(tieuMuc("Phương thức thanh toán"));
        goc.add(Box.createVerticalStrut(2));
        ButtonGroup nhom = new ButtonGroup();
        for (JRadioButton r : new JRadioButton[]{chonTienMat, chonChuyenKhoan}) {
            r.setOpaque(false);
            r.setFont(KieuNV.FONT);
            r.setFocusPainted(false);
            r.setAlignmentX(LEFT_ALIGNMENT);
            nhom.add(r);
            goc.add(r);
        }
        if (!taiKhoan.daCauHinh()) {
            chonChuyenKhoan.setEnabled(false);
            chonChuyenKhoan.setText("Chuyển khoản ngân hàng (cửa hàng chưa mở)");
        }
        goc.add(Box.createVerticalStrut(8));

        khuVuc.setOpaque(false);
        khuVuc.setAlignmentX(LEFT_ALIGNMENT);
        khuVuc.add(taoTheTienMat(), THE_TIEN_MAT);
        khuVuc.add(taoTheChuyenKhoan(), THE_CHUYEN_KHOAN);
        chonTienMat.addActionListener(e -> doiPhuongThuc());
        chonChuyenKhoan.addActionListener(e -> doiPhuongThuc());
        goc.add(khuVuc);
        goc.add(Box.createVerticalStrut(14));

        JPanel nut = new JPanel(new GridLayout(1, 2, 10, 0));
        nut.setOpaque(false);
        nut.setAlignmentX(LEFT_ALIGNMENT);
        nut.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        nutHuy.addActionListener(e -> { if (!dangXuLy) dispose(); });
        nutXacNhan.addActionListener(e -> xacNhan());
        nut.add(nutHuy);
        nut.add(nutXacNhan);
        goc.add(nut);

        oSdt.getDocument().addDocumentListener(new TrangBanHang.SuKienGoPhim(this::capNhatNoiDungCk));
        GiaoDienBanHang.hoanThien(goc);
        setContentPane(goc);
        getRootPane().setDefaultButton(nutXacNhan);
        capNhatNoiDungCk();
        doiPhuongThuc();
        pack();
        setSize(new Dimension(500, Math.max(getHeight(), 640)));
        setLocationRelativeTo(cha);
    }

    private JComponent taoTheTienMat() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel l = new JLabel("<html>Bạn thanh toán <b>" + KieuNV.tien(tong)
                + "</b> bằng tiền mặt cho người giao hàng khi nhận đơn.</html>");
        l.setFont(KieuNV.FONT);
        p.add(l, BorderLayout.NORTH);
        return p;
    }

    private JComponent taoTheChuyenKhoan() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        if (!taiKhoan.daCauHinh()) {
            JLabel l = new JLabel("<html>Cửa hàng chưa cấu hình tài khoản nhận tiền nên chưa nhận chuyển khoản.<br>"
                    + "Vui lòng chọn thanh toán tiền mặt.</html>");
            l.setFont(KieuNV.FONT);
            l.setForeground(KieuNV.DO_SANG);
            l.setAlignmentX(LEFT_ALIGNMENT);
            p.add(l);
            return p;
        }
        p.add(dong("Ngân hàng", taiKhoan.nganHang()));
        p.add(dong("Số tài khoản", taiKhoan.soTaiKhoan()));
        p.add(dong("Chủ tài khoản", taiKhoan.chuTaiKhoan()));
        p.add(dong("Số tiền", KieuNV.tien(tong)));
        nhanNoiDungCk.setFont(KieuNV.DAM);
        nhanNoiDungCk.setAlignmentX(LEFT_ALIGNMENT);
        p.add(nhanNoiDungCk);
        p.add(Box.createVerticalStrut(6));
        JLabel luuY = new JLabel("<html>Đơn sẽ ở trạng thái <b>Chờ xác nhận</b> cho đến khi cửa hàng đối chiếu giao dịch.</html>");
        luuY.setFont(KieuNV.NHO);
        luuY.setForeground(KieuNV.CHU_PHU);
        luuY.setAlignmentX(LEFT_ALIGNMENT);
        p.add(luuY);
        p.add(Box.createVerticalStrut(6));
        daChuyen.setOpaque(false);
        daChuyen.setFont(KieuNV.DAM);
        daChuyen.setAlignmentX(LEFT_ALIGNMENT);
        daChuyen.addActionListener(e -> capNhatNut());
        p.add(daChuyen);
        return p;
    }

    private JComponent dong(String ten, String giaTri) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel a = new JLabel(ten);
        a.setFont(KieuNV.FONT);
        JLabel b = new JLabel(giaTri == null || giaTri.isBlank() ? "-" : giaTri);
        b.setFont(KieuNV.DAM);
        h.add(a, BorderLayout.WEST);
        h.add(b, BorderLayout.EAST);
        h.setAlignmentX(LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return h;
    }

    private void capNhatNoiDungCk() {
        String so = oSdt.getText().replaceAll("[^0-9+]", "");
        nhanNoiDungCk.setText("Nội dung chuyển khoản: HIGHLANDS " + so);
    }

    private boolean laChuyenKhoan() {
        return chonChuyenKhoan.isSelected();
    }

    private void doiPhuongThuc() {
        theKhuVuc.show(khuVuc, laChuyenKhoan() ? THE_CHUYEN_KHOAN : THE_TIEN_MAT);
        capNhatNut();
    }

    private void capNhatNut() {
        nutXacNhan.setEnabled(!dangXuLy && (!laChuyenKhoan() || daChuyen.isSelected()));
    }

    private JComponent hang(String ten, String giaTri, boolean dam) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel a = new JLabel(ten);
        JLabel b = new JLabel(giaTri);
        a.setFont(dam ? new Font("Segoe UI", Font.BOLD, 16) : KieuNV.FONT);
        b.setFont(dam ? new Font("Segoe UI", Font.BOLD, 22) : KieuNV.DAM);
        if (dam) b.setForeground(KieuNV.DO);
        h.add(a, BorderLayout.WEST);
        h.add(b, BorderLayout.EAST);
        h.setAlignmentX(LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, dam ? 38 : 28));
        return h;
    }

    private static JLabel tieuMuc(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        l.setForeground(KieuNV.CHU);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(KieuNV.NHO);
        l.setForeground(KieuNV.CHU_PHU);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent truong(JTextField t) {
        t.setFont(KieuNV.FONT);
        t.setAlignmentX(LEFT_ALIGNMENT);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        return t;
    }

    private void khoaNhap(boolean khoa) {
        for (JComponent c : new JComponent[]{oSdt, oDiaChi, oGhiChu, chonTienMat, chonChuyenKhoan, daChuyen, nutHuy}) {
            c.setEnabled(!khoa);
        }
        if (!khoa && !taiKhoan.daCauHinh()) chonChuyenKhoan.setEnabled(false);
    }

    private void xacNhan() {
        final boolean ck = laChuyenKhoan();
        final String pt = ck ? DatHangService.CHUYEN_KHOAN : DatHangService.TIEN_MAT;
        if (ck && !daChuyen.isSelected()) { KieuNV.loi(this, "Hãy tick xác nhận bạn đã chuyển khoản."); return; }
        String noiDung = "Đặt hàng với tổng " + KieuNV.tien(tong) + (ck
                ? ", thanh toán bằng chuyển khoản?"
                : ", thanh toán tiền mặt khi nhận hàng?");
        if (JOptionPane.showConfirmDialog(this, noiDung, "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        GiaoHang gh = new GiaoHang(oSdt.getText(), oDiaChi.getText(), oGhiChu.getText());
        dangXuLy = true;
        nutXacNhan.setEnabled(false);
        khoaNhap(true);
        ChayNen.chay(() -> dich.datHang(khachHangId, gio, maKhuyenMai, pt, gh, maGiaoDich), hd -> {
            ketQua = hd;
            dispose();
        }, ex -> {
            dangXuLy = false;
            khoaNhap(false);
            capNhatNut();
            KieuNV.loi(this, ChayNen.thongDiep(ex) + "\nGiỏ hàng vẫn được giữ nguyên.");
        });
    }

    HoaDon getKetQua() { return ketQua; }
}
