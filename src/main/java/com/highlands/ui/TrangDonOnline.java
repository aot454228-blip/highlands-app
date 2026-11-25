package com.highlands.ui;

import com.highlands.service.DonOnlineService;
import com.highlands.service.DonOnlineService.DonOnline;
import com.highlands.service.TienIch;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Màn hình nghiệp vụ tiếp nhận và theo dõi đơn khách đặt online. */
public class TrangDonOnline extends JPanel {
    private final DonOnlineService service;
    private final Long nhanVienId;
    private List<DonOnline> ds = List.of();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Mã đơn", "Ngày đặt", "Khách hàng", "Điện thoại", "Tổng tiền", "Thanh toán", "Trạng thái"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable bang = new JTable(model);
    private final JButton chiTiet = KieuNV.nutPhu("Xem chi tiết");
    private final JButton capNhat = KieuNV.nutPhu("Cập nhật trạng thái");
    private final JButton thuTien = KieuNV.nutPhu("Xác nhận thu tiền");
    private final JLabel thongBao = new JLabel(" ");
    private boolean dangTai;

    private final Timer tuLamMoi = new Timer(15000, e -> {
        if (isShowing()) lamMoi(false);
    });

    @Override public void addNotify() {
        super.addNotify();
        tuLamMoi.start();
    }

    @Override public void removeNotify() {
        tuLamMoi.stop();
        super.removeNotify();
    }

