package com.highlands.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "danh_muc")
public class DanhMuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten", columnDefinition = "NVARCHAR(100) NOT NULL")
    private String ten;

    @ColumnDefault("1")
    @Column(name = "dang_ban", nullable = false)
    private boolean dangBan = true;

    public boolean isDangBan() { return dangBan; }
    public void setDangBan(boolean dangBan) { this.dangBan = dangBan; }

    public DanhMuc() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTen() { return ten; }
    public void setTen(String ten) { this.ten = ten; }
}
