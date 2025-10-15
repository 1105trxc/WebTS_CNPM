package com.alotra.service;

import com.alotra.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class OrderService {
    private static final ZoneId HCM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @PersistenceContext
    private EntityManager em;

    public OrderService() {}

    // List orders for a specific customer (legacy)
    public List<OrderRow> listOrdersByCustomer(Integer customerId, String status) {
        return listOrdersByCustomer(customerId, status, null, null, null);
    }

    // New: List orders for a specific customer with optional status, exact code and date range
    public List<OrderRow> listOrdersByCustomer(Integer customerId, String status, Integer orderId,
                                               LocalDateTime from, LocalDateTime to) {
        StringBuilder jpql = new StringBuilder("SELECT dh FROM DonHang dh WHERE dh.customer.id = :cid");
        Map<String, Object> params = new HashMap<>();
        params.put("cid", customerId);
        if (status != null && !status.isBlank()) {
            jpql.append(" AND dh.status = :st");
            params.put("st", status);
        }
        if (orderId != null) {
            jpql.append(" AND dh.id = :oid");
            params.put("oid", orderId);
        }
        if (from != null) {
            jpql.append(" AND dh.createdAt >= :from");
            params.put("from", from);
        }
        if (to != null) {
            jpql.append(" AND dh.createdAt <= :to");
            params.put("to", to);
        }
        jpql.append(" ORDER BY dh.id DESC");
        TypedQuery<DonHang> q = em.createQuery(jpql.toString(), DonHang.class);
        params.forEach(q::setParameter);
        List<DonHang> list = q.getResultList();
        List<OrderRow> out = new ArrayList<>();
        for (DonHang dh : list) out.add(mapOrderRow(dh));
        return out;
    }

    // Ensure an order belongs to a customer then return it
    public OrderRow getOrderOfCustomer(Integer orderId, Integer customerId) {
        TypedQuery<DonHang> q = em.createQuery(
                "SELECT dh FROM DonHang dh WHERE dh.id = :id AND dh.customer.id = :cid", DonHang.class);
        q.setParameter("id", orderId);
        q.setParameter("cid", customerId);
        List<DonHang> list = q.getResultList();
        return list.isEmpty() ? null : mapOrderRow(list.get(0));
    }

    // Order items with variant (product + size)
    public List<OrderItemRow> listOrderItems(Integer orderId) {
        // Fetch joins to avoid N+1
        TypedQuery<CTDonHang> q = em.createQuery(
                "SELECT ct FROM CTDonHang ct " +
                "JOIN FETCH ct.variant v " +
                "JOIN FETCH v.product p " +
                "LEFT JOIN FETCH v.size s " +
                "WHERE ct.order.id = :oid ORDER BY ct.id", CTDonHang.class);
        q.setParameter("oid", orderId);
        List<CTDonHang> items = q.getResultList();
        List<OrderItemRow> out = new ArrayList<>();
        for (CTDonHang ct : items) {
            OrderItemRow r = new OrderItemRow();
            r.id = ct.getId();
            ProductVariant v = ct.getVariant();
            Product p = v != null ? v.getProduct() : null;
            SizeSanPham sz = v != null ? v.getSize() : null;
            r.productName = p != null ? p.getName() : null;
            r.sizeName = sz != null ? sz.getName() : null;
            r.quantity = ct.getQuantity();
            r.unitPrice = ct.getUnitPrice();
            r.lineDiscount = ct.getLineDiscount();
            r.lineTotal = ct.getLineTotal();
            try { r.note = ct.getNote(); } catch (Exception ignore) { r.note = null; }
            out.add(r);
        }
        return out;
    }

    // Toppings per order item
    public List<ItemToppingRow> listOrderItemToppings(Integer orderItemId) {
        TypedQuery<CTDonHangTopping> q = em.createQuery(
                "SELECT t FROM CTDonHangTopping t JOIN FETCH t.topping tp WHERE t.orderLine.id = :lid ORDER BY tp.name",
                CTDonHangTopping.class);
        q.setParameter("lid", orderItemId);
        List<CTDonHangTopping> rows = q.getResultList();
        List<ItemToppingRow> out = new ArrayList<>();
        for (CTDonHangTopping t : rows) {
            ItemToppingRow r = new ItemToppingRow();
            Topping tp = t.getTopping();
            r.toppingName = tp != null ? tp.getName() : null;
            r.quantity = t.getQuantity();
            r.unitPrice = t.getUnitPrice();
            r.total = t.getLineTotal();
            out.add(r);
        }
        return out;
    }

    // Fetch order header by orderId regardless of customer (for vendor view)
    public OrderRow getOrder(Integer orderId) {
        DonHang dh = em.find(DonHang.class, orderId);
        return dh == null ? null : mapOrderRow(dh);
    }

    private OrderRow mapOrderRow(DonHang dh) {
        OrderRow r = new OrderRow();
        r.id = dh.getId();
        LocalDateTime ts = dh.getCreatedAt();
        r.createdAt = ts != null ? OffsetDateTime.of(ts, HCM_ZONE.getRules().getOffset(ts)) : null;
        r.status = dh.getStatus();
        r.paymentStatus = dh.getPaymentStatus();
        r.paymentMethod = dh.getPaymentMethod();
        r.total = dh.getTongThanhToan();
        // New fields
        try { r.receivingMethod = dh.getReceivingMethod(); } catch (Exception ignore) {}
        try { r.receiverName = dh.getReceiverName(); } catch (Exception ignore) {}
        try { r.receiverPhone = dh.getReceiverPhone(); } catch (Exception ignore) {}
        try { r.shippingAddress = dh.getShippingAddress(); } catch (Exception ignore) {}
        return r;
    }

    public static class OrderRow {
        public Integer id;
        public java.time.OffsetDateTime createdAt;
        public String status;
        public String paymentStatus;
        public String paymentMethod;
        public java.math.BigDecimal total;
        // New fields
        public String receivingMethod;
        public String receiverName;
        public String receiverPhone;
        public String shippingAddress;
    }

    public static class OrderItemRow {
        public Integer id; // MaCT
        public String productName;
        public String sizeName;
        public Integer quantity;
        public java.math.BigDecimal unitPrice;
        public java.math.BigDecimal lineDiscount;
        public java.math.BigDecimal lineTotal;
        public String note; // GhiChu: sugar/ice
    }

    public static class ItemToppingRow {
        public String toppingName;
        public Integer quantity;
        public java.math.BigDecimal unitPrice;
        public java.math.BigDecimal total;
    }
}