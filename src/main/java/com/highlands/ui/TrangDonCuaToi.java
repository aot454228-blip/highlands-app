package com.highlands.ui;

import com.highlands.entity.KhachHang;
import com.highlands.service.DatHangService;
import com.highlands.service.DatHangService.DonCuaToi;
import com.highlands.service.TienIch;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Các đơn khách đã đặt (mới nhất trước). Chỉ xem: bấm "Xem hóa đơn" hoặc nhấp đúp một dòng. */
public class TrangDonCuaToi extends JPanel {
    private static final DateTimeFormatter DINH_DANG = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int COT_THAO_TAC = 5;

    private final DatHangService datHang;
    private final Supplier<KhachHang> khachHang;
    private final List<DonCuaToi> ds = new ArrayList<>();
    private final Model model = new Model();
    private final JTable bang = new JTable(model);
    private final JLabel nhanTrangThai = new JLabel(" ");
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

    public TrangDonCuaToi(DatHangService datHang, Supplier<KhachHang> khachHang) {
        this.datHang = datHang;
        this.khachHang = khachHang;
        setLayout(new BorderLayout(0, 10));
        setBackground(KieuNV.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel tren = new JPanel(new BorderLayout());
        tren.setBackground(Color.WHITE);
        tren.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel tieuDe = new JLabel("Đơn hàng của tôi");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        JButton lamMoi = KieuNV.nutPhu("Làm mới");
        lamMoi.addActionListener(e -> lamMoi());
        tren.add(tieuDe, BorderLayout.WEST);
        tren.add(lamMoi, BorderLayout.EAST);
        add(tren, BorderLayout.NORTH);

        KieuNV.kieuBang(bang);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {110, 150, 130, 120, 150, 120};
        for (int i = 0; i < rong.length; i++) bang.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        bang.getColumnModel().getColumn(2).setCellRenderer(KieuNV.canPhai());
        DefaultTableCellRenderer lienKet = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, "<html><u>Xem hóa đơn</u></html>", s, f, r, c);
                setForeground(KieuNV.DO_SANG);
                setFont(KieuNV.DAM);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        bang.getColumnModel().getColumn(COT_THAO_TAC).setCellRenderer(lienKet);
        bang.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int r = bang.rowAtPoint(e.getPoint());
                int c = bang.columnAtPoint(e.getPoint());
                if (r < 0) return;
                if (c == COT_THAO_TAC || e.getClickCount() == 2) xemHoaDon(r);
            }
        });
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuon.getViewport().setBackground(Color.WHITE);
        add(cuon, BorderLayout.CENTER);

        nhanTrangThai.setFont(KieuNV.NHO);
        nhanTrangThai.setForeground(KieuNV.CHU_PHU);
        add(nhanTrangThai, BorderLayout.SOUTH);
    }

    /** Tải lại danh sách đơn (gọi mỗi khi mở trang này hoặc vừa đặt hàng xong). */
    public void lamMoi() { lamMoi(true); }

    private void lamMoi(boolean thuCong) {
        if (dangTai) return;
        dangTai = true;
        Long id = khachHang.get().getId();
        nhanTrangThai.setText("Đang tải...");
        ChayNen.chay(() -> datHang.layDonCuaToi(id), kq -> {
            dangTai = false;
            if (!ds.equals(kq)) {
                ds.clear();
                ds.addAll(kq);
                model.fireTableDataChanged();
            }
            nhanTrangThai.setText(ds.isEmpty() ? "Bạn chưa có đơn hàng nào." : ds.size() + " đơn hàng");
        }, ex -> {
            dangTai = false;
            nhanTrangThai.setText("Không tải được danh sách đơn.");
            if (thuCong && isShowing()) KieuNV.loi(this, ChayNen.thongDiep(ex));
        });
    }

    private void xemHoaDon(int dong) {
        if (dong < 0 || dong >= ds.size()) return;
        Long idKhach = khachHang.get().getId();
        Long idDon = ds.get(dong).donKhachId();
        ChayNen.chay(() -> datHang.layHoaDon(idKhach, idDon), hd ->
                new HopThoaiHoaDon(SwingUtilities.getWindowAncestor(this), hd, "Khách hàng:", false).setVisible(true),
                ex -> KieuNV.loi(this, ChayNen.thongDiep(ex)));
    }

    private final class Model extends AbstractTableModel {
        private final String[] cot = {"Mã đơn", "Ngày đặt", "Tổng tiền", "Thanh toán", "Trạng thái", ""};
        @Override public int getRowCount() { return ds.size(); }
        @Override public int getColumnCount() { return cot.length; }
        @Override public String getColumnName(int c) { return cot[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Object getValueAt(int r, int c) {
            DonCuaToi d = ds.get(r);
            return switch (c) {
                case 0 -> TienIch.maHienThi(d.maDon());
                case 1 -> d.ngay().format(DINH_DANG);
                case 2 -> KieuNV.tien(d.tong());
                case 3 -> d.phuongThuc();
                case 4 -> d.trangThai();
                default -> "";
            };
        }
    }
}
