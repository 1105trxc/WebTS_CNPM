package com.alotra.controller;


import com.alotra.dto.ProductDTO; // Thêm import
import com.alotra.service.ProductService; // Thêm import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List; // Thêm import

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.alotra.dto.ProductDTO;
import com.alotra.entity.KhachHang;
import com.alotra.service.KhachHangService;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import com.alotra.repository.SuKienKhuyenMaiRepository;
import com.alotra.repository.KhuyenMaiSanPhamRepository;
import com.alotra.entity.SuKienKhuyenMai;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import com.alotra.service.OtpService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HomeController {
	 @Autowired
	    private ProductService productService;
    @Autowired
    private KhachHangService khachHangService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private OtpService otpService;
    // Promotions
    @Autowired private SuKienKhuyenMaiRepository promoRepo;
    @Autowired private KhuyenMaiSanPhamRepository promoLinkRepo;

    @GetMapping("/")
    public String homePage(Model model) {
        model.addAttribute("pageTitle", "AloTra - Trang Chủ");
        // Sau này bạn có thể thêm dữ liệu sản phẩm nổi bật vào đây
     // Lấy danh sách sản phẩm bán chạy từ service
        List<ProductDTO> bestSellers = productService.findBestSellers();

        // Đưa danh sách vào model với tên là "bestSellers" để HTML có thể dùng
        model.addAttribute("bestSellers", bestSellers);

        // Tin tức & Khuyến mãi: lấy tối đa 8 sự kiện đang hoạt động
        List<SuKienKhuyenMai> promos = promoRepo.findTop8ByStatusOrderByStartDateDesc(1);
        List<PromotionCard> cards = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        for (SuKienKhuyenMai p : promos) {
            // Ưu tiên ảnh riêng của sự kiện; nếu trống thì lấy 1 ảnh SP áp dụng; fallback placeholder
            String eventImg = (p.getImageUrl() != null && !p.getImageUrl().isBlank()) ? p.getImageUrl() : null;
            String fallbackImg = promoLinkRepo.findByPromotion(p).stream()
                    .map(l -> l.getProduct())
                    .filter(pr -> pr != null && pr.getImageUrl() != null && !pr.getImageUrl().isBlank())
                    .map(pr -> pr.getImageUrl())
                    .findFirst()
                    .orElse(null);
            String imageUrl = eventImg != null ? eventImg : (fallbackImg != null ? fallbackImg : "/images/placeholder.png");
            String period = (p.getStartDate() != null ? df.format(p.getStartDate()) : "?") +
                    " - " + (p.getEndDate() != null ? df.format(p.getEndDate()) : "?");
            int views = p.getViews() == null ? 0 : p.getViews();
            cards.add(new PromotionCard(p.getId(), p.getName(), p.getDescription(), imageUrl, period, views));
        }
        model.addAttribute("promotions", cards);
        return "home/index"; // Trả về tên file template (home/index.html)
    }

    // Tạm thời tạo các controller cơ bản cho các link trên header/footer để tránh lỗi 404
    @GetMapping("/products")
    public String productsPage(Model model) {
        model.addAttribute("pageTitle", "Sản Phẩm của AloTra");
        return "products/product_list"; // Sẽ tạo trang này sau
    }

    @GetMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("pageTitle", "Về Chúng Tôi");
        return "about/about"; // Sẽ tạo trang này sau
    }

    @GetMapping("/contact")
    public String contactPage(Model model) {
        model.addAttribute("pageTitle", "Liên Hệ AloTra");
        return "contact/contact"; // Sẽ tạo trang này sau
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Đăng Nhập");
        return "auth/login"; // Sẽ tạo trang này sau
    }

    @GetMapping("/policy")
    public String policyPage(Model model) {
        model.addAttribute("pageTitle", "Chính Sách");
        return "policy/policy"; // Sẽ tạo trang này sau
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "Đăng Ký Tài Khoản");
        model.addAttribute("khachHang", new KhachHang());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("khachHang") KhachHang khachHang,
                                  BindingResult bindingResult,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  Model model,
                                  RedirectAttributes ra) {
        model.addAttribute("pageTitle", "Đăng Ký Tài Khoản");
        // Validate password confirmation
        if (khachHang.getPasswordHash() == null || !khachHang.getPasswordHash().equals(confirmPassword)) {
            bindingResult.rejectValue("passwordHash", "error.khachHang", "Mật khẩu xác nhận không khớp.");
        }
        // Check for duplicate username/email/phone
        if (khachHangService.findByUsername(khachHang.getUsername()) != null) {
            bindingResult.rejectValue("username", "error.khachHang", "Tên đăng nhập đã được sử dụng.");
        }
        if (khachHangService.findByEmail(khachHang.getEmail()) != null) {
            bindingResult.rejectValue("email", "error.khachHang", "Email đã được sử dụng.");
        }
        if (khachHangService.findByPhone(khachHang.getPhone()) != null) {
            bindingResult.rejectValue("phone", "error.khachHang", "Số điện thoại đã được sử dụng.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        // Hash password and set status to INACTIVE until OTP verified
        khachHang.setPasswordHash(passwordEncoder.encode(khachHang.getPasswordHash()));
        khachHang.setStatus(0); // 0 = inactive until verified
        khachHang = khachHangService.save(khachHang);
        // Send OTP to email
        otpService.sendRegisterOtp(khachHang);
        // Redirect to verify OTP page
        ra.addFlashAttribute("msg", "Đã gửi mã OTP vào email. Vui lòng kiểm tra hộp thư.");
        return "redirect:/verify-otp?email=" + khachHang.getEmail();
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtp(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("pageTitle", "Xác thực email");
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String doVerifyOtp(@RequestParam String email,
                              @RequestParam String code,
                              RedirectAttributes ra) {
        StringBuilder err = new StringBuilder();
        boolean ok = otpService.verifyRegisterOtp(email, code, err);
        if (ok) {
            return "redirect:/login?activated=1";
        }
        ra.addFlashAttribute("error", err.length() > 0 ? err.toString() : "Xác thực thất bại.");
        return "redirect:/verify-otp?email=" + email;
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@RequestParam String email, RedirectAttributes ra) {
        boolean ok = otpService.resendRegisterOtp(email);
        if (ok) {
            ra.addFlashAttribute("msg", "Đã gửi lại mã OTP vào email.");
        } else {
            ra.addFlashAttribute("error", "Không thể gửi lại mã OTP.");
        }
        return "redirect:/verify-otp?email=" + email;
    }

    // Simple view-model for promotions on homepage
    public static class PromotionCard {
        public Integer id;
        public String title;
        public String description;
        public String imageUrl;
        public String periodText;
        public int views;
        public PromotionCard(Integer id, String title, String description, String imageUrl, String periodText, int views) {
            this.id = id; this.title = title; this.description = description; this.imageUrl = imageUrl; this.periodText = periodText; this.views = views;
        }
    }
}