    public TrangDonOnline(DonOnlineService service, Long nhanVienId) {
        this.service = service;
        this.nhanVienId = nhanVienId;
        setLayout(new BorderLayout(0, 10));
        setBackground(KieuNV.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel tren = new JPanel(new BorderLayout());
        tren.setBackground(Color.WHITE);
        tren.setBorder(BorderFactory.createCompoundBorder(new Theme.VienBo(Theme.LINE, 12),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        JLabel ten = new JLabel("Đơn online");
        ten.setFont(KieuNV.TIEU_DE);
        ten.setForeground(KieuNV.DO);
        JButton moi = KieuNV.nutPhu("Làm mới");
        moi.addActionListener(e -> lamMoi());
        tren.add(ten, BorderLayout.WEST);
        tren.add(moi, BorderLayout.EAST);
        add(tren, BorderLayout.NORTH);
        KieuNV.kieuBang(bang);
        int[] rong = {110, 150, 160, 120, 120, 175, 160};
        for (int i = 0; i < rong.length; i++) bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        bang.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        bang.getColumnModel().getColumn(4).setCellRenderer(KieuNV.canPhai());
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getSelectionModel().addListSelectionListener(e -> chon());
        bang.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) xemChiTiet();
            }
        });
        add(new JScrollPane(bang), BorderLayout.CENTER);
        JPanel duoi = new JPanel(new BorderLayout());
        duoi.setOpaque(false);
        JPanel nut = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        nut.setOpaque(false);
        nut.add(chiTiet);
        nut.add(capNhat);
        nut.add(thuTien);
        duoi.add(thongBao, BorderLayout.WEST);
        duoi.add(nut, BorderLayout.EAST);
        add(duoi, BorderLayout.SOUTH);
        chiTiet.addActionListener(e -> xemChiTiet());
        capNhat.addActionListener(e -> capNhat());
        thuTien.addActionListener(e -> thuTien());
        chon();
    }
    private DonOnline donChon() {
        int r = bang.getSelectedRow();
        return r < 0 ? null : ds.get(bang.convertRowIndexToModel(r));
    }
    private void chon() {
        DonOnline d = donChon();
        String tiep = d == null ? null : DonOnlineService.trangThaiTiep(d.trangThai());
        chiTiet.setEnabled(!dangTai && d != null);
        capNhat.setEnabled(!dangTai && tiep != null);
        thuTien.setEnabled(!dangTai && d != null && !d.daThuTien()
                && !"Hủy".equals(d.trangThai()) && !"Đã hủy".equals(d.trangThai()));
        capNhat.setText(tiep == null ? "Cập nhật trạng thái" : tiep);
    }
    public void lamMoi() { lamMoi(true); }

    private void lamMoi(boolean thuCong) {
        if (dangTai) return;
        dangTai = true;
        chon();
        thongBao.setText("Đang tải...");
        ChayNen.chay(service::danhSach, kq -> {
            if (!ds.equals(kq)) {
                DonOnline cu = donChon();
                bang.clearSelection();
                model.setRowCount(0);
                ds = kq;
                for (DonOnline d : ds) {
                    model.addRow(new Object[]{TienIch.maHienThi(d.maDon()),
                            d.ngay().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), d.khach(),
                            d.sdt(), KieuNV.tien(d.tong()), d.phuongThuc()
                            + (d.daThuTien() ? " · Đã thu" : " · Chưa thu"), d.trangThai()});
                    if (cu != null && d.id().equals(cu.id())) {
                        int dong = bang.convertRowIndexToView(model.getRowCount() - 1);
                        bang.setRowSelectionInterval(dong, dong);
                    }
                }
            }
            dangTai = false;
            chon();
            thongBao.setText(ds.size() + " đơn online");
        }, ex -> {
            dangTai = false;
            chon();
            thongBao.setText("Không tải được đơn online");
            if (thuCong && isShowing()) KieuNV.loi(this, ChayNen.thongDiep(ex));
        });
    }
    private void thuTien() {
        DonOnline d = donChon();
        if (d == null || !thuTien.isEnabled()) return;
        dangTai = true;
        chon();
        int traLoi = JOptionPane.showConfirmDialog(this,
                "Xác nhận đã nhận " + KieuNV.tien(d.tong()) + " (" + d.phuongThuc()
                        + ") cho đơn " + TienIch.maHienThi(d.maDon()) + "?",
                "Xác nhận thu tiền", JOptionPane.YES_NO_OPTION);
        if (traLoi != JOptionPane.YES_OPTION) {
            dangTai = false;
            chon();
            return;
        }
        ChayNen.chay(() -> { service.xacNhanThuTien(d.id(), nhanVienId); return true; }, kq -> {
            dangTai = false;
            lamMoi(false);
        }, ex -> {
            dangTai = false;
            chon();
            KieuNV.loi(this, ChayNen.thongDiep(ex));
        });
    }

    private void xemChiTiet() {
        DonOnline d = donChon();
        if (d == null || dangTai) return;
        ChayNen.chay(() -> service.chiTiet(d.id()), ct -> {
            JDialog hop = new JDialog(SwingUtilities.getWindowAncestor(this), "Chi tiết đơn online",
                    Dialog.ModalityType.APPLICATION_MODAL);
            JTextArea thongTin = new JTextArea("Khách hàng: " + ct.don().khach()
                    + "\nĐiện thoại: " + ct.don().sdt() + "\nĐịa chỉ: " + ct.don().diaChi()
                    + "\nGhi chú: " + (ct.don().ghiChu() == null ? "" : ct.don().ghiChu())
                    + "\nThanh toán: " + ct.don().phuongThuc() + (ct.don().daThuTien() ? " · Đã thu" : " · Chưa thu")
                    + "\nTrạng thái: " + ct.don().trangThai()
                    + "\nTiền hàng: " + KieuNV.tien(ct.hoaDon().tienHang())
                    + " | Giảm giá: " + KieuNV.tien(ct.hoaDon().giamGia())
                    + " | Tổng: " + KieuNV.tien(ct.hoaDon().tongThanhToan()));
            thongTin.setEditable(false);
            thongTin.setLineWrap(true);
            thongTin.setWrapStyleWord(true);
            thongTin.setFont(KieuNV.NHO);
            DefaultTableModel mon = new DefaultTableModel(
                    new String[]{"Món", "Size", "Topping", "SL", "Đơn giá món", "Topping / món", "Thành tiền"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            for (var dong : ct.hoaDon().dong()) mon.addRow(new Object[]{dong.ten(), "—",
                    String.join(", ", dong.topping()), dong.soLuong(), KieuNV.tien(dong.donGia()),
                    KieuNV.tien(dong.giaTopping()), KieuNV.tien(dong.thanhTien())});
            JTable bangMon = new JTable(mon);
            KieuNV.kieuBang(bangMon);
            bangMon.setToolTipText("Size: hệ thống hiện chưa lưu size riêng; tên món hiển thị theo dữ liệu đã lưu.");
            hop.add(new JScrollPane(thongTin), BorderLayout.NORTH);
            hop.add(new JScrollPane(bangMon), BorderLayout.CENTER);
            hop.setSize(980, 500);
            hop.setLocationRelativeTo(this);
            hop.setVisible(true);
        }, ex -> KieuNV.loi(this, ChayNen.thongDiep(ex)));
    }
    private void capNhat() {
        DonOnline d = donChon();
        if (d == null || dangTai) return;
        String moi = DonOnlineService.trangThaiTiep(d.trangThai());
        if (moi == null) return;
        dangTai = true;
        chon();
        ChayNen.chay(() -> { service.capNhat(d.id(), d.trangThai(), moi); return true; }, kq -> {
            dangTai = false;
            lamMoi();
        }, ex -> {
            dangTai = false;
            KieuNV.loi(this, ChayNen.thongDiep(ex));
            lamMoi();
        });
    }
}
