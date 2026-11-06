package com.highlands.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

// Hộp thoại chọn phân hệ khi mở app: Quản lý hoặc Nhân viên
public final class ChonVaiTro extends JDialog {

    public enum VaiTro { QUAN_LY, NHAN_VIEN }

    private VaiTro chon = null;

    private ChonVaiTro() {
        super((Frame) null, "Highlands Coffee", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel goc = new JPanel(new BorderLayout(0, 18));
        goc.setBackground(Color.WHITE);
        goc.setBorder(new EmptyBorder(26, 34, 26, 34));

        JPanel dau = new JPanel();
        dau.setOpaque(false);
        dau.setLayout(new BoxLayout(dau, BoxLayout.Y_AXIS));
        LogoTron logo = new LogoTron(64);
        logo.setAlignmentX(CENTER_ALIGNMENT);
        JLabel ten = Theme.tieuDe("Highlands Coffee", 22);
        ten.setForeground(KieuNV.DO);
        ten.setAlignmentX(CENTER_ALIGNMENT);
        JLabel phu = Theme.nhan("Bạn vào hệ thống với vai trò nào?");
        phu.setAlignmentX(CENTER_ALIGNMENT);
        dau.add(logo);
        dau.add(Box.createVerticalStrut(10));
        dau.add(ten);
        dau.add(Box.createVerticalStrut(4));
        dau.add(phu);
        goc.add(dau, BorderLayout.NORTH);

        Nut quanLy = new Nut("Quản lý", Nut.Kieu.THUONG);
        Nut nhanVien = new Nut("Nhân viên", Nut.Kieu.CHINH);
        quanLy.addActionListener(e -> dong(VaiTro.QUAN_LY));
        nhanVien.addActionListener(e -> dong(VaiTro.NHAN_VIEN));
        JPanel nut = new JPanel(new GridLayout(1, 2, 12, 0));
        nut.setOpaque(false);
        nut.add(quanLy);
        nut.add(nhanVien);
        nut.setPreferredSize(new Dimension(320, 46));
        goc.add(nut, BorderLayout.CENTER);

        setContentPane(goc);
        getRootPane().setDefaultButton(nhanVien);
        pack();
        setLocationRelativeTo(null);
    }

    private void dong(VaiTro v) {
        chon = v;
        dispose();
    }

    // Hiện hộp thoại và chờ chọn. Đóng cửa sổ mà chưa chọn thì trả về null
    public static VaiTro hien() {
        ChonVaiTro d = new ChonVaiTro();
        d.setVisible(true);
        return d.chon;
    }
}
