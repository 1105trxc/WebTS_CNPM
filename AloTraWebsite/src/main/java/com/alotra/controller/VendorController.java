package com.alotra.controller;

import com.alotra.repository.NhanVienRepository;
import com.alotra.service.VendorOrderService;
import com.alotra.service.VendorOrderService.OrderRow;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vendor") // Tất cả URL sẽ có tiền tố /vendor
public class VendorController {

    private final VendorOrderService orders;
    private final NhanVienRepository nhanVienRepository;

    public VendorController(VendorOrderService orders, NhanVienRepository nhanVienRepository) {
        this.orders = orders;
        this.nhanVienRepository = nhanVienRepository;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Kênh Người Bán - Bảng điều khiển");
        Map<String, Object> counts = orders.getDashboardCounts();
        model.addAttribute("counts", counts);
        // Recent today orders
        model.addAttribute("recent", orders.listTodayOrders());
        return "vendor/dashboard"; // Trỏ đến file /templates/vendor/dashboard.html
    }

    @GetMapping({"/orders"})
    public String listOrders(@RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "kw", required = false) String kw,
                             @RequestParam(value = "limit", required = false) Integer limit,
                             Model model,
                             HttpServletRequest request) {
        List<OrderRow> list = orders.listOrders(status, kw, limit);
        model.addAttribute("pageTitle", "Đơn hàng cần xử lý");
        model.addAttribute("status", status);
        model.addAttribute("kw", kw);
        model.addAttribute("limit", limit);
        model.addAttribute("items", list);
        model.addAttribute("employees", nhanVienRepository.findByDeletedAtIsNull());
        // Build current URL context-relative (do not include context-path) for safe redirects
        String uri = request.getRequestURI(); // includes context path
        String ctx = request.getContextPath();
        String base = uri.startsWith(ctx) ? uri.substring(ctx.length()) : uri; // e.g. /vendor/orders
        String qs = request.getQueryString();
        model.addAttribute("from", (qs == null || qs.isBlank()) ? base : base + "?" + qs);
        return "vendor/orders";
    }

    @PostMapping("/orders/{id}/advance")
    public String advance(@PathVariable Integer id,
                          @RequestParam(value = "from", required = false, defaultValue = "/vendor/orders") String from) {
        List<OrderRow> one = orders.listOrders(null, null, 200);
        String current = one.stream().filter(o -> o.id.equals(id)).map(o -> o.status).findFirst().orElse(null);
        String next = orders.nextStatus(current);
        if (next != null && !next.equals(current)) {
            orders.updateStatus(id, next);
        }
        return "redirect:" + from;
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Integer id,
                         @RequestParam(value = "from", required = false, defaultValue = "/vendor/orders") String from) {
        List<OrderRow> one = orders.listOrders(null, null, 200);
        String current = one.stream().filter(o -> o.id.equals(id)).map(o -> o.status).findFirst().orElse(null);
        if (orders.canCancel(current)) {
            orders.updateStatus(id, "DaHuy");
        }
        return "redirect:" + from;
    }

    @PostMapping("/orders/{id}/assign")
    public String assign(@PathVariable Integer id,
                         @RequestParam("employeeId") Integer employeeId,
                         @RequestParam(value = "from", required = false, defaultValue = "/vendor/orders") String from) {
        orders.assignHandler(id, employeeId);
        return "redirect:" + from;
    }
}