package com.alotra.web;

import com.alotra.dto.ProductDTO;
import com.alotra.entity.Category;
import com.alotra.service.CategoryService;
import com.alotra.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalModelAttributes {
    private final CategoryService categoryService;
    private final ProductService productService;

    public GlobalModelAttributes(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @ModelAttribute("megaMenuCategories")
    public List<MegaMenuCategory> populateMegaMenu() {
        List<Category> categories = categoryService.findActive();
        List<MegaMenuCategory> result = new ArrayList<>();
        for (Category c : categories) {
            List<ProductDTO> prods = productService.listByCategory(c.getId());
            if (prods.size() > 6) {
                prods = prods.subList(0, 6);
            }
            MegaMenuCategory mc = new MegaMenuCategory();
            mc.setId(c.getId());
            mc.setName(c.getName());
            mc.setProducts(prods);
            result.add(mc);
        }
        return result;
    }

    public static class MegaMenuCategory {
        private Integer id;
        private String name;
        private List<ProductDTO> products;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<ProductDTO> getProducts() { return products; }
        public void setProducts(List<ProductDTO> products) { this.products = products; }
    }
}
