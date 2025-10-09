package com.alotra.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class CustomerOrderService {
    private final JdbcTemplate jdbc;

    public CustomerOrderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // List orders for a specific customer
    public List<OrderRow> listOrdersByCustomer(Integer customerId, String status) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT dh.MaDH, dh.NgayLap, dh.TrangThaiDonHang, dh.PaymentStatus, dh.PaymentMethod, dh.TongThanhToan,\n");
        sb.append(" dh.PhuongThucNhanHang, dh.TenNguoiNhan, dh.SDTNguoiNhan, dh.DiaChiNhanHang\n");
        sb.append("FROM DonHang dh WHERE dh.MaKH = ? ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        params.add(customerId);
        if (status != null && !status.isBlank()) {
            sb.append(" AND dh.TrangThaiDonHang = ?");
            params.add(status);
        }
        sb.append(" ORDER BY dh.MaDH DESC");
        return jdbc.query(sb.toString(), params.toArray(), ORDER_ROW_MAPPER);
    }

    // Ensure an order belongs to a customer then return it
    public OrderRow getOrderOfCustomer(Integer orderId, Integer customerId) {
        String sql = "SELECT dh.MaDH, dh.NgayLap, dh.TrangThaiDonHang, dh.PaymentStatus, dh.PaymentMethod, dh.TongThanhToan,\n" +
                " dh.PhuongThucNhanHang, dh.TenNguoiNhan, dh.SDTNguoiNhan, dh.DiaChiNhanHang\n" +
                "FROM DonHang dh WHERE dh.MaDH = ? AND dh.MaKH = ?";
        List<OrderRow> list = jdbc.query(sql, ORDER_ROW_MAPPER, orderId, customerId);
        return list.isEmpty() ? null : list.get(0);
    }

    // Order items with variant (product + size)
    public List<OrderItemRow> listOrderItems(Integer orderId) {
        String sql = "SELECT ct.MaCT, sp.TenSP, sz.TenSize, ct.SoLuong, ct.DonGia, ct.GiamGiaDong, ct.ThanhTien, ct.GhiChu\n" +
                "FROM CTDonHang ct\n" +
                "JOIN BienTheSanPham bt ON bt.MaBT = ct.MaBT\n" +
                "JOIN SanPham sp ON sp.MaSP = bt.MaSP\n" +
                "JOIN SizeSanPham sz ON sz.MaSize = bt.MaSize\n" +
                "WHERE ct.MaDH = ?\n" +
                "ORDER BY ct.MaCT";
        return jdbc.query(sql, ORDER_ITEM_ROW_MAPPER, orderId);
    }

    // Toppings per order item
    public List<ItemToppingRow> listOrderItemToppings(Integer orderItemId) {
        String sql = "SELECT t.TenTopping, dgt.SoLuong, dgt.DonGia, dgt.ThanhTien\n" +
                "FROM CTDonHang_Topping dgt\n" +
                "JOIN Topping t ON t.MaTopping = dgt.MaTopping\n" +
                "WHERE dgt.MaCT = ?\n" +
                "ORDER BY t.TenTopping";
        return jdbc.query(sql, ITEM_TOPPING_ROW_MAPPER, orderItemId);
    }

    // Fetch order header by orderId regardless of customer (for vendor view)
    public OrderRow getOrder(Integer orderId) {
        String sql = "SELECT dh.MaDH, dh.NgayLap, dh.TrangThaiDonHang, dh.PaymentStatus, dh.PaymentMethod, dh.TongThanhToan,\n" +
                " dh.PhuongThucNhanHang, dh.TenNguoiNhan, dh.SDTNguoiNhan, dh.DiaChiNhanHang FROM DonHang dh WHERE dh.MaDH = ?";
        List<OrderRow> list = jdbc.query(sql, ORDER_ROW_MAPPER, orderId);
        return list.isEmpty() ? null : list.get(0);
    }

    public static class OrderRow {
        public Integer id;
        public java.time.OffsetDateTime createdAt;
        public String status;
        public String paymentStatus;
        public String paymentMethod;
        public java.math.BigDecimal total;
        // New fields
        public String receivingMethod;
        public String receiverName;
        public String receiverPhone;
        public String shippingAddress;
    }

    public static class OrderItemRow {
        public Integer id; // MaCT
        public String productName;
        public String sizeName;
        public Integer quantity;
        public java.math.BigDecimal unitPrice;
        public java.math.BigDecimal lineDiscount;
        public java.math.BigDecimal lineTotal;
        public String note; // GhiChu: sugar/ice
    }

    public static class ItemToppingRow {
        public String toppingName;
        public Integer quantity;
        public java.math.BigDecimal unitPrice;
        public java.math.BigDecimal total;
    }

    private static final RowMapper<OrderRow> ORDER_ROW_MAPPER = new RowMapper<>() {
        @Override
        public OrderRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            OrderRow r = new OrderRow();
            r.id = rs.getInt("MaDH");
            java.sql.Timestamp ts = rs.getTimestamp("NgayLap");
            r.createdAt = ts != null ? ts.toInstant().atOffset(java.time.ZoneOffset.UTC) : null;
            r.status = rs.getString("TrangThaiDonHang");
            r.paymentStatus = rs.getString("PaymentStatus");
            r.paymentMethod = rs.getString("PaymentMethod");
            r.total = rs.getBigDecimal("TongThanhToan");
            // New mappings (defensive in case columns missing)
            try { r.receivingMethod = rs.getString("PhuongThucNhanHang"); } catch (SQLException ignore) {}
            try { r.receiverName = rs.getString("TenNguoiNhan"); } catch (SQLException ignore) {}
            try { r.receiverPhone = rs.getString("SDTNguoiNhan"); } catch (SQLException ignore) {}
            try { r.shippingAddress = rs.getString("DiaChiNhanHang"); } catch (SQLException ignore) {}
            return r;
        }
    };

    private static final RowMapper<OrderItemRow> ORDER_ITEM_ROW_MAPPER = new RowMapper<>() {
        @Override
        public OrderItemRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            OrderItemRow r = new OrderItemRow();
            r.id = rs.getInt("MaCT");
            r.productName = rs.getString("TenSP");
            r.sizeName = rs.getString("TenSize");
            r.quantity = rs.getInt("SoLuong");
            r.unitPrice = rs.getBigDecimal("DonGia");
            r.lineDiscount = rs.getBigDecimal("GiamGiaDong");
            r.lineTotal = rs.getBigDecimal("ThanhTien");
            try { r.note = rs.getString("GhiChu"); } catch (SQLException ignore) { r.note = null; }
            return r;
        }
    };

    private static final RowMapper<ItemToppingRow> ITEM_TOPPING_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ItemToppingRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            ItemToppingRow r = new ItemToppingRow();
            r.toppingName = rs.getString("TenTopping");
            r.quantity = rs.getInt("SoLuong");
            r.unitPrice = rs.getBigDecimal("DonGia");
            r.total = rs.getBigDecimal("ThanhTien");
            return r;
        }
    };
}