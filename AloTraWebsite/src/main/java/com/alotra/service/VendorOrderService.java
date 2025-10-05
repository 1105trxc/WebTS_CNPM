package com.alotra.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class VendorOrderService {
    private final JdbcTemplate jdbc;

    public VendorOrderService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> getDashboardCounts() {
        Map<String, Object> m = new HashMap<>();
        m.put("pending", countByStatus("ChoXuLy"));
        m.put("preparing", countByStatus("DangPhaChe"));
        m.put("shipping", countByStatus("DangGiao"));
        // Today orders by date only (UTC)
        String sqlToday = "SELECT COUNT(*) FROM DonHang WHERE CONVERT(date, NgayLap) = CONVERT(date, SYSUTCDATETIME())";
        Integer today = jdbc.queryForObject(sqlToday, Integer.class);
        m.put("today", today == null ? 0 : today);
        return m;
    }

    public int countByStatus(String status) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM DonHang WHERE TrangThaiDonHang = ?", Integer.class, status);
        return n == null ? 0 : n;
    }

    public List<OrderRow> listOrders(String status, String kw, Integer limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT TOP ");
        sb.append(limit != null && limit > 0 ? limit : 50);
        sb.append(" dh.MaDH, dh.NgayLap, dh.TrangThaiDonHang, dh.PaymentStatus, dh.TongThanhToan, kh.TenKH, kh.SoDienThoai\n");
        sb.append("FROM DonHang dh JOIN KhachHang kh ON kh.MaKH = dh.MaKH WHERE 1=1 ");
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (status != null && !status.isBlank()) {
            sb.append(" AND dh.TrangThaiDonHang = ?");
            params.add(status);
        }
        if (kw != null && !kw.isBlank()) {
            sb.append(" AND (LOWER(kh.TenKH) LIKE LOWER(?) OR kh.SoDienThoai LIKE ?) ");
            String like = "%" + kw.trim() + "%";
            params.add(like);
            params.add(like);
        }
        sb.append(" ORDER BY dh.MaDH DESC");
        return jdbc.query(sb.toString(), params.toArray(), ORDER_ROW_MAPPER);
    }

    public void updateStatus(Integer id, String newStatus) {
        jdbc.update("UPDATE DonHang SET TrangThaiDonHang = ? WHERE MaDH = ?", newStatus, id);
    }

    public void assignHandler(Integer id, Integer employeeId) {
        jdbc.update("UPDATE DonHang SET MaNV = ? WHERE MaDH = ?", employeeId, id);
    }

    public static class OrderRow {
        public Integer id;
        public java.time.OffsetDateTime createdAt;
        public String status;
        public String paymentStatus;
        public java.math.BigDecimal total;
        public String customerName;
        public String customerPhone;
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
            r.total = rs.getBigDecimal("TongThanhToan");
            r.customerName = rs.getString("TenKH");
            r.customerPhone = rs.getString("SoDienThoai");
            return r;
        }
    };

    public String nextStatus(String current) {
        if (current == null) return "ChoXuLy";
        return switch (current) {
            case "ChoXuLy" -> "DangPhaChe";
            case "DangPhaChe" -> "DangGiao";
            case "DangGiao" -> "DaGiao";
            default -> current; // DaGiao, DaHuy remain
        };
    }

    public boolean canCancel(String current) {
        if (current == null) return false;
        return current.equals("ChoXuLy") || current.equals("DangPhaChe");
    }

    public List<OrderRow> listTodayOrders() {
        String sql = "SELECT dh.MaDH, dh.NgayLap, dh.TrangThaiDonHang, dh.PaymentStatus, dh.TongThanhToan, kh.TenKH, kh.SoDienThoai\n" +
                "FROM DonHang dh JOIN KhachHang kh ON kh.MaKH = dh.MaKH\n" +
                "WHERE CONVERT(date, dh.NgayLap) = CONVERT(date, SYSUTCDATETIME())\n" +
                "ORDER BY dh.MaDH DESC";
        return jdbc.query(sql, ORDER_ROW_MAPPER);
    }
}