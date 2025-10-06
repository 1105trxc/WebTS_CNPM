package com.alotra.controller;

import com.alotra.repository.NhanVienRepository;
import com.alotra.service.VendorOrderService;

/**
 * Deprecated duplicate of vendor controller. Kept only for reference.
 * Use com.alotra.controller.vendor.VendorController instead.
 */
@Deprecated
public class VendorController {

    // This class is intentionally deactivated to avoid bean conflicts.
    // All vendor routes are handled in com.alotra.controller.vendor.VendorController

    private final VendorOrderService orders;
    private final NhanVienRepository nhanVienRepository;

    public VendorController(VendorOrderService orders, NhanVienRepository nhanVienRepository) {
        this.orders = orders;
        this.nhanVienRepository = nhanVienRepository;
    }
}
