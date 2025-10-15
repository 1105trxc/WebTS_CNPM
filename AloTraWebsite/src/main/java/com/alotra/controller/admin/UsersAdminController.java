package com.alotra.controller.admin;

import com.alotra.entity.KhachHang;
import com.alotra.entity.NhanVien;
import com.alotra.service.KhachHangService;
import com.alotra.service.NhanVienService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/users")
public class UsersAdminController {
    private final KhachHangService khService;
    private final NhanVienService nvService;

    public UsersAdminController(KhachHangService khService, NhanVienService nvService) {
        this.khService = khService;
        this.nvService = nvService;
    }

    @GetMapping
    public String index(Model model,
                        @RequestParam(value = "tab", required = false, defaultValue = "customers") String tab,
                        // Customer filters
                        @RequestParam(value = "kwC", required = false) String kwC,
                        @RequestParam(value = "statusC", required = false) Integer statusC,
                        // Employee filters
                        @RequestParam(value = "kwE", required = false) String kwE,
                        @RequestParam(value = "roleE", required = false) Integer roleE,
                        @RequestParam(value = "statusE", required = false) Integer statusE) {
        List<KhachHang> customers = khService.search(kwC, statusC);
        List<NhanVien> employees = nvService.search(kwE, roleE, statusE); // excludes trashed by repo query
        model.addAttribute("pageTitle", "Người dùng");
        model.addAttribute("currentPage", "users");
        model.addAttribute("tab", tab);
        model.addAttribute("customers", customers);
        model.addAttribute("employees", employees);
        model.addAttribute("kwC", kwC);
        model.addAttribute("statusC", statusC);
        model.addAttribute("kwE", kwE);
        model.addAttribute("roleE", roleE);
        model.addAttribute("statusE", statusE);
        return "admin/users/index";
    }

    @GetMapping("/customers/{id}")
    public String customerDetail(@PathVariable Integer id, Model model){
        KhachHang kh = khService.findById(id);
        if (kh == null) throw new IllegalArgumentException("Không tìm thấy khách hàng");
        model.addAttribute("pageTitle", "Chi tiết khách hàng");
        model.addAttribute("currentPage", "users");
        model.addAttribute("kh", kh);
        return "admin/users/customer-detail";
    }

    // === Customers: lock/unlock/delete ===
    @PostMapping("/customers/{id}/lock")
    public String lockCustomer(@PathVariable Integer id, RedirectAttributes ra) {
        KhachHang kh = khService.findById(id);
        if (kh == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khách hàng.");
        } else if ("boss".equalsIgnoreCase(String.valueOf(kh.getUsername())) ||
                   "boss@alotra.com".equalsIgnoreCase(String.valueOf(kh.getEmail()))) {
            ra.addFlashAttribute("error", "Không thể khóa tài khoản quản trị hệ thống.");
        } else {
            kh.setStatus(0);
            khService.save(kh);
            ra.addFlashAttribute("msg", "Đã khóa tài khoản khách hàng.");
        }
        return "redirect:/admin/users?tab=customers";
    }

    @PostMapping("/customers/{id}/unlock")
    public String unlockCustomer(@PathVariable Integer id, RedirectAttributes ra) {
        KhachHang kh = khService.findById(id);
        if (kh == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khách hàng.");
        } else {
            kh.setStatus(1);
            khService.save(kh);
            ra.addFlashAttribute("msg", "Đã mở khóa tài khoản khách hàng.");
        }
        return "redirect:/admin/users?tab=customers";
    }

    // Support both POST and GET delete patterns
    @PostMapping("/customers/{id}/delete")
    public String deleteCustomerPost(@PathVariable Integer id, RedirectAttributes ra) {
        return deleteCustomerInternal(id, ra);
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomerGet(@PathVariable Integer id, RedirectAttributes ra) {
        return deleteCustomerInternal(id, ra);
    }

    private String deleteCustomerInternal(Integer id, RedirectAttributes ra) {
        KhachHang kh = khService.findById(id);
        if (kh == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khách hàng.");
            return "redirect:/admin/users?tab=customers";
        }
        if ("boss".equalsIgnoreCase(String.valueOf(kh.getUsername())) ||
            "boss@alotra.com".equalsIgnoreCase(String.valueOf(kh.getEmail()))) {
            ra.addFlashAttribute("error", "Không thể xóa tài khoản quản trị hệ thống.");
            return "redirect:/admin/users?tab=customers";
        }
        try {
            khService.deleteById(id);
            ra.addFlashAttribute("msg", "Đã xóa tài khoản khách hàng.");
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "Không thể xóa vì tài khoản đã phát sinh dữ liệu. Vui lòng khóa thay vì xóa.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Không thể xóa: " + ex.getMessage());
        }
        return "redirect:/admin/users?tab=customers";
    }

