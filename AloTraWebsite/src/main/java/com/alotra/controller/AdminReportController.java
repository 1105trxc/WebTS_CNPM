package com.alotra.controller;

import com.alotra.service.NhanVienService;
import com.alotra.service.ShiftReportService;
import com.alotra.service.ShiftReportService.ShiftReport;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {
    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ShiftReportService shiftReportService;
    private final NhanVienService nhanVienService;

    public AdminReportController(ShiftReportService shiftReportService, NhanVienService nhanVienService) {
        this.shiftReportService = shiftReportService;
        this.nhanVienService = nhanVienService;
    }

    @GetMapping("/shift")
    public String shiftReport(@RequestParam(value = "employeeId", required = false) Integer employeeId,
                              @RequestParam(value = "from", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
                              @RequestParam(value = "to", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to,
                              Model model) {
        model.addAttribute("pageTitle", "Báo cáo cuối ca");
        model.addAttribute("currentPage", "reports");
        model.addAttribute("employees", nhanVienService.findActive());

        if (from == null) from = LocalDate.now(HCM_ZONE).atStartOfDay();
        if (to == null) to = LocalDateTime.now(HCM_ZONE);
        if (to.isBefore(from)) { LocalDateTime tmp = from; from = to; to = tmp; }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        model.addAttribute("fromStr", fmt.format(from));
        model.addAttribute("toStr", fmt.format(to));

        if (employeeId != null) {
            ShiftReport report = shiftReportService.getReport(employeeId, from, to);
            model.addAttribute("employee", nhanVienService.findById(employeeId).orElse(null));
            model.addAttribute("report", report);
        }
        return "admin/shift-report";
    }

    @GetMapping("/shift/export")
    public ResponseEntity<byte[]> exportCsv(@RequestParam("employeeId") Integer employeeId,
                                            @RequestParam(value = "from", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime from,
                                            @RequestParam(value = "to", required = false)
                                            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime to) {
        if (employeeId == null) {
            return ResponseEntity.badRequest().body("Missing employeeId".getBytes(StandardCharsets.UTF_8));
        }
        if (from == null) from = LocalDate.now(HCM_ZONE).atStartOfDay();
        if (to == null) to = LocalDateTime.now(HCM_ZONE);
        if (to.isBefore(from)) { LocalDateTime tmp = from; from = to; to = tmp; }

        ShiftReport r = shiftReportService.getReport(employeeId, from, to);
        String empName = nhanVienService.findById(employeeId).map(e -> e.getFullName() + " (" + e.getUsername() + ")").orElse("NV-" + employeeId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
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
        String filename = "shift_report_" + employeeId + "_" + from.toLocalDate() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    private String escape(String s) {
        if (s == null) return "";
        boolean need = s.contains(",") || s.contains("\n") || s.contains("\"");
        String t = s.replace("\"", "\"\"");
        return need ? ("\"" + t + "\"") : t;
    }
}
