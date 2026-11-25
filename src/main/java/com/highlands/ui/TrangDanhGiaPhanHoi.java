package com.highlands.ui;

import com.highlands.entity.DanhGiaSanPham;
import com.highlands.entity.KhachHang;
import com.highlands.entity.PhanHoi;
import com.highlands.entity.SanPham;
import com.highlands.service.DanhGiaPhanHoiService;
import com.highlands.service.TienIch;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Đánh giá &amp; phản hồi: chấm sao cho sản phẩm đã mua, và gửi phản hồi cho cửa hàng. */
public class TrangDanhGiaPhanHoi extends JPanel {
    private static final String THE_DANH_GIA = "danhgia";
    private static final String THE_PHAN_HOI = "phanhoi";
    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DanhGiaPhanHoiService dich;
    private final Supplier<KhachHang> khachHang;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel noiDung = new JPanel(cardLayout);
    private final Nut tabDanhGia = new Nut("Đánh giá sản phẩm", Nut.Kieu.TAB_CHON);
    private final Nut tabPhanHoi = new Nut("Gửi phản hồi", Nut.Kieu.TAB);

    // --- đánh giá sản phẩm ---
    private final DefaultComboBoxModel<SanPham> modelSanPham = new DefaultComboBoxModel<>();
    private final JComboBox<SanPham> oSanPham = new JComboBox<>(modelSanPham);
    private final OSao oSao = new OSao();
    private final JTextArea oNhanXet = new JTextArea(5, 30);
    private final JLabel nhanDanhGia = new JLabel(" ");
    private final JButton nutGuiDanhGia = KieuNV.nut("Gửi đánh giá");
    private final List<DanhGiaSanPham> dsDanhGia = new ArrayList<>();
    private final DanhGiaModel modelDanhGia = new DanhGiaModel();
    private final JTable bangDanhGia = new JTable(modelDanhGia);
    private boolean dangNapCombo;
    private boolean dangBaoChuaMua;

    // --- phản hồi ---
    private final JTextField oTieuDe = new JTextField();
    private final JTextArea oNoiDung = new JTextArea(6, 30);
    private final JLabel nhanPhanHoi = new JLabel(" ");
    private final JButton nutGuiPhanHoi = KieuNV.nut("Gửi phản hồi");
    private final List<PhanHoi> dsPhanHoi = new ArrayList<>();
    private final PhanHoiModel modelPhanHoi = new PhanHoiModel();
    private final JTable bangPhanHoi = new JTable(modelPhanHoi);