    @GetMapping("/employees/new")
    public String newEmployee(Model model){
        NhanVien nv = new NhanVien();
        nv.setRole(2); // default: Nhân viên
        nv.setStatus(1);
        model.addAttribute("pageTitle", "Thêm nhân viên");
        model.addAttribute("currentPage", "users");
        model.addAttribute("nv", nv);
        return "admin/users/employee-form";
    }

    @GetMapping("/employees/edit/{id}")
    public String editEmployee(@PathVariable Integer id, Model model){
        NhanVien nv = nvService.findById(id).orElseThrow();
        model.addAttribute("pageTitle", "Sửa nhân viên");
        model.addAttribute("currentPage", "users");
        model.addAttribute("nv", nv);
        return "admin/users/employee-form";
    }

    @PostMapping("/employees/save")
    public String saveEmployee(@ModelAttribute("nv") @Valid NhanVien nv, BindingResult result, Model model, RedirectAttributes ra){
        // Validate unique username/email/phone
        NhanVien byU = nv.getUsername()!=null ? nvService.findByUsername(nv.getUsername()) : null;
        if (byU != null && (nv.getId()==null || !byU.getId().equals(nv.getId()))) {
            result.rejectValue("username","dup","Tên đăng nhập đã tồn tại");
        }
        NhanVien byE = nv.getEmail()!=null ? nvService.findByEmail(nv.getEmail()) : null;
        if (byE != null && (nv.getId()==null || !byE.getId().equals(nv.getId()))) {
            result.rejectValue("email","dup","Email đã tồn tại");
        }
        if (nv.getPhone()!=null && !nv.getPhone().isBlank()){
            NhanVien byP = nvService.findByPhone(nv.getPhone());
            if (byP != null && (nv.getId()==null || !byP.getId().equals(nv.getId()))) {
                result.rejectValue("phone","dup","Số điện thoại đã tồn tại");
            }
        }
        // Password confirmation if provided or creating new
        boolean isNew = nv.getId()==null;
        String pw = nv.getPlainPassword();
        String cpw = nv.getConfirmPassword();
        if (isNew && (pw==null || pw.isBlank())) {
            result.rejectValue("plainPassword","empty","Vui lòng nhập mật khẩu");
        }
        if (pw!=null && !pw.isBlank() && !pw.equals(cpw)){
            result.rejectValue("confirmPassword","mismatch","Mật khẩu xác nhận không khớp");
        }
        if (result.hasErrors()){
            model.addAttribute("pageTitle", isNew ? "Thêm nhân viên" : "Sửa nhân viên");
            model.addAttribute("currentPage", "users");
            return "admin/users/employee-form";
        }
        nvService.saveHandlingPassword(nv);
        ra.addFlashAttribute("msg", isNew ? "Đã thêm nhân viên thành công." : "Đã cập nhật nhân viên thành công.");
        return "redirect:/admin/users?tab=employees";
    }

    @GetMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Integer id, RedirectAttributes ra){
        // Always soft-delete to trash; no FK risk here. Hard delete is only allowed from Trash page.
        nvService.softDeleteToTrash(id);
        ra.addFlashAttribute("msg", "Đã chuyển nhân viên vào thùng rác.");
        return "redirect:/admin/users?tab=employees";
    }

    @PostMapping("/employees/{id}/reset-password")
    public String resetPassword(@PathVariable Integer id, RedirectAttributes ra) {
        String temp = nvService.resetPassword(id);
        ra.addFlashAttribute("msg", "Đã đặt lại mật khẩu. Mật khẩu tạm thời: " + temp);
        return "redirect:/admin/users?tab=employees";
    }
}