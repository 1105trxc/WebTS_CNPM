package com.alotra.controller;

import com.alotra.dto.ProductDTO;
import com.alotra.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {
    private final ProductService productService;

    public SearchController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false) String q, Model model) {
        String kw = q == null ? "" : q.trim();
        List<ProductDTO> results = kw.isEmpty() ? productService.findBestSellers() : productService.search(kw);
        model.addAttribute("pageTitle", kw.isEmpty() ? "Tìm kiếm" : ("Tìm kiếm: " + kw));
        model.addAttribute("q", kw);
        model.addAttribute("results", results);
        return "search/results";
    }
}
