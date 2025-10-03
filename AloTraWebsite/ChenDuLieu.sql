use Website_BanTraSua
go

-- 1. Danh mục sản phẩm
INSERT INTO DanhMucSanPham (TenDM, MoTa) VALUES
(N'Trà sữa', N'Các loại trà sữa đặc biệt'),
(N'Trà trái cây', N'Trà kết hợp trái cây tươi'),
(N'Nước ép', N'Nước ép hoa quả nguyên chất');

-- 2. Sản phẩm
INSERT INTO SanPham (MaDM, TenSP, MoTa, TrangThai) VALUES
(1, N'Trà Sữa Truyền Thống', N'Trà sữa vị truyền thống', 1),
(1, N'Trà Sữa Matcha', N'Trà sữa vị matcha xanh', 1),
(2, N'Trà Đào Cam Sả', N'Trà đào cam sả thơm mát', 1),
(3, N'Nước ép cam', N'Nước ép cam tươi', 1);

-- 3. Size sản phẩm
INSERT INTO SizeSanPham (TenSize, TrangThai) VALUES
(N'S', 1),
(N'M', 1),
(N'L', 1);

-- 4. Biến thể sản phẩm (Sản phẩm + Size)
INSERT INTO BienTheSanPham (MaSP, MaSize, GiaBan, TrangThai) VALUES
(1, 1, 20000, 1),  -- Trà Sữa Truyền Thống, Size S
(1, 2, 25000, 1),  -- Trà Sữa Truyền Thống, Size M
(1, 3, 30000, 1),  -- Trà Sữa Truyền Thống, Size L
(2, 1, 22000, 1),  -- Trà Sữa Matcha, Size S
(2, 2, 27000, 1),  -- Trà Sữa Matcha, Size M
(2, 3, 32000, 1),  -- Trà Sữa Matcha, Size L
(3, 1, 25000, 1),  -- Trà Đào Cam Sả, Size S
(3, 2, 30000, 1),  -- Trà Đào Cam Sả, Size M
(3, 3, 35000, 1),  -- Trà Đào Cam Sả, Size L
(4, 1, 18000, 1),  -- Nước ép cam, Size S
(4, 2, 22000, 1),  -- Nước ép cam, Size M
(4, 3, 27000, 1);  -- Nước ép cam, Size L

-- 5. Topping
INSERT INTO Topping (TenTopping, GiaThem, TrangThai) VALUES
(N'Trân châu đen', 5000, 1),
(N'Trân châu trắng', 6000, 1),
(N'Thạch dừa', 4000, 1),
(N'Pudding trứng', 7000, 1);

-- 6. Sự kiện khuyến mãi
INSERT INTO SuKienKhuyenMai (TenSuKien, MoTa, NgayBD, NgayKT, TrangThai) VALUES
(N'Khuyến mãi mùa hè', N'Giảm giá các món trà sữa', '2025-07-01', '2025-09-30', 1),
(N'Sinh nhật cửa hàng', N'Ưu đãi toàn menu', '2025-10-01', '2025-10-10', 1);

-- 7. Khuyến mãi sản phẩm
INSERT INTO KhuyenMaiSanPham (MaKM, MaSP, PhanTramGiam) VALUES
(1, 1, 10),  -- KM mùa hè cho Trà Sữa Truyền Thống
(1, 2, 15),  -- KM mùa hè cho Trà Sữa Matcha
(2, 3, 20);  -- KM sinh nhật cho Trà Đào Cam Sả

-- 8. Khách hàng
INSERT INTO KhachHang (Username, MatKhauHash, Email, TenKH, SoDienThoai, TrangThai) VALUES
(N'khanhnguyen', N'123456hash', N'khanh@example.com', N'Nguyễn Khánh', N'0912345678', 1),
(N'linhpham', N'abcdefhash', N'linh@example.com', N'Phạm Linh', N'0987654321', 1);

-- 9. Nhân viên
INSERT INTO NhanVien (Username, MatKhauHash, Email, TenNV, VaiTro, SoDienThoai, TrangThai) VALUES
(N'admin', N'adminhash', N'admin@trasua.com', N'Nguyễn Quản Lý', 1, N'0900000001', 1),
(N'staff1', N'staffhash', N'staff1@trasua.com', N'Lê Nhân Viên', 2, N'0900000002', 1);

-- 10. Giỏ hàng (tạo giỏ hàng ACTIVE cho mỗi khách)
INSERT INTO GioHang (MaKH, TrangThai, CreatedAt, UpdatedAt) VALUES
(1, N'ACTIVE', SYSDATETIME(), SYSDATETIME()),
(2, N'ACTIVE', SYSDATETIME(), SYSDATETIME());

-- 11. Chi tiết giỏ hàng (giỏ 1 của Khách 1 đặt 1 món Trà Sữa Truyền Thống Size M)
INSERT INTO GioHangCT (MaGH, MaBT, SoLuong, DonGia, ThanhTien, GhiChu) VALUES
(1, 2, 2, 25000, 50000, N'Ít đá'),
(1, 5, 1, 27000, 27000, NULL);

-- 12. Topping của chi tiết giỏ hàng
INSERT INTO GioHangCT_Topping (MaCTGH, MaTopping, SoLuong, DonGia, ThanhTien) VALUES
(1, 1, 1, 5000, 5000),    -- Trân châu đen cho dòng 1
(1, 3, 1, 4000, 4000),    -- Thạch dừa cho dòng 1
(2, 2, 1, 6000, 6000);    -- Trân châu trắng cho dòng 2

-- 13. Đơn hàng (đơn của Khách 1, áp KM mùa hè, xử lý bởi admin)
INSERT INTO DonHang (MaKH, MaNV, MaKM, NgayLap, TrangThaiDonHang, PaymentStatus, PaymentMethod, PaidAt, TongHang, GiamGiaDon, PhiVanChuyen, TongThanhToan, GhiChu)
VALUES (1, 1, 1, SYSDATETIME(), N'ChoXuLy', N'ChuaThanhToan', N'Tiền mặt', NULL, 54000, 5000, 10000, 59000, N'Giao trước 17h');

-- 14. Chi tiết đơn hàng (dùng biến thể, snapshot giá)
INSERT INTO CTDonHang (MaDH, MaBT, SoLuong, DonGia, GiamGiaDong, ThanhTien) VALUES
(1, 2, 2, 25000, 2000, 48000),   -- 2 ly Trà Sữa Truyền Thống Size M, giá 25k, giảm 2k dòng, tổng 48k
(1, 5, 1, 27000, 0, 27000);      -- 1 ly Trà Sữa Matcha Size M, giá 27k, không giảm

-- 15. Topping cho chi tiết đơn hàng
INSERT INTO CTDonHang_Topping (MaCT, MaTopping, SoLuong, DonGia, ThanhTien) VALUES
(1, 1, 1, 5000, 5000),    -- Trân châu đen cho dòng 1
(1, 3, 1, 4000, 4000),    -- Thạch dừa cho dòng 1
(2, 2, 1, 6000, 6000);    -- Trân châu trắng cho dòng 2

-- 16. Đánh giá (Khách 1 đánh giá dòng 1 của đơn hàng)
INSERT INTO DanhGia (MaKH, MaCT, SoSao, BinhLuan, NgayDG) VALUES
(1, 1, 5, N'Ngon, đúng ý!', SYSDATETIME());

-- (Có thể thêm nhiều đơn hàng/giỏ/món/topping khác tương tự)