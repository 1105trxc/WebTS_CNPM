package com.alotra.controller.api;

import com.alotra.dto.ProductDTO;
import com.alotra.entity.Category;
import com.alotra.service.CategoryService;
import com.alotra.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchApiController {
    private final ProductService productService;
    private final CategoryService categoryService;

    public SearchApiController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/suggest")
    public Map<String, Object> suggest(@RequestParam(value = "q", required = false) String q) {
        String kw = q == null ? "" : q.trim();
        List<ProductDTO> products = kw.isEmpty() ? productService.findBestSellers() : productService.search(kw);
        if (products.size() > 8) products = products.subList(0, 8);
        List<Map<String, Object>> prodList = new ArrayList<>();
        for (ProductDTO p : products) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("imageUrl", p.getImageUrl());
            m.put("price", p.getPrice());
            prodList.add(m);
        }

        List<Category> cats = categoryService.findActive();
        if (!kw.isEmpty()) {
            String lower = kw.toLowerCase(Locale.ROOT);
            cats = cats.stream().filter(c -> c.getName() != null && c.getName().toLowerCase(Locale.ROOT).contains(lower)).collect(Collectors.toList());
        }
        if (cats.size() > 6) cats = cats.subList(0, 6);
        List<Map<String, Object>> catList = new ArrayList<>();
        for (Category c : cats) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            catList.add(m);
        }

        // Simple static trending keywords; could be replaced by analytics later
        List<String> trending = Arrays.asList("Trà sữa", "Trà đào", "Trân châu", "Ít đường", "Size L", "Topping phô mai");
        List<String> keywords = trending;
        if (!kw.isEmpty()) {
            String lower = kw.toLowerCase(Locale.ROOT);
            keywords = trending.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(lower)).collect(Collectors.toList());
            if (keywords.isEmpty()) {
                keywords = List.of(kw);
            }
        }
        if (keywords.size() > 6) keywords = keywords.subList(0, 6);

        Map<String, Object> res = new HashMap<>();
        res.put("products", prodList);
        res.put("categories", catList);
        res.put("keywords", keywords);
        return res;
    }
}