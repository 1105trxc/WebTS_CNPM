package com.alotra.controller.vendor;

import com.alotra.entity.DonHang;
import com.alotra.entity.NhanVien;
import com.alotra.repository.DonHangRepository;
import com.alotra.service.OrderService;
import com.alotra.service.VendorOrderService;
import com.alotra.service.OrderService.OrderItemRow;
import com.alotra.service.OrderService.ItemToppingRow;
import com.alotra.service.OrderService.OrderRow;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alotra.security.NhanVienUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
@RequestMapping("/vendor")
public class VendorController {
    private final VendorOrderService vendorOrderService;
    private final OrderService customerOrderService;
    private final DonHangRepository donHangRepository;

    public VendorController(VendorOrderService vendorOrderService,
                            OrderService customerOrderService,
                            DonHangRepository donHangRepository) {
        this.vendorOrderService = vendorOrderService;
        this.customerOrderService = customerOrderService;
        this.donHangRepository = donHangRepository;
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
        model.addAttribute("pageTitle", "Đơn #" + id);
        return "vendor/order-detail";
    }

    @GetMapping("/orders/{id}/invoice")
    public String invoice(@PathVariable Integer id, Model model) {
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
        model.addAttribute("storeName", "AloTra");
        model.addAttribute("storeAddress", "Khu pho 6, P. Linh Trung, TP. Thu Duc, TP. HCM");
        model.addAttribute("storePhone", "1900 1234");
        return "vendor/invoice";
    }

    @PostMapping("/orders/{id}/advance")
    public String advance(@PathVariable Integer id,
                          @RequestParam(required = false) String from,
                          @AuthenticationPrincipal NhanVienUserDetails current) {
        DonHang dh = donHangRepository.findById(id).orElse(null);
        if (dh != null) {
            // Assign current employee if not yet assigned
            if (current != null && dh.getEmployee() == null) {
                NhanVien nv = new NhanVien();
                nv.setId(current.getId());
                dh.setEmployee(nv);
                donHangRepository.save(dh);
            }
            // Block advancing when unpaid bank transfer
            if ("ChuyenKhoan".equalsIgnoreCase(String.valueOf(dh.getPaymentMethod()))
                    && !"DaThanhToan".equals(dh.getPaymentStatus())) {
                return redirectFrom(id, from);
            }
            String currentSt = dh.getStatus();
            String next = vendorOrderService.nextStatus(currentSt);
            if (next != null && !next.equals(currentSt)) {
                vendorOrderService.updateStatus(id, next);
            }
        }
        return redirectFrom(id, from);
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Integer id,
                         @RequestParam(required = false) String from,
                         @AuthenticationPrincipal NhanVienUserDetails current) {
        DonHang dh = donHangRepository.findById(id).orElse(null);
        String currentSt = dh != null ? dh.getStatus() : null;
        if (dh != null) {
            if (current != null && dh.getEmployee() == null) {
                NhanVien nv = new NhanVien();
                nv.setId(current.getId());
                dh.setEmployee(nv);
                donHangRepository.save(dh);
            }
            if (vendorOrderService.canCancel(currentSt)) {
                vendorOrderService.updateStatus(id, "DaHuy");
            }
        }
        return redirectFrom(id, from);
    }

    @PostMapping("/orders/{id}/mark-cash-paid")
    public String markCashPaid(@PathVariable Integer id,
                               @RequestParam(required = false) String from,
                               @AuthenticationPrincipal NhanVienUserDetails current) {
        donHangRepository.findById(id).ifPresent(dh -> {
            if (!"DaThanhToan".equals(dh.getPaymentStatus())) {
                dh.setPaymentStatus("DaThanhToan");
                dh.setPaidAt(LocalDateTime.now());
                // Assign employee if not yet assigned
                if (current != null && dh.getEmployee() == null) {
                    NhanVien nv = new NhanVien();
                    nv.setId(current.getId());
                    dh.setEmployee(nv);
                }
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