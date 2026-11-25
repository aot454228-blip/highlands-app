package com.highlands.ui;

import com.highlands.entity.KhachHang;
import com.highlands.entity.Topping;
import com.highlands.entity.DanhMuc;
import com.highlands.service.BanHangService;
import com.highlands.service.BanHangService.MonThucDon;
import com.highlands.service.BanHangService.ThucDon;
import com.highlands.service.DatHangService;
import com.highlands.service.DongGio;
import com.highlands.service.ThanhToanService.HoaDon;
import com.highlands.service.TienIch;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;

/**
 * Trang Đặt hàng của khách: trái = thực đơn (chọn sản phẩm), phải = giỏ hàng (đổi số lượng, xóa)
 * + mã khuyến mãi + nút Đặt hàng (mở hộp thoại thanh toán tiền mặt / chuyển khoản).
 */
public class TrangDatHang extends JPanel {
    private final BanHangService banHang;
    private final DatHangService datHang;
    private final Supplier<KhachHang> khachHang;
    private final Runnable sauKhiDatHang;

    // --- thực đơn ---
    private List<MonThucDon> thucDon = List.of();
    private ThucDon thucDonDaTai;
    private List<Topping> dsTopping = List.of();
    private final JButton lamMoiThucDon = KieuNV.nutPhu("Làm mới thực đơn");
    private final Timer tuLamMoi = new Timer(15000, e -> {
        if (isShowing()) taiThucDon(false);
    });
    private boolean dangTaiThucDon;

    @Override public void addNotify() {
        super.addNotify();
        tuLamMoi.start();
    }

    @Override public void removeNotify() {
        tuLamMoi.stop();
        super.removeNotify();
    }
    private Object khoaDanhMuc = null;               // null = tất cả
    private final JTextField oTim = new ONhap("Tên sản phẩm...");
    private final JPanel hangDanhMuc = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    private final ButtonGroup nhomDanhMuc = new ButtonGroup();
    private final JPanel luoi = new GiaoDienBanHang.LuoiSanPham();
    private final JLabel nhanTrangThai = new JLabel(" ");
    private final Map<String, ImageIcon> boNhoAnh = new HashMap<>();

    // --- giỏ hàng ---
    private final List<DongGio> gio = new ArrayList<>();
    private final GioModel model = new GioModel();
    private final JTable bang = new JTable(model);
    private final JTextField oMa = new ONhap("Mã khuyến mãi");
    private final JLabel nhanKm = new JLabel(" ");
    private final JLabel lTienHang = new JLabel();
    private final JLabel lGiam = new JLabel();
    private final JLabel lTong = new JLabel();
    private final JButton nutDatHang = KieuNV.nut("ĐẶT HÀNG");
    private String maApDung = null;
    private BigDecimal giam = BigDecimal.ZERO;
    private int soLanKiemTra = 0;
    private String maGiaoDich = UUID.randomUUID().toString();

    /** sauKhiDatHang: chạy sau khi đặt thành công (để form chính làm mới danh sách đơn). */
    public TrangDatHang(BanHangService banHang, DatHangService datHang, Supplier<KhachHang> khachHang,
                        Runnable sauKhiDatHang) {
        this.banHang = banHang;
        this.datHang = datHang;
        this.khachHang = khachHang;
        this.sauKhiDatHang = sauKhiDatHang;
        setLayout(new BorderLayout(14, 14));
        setBackground(KieuNV.NEN);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel noiDung = new JPanel(new BorderLayout(14, 14));
        noiDung.setOpaque(false);
        noiDung.add(taoThucDon(), BorderLayout.CENTER);
        noiDung.add(taoGioHang(), BorderLayout.EAST);
        add(noiDung, BorderLayout.CENTER);
        add(GiaoDienBanHang.loiChao("Một chút Highlands, trọn ngày hứng khởi",
                "Chọn món yêu thích và theo dõi đơn hàng ngay tại đây."), BorderLayout.NORTH);
        capNhatTong();
        taiThucDon(false);
    }

