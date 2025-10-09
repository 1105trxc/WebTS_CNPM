package com.alotra.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class StatsService {
    private final JdbcTemplate jdbc;

    public StatsService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public DashboardStats loadDashboardStats() {
        DashboardStats s = new DashboardStats();
        // Doanh thu: chỉ tính đơn đã thanh toán
        s.totalRevenue = nnBig(queryBigDecimal("SELECT SUM(TongThanhToan) FROM DonHang WHERE PaymentStatus='DaThanhToan'"));
        s.totalOrders = nnLong(queryLong("SELECT COUNT(*) FROM DonHang"));
        s.totalCustomers = nnLong(queryLong("SELECT COUNT(*) FROM KhachHang"));
        s.totalProducts = nnLong(queryLong("SELECT COUNT(*) FROM SanPham WHERE DeletedAt IS NULL"));
        s.totalReviews = nnLong(queryLong("SELECT COUNT(*) FROM DanhGia"));
        s.avgStars = nnDouble(queryDouble("SELECT AVG(CAST(SoSao AS FLOAT)) FROM DanhGia"));
        s.revenueDaily = revenueDaily(14);
        s.orderStatus = orderStatusDistribution();
        s.topProducts = topProducts(8);
        s.categorySales = categorySales();
        s.topCustomers = topCustomers(8);
        return s;
    }

    // --- Query helpers ---
    private BigDecimal queryBigDecimal(String sql){ return jdbc.queryForObject(sql, BigDecimal.class); }
    private Long queryLong(String sql){ return jdbc.queryForObject(sql, Long.class); }
    private Double queryDouble(String sql){ return jdbc.queryForObject(sql, Double.class); }
    private BigDecimal nnBig(BigDecimal v){ return v==null? BigDecimal.ZERO: v; }
    private long nnLong(Long v){ return v==null?0L:v; }
    private double nnDouble(Double v){ return v==null?0d:v; }

    private List<Map<String,Object>> revenueDaily(int days){
        String sql = "SELECT CONVERT(date, NgayLap) d, SUM(TongThanhToan) total FROM DonHang " +
                "WHERE NgayLap >= DATEADD(day, -?, CONVERT(date, SYSUTCDATETIME())) " +
                "AND PaymentStatus='DaThanhToan' " +
                "GROUP BY CONVERT(date, NgayLap) ORDER BY d";
        return jdbc.query(sql, ps -> ps.setInt(1, days-1), (rs,i)-> Map.of(
                "date", rs.getDate("d").toLocalDate().toString(),
                "total", rs.getBigDecimal("total") == null ? BigDecimal.ZERO : rs.getBigDecimal("total")
        ));
    }

    private List<Map<String,Object>> orderStatusDistribution(){
        String sql = "SELECT TrangThaiDonHang status, COUNT(*) cnt FROM DonHang GROUP BY TrangThaiDonHang";
        return jdbc.query(sql, (rs,i)-> Map.of(
                "status", rs.getString("status"),
                "count", rs.getLong("cnt")
        ));
    }

    private List<Map<String,Object>> topProducts(int limit){
        String sql = "SELECT TOP " + limit + " sp.MaSP id, sp.TenSP name, SUM(ct.SoLuong) qty, SUM(ct.ThanhTien) amount " +
                "FROM CTDonHang ct JOIN DonHang dh ON dh.MaDH = ct.MaDH " +
                "JOIN BienTheSanPham bt ON bt.MaBT = ct.MaBT " +
                "JOIN SanPham sp ON sp.MaSP = bt.MaSP " +
                "WHERE dh.PaymentStatus='DaThanhToan' " +
                "GROUP BY sp.MaSP, sp.TenSP ORDER BY qty DESC";
        return jdbc.query(sql, (rs,i)-> Map.of(
                "id", rs.getInt("id"),
                "name", rs.getString("name"),
                "qty", rs.getLong("qty"),
                "amount", rs.getBigDecimal("amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("amount")
        ));
    }

    private List<Map<String,Object>> categorySales(){
        String sql = "SELECT dm.TenDM name, SUM(ct.SoLuong) qty, SUM(ct.ThanhTien) amount " +
                "FROM CTDonHang ct JOIN DonHang dh ON dh.MaDH = ct.MaDH " +
                "JOIN BienTheSanPham bt ON bt.MaBT = ct.MaBT " +
                "JOIN SanPham sp ON sp.MaSP = bt.MaSP " +
                "LEFT JOIN DanhMucSanPham dm ON dm.MaDM = sp.MaDM " +
                "WHERE dh.PaymentStatus='DaThanhToan' " +
                "GROUP BY dm.TenDM ORDER BY amount DESC";
        return jdbc.query(sql, (rs,i)-> Map.of(
                "name", rs.getString("name"),
                "qty", rs.getLong("qty"),
                "amount", rs.getBigDecimal("amount") == null ? BigDecimal.ZERO : rs.getBigDecimal("amount")
        ));
    }

    private List<Map<String,Object>> topCustomers(int limit){
        String sql = "SELECT TOP " + limit + " kh.MaKH id, kh.TenKH name, COUNT(dh.MaDH) orders, SUM(dh.TongThanhToan) spend " +
                "FROM DonHang dh JOIN KhachHang kh ON kh.MaKH = dh.MaKH " +
                "WHERE dh.PaymentStatus='DaThanhToan' " +
                "GROUP BY kh.MaKH, kh.TenKH ORDER BY spend DESC";
        return jdbc.query(sql, (rs,i)-> Map.of(
                "id", rs.getInt("id"),
                "name", rs.getString("name"),
                "orders", rs.getLong("orders"),
                "spend", rs.getBigDecimal("spend") == null ? BigDecimal.ZERO : rs.getBigDecimal("spend")
        ));
    }

    public static class DashboardStats {
        public BigDecimal totalRevenue;
        public long totalOrders;
        public long totalCustomers;
        public long totalProducts;
        public long totalReviews;
        public double avgStars;
        public List<Map<String,Object>> revenueDaily;
        public List<Map<String,Object>> orderStatus;
        public List<Map<String,Object>> topProducts;
        public List<Map<String,Object>> categorySales;
        public List<Map<String,Object>> topCustomers;
    }
}