// 📁 com/alotra/dto/ProductDTO.java
package com.alotra.dto;

import java.math.BigDecimal;

public class ProductDTO {
    private Integer id;
    private String name;
    private String imageUrl;
    private BigDecimal price; // final price after discount (if any)

    // New fields for promotion display
    private BigDecimal originalPrice; // before discount
    private Integer discountPercent;  // max active percent

    // Constructors
    public ProductDTO() {}

    public ProductDTO(Integer id, String name, String imageUrl, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.price = price;
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }
}