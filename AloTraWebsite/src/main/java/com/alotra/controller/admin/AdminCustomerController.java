package com.alotra.controller.admin;

import com.alotra.entity.KhachHang;
import com.alotra.service.KhachHangService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/customers")
public class AdminCustomerController {
    private final KhachHangService khachHangService;

    public AdminCustomerController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }

    @GetMapping
    public String list(Model model) {
        List<KhachHang> items = khachHangService.findAll();
        model.addAttribute("pageTitle", "Khách hàng");
        model.addAttribute("currentPage", "customers");
        model.addAttribute("items", items);
        return "admin/customers";
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Integer id, RedirectAttributes ra) {
        KhachHang kh = khachHangService.findById(id);
        if (kh == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khách hàng.");
        } else {
            kh.setStatus(0);
            khachHangService.save(kh);
            ra.addFlashAttribute("message", "Đã khóa tài khoản khách hàng.");
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Integer id, RedirectAttributes ra) {
        KhachHang kh = khachHangService.findById(id);
        if (kh == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khách hàng.");
        } else {
            kh.setStatus(1);
            khachHangService.save(kh);
            ra.addFlashAttribute("message", "Đã mở khóa tài khoản khách hàng.");
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            KhachHang kh = khachHangService.findById(id);
            if (kh == null) {
                ra.addFlashAttribute("error", "Không tìm thấy khách hàng.");
            } else {
                // Thận trọng: xóa cứng có thể lỗi ràng buộc khóa ngoại nếu có đơn hàng/đánh giá
                khachHangService.deleteById(id);
                ra.addFlashAttribute("message", "Đã xóa tài khoản khách hàng.");
            }
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì tài khoản đã phát sinh dữ liệu (đơn hàng/đánh giá). Hãy khóa thay vì xóa.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Không thể xóa: " + ex.getMessage());
        }
        return "redirect:/admin/customers";
    }
}
