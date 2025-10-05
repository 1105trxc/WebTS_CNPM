package com.alotra.service;

import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CartService {
    private final GioHangRepository cartRepo;
    private final GioHangCTRepository itemRepo;
    private final ProductVariantRepository variantRepo;
    private final ProductRepository productRepo;
    private final DonHangRepository orderRepo;
    private final CTDonHangRepository orderLineRepo;
    private final GioHangCTToppingRepository cartToppingRepo;
    private final ToppingRepository toppingRepo;
    private final CTDonHangToppingRepository orderToppingRepo;

    public CartService(GioHangRepository cartRepo, GioHangCTRepository itemRepo,
                       ProductVariantRepository variantRepo, ProductRepository productRepo,
                       DonHangRepository orderRepo, CTDonHangRepository orderLineRepo,
                       GioHangCTToppingRepository cartToppingRepo, ToppingRepository toppingRepo,
                       CTDonHangToppingRepository orderToppingRepo) {
        this.cartRepo = cartRepo;
        this.itemRepo = itemRepo;
        this.variantRepo = variantRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.orderLineRepo = orderLineRepo;
        this.cartToppingRepo = cartToppingRepo;
        this.toppingRepo = toppingRepo;
        this.orderToppingRepo = orderToppingRepo;
    }

    @Transactional
    public GioHang getOrCreateActiveCart(KhachHang kh) {
        return cartRepo.findFirstByCustomerAndStatus(kh, "ACTIVE").orElseGet(() -> {
            GioHang g = new GioHang();
            g.setCustomer(kh);
            g.setStatus("ACTIVE");
            return cartRepo.save(g);
        });
    }

    @Transactional
    public GioHangCT addItem(KhachHang kh, Integer productId, Integer variantId, int qty, String note) {
        if (qty <= 0) qty = 1;
        GioHang cart = getOrCreateActiveCart(kh);

        ProductVariant variant = resolveVariant(productId, variantId);
        if (variant == null) {
            throw new IllegalArgumentException("Không tìm thấy biến thể hợp lệ để thêm vào giỏ.");
        }
        BigDecimal unitPrice = variant.getPrice();
        // merge same variant line
        GioHangCT line = itemRepo.findByCartAndVariant(cart, variant).orElseGet(() -> {
            GioHangCT l = new GioHangCT();
            l.setCart(cart);
            l.setVariant(variant);
            l.setQuantity(0);
            l.setUnitPrice(unitPrice);
            l.setLineTotal(BigDecimal.ZERO);
            return l;
        });
        line.setQuantity(line.getQuantity() + qty);
        line.setUnitPrice(unitPrice); // snapshot current variant price
        line.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())));
        line.setNote(note);
        return itemRepo.save(line);
    }

    @Transactional
    public GioHangCT addItemWithOptions(KhachHang kh, Integer variantId, int qty, Map<Integer,Integer> toppingQty, String note) {
        if (qty <= 0) qty = 1;
        GioHang cart = getOrCreateActiveCart(kh);
        ProductVariant variant = resolveVariant(null, variantId);
        if (variant == null) throw new IllegalArgumentException("Biến thể không hợp lệ.");
        BigDecimal base = variant.getPrice();
        // Create a fresh line (do not merge when options differ)
        GioHangCT line = new GioHangCT();
        line.setCart(cart);
        line.setVariant(variant);
        line.setQuantity(qty);
        line.setUnitPrice(base);
        line.setNote(note);
        // Compute toppings per drink and total
        BigDecimal toppingPerDrink = BigDecimal.ZERO;
        if (toppingQty != null) {
            for (Map.Entry<Integer,Integer> e : toppingQty.entrySet()) {
                Integer tid = e.getKey();
                Integer perDrink = e.getValue();
                if (perDrink == null || perDrink <= 0) continue;
                Topping t = toppingRepo.findById(tid).orElse(null);
                if (t == null) continue;
                BigDecimal up = t.getExtraPrice();
                toppingPerDrink = toppingPerDrink.add(up.multiply(BigDecimal.valueOf(perDrink)));
            }
        }
        BigDecimal lineTotal = base.add(toppingPerDrink).multiply(BigDecimal.valueOf(qty));
        line.setLineTotal(lineTotal);
        line = itemRepo.save(line);
        // Persist topping items as total quantity across line (perDrink * qty)
        if (toppingQty != null) {
            for (Map.Entry<Integer,Integer> e : toppingQty.entrySet()) {
                Integer tid = e.getKey();
                Integer perDrink = e.getValue();
                if (perDrink == null || perDrink <= 0) continue;
                Topping t = toppingRepo.findById(tid).orElse(null);
                if (t == null) continue;
                GioHangCTTopping ct = new GioHangCTTopping();
                ct.setCartItem(line);
                ct.setTopping(t);
                ct.setQuantity(perDrink * qty);
                ct.setUnitPrice(t.getExtraPrice());
                ct.setLineTotal(t.getExtraPrice().multiply(BigDecimal.valueOf(ct.getQuantity())));
                cartToppingRepo.save(ct);
            }
        }
        return line;
    }

    private ProductVariant resolveVariant(Integer productId, Integer variantId) {
        if (variantId != null) {
            return variantRepo.findById(variantId).orElse(null);
        }
        if (productId != null) {
            return productRepo.findById(productId)
                    .map(p -> variantRepo.findByProduct(p).stream()
                            .filter(v -> v.getStatus() != null && v.getStatus() == 1)
                            .min(Comparator.comparing(ProductVariant::getPrice))
                            .orElse(null))
                    .orElse(null);
        }
        return null;
    }

    public List<GioHangCT> listItems(KhachHang kh) {
        GioHang c = cartRepo.findFirstByCustomerAndStatus(kh, "ACTIVE").orElse(null);
        if (c == null) return List.of();
        return itemRepo.findByCart(c);
    }

    @Transactional
    public void updateQuantity(KhachHang kh, Integer itemId, int qty) {
        GioHangCT item = itemRepo.findById(itemId).orElseThrow();
        validateOwnership(kh, item);
        if (qty <= 0) {
            itemRepo.delete(item);
            return;
        }
        item.setQuantity(qty);
        // Recompute line total including toppings linked to this cart item
        BigDecimal toppingTotal = cartToppingRepo.findAll().stream()
                .filter(t -> t.getCartItem().getId().equals(item.getId()))
                .map(GioHangCTTopping::getUnitPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // toppingTotal above is per-unit sum? We don't have perDrink; recompute from quantities instead
        toppingTotal = cartToppingRepo.findAll().stream()
                .filter(t -> t.getCartItem().getId().equals(item.getId()))
                .map(t -> t.getUnitPrice().multiply(BigDecimal.valueOf(t.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // item total = base*qty + toppingTotal
        BigDecimal newTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(qty)).add(toppingTotal);
        item.setLineTotal(newTotal);
        itemRepo.save(item);
    }

    @Transactional
    public void removeItem(KhachHang kh, Integer itemId) {
        GioHangCT item = itemRepo.findById(itemId).orElseThrow();
        validateOwnership(kh, item);
        itemRepo.delete(item);
    }

    private void validateOwnership(KhachHang kh, GioHangCT item) {
        if (!Objects.equals(item.getCart().getCustomer().getId(), kh.getId())) {
            throw new SecurityException("Không có quyền với mục giỏ hàng này");
        }
    }

    public BigDecimal calcTotal(List<GioHangCT> items) {
        return items.stream().map(GioHangCT::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public DonHang checkout(KhachHang kh, List<Integer> itemIds, String paymentMethod) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn sản phẩm để đặt hàng");
        }
        List<GioHangCT> items = new ArrayList<>();
        for (Integer id : itemIds) {
            GioHangCT item = itemRepo.findById(id).orElseThrow();
            validateOwnership(kh, item);
            items.add(item);
        }
        DonHang order = new DonHang();
        order.setCustomer(kh);
        order.setPaymentMethod(paymentMethod);
        // Compute totals
        BigDecimal tongHang = calcTotal(items);
        order.setTongHang(tongHang);
        order.setGiamGiaDon(BigDecimal.ZERO);
        order.setPhiVanChuyen(BigDecimal.ZERO);
        order.setTongThanhToan(tongHang);
        order = orderRepo.save(order);
        for (GioHangCT ci : items) {
            CTDonHang ol = new CTDonHang();
            ol.setOrder(order);
            ol.setVariant(ci.getVariant());
            ol.setQuantity(ci.getQuantity());
            ol.setUnitPrice(ci.getUnitPrice());
            ol.setLineDiscount(BigDecimal.ZERO);
            ol.setLineTotal(ci.getLineTotal());
            ol = orderLineRepo.save(ol);
            // Copy toppings from cart item to order line
            for (GioHangCTTopping ct : cartToppingRepo.findAll()) {
                if (!ct.getCartItem().getId().equals(ci.getId())) continue;
                CTDonHangTopping ot = new CTDonHangTopping();
                ot.setOrderLine(ol);
                ot.setTopping(ct.getTopping());
                ot.setQuantity(ct.getQuantity());
                ot.setUnitPrice(ct.getUnitPrice());
                ot.setLineTotal(ct.getLineTotal());
                orderToppingRepo.save(ot);
            }
        }
        // Remove checked-out items from cart
        for (GioHangCT ci : items) itemRepo.delete(ci);
        return order;
    }
}