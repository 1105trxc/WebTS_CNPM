package com.alotra.controller.vendor;

import com.alotra.security.NhanVienUserDetails;
import com.alotra.service.NhanVienService;
import com.alotra.service.ShiftReportService;
import com.alotra.service.ShiftReportService.ShiftReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/vendor/reports")
public class VendorReportController {
    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ShiftReportService shiftReportService;
    private final NhanVienService nhanVienService;
    private final JdbcTemplate jdbcTemplate;

    public VendorReportController(ShiftReportService shiftReportService, NhanVienService nhanVienService, JdbcTemplate jdbcTemplate) {
        this.shiftReportService = shiftReportService;
        this.nhanVienService = nhanVienService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/shift")
    public String shiftReport(@AuthenticationPrincipal NhanVienUserDetails current,
                              @RequestParam(value = "employeeId", required = false) Integer employeeId,
                              @RequestParam(value = "from", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
                              @RequestParam(value = "to", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to,
                              Model model) {
        Integer empId = (employeeId != null ? employeeId : (current != null ? current.getId() : null));
        if (empId == null) {
            model.addAttribute("error", "Không xác định được nhân viên.");
            return "vendor/shift-report";
        }
        // Default range: today 00:00 (GMT+7) -> now (GMT+7)
        if (from == null) from = LocalDate.now(HCM_ZONE).atStartOfDay();
        if (to == null) to = LocalDateTime.now(HCM_ZONE);
        if (to.isBefore(from)) {
            LocalDateTime tmp = from; from = to; to = tmp;
        }
        ShiftReport report = shiftReportService.getReport(empId, from, to);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("employee", nhanVienService.findById(empId).orElse(null));
        model.addAttribute("fromStr", fmt.format(from));
        model.addAttribute("toStr", fmt.format(to));
        model.addAttribute("report", report);
        model.addAttribute("pageTitle", "Báo cáo cuối ca");
        return "vendor/shift-report";
    }

    @GetMapping("/shift/export")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal NhanVienUserDetails current,
                                            @RequestParam(value = "employeeId", required = false) Integer employeeId,
                                            @RequestParam(value = "from", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
                                            @RequestParam(value = "to", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to) {
        Integer empId = (employeeId != null ? employeeId : (current != null ? current.getId() : null));
        if (empId == null) {
            return ResponseEntity.badRequest().body("Missing employeeId".getBytes(StandardCharsets.UTF_8));
        }
        if (from == null) from = LocalDate.now(HCM_ZONE).atStartOfDay();
        if (to == null) to = LocalDateTime.now(HCM_ZONE);
        if (to.isBefore(from)) { LocalDateTime tmp = from; from = to; to = tmp; }

        ShiftReport r = shiftReportService.getReport(empId, from, to);
        String empName = nhanVienService.findById(empId).map(e -> e.getFullName() + " (" + e.getUsername() + ")").orElse("NV-" + empId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        sb.append('\uFEFF');
        // Header summary lines
        sb.append("Shift Report").append('\n');
        sb.append("Employee, ").append(escape(empName)).append('\n');
        sb.append("From, ").append(fmt.format(from)).append('\n');
        sb.append("To, ").append(fmt.format(to)).append('\n');
        sb.append("Total Orders, ").append(r.totalOrders).append('\n');
        sb.append("Delivered, ").append(r.delivered).append('\n');
        sb.append("Canceled, ").append(r.canceled).append('\n');
        sb.append("In Progress, ").append(r.inProgress).append('\n');
        sb.append("Drinks Made, ").append(r.drinksMade).append('\n');
        sb.append("Paid Total, ").append(r.paidTotal).append('\n');
        sb.append("Cash Paid, ").append(r.cashPaid).append('\n');
        sb.append("Bank Paid, ").append(r.bankPaid).append('\n');
        sb.append("Unpaid Count, ").append(r.unpaidCount).append('\n');
        sb.append('\n');
        // Orders table
        sb.append("Order ID,Time,Status,Payment Status,Payment Method,Customer,Phone,Total").append('\n');
        for (var o : r.orders) {
            String id = String.valueOf(o.get("id"));
            String time = o.get("createdAt") != null ? o.get("createdAt").toString() : "";
            String st = String.valueOf(o.get("status"));
            String pst = String.valueOf(o.get("paymentStatus"));
            String pm = String.valueOf(o.get("paymentMethod"));
            String cn = o.get("customerName") != null ? o.get("customerName").toString() : "";
            String cp = o.get("customerPhone") != null ? o.get("customerPhone").toString() : "";
            String total = String.valueOf(o.get("total"));
            sb.append(id).append(',')
              .append(escape(time)).append(',')
              .append(escape(st)).append(',')
              .append(escape(pst)).append(',')
              .append(escape(pm)).append(',')
              .append(escape(cn)).append(',')
              .append(escape(cp)).append(',')
              .append(total)
              .append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "shift_report_" + empId + "_" + from.toLocalDate().toString() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    @PostMapping("/shift/assign-me")
    public String assignAllToMe(@AuthenticationPrincipal NhanVienUserDetails current,
                                @RequestParam(value = "from") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
                                @RequestParam(value = "to") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to) {
        if (current == null) return "redirect:/vendor/reports/shift";
        if (from == null) from = LocalDate.now(HCM_ZONE).atStartOfDay();
        if (to == null) to = LocalDateTime.now(HCM_ZONE);
        if (to.isBefore(from)) { LocalDateTime tmp = from; from = to; to = tmp; }
        // Assign all orders in the time range that are currently unassigned
        java.sql.Timestamp fromTs = java.sql.Timestamp.valueOf(from);
        java.sql.Timestamp toTs = java.sql.Timestamp.valueOf(to);
        jdbcTemplate.update("UPDATE DonHang SET MaNV = ? WHERE MaNV IS NULL AND NgayLap BETWEEN ? AND ?", current.getId(), fromTs, toTs);
        return "redirect:/vendor/reports/shift?employeeId=" + current.getId() + "&from=" + from.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")) + "&to=" + to.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
    }

    private String escape(String s) {
        if (s == null) return "";
        boolean need = s.contains(",") || s.contains("\n") || s.contains("\"");
        String t = s.replace("\"", "\"\"");
        return need ? ("\"" + t + "\"") : t;
    }
}