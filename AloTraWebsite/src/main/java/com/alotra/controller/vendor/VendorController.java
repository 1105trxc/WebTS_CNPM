package com.alotra.controller.vendor;

import com.alotra.entity.DonHang;
import com.alotra.repository.DonHangRepository;
import com.alotra.service.CustomerOrderService;
import com.alotra.service.NhanVienService;
import com.alotra.service.VendorOrderService;
import com.alotra.service.CustomerOrderService.OrderItemRow;
import com.alotra.service.CustomerOrderService.ItemToppingRow;
import com.alotra.service.CustomerOrderService.OrderRow;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/vendor")
public class VendorController {
    private final VendorOrderService vendorOrderService;
    private final CustomerOrderService customerOrderService;
    private final DonHangRepository donHangRepository;
    private final NhanVienService nhanVienService;

    public VendorController(VendorOrderService vendorOrderService,
                            CustomerOrderService customerOrderService,
                            DonHangRepository donHangRepository,
                            NhanVienService nhanVienService) {
        this.vendorOrderService = vendorOrderService;
        this.customerOrderService = customerOrderService;
        this.donHangRepository = donHangRepository;
        this.nhanVienService = nhanVienService;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("counts", vendorOrderService.getDashboardCounts());
        model.addAttribute("recent", vendorOrderService.listTodayOrders());
        return "vendor/dashboard";
    }

    @GetMapping("/orders")
    public String listOrders(@RequestParam(required = false) String status,
                             @RequestParam(required = false) String kw,
                             @RequestParam(required = false) Integer limit,
                             @RequestParam(required = false, defaultValue = "list") String from,
                             Model model) {
        model.addAttribute("items", vendorOrderService.listOrders(status, kw, limit));
        model.addAttribute("employees", nhanVienService.search(null, 2, 1));
        model.addAttribute("status", status);
        model.addAttribute("kw", kw);
        model.addAttribute("limit", limit);
        model.addAttribute("from", from);
        return "vendor/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Integer id, Model model) {
        OrderRow order = customerOrderService.getOrder(id);
        if (order == null) {
            return "redirect:/vendor/orders";
        }
        List<OrderItemRow> items = customerOrderService.listOrderItems(id);
        Map<Integer, List<ItemToppingRow>> toppings = new HashMap<>();
        for (OrderItemRow it : items) {
            toppings.put(it.id, customerOrderService.listOrderItemToppings(it.id));
        }
        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("toppings", toppings);
        model.addAttribute("employees", nhanVienService.search(null, 2, 1));
        model.addAttribute("pageTitle", "Đơn #" + id);
        return "vendor/order-detail";
    }

    @PostMapping("/orders/{id}/advance")
    public String advance(@PathVariable Integer id, @RequestParam(required = false) String from) {
        DonHang dh = donHangRepository.findById(id).orElse(null);
        if (dh != null) {
            // Gate: block advancing any step for unpaid bank transfer
            if ("ChuyenKhoan".equalsIgnoreCase(String.valueOf(dh.getPaymentMethod()))
                    && !"DaThanhToan".equals(dh.getPaymentStatus())) {
                return redirectFrom(id, from);
            }
            String current = dh.getStatus();
            String next = vendorOrderService.nextStatus(current);
            if (next != null && !next.equals(current)) {
                vendorOrderService.updateStatus(id, next);
            }
        }
        return redirectFrom(id, from);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Integer id, @RequestParam(required = false) String from) {
        String current = donHangRepository.findById(id).map(DonHang::getStatus).orElse(null);
        if (vendorOrderService.canCancel(current)) {
            vendorOrderService.updateStatus(id, "DaHuy");
        }
        return redirectFrom(id, from);
    }

    @PostMapping("/orders/{id}/assign")
    public String assign(@PathVariable Integer id, @RequestParam Integer employeeId,
                         @RequestParam(required = false) String from) {
        DonHang dh = donHangRepository.findById(id).orElse(null);
        if (dh != null) {
            // Gate: block assigning for unpaid bank transfer
            if ("ChuyenKhoan".equalsIgnoreCase(String.valueOf(dh.getPaymentMethod()))
                    && !"DaThanhToan".equals(dh.getPaymentStatus())) {
                return redirectFrom(id, from);
            }
            vendorOrderService.assignHandler(id, employeeId);
        }
        return redirectFrom(id, from);
    }

    // Staff marks orders as paid (regardless of method)
    @PostMapping("/orders/{id}/mark-cash-paid")
    public String markCashPaid(@PathVariable Integer id, @RequestParam(required = false) String from) {
        donHangRepository.findById(id).ifPresent(dh -> {
            if (!"DaThanhToan".equals(dh.getPaymentStatus())) {
                dh.setPaymentStatus("DaThanhToan");
                dh.setPaidAt(LocalDateTime.now());
                donHangRepository.save(dh);
            }
        });
        return redirectFrom(id, from);
    }

    private String redirectFrom(Integer id, String from) {
        if ("detail".equalsIgnoreCase(from)) {
            return "redirect:/vendor/orders/" + id;
        }
        return "redirect:/vendor/orders";
    }
}