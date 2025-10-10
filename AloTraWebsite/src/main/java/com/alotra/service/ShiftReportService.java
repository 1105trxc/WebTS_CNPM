package com.alotra.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShiftReportService {
    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final JdbcTemplate jdbc;

    public ShiftReportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ShiftReport getReport(Integer employeeId, LocalDateTime from, LocalDateTime to) {
        if (employeeId == null) throw new IllegalArgumentException("employeeId is required");
        // Treat provided range as local time (GMT+7)
        Timestamp fromTs = Timestamp.valueOf(from);
        Timestamp toTs = Timestamp.valueOf(to);

        ShiftReport r = new ShiftReport();
        r.employeeId = employeeId;
        r.from = from;
        r.to = to;

        r.totalOrders = nn(queryLong("SELECT COUNT(*) FROM DonHang WHERE MaNV = ? AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));
        r.delivered = nn(queryLong("SELECT COUNT(*) FROM DonHang WHERE MaNV = ? AND TrangThaiDonHang = 'DaGiao' AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));
        r.canceled = nn(queryLong("SELECT COUNT(*) FROM DonHang WHERE MaNV = ? AND TrangThaiDonHang = 'DaHuy' AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));
        r.inProgress = r.totalOrders - r.delivered - r.canceled;

        r.paidTotal = nnBig(queryBig("SELECT SUM(TongThanhToan) FROM DonHang WHERE MaNV = ? AND PaymentStatus = 'DaThanhToan' AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));
        r.cashPaid = nnBig(queryBig("SELECT SUM(TongThanhToan) FROM DonHang WHERE MaNV = ? AND PaymentStatus = 'DaThanhToan' AND PaymentMethod = 'TienMat' AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));
        r.bankPaid = nnBig(queryBig("SELECT SUM(TongThanhToan) FROM DonHang WHERE MaNV = ? AND PaymentStatus = 'DaThanhToan' AND PaymentMethod = 'ChuyenKhoan' AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));
        r.unpaidCount = nn(queryLong("SELECT COUNT(*) FROM DonHang WHERE MaNV = ? AND (PaymentStatus IS NULL OR PaymentStatus <> 'DaThanhToan') AND NgayLap BETWEEN ? AND ?", employeeId, fromTs, toTs));

        r.drinksMade = nn(queryLong(
                "SELECT COALESCE(SUM(ct.SoLuong),0) FROM CTDonHang ct JOIN DonHang dh ON dh.MaDH = ct.MaDH WHERE dh.MaNV = ? AND dh.NgayLap BETWEEN ? AND ?",
                employeeId, fromTs, toTs));

        // Status breakdown
        r.statusCounts = jdbc.query(
                "SELECT TrangThaiDonHang st, COUNT(*) cnt FROM DonHang WHERE MaNV = ? AND NgayLap BETWEEN ? AND ? GROUP BY TrangThaiDonHang",
                ps -> { ps.setInt(1, employeeId); ps.setTimestamp(2, fromTs); ps.setTimestamp(3, toTs); },
                (rs, i) -> Map.of("status", rs.getString("st"), "count", rs.getLong("cnt"))
        );

        // Recent orders list for table
        r.orders = jdbc.query(
                "SELECT dh.MaDH, dh.NgayLap, dh.TrangThaiDonHang, dh.PaymentStatus, dh.PaymentMethod, dh.TongThanhToan, kh.TenKH, kh.SoDienThoai " +
                        "FROM DonHang dh JOIN KhachHang kh ON kh.MaKH = dh.MaKH " +
                        "WHERE dh.MaNV = ? AND dh.NgayLap BETWEEN ? AND ? ORDER BY dh.MaDH DESC",
                ps -> { ps.setInt(1, employeeId); ps.setTimestamp(2, fromTs); ps.setTimestamp(3, toTs); },
                (rs, i) -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", rs.getInt("MaDH"));
                    java.sql.Timestamp ts = rs.getTimestamp("NgayLap");
                    m.put("createdAt", ts != null ? ts.toInstant().atZone(HCM_ZONE).toOffsetDateTime() : null);
                    m.put("status", rs.getString("TrangThaiDonHang"));
                    m.put("paymentStatus", rs.getString("PaymentStatus"));
                    m.put("paymentMethod", rs.getString("PaymentMethod"));
                    m.put("total", rs.getBigDecimal("TongThanhToan"));
                    m.put("customerName", rs.getString("TenKH"));
                    m.put("customerPhone", rs.getString("SoDienThoai"));
                    return m;
                }
        );

        return r;
    }

    private Long queryLong(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v;
    }

    private BigDecimal queryBig(String sql, Object... args) {
        return jdbc.queryForObject(sql, BigDecimal.class, args);
    }

    private long nn(Long v) { return v == null ? 0L : v; }
    private BigDecimal nnBig(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    public static class ShiftReport {
        public Integer employeeId;
        public LocalDateTime from;
        public LocalDateTime to;
        public long totalOrders;
        public long delivered;
        public long canceled;
        public long inProgress;
        public long unpaidCount;
        public long drinksMade;
        public BigDecimal paidTotal;
        public BigDecimal cashPaid;
        public BigDecimal bankPaid;
        public List<Map<String, Object>> statusCounts;
        public List<Map<String, Object>> orders;
    }
}