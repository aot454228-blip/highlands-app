package com.highlands.ui;

import com.highlands.service.DongGio;
import com.highlands.service.ThanhToanService;
import com.highlands.service.ThanhToanService.HoaDon;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/** Hộp thoại thanh toán tiền mặt. Chỉ đóng với kết quả khác null khi đã lưu đơn thành công. */
class TrangThanhToan extends JDialog {
    private final ThanhToanService dich;
    private final Long nhanVienId;
    private final List<DongGio> gio;
    private final String maKhuyenMai;
    private final BigDecimal tong;
    private final String maGiaoDich;

    private final JTextField oKhach = new JTextField();
    private final JLabel nhanThua = new JLabel(" ");
    private final JButton nutXacNhan = KieuNV.nut("Xác nhận thanh toán");
    private final JButton nutHuy = KieuNV.nutPhu("Hủy");
    private final JPanel luoiNhanh = new JPanel(new GridLayout(1, 0, 6, 0));
    private HoaDon ketQua;
    private boolean dangXuLy;

    TrangThanhToan(Window cha, ThanhToanService dich, Long nhanVienId, List<DongGio> gio, String maKhuyenMai,
                   BigDecimal tienHang, BigDecimal giam, String maGiaoDich) {
        super(cha, "Thanh toán", ModalityType.APPLICATION_MODAL);
        this.dich = dich;
        this.nhanVienId = nhanVienId;
        this.gio = gio;
        this.maKhuyenMai = maKhuyenMai;
        this.maGiaoDich = maGiaoDich;
        this.tong = tienHang.subtract(giam);

        JPanel goc = new JPanel();
        goc.setLayout(new BoxLayout(goc, BoxLayout.Y_AXIS));
        goc.setBackground(Color.WHITE);
        goc.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel tieuDe = new JLabel("Thanh toán đơn hàng");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        tieuDe.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(tieuDe);
        goc.add(Box.createVerticalStrut(12));
        goc.add(hang("Tổng tiền hàng", KieuNV.tien(tienHang), false));
        goc.add(hang("Tiền giảm giá" + (maKhuyenMai == null ? "" : " (" + maKhuyenMai + ")"),
                giam.signum() > 0 ? "-" + KieuNV.tien(giam) : KieuNV.tien(BigDecimal.ZERO), false));
        goc.add(hang("TỔNG THANH TOÁN", KieuNV.tien(tong), true));
        goc.add(Box.createVerticalStrut(12));

        JLabel pt = new JLabel("Phương thức thanh toán");
        pt.setFont(KieuNV.DAM);
        pt.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(pt);
        goc.add(Box.createVerticalStrut(4));
        JComboBox<String> phuongThuc = new JComboBox<>(new String[]{ThanhToanService.TIEN_MAT});
        phuongThuc.setFont(KieuNV.FONT);
        phuongThuc.setAlignmentX(LEFT_ALIGNMENT);
        phuongThuc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        goc.add(phuongThuc);
        goc.add(Box.createVerticalStrut(12));

        JLabel kh = new JLabel("Tiền khách đưa (VNĐ)");
        kh.setFont(KieuNV.DAM);
        kh.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(kh);
        goc.add(Box.createVerticalStrut(4));
        oKhach.setFont(new Font("Segoe UI", Font.BOLD, 20));
        oKhach.setHorizontalAlignment(SwingConstants.RIGHT);
        oKhach.setAlignmentX(LEFT_ALIGNMENT);
        oKhach.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        oKhach.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        oKhach.getDocument().addDocumentListener(new TrangBanHang.SuKienGoPhim(this::capNhat));
        goc.add(oKhach);
        goc.add(Box.createVerticalStrut(8));

        luoiNhanh.setOpaque(false);
        luoiNhanh.setAlignmentX(LEFT_ALIGNMENT);
        luoiNhanh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JButton vua = KieuNV.nutPhu("Vừa đủ");
        vua.addActionListener(e -> datTienKhach(tong));
        luoiNhanh.add(vua);
        for (long menhGia : new long[]{50_000, 100_000, 200_000, 500_000}) {
            JButton b = KieuNV.nutPhu(String.format("%,d", menhGia).replace(',', '.'));
            b.addActionListener(e -> datTienKhach(BigDecimal.valueOf(menhGia)));
            luoiNhanh.add(b);
        }
        goc.add(luoiNhanh);
        goc.add(Box.createVerticalStrut(10));
        nhanThua.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nhanThua.setAlignmentX(LEFT_ALIGNMENT);
        goc.add(nhanThua);
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

        setContentPane(goc);
        getRootPane().setDefaultButton(nutXacNhan);
        setPreferredSize(new Dimension(470, 560));
        pack();
        setLocationRelativeTo(cha);
        capNhat();
        SwingUtilities.invokeLater(oKhach::requestFocusInWindow);
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

    private BigDecimal tienKhach() {
        String so = oKhach.getText().replaceAll("\\D", "");
        if (so.isEmpty() || so.length() > 12) return BigDecimal.ZERO;
        return new BigDecimal(so);
    }

    private void datTienKhach(BigDecimal v) {
        oKhach.setText(v.toPlainString());
    }

    private void capNhat() {
        BigDecimal kh = tienKhach();
        BigDecimal chenh = kh.subtract(tong);
        if (kh.signum() == 0) {
            nhanThua.setForeground(KieuNV.CHU_PHU);
            nhanThua.setText("Nhập số tiền khách đưa");
        } else if (chenh.signum() < 0) {
            nhanThua.setForeground(KieuNV.DO_SANG);
            nhanThua.setText("Còn thiếu: " + KieuNV.tien(chenh.negate()));
        } else {
            nhanThua.setForeground(KieuNV.XANH);
            nhanThua.setText("Tiền thừa: " + KieuNV.tien(chenh));
        }
        nutXacNhan.setEnabled(!dangXuLy && kh.compareTo(tong) >= 0 && kh.signum() > 0);
    }

    private void xacNhan() {
        BigDecimal kh = tienKhach();
        if (kh.compareTo(tong) < 0) { KieuNV.loi(this, "Tiền khách đưa chưa đủ."); return; }
        String noiDung = "Xác nhận thanh toán " + KieuNV.tien(tong) + " bằng tiền mặt?\nTiền thừa trả khách: "
                + KieuNV.tien(kh.subtract(tong));
        if (JOptionPane.showConfirmDialog(this, noiDung, "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        dangXuLy = true;
        nutXacNhan.setEnabled(false);
        nutHuy.setEnabled(false);
        oKhach.setEnabled(false);
        ChayNen.chay(() -> dich.thanhToan(nhanVienId, gio, maKhuyenMai, kh, maGiaoDich), hd -> {
            ketQua = hd;
            dispose();
        }, ex -> {
            dangXuLy = false;
            nutHuy.setEnabled(true);
            oKhach.setEnabled(true);
            capNhat();
            KieuNV.loi(this, ChayNen.thongDiep(ex) + "\nGiỏ hàng vẫn được giữ nguyên.");
        });
    }

    HoaDon getKetQua() { return ketQua; }
}
