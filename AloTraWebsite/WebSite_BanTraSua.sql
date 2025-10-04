/* Tạo/Cập nhật CSDL */
IF DB_ID('Website_BanTraSua') IS NULL
BEGIN
  CREATE DATABASE Website_BanTraSua;
END;
GO
USE Website_BanTraSua;
GO

/* Xóa bảng theo thứ tự phụ thuộc (nếu đã tồn tại) */
IF OBJECT_ID('dbo.DanhGia','U') IS NOT NULL DROP TABLE dbo.DanhGia;
IF OBJECT_ID('dbo.CTDonHang_Topping','U') IS NOT NULL DROP TABLE dbo.CTDonHang_Topping;
IF OBJECT_ID('dbo.CTDonHang','U') IS NOT NULL DROP TABLE dbo.CTDonHang;
IF OBJECT_ID('dbo.DonHang','U') IS NOT NULL DROP TABLE dbo.DonHang;

IF OBJECT_ID('dbo.GioHangCT_Topping','U') IS NOT NULL DROP TABLE dbo.GioHangCT_Topping;
IF OBJECT_ID('dbo.GioHangCT','U') IS NOT NULL DROP TABLE dbo.GioHangCT;
IF OBJECT_ID('dbo.GioHang','U') IS NOT NULL DROP TABLE dbo.GioHang;

IF OBJECT_ID('dbo.KhuyenMaiSanPham','U') IS NOT NULL DROP TABLE dbo.KhuyenMaiSanPham;
IF OBJECT_ID('dbo.SuKienKhuyenMai','U') IS NOT NULL DROP TABLE dbo.SuKienKhuyenMai;

IF OBJECT_ID('dbo.ToppingApDungSanPham','U') IS NOT NULL DROP TABLE dbo.ToppingApDungSanPham;
IF OBJECT_ID('dbo.Topping','U') IS NOT NULL DROP TABLE dbo.Topping;

IF OBJECT_ID('dbo.BienTheSanPham','U') IS NOT NULL DROP TABLE dbo.BienTheSanPham;
IF OBJECT_ID('dbo.SizeSanPham','U') IS NOT NULL DROP TABLE dbo.SizeSanPham;

IF OBJECT_ID('dbo.SanPham','U') IS NOT NULL DROP TABLE dbo.SanPham;
IF OBJECT_ID('dbo.DanhMucSanPham','U') IS NOT NULL DROP TABLE dbo.DanhMucSanPham;

IF OBJECT_ID('dbo.NhanVien','U') IS NOT NULL DROP TABLE dbo.NhanVien;
IF OBJECT_ID('dbo.KhachHang','U') IS NOT NULL DROP TABLE dbo.KhachHang;
GO

/* Danh mục sản phẩm */
CREATE TABLE dbo.DanhMucSanPham (
    MaDM INT IDENTITY(1,1) PRIMARY KEY,
    TenDM NVARCHAR(255) NOT NULL UNIQUE,
    MoTa NVARCHAR(MAX) NULL,
    DeletedAt DATETIME2 NULL
);

CREATE TABLE dbo.SanPham (
    MaSP INT IDENTITY(1,1) PRIMARY KEY,
    MaDM INT NOT NULL,
    TenSP NVARCHAR(255) NOT NULL,
    MoTa NVARCHAR(MAX) NULL,
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1)), -- 0: Ngừng bán, 1: Đang bán
    UrlAnh NVARCHAR(500) NULL,
    DeletedAt DATETIME2 NULL,
    CONSTRAINT FK_SanPham_DanhMuc FOREIGN KEY (MaDM) REFERENCES dbo.DanhMucSanPham(MaDM)
);

/* Size và Biến thể sản phẩm (SP + Size -> giá bán) */
CREATE TABLE dbo.SizeSanPham (
    MaSize INT IDENTITY(1,1) PRIMARY KEY,
    TenSize NVARCHAR(10) NOT NULL UNIQUE, -- S, M, L...
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1))
);

CREATE TABLE dbo.BienTheSanPham (
    MaBT INT IDENTITY(1,1) PRIMARY KEY,
    MaSP INT NOT NULL,
    MaSize INT NOT NULL,
    GiaBan DECIMAL(10,2) NOT NULL CHECK (GiaBan > 0),
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1)),
    CONSTRAINT UQ_BienThe_Unique UNIQUE (MaSP, MaSize),
    CONSTRAINT FK_BienThe_SanPham FOREIGN KEY (MaSP) REFERENCES dbo.SanPham(MaSP),
    CONSTRAINT FK_BienThe_Size FOREIGN KEY (MaSize) REFERENCES dbo.SizeSanPham(MaSize)
);

/* Topping và (tùy chọn) Topping áp dụng theo sản phẩm */
CREATE TABLE dbo.Topping (
    MaTopping INT IDENTITY(1,1) PRIMARY KEY,
    TenTopping NVARCHAR(255) NOT NULL UNIQUE,
    GiaThem DECIMAL(10,2) NOT NULL CHECK (GiaThem >= 0),
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1)),
    UrlAnh NVARCHAR(500) NULL,
    DeletedAt DATETIME2 NULL
);

