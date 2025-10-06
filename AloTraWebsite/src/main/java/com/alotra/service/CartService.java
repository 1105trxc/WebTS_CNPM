package com.alotra.service;

import com.alotra.entity.*;
import com.alotra.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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
    private final KhuyenMaiSanPhamRepository promoRepo; // new

    public CartService(GioHangRepository cartRepo, GioHangCTRepository itemRepo,
                       ProductVariantRepository variantRepo, ProductRepository productRepo,
                       DonHangRepository orderRepo, CTDonHangRepository orderLineRepo,
                       GioHangCTToppingRepository cartToppingRepo, ToppingRepository toppingRepo,
                       CTDonHangToppingRepository orderToppingRepo,
                       KhuyenMaiSanPhamRepository promoRepo) {
        this.cartRepo = cartRepo;
        this.itemRepo = itemRepo;
        this.variantRepo = variantRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.orderLineRepo = orderLineRepo;
        this.cartToppingRepo = cartToppingRepo;
        this.toppingRepo = toppingRepo;
        this.orderToppingRepo = orderToppingRepo;
        this.promoRepo = promoRepo;
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
        // Apply active product promotion percent to base price
        BigDecimal basePrice = variant.getPrice();
        Integer percent = (variant.getProduct() != null) ? promoRepo.findActiveMaxDiscountPercentForProduct(variant.getProduct().getId()) : null;
        BigDecimal unitPrice = applyPercent(basePrice, percent);
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
        line.setUnitPrice(unitPrice); // snapshot discounted price
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
        // Apply active product promotion to base
        BigDecimal base = variant.getPrice();
        Integer percent = (variant.getProduct() != null) ? promoRepo.findActiveMaxDiscountPercentForProduct(variant.getProduct().getId()) : null;
        BigDecimal unitPrice = applyPercent(base, percent);
        // Create a fresh line (do not merge when options differ)
        GioHangCT line = new GioHangCT();
        line.setCart(cart);
        line.setVariant(variant);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
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
        BigDecimal lineTotal = unitPrice.add(toppingPerDrink).multiply(BigDecimal.valueOf(qty));
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

    private BigDecimal applyPercent(BigDecimal base, Integer percent) {
        if (base == null) return null;
        if (percent == null || percent <= 0) return base;
        java.math.RoundingMode RM = java.math.RoundingMode.HALF_UP;
        java.math.BigDecimal p = java.math.BigDecimal.valueOf(100 - Math.min(100, percent))
                .divide(java.math.BigDecimal.valueOf(100), 4, RM);
        return base.multiply(p).setScale(0, RM);
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
        // Only allow items from the current ACTIVE cart of this customer
        GioHang activeCart = getOrCreateActiveCart(kh);
        // Load all existing items by IDs (ignores missing IDs), then keep only those in this cart
        List<GioHangCT> items = itemRepo.findAllById(new HashSet<>(itemIds)).stream()
                .filter(it -> it.getCart() != null && Objects.equals(it.getCart().getId(), activeCart.getId()))
                .collect(Collectors.toList());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm hợp lệ để đặt hàng");
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
            for (GioHangCTTopping ct : cartToppingRepo.findByCartItem(ci)) {
                CTDonHangTopping ot = new CTDonHangTopping();
                ot.setOrderLine(ol);
                ot.setTopping(ct.getTopping());
                ot.setQuantity(ct.getQuantity());
                ot.setUnitPrice(ct.getUnitPrice());
                ot.setLineTotal(ct.getLineTotal());
                orderToppingRepo.save(ot);
            }
        }
        // Remove checked-out items from cart (toppings will be deleted by ON DELETE CASCADE; manual cleanup safe)
        for (GioHangCT ci : items) {
            for (GioHangCTTopping t : cartToppingRepo.findByCartItem(ci)) {
                cartToppingRepo.delete(t);
            }
            itemRepo.delete(ci);
        }
        // If all items in the current ACTIVE cart were checked out, close the cart and open a fresh one
        boolean noMoreItems = itemRepo.findByCart(activeCart).isEmpty();
        if (noMoreItems) {
            activeCart.setStatus("CHECKED_OUT");
            cartRepo.save(activeCart);
            // Create a new ACTIVE cart for subsequent shopping
            getOrCreateActiveCart(kh);
        }
        return order;
    }

    // List all active toppings for UI
    public List<Topping> listActiveToppings() {
        return toppingRepo.findByDeletedAtIsNull();
    }

    // For the cart page: map itemId -> toppings on that item
    public Map<Integer, List<GioHangCTTopping>> getToppingsForItems(List<GioHangCT> items) {
        Map<Integer, List<GioHangCTTopping>> map = new HashMap<>();
        for (GioHangCT it : items) {
            map.put(it.getId(), cartToppingRepo.findByCartItem(it));
        }
        return map;
    }

    @Transactional
    public void updateToppings(KhachHang kh, Integer itemId, Map<Integer,Integer> toppingQtyById) {
        GioHangCT item = itemRepo.findById(itemId).orElseThrow();
        validateOwnership(kh, item);
        // Existing toppings for this line
        List<GioHangCTTopping> existing = cartToppingRepo.findByCartItem(item);
        Map<Integer, GioHangCTTopping> existingByTid = existing.stream()
                .collect(Collectors.toMap(t -> t.getTopping().getId(), t -> t));
        // Upsert or delete based on incoming map
        if (toppingQtyById != null) {
            for (Map.Entry<Integer,Integer> e : toppingQtyById.entrySet()) {
                Integer tid = e.getKey();
                Integer qty = e.getValue() == null ? 0 : Math.max(0, e.getValue());
                Topping topping = toppingRepo.findById(tid).orElse(null);
                if (topping == null) continue;
                if (qty == 0) {
                    GioHangCTTopping exist = existingByTid.get(tid);
                    if (exist != null) cartToppingRepo.delete(exist);
                } else {
                    GioHangCTTopping exist = existingByTid.get(tid);
                    if (exist == null) {
                        exist = new GioHangCTTopping();
                        exist.setCartItem(item);
                        exist.setTopping(topping);
                    }
                    exist.setQuantity(qty);
                    exist.setUnitPrice(topping.getExtraPrice());
                    exist.setLineTotal(topping.getExtraPrice().multiply(BigDecimal.valueOf(qty)));
                    cartToppingRepo.save(exist);
                }
            }
        }
        recomputeLineTotal(item);
    }

    private void recomputeLineTotal(GioHangCT item) {
        // base*qty + sum(all topping line totals for this item)
        BigDecimal toppingSum = cartToppingRepo.findByCartItem(item).stream()
                .map(GioHangCTTopping::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal base = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setLineTotal(base.add(toppingSum));
        itemRepo.save(item);
    }
}