    // ================= CHỌN SẢN PHẨM =================
    private JComponent taoThucDon() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);

        JPanel tren = new JPanel(new BorderLayout(0, 6));
        tren.setBackground(Color.WHITE);
        tren.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(10, 12, 6, 12)));
        JLabel tieuDe = new JLabel("Thực đơn");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        JPanel hangTim = new JPanel(new BorderLayout(10, 0));
        hangTim.setOpaque(false);
        hangTim.add(tieuDe, BorderLayout.WEST);
        oTim.setFont(KieuNV.FONT);
        oTim.setPreferredSize(new Dimension(180, 40));
        oTim.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 10, 2, 10)));
        oTim.putClientProperty("JTextField.placeholderText", "Tìm sản phẩm...");
        oTim.getDocument().addDocumentListener(new TrangBanHang.SuKienGoPhim(this::veLuoi));
        JPanel boTim = new JPanel(new BorderLayout(6, 0));
        boTim.setOpaque(false);
        boTim.add(new JLabel("Tìm kiếm:"), BorderLayout.WEST);
        boTim.add(oTim, BorderLayout.CENTER);
        lamMoiThucDon.addActionListener(e -> taiThucDon(true));
        boTim.add(lamMoiThucDon, BorderLayout.EAST);
        hangTim.add(boTim, BorderLayout.CENTER);
        hangDanhMuc.setOpaque(false);
        JScrollPane cuonDm = new JScrollPane(hangDanhMuc, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cuonDm.setBorder(null);
        cuonDm.setOpaque(false);
        cuonDm.getViewport().setOpaque(false);
        tren.add(hangTim, BorderLayout.NORTH);
        tren.setPreferredSize(new Dimension(0, 112));
        tren.add(cuonDm, BorderLayout.CENTER);

        luoi.setOpaque(false);
        JScrollPane cuon = Theme.kieuCuon(new JScrollPane(luoi));
        cuon.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuon.getViewport().setBackground(KieuNV.NEN);
        cuon.getVerticalScrollBar().setUnitIncrement(24);
        luoi.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        nhanTrangThai.setFont(KieuNV.NHO);
        nhanTrangThai.setForeground(KieuNV.CHU_PHU);
        p.add(tren, BorderLayout.NORTH);
        p.add(cuon, BorderLayout.CENTER);
        p.add(nhanTrangThai, BorderLayout.SOUTH);
        return p;
    }

    private void taiThucDon(boolean thuCong) {
        if (dangTaiThucDon) return;
        dangTaiThucDon = true;
        lamMoiThucDon.setEnabled(false);
        if (thucDon.isEmpty() || thuCong) nhanTrangThai.setText("Đang tải thực đơn...");
        ChayNen.chay(banHang::taiThucDon, kq -> {
            dangTaiThucDon = false;
            lamMoiThucDon.setEnabled(true);
            dsTopping = kq.topping();
            if (!kq.cungLuoi(thucDonDaTai)) {
                thucDon = kq.mon();
                dungDanhMuc(kq.danhMuc());
                veLuoi();
            } else {
                hienSoMon(luoi.getComponentCount());
            }
            thucDonDaTai = kq;
            int dongChon = bang.getSelectedRow();
            if (kq.capNhatGio(gio)) {
                capNhatTienGio();
                if (dongChon >= 0) bang.setRowSelectionInterval(dongChon, dongChon);
            }
        }, ex -> {
            dangTaiThucDon = false;
            lamMoiThucDon.setEnabled(true);
            nhanTrangThai.setText("Không tải được thực đơn.");
            if (thuCong && isShowing()) KieuNV.loi(this, ChayNen.thongDiep(ex));
        });
    }

    private void dungDanhMuc(List<DanhMuc> ds) {
        hangDanhMuc.removeAll();
        while (nhomDanhMuc.getButtonCount() > 0)
            nhomDanhMuc.remove(nhomDanhMuc.getElements().nextElement());
        if (khoaDanhMuc != null && ds.stream().noneMatch(d -> d.getId().equals(khoaDanhMuc)))
            khoaDanhMuc = null;
        hangDanhMuc.add(taoChip("Tất cả", null, khoaDanhMuc == null));
        for (var d : ds) hangDanhMuc.add(taoChip(d.getTen(), d.getId(), d.getId().equals(khoaDanhMuc)));
        hangDanhMuc.revalidate();
        hangDanhMuc.repaint();
    }

    private JToggleButton taoChip(String ten, Object khoa, boolean chon) {
        JToggleButton b = new JToggleButton(ten, chon) {
            @Override public Color getForeground() { return isSelected() ? Color.WHITE : KieuNV.DO; }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(isSelected() ? KieuNV.DO_SANG : Color.WHITE);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g.setColor(KieuNV.DO_SANG);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        b.setFont(KieuNV.DAM);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        b.addActionListener(e -> { khoaDanhMuc = khoa; veLuoi(); });
        nhomDanhMuc.add(b);
        return b;
    }

    private void veLuoi() {
        String tuKhoa = TienIch.boDau(oTim.getText());
        luoi.removeAll();
        int dem = 0;
        for (MonThucDon m : thucDon) {
            if (khoaDanhMuc != null && !Objects.equals(khoaDanhMuc, m.khoaDanhMuc())) continue;
            if (!tuKhoa.isEmpty() && !TienIch.boDau(TienIch.str(m.sanPham().getTen())).contains(tuKhoa)) continue;
            luoi.add(taoThe(m));
            dem++;
        }
        hienSoMon(dem);
        luoi.revalidate();
        luoi.repaint();
    }

    private void hienSoMon(int dem) {
        nhanTrangThai.setText(dem == 0 ? "Không có sản phẩm phù hợp." : dem + " sản phẩm");
    }

    private JComponent taoThe(MonThucDon m) {
        String ten = TienIch.str(m.sanPham().getTen());
        JPanel p = GiaoDienBanHang.theSanPham();

        JLabel anh = new JLabel();
        anh.setHorizontalAlignment(SwingConstants.CENTER);
        anh.setPreferredSize(new Dimension(180, 130));
        napAnh(anh, TienIch.str(m.sanPham().getHinhAnh()), ten);

        JPanel giua = new JPanel();
        giua.setOpaque(false);
        giua.setLayout(new BoxLayout(giua, BoxLayout.Y_AXIS));
        JLabel nTen = new JLabel("<html><div style='width:132px'>" + KieuNV.escape(ten) + "</div></html>");
        nTen.setFont(KieuNV.DAM);
        nTen.setToolTipText(ten);
        nTen.setForeground(KieuNV.CHU);
        nTen.setAlignmentX(LEFT_ALIGNMENT);
        JLabel nGia = new JLabel(KieuNV.tien(TienIch.bd(m.sanPham().getGia())));
        nGia.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nGia.setForeground(Theme.BRAND);
        nGia.setAlignmentX(LEFT_ALIGNMENT);
        giua.add(nTen);
        giua.add(Box.createVerticalStrut(4));
        giua.add(nGia);

        JButton them = KieuNV.nut("+ Thêm vào giỏ");
        them.addActionListener(e -> chonSanPham(m));
        p.add(anh, BorderLayout.NORTH);
        p.add(giua, BorderLayout.CENTER);
        p.add(them, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(206, Math.max(292, p.getPreferredSize().height)));
        return p;
    }

    private void napAnh(JLabel nhan, String duongDan, String ten) {
        String khoa = duongDan + "|" + ten;
        ImageIcon co = boNhoAnh.get(khoa);
        if (co != null) { nhan.setIcon(co); return; }
        ChayNen.chay(() -> KieuNV.taiAnh(duongDan, 180, 130, ten), img -> {
            ImageIcon ic = new ImageIcon(img);
            boNhoAnh.put(khoa, ic);
            nhan.setIcon(ic);
        }, ex -> { });
    }

    // ================= GIỎ HÀNG =================
    private JComponent taoGioHang() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Color.WHITE);
        p.setPreferredSize(new Dimension(420, 0));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JLabel tieuDe = new JLabel("Giỏ hàng của bạn");
        tieuDe.setFont(KieuNV.TIEU_DE);
        tieuDe.setForeground(KieuNV.DO);
        p.add(tieuDe, BorderLayout.NORTH);

        KieuNV.kieuBang(bang);
        bang.setRowHeight(50);
        bang.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bang.getColumnModel().getColumn(0).setPreferredWidth(170);
        bang.getColumnModel().getColumn(1).setPreferredWidth(36);
        bang.getColumnModel().getColumn(2).setPreferredWidth(84);
        bang.getColumnModel().getColumn(3).setPreferredWidth(96);
        bang.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, "", s, f, r, c);
                DongGio d = gio.get(r);
                String tp = d.getTopping().isEmpty() ? "" : "<br><span style='color:#69778C;font-size:10px'>+ "
                        + KieuNV.escape(d.moTaTopping()) + "</span>";
                setText("<html><b>" + KieuNV.escape(TienIch.str(d.getSanPham().getTen())) + "</b>" + tp + "</html>");
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
                return this;
            }
        });
        DefaultTableCellRenderer giua = new DefaultTableCellRenderer();
        giua.setHorizontalAlignment(SwingConstants.CENTER);
        bang.getColumnModel().getColumn(1).setCellRenderer(giua);
        bang.getColumnModel().getColumn(2).setCellRenderer(KieuNV.canPhai());
        bang.getColumnModel().getColumn(3).setCellRenderer(KieuNV.canPhai());
        GiaoDienBanHang.gioHang(bang);
        JScrollPane cuon = new JScrollPane(bang);
        cuon.setBorder(BorderFactory.createLineBorder(KieuNV.VIEN));
        cuon.getViewport().setBackground(Color.WHITE);

        // Quản lý giỏ hàng: thay đổi số lượng / xóa khỏi giỏ hàng
        JPanel hangNut = new JPanel(new GridLayout(1, 4, 6, 0));
        hangNut.setOpaque(false);
        JButton giamSl = KieuNV.nutPhu("−");
        JButton tangSl = KieuNV.nutPhu("+");
        JButton xoa = KieuNV.nutPhu("Xóa món");
        JButton xoaHet = KieuNV.nutPhu("Xóa hết");
        giamSl.addActionListener(e -> doiSoLuong(-1));
        tangSl.addActionListener(e -> doiSoLuong(+1));
        xoa.addActionListener(e -> xoaDong());
        xoaHet.addActionListener(e -> xoaTatCa());
        hangNut.add(giamSl); hangNut.add(tangSl); hangNut.add(xoa); hangNut.add(xoaHet);

        JPanel giuaP = new JPanel(new BorderLayout(0, 6));
        giuaP.setOpaque(false);
        giuaP.add(cuon, BorderLayout.CENTER);
        giuaP.add(hangNut, BorderLayout.SOUTH);
        p.add(giuaP, BorderLayout.CENTER);

        // khuyến mãi + tổng tiền
        JPanel duoi = new JPanel();
        duoi.setOpaque(false);
        duoi.setLayout(new BoxLayout(duoi, BoxLayout.Y_AXIS));
        JPanel hangKm = new JPanel(new BorderLayout(6, 0));
        hangKm.setOpaque(false);
        oMa.setFont(KieuNV.FONT);
        oMa.setPreferredSize(new Dimension(150, 34));
        oMa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(KieuNV.VIEN), BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        oMa.putClientProperty("JTextField.placeholderText", "Mã khuyến mãi");
        oMa.addActionListener(e -> apDungMa());
        JButton apDung = KieuNV.nut("Áp dụng");
        JButton bo = KieuNV.nutPhu("Bỏ");
        apDung.addActionListener(e -> apDungMa());
        bo.addActionListener(e -> boMa());
        JPanel nutKm = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        nutKm.setOpaque(false);
        nutKm.add(apDung); nutKm.add(bo);
        hangKm.add(oMa, BorderLayout.CENTER);
        hangKm.add(nutKm, BorderLayout.EAST);
        hangKm.setAlignmentX(LEFT_ALIGNMENT);
        hangKm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        nhanKm.setFont(KieuNV.NHO);
        nhanKm.setAlignmentX(LEFT_ALIGNMENT);
        duoi.add(hangKm);
        duoi.add(Box.createVerticalStrut(3));
        duoi.add(nhanKm);
        duoi.add(Box.createVerticalStrut(6));
        duoi.add(hangTong("Tiền hàng", lTienHang, false));
        duoi.add(hangTong("Khuyến mãi", lGiam, false));
        duoi.add(hangTong("TỔNG CỘNG", lTong, true));
        duoi.add(Box.createVerticalStrut(8));
        nutDatHang.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nutDatHang.setAlignmentX(LEFT_ALIGNMENT);
        nutDatHang.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        nutDatHang.addActionListener(e -> xacNhanDatHang());
        duoi.add(nutDatHang);
        p.add(duoi, BorderLayout.SOUTH);
        return p;
    }

    private JComponent hangTong(String ten, JLabel gia, boolean lon) {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JLabel t = new JLabel(ten);
        t.setFont(lon ? new Font("Segoe UI", Font.BOLD, 17) : KieuNV.FONT);
        gia.setFont(lon ? new Font("Segoe UI", Font.BOLD, 22) : KieuNV.DAM);
        gia.setForeground(lon ? KieuNV.DO : KieuNV.CHU);
        h.add(t, BorderLayout.WEST);
        h.add(gia, BorderLayout.EAST);
        h.setAlignmentX(LEFT_ALIGNMENT);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, lon ? 36 : 26));
        return h;
    }

    private void chonSanPham(MonThucDon m) {
        if (dsTopping.isEmpty()) {
            themVaoGio(m, List.of(), 1);
            return;
        }
        HopThoaiTopping hop = new HopThoaiTopping(SwingUtilities.getWindowAncestor(this), m.sanPham(), dsTopping);
        hop.setVisible(true);
        if (hop.daChon()) themVaoGio(m, hop.getTopping(), hop.getSoLuong());
    }

    private void themVaoGio(MonThucDon m, List<Topping> tp, int sl) {
        for (DongGio d : gio) {
            if (d.cungLoai(m.sanPham(), tp)) {
                d.setSoLuong(Math.min(99, d.getSoLuong() + sl));
                thayDoiGio();
                return;
            }
        }
        gio.add(new DongGio(m.sanPham(), tp, sl));
        thayDoiGio();
        bang.setRowSelectionInterval(gio.size() - 1, gio.size() - 1);
    }

    // Thay đổi số lượng của dòng đang chọn; giảm về 0 thì bỏ khỏi giỏ
    private void doiSoLuong(int delta) {
        int r = bang.getSelectedRow();
        if (r < 0) { KieuNV.thongTin(this, "Vui lòng chọn một món trong giỏ hàng."); return; }
        DongGio d = gio.get(r);
        int moi = d.getSoLuong() + delta;
        if (moi <= 0) { gio.remove(r); thayDoiGio(); return; }
        d.setSoLuong(Math.min(99, moi));
        thayDoiGio();
        bang.setRowSelectionInterval(r, r);
    }

    private void xoaDong() {
        int r = bang.getSelectedRow();
        if (r < 0) { KieuNV.thongTin(this, "Vui lòng chọn một món trong giỏ hàng."); return; }
        gio.remove(r);
        thayDoiGio();
    }

    private void xoaTatCa() {
        if (gio.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Xóa toàn bộ giỏ hàng?", "Xóa giỏ hàng",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        gio.clear();
        thayDoiGio();
    }

    /** Mọi thay đổi giỏ đều đổi mã giao dịch (tránh nhầm với lần đặt lỗi trước đó) và tính lại tiền. */
    private void thayDoiGio() {
        maGiaoDich = UUID.randomUUID().toString();
        capNhatTienGio();
    }

    private void capNhatTienGio() {
        model.fireTableDataChanged();
        if (gio.isEmpty()) boMa();
        else if (maApDung != null) kiemTraLaiMa();
        capNhatTong();
    }

    private BigDecimal tienHang() {
        BigDecimal t = BigDecimal.ZERO;
        for (DongGio d : gio) t = t.add(d.thanhTien());
        return t;
    }

    private void capNhatTong() {
        BigDecimal th = tienHang();
        BigDecimal g = giam.min(th);
        lTienHang.setText(KieuNV.tien(th));
        lGiam.setText(g.signum() > 0 ? "-" + KieuNV.tien(g) : KieuNV.tien(BigDecimal.ZERO));
        lTong.setText(KieuNV.tien(th.subtract(g)));
        nutDatHang.setEnabled(!gio.isEmpty());
    }

    // ================= KHUYẾN MÃI =================
    private void apDungMa() {
        String ma = oMa.getText().trim();
        BigDecimal th = tienHang();
        int lan = ++soLanKiemTra;
        nhanKm.setForeground(KieuNV.CHU_PHU);
        nhanKm.setText("Đang kiểm tra...");
        ChayNen.chay(() -> banHang.kiemTraKhuyenMai(ma, th), kq -> {
            if (lan != soLanKiemTra) return;
            if (kq.hopLe()) {
                maApDung = ma;
                giam = kq.giam();
                nhanKm.setForeground(KieuNV.XANH);
                nhanKm.setText("Đã áp dụng: " + kq.ten() + " (-" + KieuNV.tien(kq.giam()) + ")");
            } else {
                maApDung = null;
                giam = BigDecimal.ZERO;
                nhanKm.setForeground(KieuNV.DO_SANG);
                nhanKm.setText(kq.thongBao());
            }
            capNhatTong();
        }, ex -> {
            if (lan != soLanKiemTra) return;
            nhanKm.setForeground(KieuNV.DO_SANG);
            nhanKm.setText(ChayNen.thongDiep(ex));
        });
    }

    private void kiemTraLaiMa() {
        String ma = maApDung;
        BigDecimal th = tienHang();
        int lan = ++soLanKiemTra;
        ChayNen.chay(() -> banHang.kiemTraKhuyenMai(ma, th), kq -> {
            if (lan != soLanKiemTra) return;
            if (kq.hopLe()) {
                giam = kq.giam();
                nhanKm.setText("Đã áp dụng: " + kq.ten() + " (-" + KieuNV.tien(kq.giam()) + ")");
            } else {
                maApDung = null;
                giam = BigDecimal.ZERO;
                nhanKm.setForeground(KieuNV.DO_SANG);
                nhanKm.setText(kq.thongBao() + " Mã đã được gỡ.");
            }
            capNhatTong();
        }, ex -> { });
    }

    private void boMa() {
        soLanKiemTra++;
        maApDung = null;
        giam = BigDecimal.ZERO;
        oMa.setText("");
        nhanKm.setText(" ");
        capNhatTong();
    }

    // ================= ĐẶT HÀNG =================
    private void xacNhanDatHang() {
        if (gio.isEmpty()) { KieuNV.thongTin(this, "Giỏ hàng đang trống."); return; }
        BigDecimal th = tienHang();
        BigDecimal g = giam.min(th);
        Window cha = SwingUtilities.getWindowAncestor(this);
        HopThoaiThanhToanKhach hop = new HopThoaiThanhToanKhach(cha, datHang, khachHang.get(),
                new ArrayList<>(gio), maApDung, th, g, maGiaoDich);
        hop.setVisible(true);
        HoaDon hd = hop.getKetQua();
        if (hd == null) return;                    // hủy hoặc lỗi: giữ nguyên giỏ hàng
        gio.clear();
        model.fireTableDataChanged();
        boMa();
        maGiaoDich = UUID.randomUUID().toString();
        sauKhiDatHang.run();
        new HopThoaiHoaDon(cha, hd, "Khách hàng:", false).setVisible(true);
    }

    // ================= MODEL BẢNG GIỎ =================
    private final class GioModel extends AbstractTableModel {
        private final String[] cot = {"Sản phẩm", "SL", "Đơn giá", "Thành tiền"};
        @Override public int getRowCount() { return gio.size(); }
        @Override public int getColumnCount() { return cot.length; }
        @Override public String getColumnName(int c) { return cot[c]; }
        @Override public boolean isCellEditable(int r, int c) { return false; }
        @Override public Object getValueAt(int r, int c) {
            DongGio d = gio.get(r);
            return switch (c) {
                case 0 -> TienIch.str(d.getSanPham().getTen());
                case 1 -> d.getSoLuong();
                case 2 -> KieuNV.tien(d.donGia());
                default -> KieuNV.tien(d.thanhTien());
            };
        }
    }
}