/* Khuyến mãi (giữ như cũ, áp theo Sản phẩm; có thể nâng cấp để áp theo Biến thể nếu cần) */
CREATE TABLE dbo.SuKienKhuyenMai (
    MaKM INT IDENTITY(1,1) PRIMARY KEY,
    TenSuKien NVARCHAR(255) NOT NULL,
    MoTa NVARCHAR(MAX) NULL,
    NgayBD DATE NOT NULL,
    NgayKT DATE NOT NULL,
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1)),
    CONSTRAINT CK_KM_Ngay CHECK (NgayKT >= NgayBD)
);

CREATE TABLE dbo.KhuyenMaiSanPham (
    MaKM INT NOT NULL,
    MaSP INT NOT NULL,
    PhanTramGiam INT NOT NULL CHECK (PhanTramGiam BETWEEN 1 AND 100),
    PRIMARY KEY (MaKM, MaSP),
    CONSTRAINT FK_KMSP_KM FOREIGN KEY (MaKM) REFERENCES dbo.SuKienKhuyenMai(MaKM),
    CONSTRAINT FK_KMSP_SP FOREIGN KEY (MaSP) REFERENCES dbo.SanPham(MaSP)
);

/* Người dùng: KH & NV (chứa luôn thông tin đăng nhập) */
CREATE TABLE dbo.KhachHang (
    MaKH INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    MatKhauHash NVARCHAR(255) NOT NULL,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    TenKH NVARCHAR(255) NOT NULL,
    SoDienThoai NVARCHAR(20) NULL UNIQUE,
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1)) -- 0: Khóa, 1: Hoạt động
);

CREATE TABLE dbo.NhanVien (
    MaNV INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    MatKhauHash NVARCHAR(255) NOT NULL,
    Email NVARCHAR(255) NOT NULL UNIQUE,
    TenNV NVARCHAR(255) NOT NULL,
    VaiTro TINYINT NOT NULL CHECK (VaiTro IN (1,2)), -- 1: Chủ cửa hàng, 2: Nhân viên
    SoDienThoai NVARCHAR(20) NULL UNIQUE,
    TrangThai TINYINT NOT NULL DEFAULT 1 CHECK (TrangThai IN (0,1)),
    DeletedAt DATETIME2 NULL
);

