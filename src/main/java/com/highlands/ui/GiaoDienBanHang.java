package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;

/** Thành phần trình bày chỉ dành cho Nhân viên và Khách hàng. */
final class GiaoDienBanHang {
    private GiaoDienBanHang() { }

    static JPanel theSanPham() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(new Theme.VienBo(Theme.LINE, 16),
                new EmptyBorder(12, 12, 12, 12)));
        p.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { vien(Theme.BRAND); }
            @Override public void mouseExited(MouseEvent e) { vien(Theme.LINE); }
            private void vien(Color c) {
                p.setBorder(BorderFactory.createCompoundBorder(new Theme.VienBo(c, 16),
                        new EmptyBorder(12, 12, 12, 12)));
            }
        });
        return p;
    }

    static JComponent loiChao(String ten, String moTa) {
        JPanel p = new JPanel(new BorderLayout(0, 5));
        p.setBackground(Theme.TOPBAR);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        JLabel t = new JLabel(ten);
        t.setFont(Theme.font(Font.BOLD, 22));
        t.setForeground(Color.WHITE);
        JLabel m = new JLabel(moTa);
        m.setFont(KieuNV.NHO);
        m.setForeground(Theme.SIDEBAR_TEXT);
        p.add(t, BorderLayout.NORTH);
        p.add(m, BorderLayout.SOUTH);
        return p;
    }

    static Icon bieuTuongMenu(String ten) {
        return new Icon() {
            public int getIconWidth() { return 20; }
            public int getIconHeight() { return 20; }
            public void paintIcon(Component c, Graphics g0, int x, int y) {
                Graphics2D g = (Graphics2D) g0.create();
                Theme.khuDangCua(g);
                g.translate(x, y);
                g.setColor(c.getForeground());
                g.setStroke(new BasicStroke(1.6f));
                if (ten.contains("Thông tin")) {
                    g.drawOval(6, 1, 8, 8);
                    g.drawArc(2, 11, 16, 14, 0, 180);
                } else if (ten.contains("Đánh giá")) {
                    g.drawRoundRect(1, 2, 17, 13, 4, 4);
                    g.drawLine(5, 15, 3, 19);
                    g.drawLine(3, 19, 10, 15);
                    g.drawLine(5, 7, 14, 7);
                } else {
                    g.drawRoundRect(2, 3, 16, 15, 4, 4);
                    g.drawLine(6, 1, 6, 5);
                    g.drawLine(14, 1, 14, 5);
                    g.drawLine(6, 9, 14, 9);
                    g.drawLine(6, 13, 12, 13);
                }
                g.dispose();
            }
        };
    }

    static JScrollPane thanhLoc(JComponent c) {
        JScrollPane s = Theme.kieuCuon(new JScrollPane(c,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setPreferredSize(new Dimension(0, c.getPreferredSize().height + 16));
        return s;
    }

    static void gioHang(JTable t) {
        TableCellRenderer tieuDe = t.getTableHeader().getDefaultRenderer();
        t.getTableHeader().setDefaultRenderer((bang, v, chon, focus, row, col) -> {
            Component c = tieuDe.getTableCellRendererComponent(bang, v, chon, focus, row, col);
            if (c instanceof JLabel l) {
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.LINE), new EmptyBorder(0, 5, 0, 5)));
                if (col == 1) l.setHorizontalAlignment(SwingConstants.CENTER);
            }
            return c;
        });
        TableCellRenderer cu = t.getColumnModel().getColumn(0).getCellRenderer();
        t.getColumnModel().getColumn(0).setCellRenderer((bang, v, chon, focus, row, col) -> {
            Component c = cu.getTableCellRendererComponent(bang, v, chon, focus, row, col);
            if (c instanceof JLabel l) {
                String text = l.getText();
                l.setToolTipText(text);
                int width = Math.max(60, bang.getColumnModel().getColumn(0).getWidth() - 14);
                if (text != null && text.startsWith("<html>")) {
                    l.setText("<html><div style='width:" + (width * 3 / 4) + "px'>"
                            + text.substring(6).replace("</html>", "") + "</div></html>");
                }
                int height = Math.max(64, l.getPreferredSize().height + 16);
                if (bang.getRowHeight(row) != height) bang.setRowHeight(row, height);
            }
            return c;
        });
    }

    /** Viewport quyết định chiều rộng, không ép card nhỏ hơn nội dung. */
    static final class LuoiSanPham extends JPanel implements Scrollable {
        LuoiSanPham() {
            super(new GridLayout(0, 1, 14, 14));
            setOpaque(false);
            addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent e) { doiCot(); }
            });
        }
        private void doiCot() {
            Insets i = getInsets();
            int cot = Math.max(1, (getWidth() - i.left - i.right + 14) / 234);
            GridLayout layout = (GridLayout) getLayout();
            if (layout.getColumns() != cot) { layout.setColumns(cot); revalidate(); }
        }
        @Override public Dimension getPreferredSize() {
            Insets i = getInsets();
            int cot = ((GridLayout) getLayout()).getColumns();
            int hang = (getComponentCount() + cot - 1) / cot;
            int height = 292;
            for (Component card : getComponents()) height = Math.max(height, card.getPreferredSize().height);
            return new Dimension(220, i.top + i.bottom + hang * height + Math.max(0, hang - 1) * 14);
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(460, 500); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(24, r.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** Chỉ gọi trên cây thành phần riêng, không áp dụng cho Quản lý. */
    static void hoanThien(Component c) {
        if (c instanceof JTextField t) {
            Font font = t.getFont();
            Theme.kieuO(t);
            t.setFont(font);
            t.setForeground(Theme.TEXT);
            t.setCaretColor(Theme.BRAND);
        }
        if (c instanceof JTable t) {
            t.setRowHeight(Math.max(56, t.getRowHeight()));
            t.setSelectionBackground(Theme.CHON);
        }
        if (c instanceof JScrollPane s) Theme.kieuCuon(s);
        if (c instanceof JButton b && !(b instanceof Nut)) b.setFont(KieuNV.DAM);
        if (c instanceof Container p) for (Component child : p.getComponents()) hoanThien(child);
    }
}