    public TrangDanhGiaPhanHoi(DanhGiaPhanHoiService dich, Supplier<KhachHang> khachHang) {
        this.dich = dich;
        this.khachHang = khachHang;
        setLayout(new BorderLayout(0, 10));
        setBackground(KieuNV.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel tren = new JPanel(new BorderLayout(0, 8));
        tren.setBackground(Color.WHITE);
        tren.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel tieuDe = new JLabel("Đánh giá & phản hồi");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        JPanel hangTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hangTab.setOpaque(false);
        hangTab.add(tabDanhGia);
        hangTab.add(tabPhanHoi);
        tren.add(tieuDe, BorderLayout.NORTH);
        tren.add(hangTab, BorderLayout.CENTER);
        add(tren, BorderLayout.NORTH);

        noiDung.setOpaque(false);
        noiDung.add(taoTrangDanhGia(), THE_DANH_GIA);
        noiDung.add(taoTrangPhanHoi(), THE_PHAN_HOI);
        add(noiDung, BorderLayout.CENTER);

        tabDanhGia.addActionListener(e -> chuyen(THE_DANH_GIA));
        tabPhanHoi.addActionListener(e -> chuyen(THE_PHAN_HOI));
    }

    private void chuyen(String the) {
        cardLayout.show(noiDung, the);
        tabDanhGia.setKieu(THE_DANH_GIA.equals(the) ? Nut.Kieu.TAB_CHON : Nut.Kieu.TAB);
        tabPhanHoi.setKieu(THE_PHAN_HOI.equals(the) ? Nut.Kieu.TAB_CHON : Nut.Kieu.TAB);
    }

    /** Tải lại dữ liệu (gọi mỗi khi mở trang này, vì khách có thể vừa mua thêm sản phẩm). */
    public void lamMoi() {
        taiDanhGia();
        taiPhanHoi();
    }

    // ================= ĐÁNH GIÁ SẢN PHẨM =================
    private JComponent taoTrangDanhGia() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel the = khungThe("Chấm điểm sản phẩm bạn đã mua");
        the.add(nhan("Sản phẩm"));
        oSanPham.setFont(KieuNV.FONT);
        oSanPham.setAlignmentX(LEFT_ALIGNMENT);
        oSanPham.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        oSanPham.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                return super.getListCellRendererComponent(l, v instanceof SanPham sp ? sp.getTen() : "", i, s, f);
            }
        });
        oSanPham.addActionListener(e -> { if (!dangNapCombo) napDanhGiaCuaSanPham(); });
        the.add(oSanPham);
        the.add(Box.createVerticalStrut(10));
        the.add(nhan("Số sao"));
        oSao.setAlignmentX(LEFT_ALIGNMENT);
        the.add(oSao);
        the.add(Box.createVerticalStrut(10));
        the.add(nhan("Nhận xét (không bắt buộc)"));
        oNhanXet.setFont(KieuNV.FONT);
        oNhanXet.setLineWrap(true);
        oNhanXet.setWrapStyleWord(true);
        oNhanXet.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane cuonNx = new JScrollPane(oNhanXet);
        cuonNx.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuonNx.setAlignmentX(LEFT_ALIGNMENT);
        cuonNx.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        the.add(cuonNx);
        the.add(Box.createVerticalStrut(8));
        nhanDanhGia.setFont(KieuNV.FONT);
        nhanDanhGia.setAlignmentX(LEFT_ALIGNMENT);
        the.add(nhanDanhGia);
        the.add(Box.createVerticalStrut(6));
        nutGuiDanhGia.setAlignmentX(LEFT_ALIGNMENT);
        nutGuiDanhGia.setMaximumSize(new Dimension(260, 44));
        nutGuiDanhGia.addActionListener(e -> guiDanhGia());
        the.add(nutGuiDanhGia);
        p.add(the, BorderLayout.NORTH);

        KieuNV.kieuBang(bangDanhGia);
        bangDanhGia.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {200, 90, 380, 140};
        for (int i = 0; i < rong.length; i++) bangDanhGia.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        bangDanhGia.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;
                int r = bangDanhGia.rowAtPoint(e.getPoint());
                if (r >= 0) chonSanPhamTheoId(dsDanhGia.get(r).getSanPham().getId());
            }
        });
        JScrollPane cuon = new JScrollPane(bangDanhGia);
        cuon.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(KieuNV.VIEN),
                "Đánh giá của tôi (nhấp đúp để sửa)"));
        cuon.getViewport().setBackground(Color.WHITE);
        p.add(cuon, BorderLayout.CENTER);
        return p;
    }

    private void taiDanhGia() {
        Long id = khachHang.get().getId();
        ChayNen.chay(() -> new Object[]{dich.layDanhSachDaMua(id), dich.layDanhGiaCuaToi(id)}, kq -> {
            @SuppressWarnings("unchecked") List<SanPham> daMua = (List<SanPham>) kq[0];
            @SuppressWarnings("unchecked") List<DanhGiaSanPham> dg = (List<DanhGiaSanPham>) kq[1];
            SanPham dangChon = (SanPham) oSanPham.getSelectedItem();
            dangNapCombo = true;
            modelSanPham.removeAllElements();
            for (SanPham s : daMua) modelSanPham.addElement(s);
            if (dangChon != null) {
                for (SanPham s : daMua) if (s.getId().equals(dangChon.getId())) { oSanPham.setSelectedItem(s); break; }
            }
            dangNapCombo = false;
            dsDanhGia.clear();
            dsDanhGia.addAll(dg);
            modelDanhGia.fireTableDataChanged();
            boolean coSp = !daMua.isEmpty();
            oSanPham.setEnabled(coSp);
            nutGuiDanhGia.setEnabled(coSp);
            oSao.setEnabled(coSp);
            oNhanXet.setEnabled(coSp);
            if (!coSp) {
                baoThongBao(nhanDanhGia, "Bạn chưa mua sản phẩm nào nên chưa thể đánh giá. Hãy đặt hàng trước nhé!", false);
                dangBaoChuaMua = true;
            } else if (dangBaoChuaMua) {
                baoThongBao(nhanDanhGia, " ", false);
                dangBaoChuaMua = false;
            }
            napDanhGiaCuaSanPham();
        }, ex -> baoThongBao(nhanDanhGia, ChayNen.thongDiep(ex), false));
    }

    // Chọn sản phẩm nào thì điền sẵn đánh giá cũ của sản phẩm đó (nếu có) để khách sửa
    private void napDanhGiaCuaSanPham() {
        SanPham sp = (SanPham) oSanPham.getSelectedItem();
        DanhGiaSanPham cu = null;
        if (sp != null) {
            for (DanhGiaSanPham d : dsDanhGia) if (d.getSanPham().getId().equals(sp.getId())) { cu = d; break; }
        }
        oSao.setGiaTri(cu == null ? 0 : cu.getSoSao());
        oNhanXet.setText(cu == null ? "" : TienIch.str(cu.getNoiDung()));
        nutGuiDanhGia.setText(cu == null ? "Gửi đánh giá" : "Cập nhật đánh giá");
    }

    private void chonSanPhamTheoId(Long id) {
        for (int i = 0; i < modelSanPham.getSize(); i++) {
            if (modelSanPham.getElementAt(i).getId().equals(id)) {
                oSanPham.setSelectedIndex(i);
                return;
            }
        }
    }

    private void guiDanhGia() {
        SanPham sp = (SanPham) oSanPham.getSelectedItem();
        if (sp == null) { baoThongBao(nhanDanhGia, "Vui lòng chọn sản phẩm cần đánh giá.", false); return; }
        if (oSao.getGiaTri() == 0) { baoThongBao(nhanDanhGia, "Vui lòng chọn số sao (bấm vào ngôi sao).", false); return; }
        Long idKh = khachHang.get().getId();
        Long idSp = sp.getId();
        int sao = oSao.getGiaTri();
        String nx = oNhanXet.getText();
        nutGuiDanhGia.setEnabled(false);
        baoThongBao(nhanDanhGia, " ", false);
        ChayNen.chay(() -> dich.guiDanhGia(idKh, idSp, sao, nx), dg -> {
            nutGuiDanhGia.setEnabled(true);
            ChayNen.chay(() -> dich.layDanhGiaCuaToi(idKh), ds -> {
                dsDanhGia.clear();
                dsDanhGia.addAll(ds);
                modelDanhGia.fireTableDataChanged();
                nutGuiDanhGia.setText("Cập nhật đánh giá");
                baoThongBao(nhanDanhGia, "Cảm ơn bạn đã đánh giá \"" + sp.getTen() + "\"!", true);
            }, ex -> baoThongBao(nhanDanhGia, ChayNen.thongDiep(ex), false));
        }, ex -> {
            nutGuiDanhGia.setEnabled(true);
            baoThongBao(nhanDanhGia, ChayNen.thongDiep(ex), false);
        });
    }

    // ================= PHẢN HỒI =================
    private JComponent taoTrangPhanHoi() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel the = khungThe("Gửi phản hồi cho cửa hàng");
        the.add(nhan("Tiêu đề *"));
        oTieuDe.setFont(KieuNV.FONT);
        oTieuDe.setAlignmentX(LEFT_ALIGNMENT);
        oTieuDe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        oTieuDe.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        the.add(oTieuDe);
        the.add(Box.createVerticalStrut(10));
        the.add(nhan("Nội dung * (ít nhất 10 ký tự)"));
        oNoiDung.setFont(KieuNV.FONT);
        oNoiDung.setLineWrap(true);
        oNoiDung.setWrapStyleWord(true);
        oNoiDung.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JScrollPane cuonNd = new JScrollPane(oNoiDung);
        cuonNd.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuonNd.setAlignmentX(LEFT_ALIGNMENT);
        cuonNd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        the.add(cuonNd);
        the.add(Box.createVerticalStrut(8));
        nhanPhanHoi.setFont(KieuNV.FONT);
        nhanPhanHoi.setAlignmentX(LEFT_ALIGNMENT);
        the.add(nhanPhanHoi);
        the.add(Box.createVerticalStrut(6));
        nutGuiPhanHoi.setAlignmentX(LEFT_ALIGNMENT);
        nutGuiPhanHoi.setMaximumSize(new Dimension(260, 44));
        nutGuiPhanHoi.addActionListener(e -> guiPhanHoi());
        the.add(nutGuiPhanHoi);
        p.add(the, BorderLayout.NORTH);

        KieuNV.kieuBang(bangPhanHoi);
        bangPhanHoi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] rong = {140, 220, 380, 100};
        for (int i = 0; i < rong.length; i++) bangPhanHoi.getColumnModel().getColumn(i).setPreferredWidth(rong[i]);
        JScrollPane cuon = new JScrollPane(bangPhanHoi);
        cuon.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(KieuNV.VIEN), "Phản hồi đã gửi"));
        cuon.getViewport().setBackground(Color.WHITE);
        p.add(cuon, BorderLayout.CENTER);
        return p;
    }

    private void taiPhanHoi() {
        Long id = khachHang.get().getId();
        ChayNen.chay(() -> dich.layPhanHoiCuaToi(id), ds -> {
            dsPhanHoi.clear();
            dsPhanHoi.addAll(ds);
            modelPhanHoi.fireTableDataChanged();
        }, ex -> baoThongBao(nhanPhanHoi, ChayNen.thongDiep(ex), false));
    }

    private void guiPhanHoi() {
        Long id = khachHang.get().getId();
        String td = oTieuDe.getText();
        String nd = oNoiDung.getText();
        nutGuiPhanHoi.setEnabled(false);
        baoThongBao(nhanPhanHoi, " ", false);
        ChayNen.chay(() -> dich.guiPhanHoi(id, td, nd), p -> {
            nutGuiPhanHoi.setEnabled(true);
            oTieuDe.setText("");
            oNoiDung.setText("");
            baoThongBao(nhanPhanHoi, "Đã gửi phản hồi. Cảm ơn bạn đã góp ý!", true);
            taiPhanHoi();
        }, ex -> {
            nutGuiPhanHoi.setEnabled(true);
            baoThongBao(nhanPhanHoi, ChayNen.thongDiep(ex), false);
        });
    }

    // ================= TIỆN ÍCH GIAO DIỆN =================
    private static JPanel khungThe(String tieuDe) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(14, 18, 16, 18)));
        JLabel t = new JLabel(tieuDe);
        t.setFont(new Font("Segoe UI", Font.BOLD, 16));
        t.setForeground(KieuNV.CHU);
        t.setAlignmentX(LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private static JLabel nhan(String chu) {
        JLabel l = new JLabel(chu);
        l.setFont(KieuNV.DAM);
        l.setForeground(KieuNV.CHU);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static void baoThongBao(JLabel nhan, String noiDung, boolean thanhCong) {
        nhan.setForeground(thanhCong ? KieuNV.XANH : KieuNV.DO_SANG);
        nhan.setText(" ".equals(noiDung) ? " " : "<html>" + KieuNV.escape(noiDung) + "</html>");
    }

    // ================= Ô CHẤM SAO =================
    /** 5 ngôi sao bấm được (vẽ bằng đường nét nên không phụ thuộc phông chữ). Giá trị 0 = chưa chọn. */
    static final class OSao extends JComponent {
        private static final int CO = 30, KHOANG = 6;
        private int giaTri = 0;
        private int dangRe = 0;

        OSao() {
            Dimension d = new Dimension(5 * CO + 4 * KHOANG, CO + 4);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    if (!isEnabled()) return;
                    int s = saoTaiDiem(e.getX());
                    if (s > 0) setGiaTri(s);
                }
                @Override public void mouseExited(MouseEvent e) { dangRe = 0; repaint(); }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) { dangRe = isEnabled() ? saoTaiDiem(e.getX()) : 0; repaint(); }
            });
        }

        int getGiaTri() { return giaTri; }

        void setGiaTri(int v) {
            giaTri = Math.max(0, Math.min(5, v));
            repaint();
        }

        private int saoTaiDiem(int x) {
            int i = x / (CO + KHOANG);
            return i >= 0 && i < 5 && x - i * (CO + KHOANG) <= CO ? i + 1 : 0;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            Theme.khuDangCua(g);
            int hien = dangRe > 0 ? dangRe : giaTri;
            for (int i = 0; i < 5; i++) {
                Shape sao = hinhSao(i * (CO + KHOANG) + CO / 2.0, 2 + CO / 2.0, CO / 2.0);
                boolean dac = i < hien;
                g.setColor(!isEnabled() ? new Color(0xE5, 0xE7, 0xEB) : dac ? new Color(0xF5, 0xA6, 0x23) : Color.WHITE);
                g.fill(sao);
                g.setColor(!isEnabled() ? new Color(0xD1, 0xD5, 0xDB) : dac ? new Color(0xD9, 0x8A, 0x0B) : KieuNV.VIEN);
                g.setStroke(new BasicStroke(1.4f));
                g.draw(sao);
            }
            g.dispose();
        }

        private static Shape hinhSao(double tx, double ty, double r) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double goc = -Math.PI / 2 + i * Math.PI / 5;
                double ban = i % 2 == 0 ? r : r * 0.45;
                double x = tx + ban * Math.cos(goc), y = ty + ban * Math.sin(goc);
                if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }
    }

    // ================= MODEL BẢNG =================
    private final class DanhGiaModel extends AbstractTableModel {
        private final String[] cot = {"Sản phẩm", "Số sao", "Nhận xét", "Cập nhật lúc"};
        @Override public int getRowCount() { return dsDanhGia.size(); }
        @Override public int getColumnCount() { return cot.length; }
        @Override public String getColumnName(int c) { return cot[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Object getValueAt(int r, int c) {
            DanhGiaSanPham d = dsDanhGia.get(r);
            return switch (c) {
                case 0 -> TienIch.str(d.getSanPham().getTen());
                case 1 -> d.getSoSao() + " / 5 sao";
                case 2 -> TienIch.str(d.getNoiDung());
                default -> d.getNgayCapNhat().format(NGAY_GIO);
            };
        }
    }

    private final class PhanHoiModel extends AbstractTableModel {
        private final String[] cot = {"Ngày gửi", "Tiêu đề", "Nội dung", "Trạng thái"};
        @Override public int getRowCount() { return dsPhanHoi.size(); }
        @Override public int getColumnCount() { return cot.length; }
        @Override public String getColumnName(int c) { return cot[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Object getValueAt(int r, int c) {
            PhanHoi p = dsPhanHoi.get(r);
            return switch (c) {
                case 0 -> p.getNgayGui().format(NGAY_GIO);
                case 1 -> p.getTieuDe();
                case 2 -> p.getNoiDung();
                default -> p.getTrangThai();
            };
        }
    }
}