/* Giỏ hàng (1 khách tối đa 1 giỏ ACTIVE) */
CREATE TABLE dbo.GioHang (
    MaGH INT IDENTITY(1,1) PRIMARY KEY,
    MaKH INT NOT NULL,
    TrangThai NVARCHAR(20) NOT NULL DEFAULT N'ACTIVE' CHECK (TrangThai IN (N'ACTIVE', N'CHECKED_OUT', N'CANCELLED')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT FK_GH_KH FOREIGN KEY (MaKH) REFERENCES dbo.KhachHang(MaKH)
);
-- Mỗi KH chỉ 1 giỏ ACTIVE
CREATE UNIQUE INDEX UX_GioHang_Active_OnePerKH ON dbo.GioHang(MaKH) WHERE TrangThai = N'ACTIVE';

CREATE TABLE dbo.GioHangCT (
    MaCTGH INT IDENTITY(1,1) PRIMARY KEY,
    MaGH INT NOT NULL,
    MaBT INT NOT NULL, -- biến thể SP
    SoLuong INT NOT NULL CHECK (SoLuong > 0),
    DonGia DECIMAL(10,2) NOT NULL CHECK (DonGia >= 0),      -- snapshot giá biến thể
    ThanhTien DECIMAL(10,2) NOT NULL CHECK (ThanhTien >= 0),-- SoLuong * (DonGia + sum topping)
    GhiChu NVARCHAR(500) NULL,
    CONSTRAINT FK_GHCT_GH FOREIGN KEY (MaGH) REFERENCES dbo.GioHang(MaGH) ON DELETE CASCADE,
    CONSTRAINT FK_GHCT_BT FOREIGN KEY (MaBT) REFERENCES dbo.BienTheSanPham(MaBT)
);

CREATE TABLE dbo.GioHangCT_Topping (
    MaCTGH INT NOT NULL,
    MaTopping INT NOT NULL,
    SoLuong INT NOT NULL DEFAULT 1 CHECK (SoLuong > 0),
    DonGia DECIMAL(10,2) NOT NULL CHECK (DonGia >= 0), -- snapshot giá topping
    ThanhTien DECIMAL(10,2) NOT NULL CHECK (ThanhTien >= 0),
    PRIMARY KEY (MaCTGH, MaTopping),
    CONSTRAINT FK_GHCTT_GHCT FOREIGN KEY (MaCTGH) REFERENCES dbo.GioHangCT(MaCTGH) ON DELETE CASCADE,
    CONSTRAINT FK_GHCTT_T FOREIGN KEY (MaTopping) REFERENCES dbo.Topping(MaTopping)
);

/* Đơn hàng (gộp trạng thái thanh toán) */
CREATE TABLE dbo.DonHang (
    MaDH INT IDENTITY(1,1) PRIMARY KEY,
    MaKH INT NOT NULL,
    MaNV INT NULL, -- NV xử lý
    MaKM INT NULL, -- KM áp cho đơn (nếu có)
    NgayLap DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    TrangThaiDonHang NVARCHAR(30) NOT NULL DEFAULT N'ChoXuLy'
        CHECK (TrangThaiDonHang IN (N'ChoXuLy', N'DangPhaChe', N'DangGiao', N'DaGiao', N'DaHuy')),

    PaymentStatus NVARCHAR(30) NOT NULL DEFAULT N'ChuaThanhToan'
        CHECK (PaymentStatus IN (N'ChuaThanhToan', N'DaThanhToan')),

    PaymentMethod NVARCHAR(50) NULL, -- Tiền mặt, Momo, VNPay...
    PaidAt DATETIME2 NULL,

    TongHang DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (TongHang >= 0),
    GiamGiaDon DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (GiamGiaDon >= 0),
    PhiVanChuyen DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (PhiVanChuyen >= 0),
    TongThanhToan DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (TongThanhToan >= 0),

    GhiChu NVARCHAR(500) NULL,

    CONSTRAINT FK_DH_KH FOREIGN KEY (MaKH) REFERENCES dbo.KhachHang(MaKH),
    CONSTRAINT FK_DH_NV FOREIGN KEY (MaNV) REFERENCES dbo.NhanVien(MaNV),
    CONSTRAINT FK_DH_KM FOREIGN KEY (MaKM) REFERENCES dbo.SuKienKhuyenMai(MaKM)
);

CREATE TABLE dbo.CTDonHang (
    MaCT INT IDENTITY(1,1) PRIMARY KEY,
    MaDH INT NOT NULL,
    MaBT INT NOT NULL, -- biến thể (SP + Size)
    SoLuong INT NOT NULL CHECK (SoLuong > 0),
    DonGia DECIMAL(10,2) NOT NULL CHECK (DonGia >= 0),      -- snapshot giá biến thể
    GiamGiaDong DECIMAL(10,2) NOT NULL DEFAULT 0 CHECK (GiamGiaDong >= 0),
    ThanhTien DECIMAL(10,2) NOT NULL CHECK (ThanhTien >= 0),-- SoLuong*(DonGia + topping) - GiamGiaDong
    CONSTRAINT FK_CTDH_DH FOREIGN KEY (MaDH) REFERENCES dbo.DonHang(MaDH) ON DELETE CASCADE,
    CONSTRAINT FK_CTDH_BT FOREIGN KEY (MaBT) REFERENCES dbo.BienTheSanPham(MaBT)
);

CREATE TABLE dbo.CTDonHang_Topping (
    MaCT INT NOT NULL,
    MaTopping INT NOT NULL,
    SoLuong INT NOT NULL DEFAULT 1 CHECK (SoLuong > 0),
    DonGia DECIMAL(10,2) NOT NULL CHECK (DonGia >= 0), -- snapshot giá topping
    ThanhTien DECIMAL(10,2) NOT NULL CHECK (ThanhTien >= 0),
    PRIMARY KEY (MaCT, MaTopping),
    CONSTRAINT FK_CTDHT_CTDH FOREIGN KEY (MaCT) REFERENCES dbo.CTDonHang(MaCT) ON DELETE CASCADE,
    CONSTRAINT FK_CTDHT_T FOREIGN KEY (MaTopping) REFERENCES dbo.Topping(MaTopping)
);

/* Đánh giá: gắn với dòng đơn hàng đã mua (đảm bảo mua rồi mới được review) */
CREATE TABLE dbo.DanhGia (
    MaDG INT IDENTITY(1,1) PRIMARY KEY,
    MaKH INT NOT NULL,
    MaCT INT NOT NULL, -- dòng chi tiết đã mua
    SoSao INT NOT NULL CHECK (SoSao BETWEEN 1 AND 5),
    BinhLuan NVARCHAR(MAX) NULL,
    NgayDG DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT UQ_DanhGia_OnePerLine UNIQUE (MaKH, MaCT),
    CONSTRAINT FK_DG_KH FOREIGN KEY (MaKH) REFERENCES dbo.KhachHang(MaKH),
    CONSTRAINT FK_DG_CT FOREIGN KEY (MaCT) REFERENCES dbo.CTDonHang(MaCT)
);
GO

/* View tính tổng tiền động (tham khảo). Có thể dùng để cross-check DonHang.TongThanhToan */
IF OBJECT_ID('dbo.v_DonHang_TinhTong','V') IS NOT NULL DROP VIEW dbo.v_DonHang_TinhTong;
GO
CREATE VIEW dbo.v_DonHang_TinhTong
AS
SELECT
    dh.MaDH,
    SUM(ct.ThanhTien) AS TongHangDong,
    dh.GiamGiaDon,
    dh.PhiVanChuyen,
    (SUM(ct.ThanhTien) - dh.GiamGiaDon + dh.PhiVanChuyen) AS TongPhaiTra
FROM dbo.DonHang dh
JOIN dbo.CTDonHang ct ON ct.MaDH = dh.MaDH
GROUP BY dh.MaDH, dh.GiamGiaDon, dh.PhiVanChuyen;
GO