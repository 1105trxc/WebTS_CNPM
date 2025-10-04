package com.alotra.controller;

import com.alotra.entity.Category;
import com.alotra.entity.Topping;
import com.alotra.repository.CategoryRepository;
import com.alotra.repository.ToppingRepository;
import com.alotra.service.CloudinaryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.alotra.repository.ProductRepository; // add

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final CategoryRepository categoryRepository;
    private final ToppingRepository toppingRepository;
    private final CloudinaryService cloudinaryService;
    private final ProductRepository productRepository; // add

    public AdminController(CategoryRepository categoryRepository, ToppingRepository toppingRepository, CloudinaryService cloudinaryService, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.toppingRepository = toppingRepository;
        this.cloudinaryService = cloudinaryService;
        this.productRepository = productRepository; // add
    }

    @GetMapping
    public String showAdminRoot() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("currentPage", "dashboard");
        return "admin/dashboard";
    }

    // --- Categories ---
    @GetMapping("/categories")
    public String showCategories(Model model) {
        List<Category> categories = categoryRepository.findByDeletedAtIsNull();
        model.addAttribute("pageTitle", "Danh mục");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("categoryList", categories);
        return "admin/categories";
    }

    @GetMapping("/categories/add")
    public String addCategory(Model model) {
        model.addAttribute("pageTitle", "Thêm Danh mục");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("category", new Category());
        return "admin/category-form";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategory(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Optional<Category> categoryOpt = categoryRepository.findById(id);
        if (categoryOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy danh mục.");
            return "redirect:/admin/categories";
        }
        model.addAttribute("pageTitle", "Sửa Danh mục");
        model.addAttribute("currentPage", "categories");
        model.addAttribute("category", categoryOpt.get());
        return "admin/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute Category category, RedirectAttributes ra) {
        categoryRepository.save(category);
        ra.addFlashAttribute("message", "Lưu danh mục thành công.");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes ra) {
        categoryRepository.findById(id).ifPresentOrElse(c -> {
            // Guard: prevent delete if category still has active products
            long cnt = productRepository.countByCategoryAndDeletedAtIsNull(c);
            if (cnt > 0) {
                ra.addFlashAttribute("error", "Không thể xóa danh mục vì còn " + cnt + " sản phẩm đang thuộc danh mục này.");
            } else {
                c.setDeletedAt(LocalDateTime.now());
                categoryRepository.save(c);
                ra.addFlashAttribute("message", "Đã chuyển danh mục vào thùng rác.");
            }
        }, () -> ra.addFlashAttribute("error", "Không tìm thấy danh mục."));
        return "redirect:/admin/categories";
    }

    // --- Toppings ---
    @GetMapping("/toppings")
    public String showToppings(Model model) {
        List<Topping> toppings = toppingRepository.findByDeletedAtIsNull();
        model.addAttribute("pageTitle", "Topping");
        model.addAttribute("currentPage", "toppings");
        model.addAttribute("toppingList", toppings);
        return "admin/toppings";
    }

    @GetMapping("/toppings/add")
    public String addTopping(Model model) {
        model.addAttribute("pageTitle", "Thêm Topping");
        model.addAttribute("currentPage", "toppings");
        model.addAttribute("topping", new Topping());
        return "admin/topping-form";
    }

    @GetMapping("/toppings/edit/{id}")
    public String editTopping(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Optional<Topping> toppingOpt = toppingRepository.findById(id);
        if (toppingOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Không tìm thấy topping.");
            return "redirect:/admin/toppings";
        }
        model.addAttribute("pageTitle", "Sửa Topping");
        model.addAttribute("currentPage", "toppings");
        model.addAttribute("topping", toppingOpt.get());
        return "admin/topping-form";
    }

    @PostMapping("/toppings/save")
    public String saveTopping(@ModelAttribute Topping topping,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              RedirectAttributes ra) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String url = cloudinaryService.uploadFile(imageFile);
                topping.setImageUrl(url);
            }
            toppingRepository.save(topping);
            ra.addFlashAttribute("message", "Lưu topping thành công.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Tải ảnh thất bại: " + e.getMessage());
        }
        return "redirect:/admin/toppings";
    }

    @GetMapping("/toppings/delete/{id}")
    public String deleteTopping(@PathVariable Integer id, RedirectAttributes ra) {
        toppingRepository.findById(id).ifPresent(t -> {
            t.setDeletedAt(LocalDateTime.now());
            t.setStatus(0);
            toppingRepository.save(t);
        });
        ra.addFlashAttribute("message", "Đã chuyển topping vào thùng rác.");
        return "redirect:/admin/toppings";
    }

    // --- Other admin pages ---
    @GetMapping("/branches")
    public String showBranches(Model model) {
        model.addAttribute("pageTitle", "Cửa hàng");
        model.addAttribute("currentPage", "branches");
        return "admin/branches";
    }

    @GetMapping("/orders")
    public String showOrders(Model model) {
        model.addAttribute("pageTitle", "Đơn hàng");
        model.addAttribute("currentPage", "orders");
        return "admin/orders";
    }

    @GetMapping("/marketing")
    public String showMarketing(Model model) {
        model.addAttribute("pageTitle", "Marketing");
        model.addAttribute("currentPage", "marketing");
        return "admin/marketing";
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        model.addAttribute("pageTitle", "Cài đặt");
        model.addAttribute("currentPage", "settings");
        return "admin/settings";
    }
}
