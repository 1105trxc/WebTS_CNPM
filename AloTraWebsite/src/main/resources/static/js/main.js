"use strict";

$(document).ready(function() {
    console.log("AloTra Website - Frontend Initialized!");

    // ===================================================================
    // GLOBALS: context path + CSRF
    // ===================================================================
    const ctx = (window.ctx || '').replace(/\/$/, '') + '/';
    const csrfToken = $('meta[name="_csrf"]').attr('content');
    const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

    function api(url) { return ctx + url.replace(/^\//, ''); }

    // ===================================================================
    // VIEW MORE buttons
    // ===================================================================
    const productContainer = $("#product-list-container");
    const viewMoreBtn = $("#view-more-btn");

    if (productContainer.length && viewMoreBtn.length) {
        const hiddenProducts = productContainer.find(".col:gt(4)").hide();
        if (hiddenProducts.length === 0) {
            viewMoreBtn.hide();
        }
        viewMoreBtn.on('click', function() {
            hiddenProducts.show();
            $(this).hide();
        });
    }

    // Smooth scroll for in-page anchors
    $('a[href^="#"]').on('click', function(event) {
        const href = $(this).attr('href');
        if (href && href.length > 1) {
            const target = $(href);
            if (target.length) {
                event.preventDefault();
                $('html, body').stop().animate({ scrollTop: target.offset().top }, 800);
            }
        }
    });

    // ===================================================================
    // CART BADGE SYNC
    // ===================================================================
    const $badge = $('#cart-count-badge');
    function setBadge(n) {
        const v = Number(n || 0);
        if ($badge.length) $badge.text(v);
    }
    function refreshCartCount() {
        $.ajax({
            url: api('api/cart/count'),
            method: 'GET',
            success: function(res){ setBadge(res && typeof res.count !== 'undefined' ? res.count : 0); },
            error: function(){ /* ignore when not logged in */ setBadge(0); }
        });
    }

    // call on load and every 60s
    refreshCartCount();
    setInterval(refreshCartCount, 60000);

    // ===================================================================
    // ADD TO CART + BUY NOW
    // ===================================================================
    function parseProductIdFromHref(href) {
        try {
            const url = new URL(href, window.location.origin);
            const pid = url.searchParams.get('productId');
            return pid ? parseInt(pid, 10) : null;
        } catch { return null; }
    }

    $(document).on('click', '.btn-add-to-cart', function(e) {
        const $btn = $(this);
        const go = ($btn.data('go') || '').toString(); // 'home' or 'cart' or ''
        let productId = $btn.data('product-id');
        if (!productId) {
            const href = $btn.attr('href') || '';
            productId = parseProductIdFromHref(href);
        }
        if (!productId) {
            // fall back: let the default link work
            return;
        }
        e.preventDefault();
        // POST /api/cart/add
        $.ajax({
            url: api('api/cart/add'),
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ productId: productId, quantity: 1 }),
            beforeSend: function(xhr){ if (csrfToken && csrfHeader) xhr.setRequestHeader(csrfHeader, csrfToken); },
            success: function(res){
                setBadge(res && typeof res.count !== 'undefined' ? res.count : undefined);
                if (go === 'cart') {
                    window.location.href = api('cart');
                } else if (go === 'home') {
                    window.location.href = api('');
                } else {
                    // stay and show feedback
                    try { bootstrap.Toast && showToast('Đã thêm vào giỏ hàng.'); } catch { alert('Đã thêm vào giỏ hàng.'); }
                }
            },
            statusCode: {
                401: function(){ window.location.href = api('login'); }
            },
            error: function(xhr){ if (xhr.status !== 401) alert('Không thể thêm sản phẩm.'); }
        });
    });

    function showToast(msg){
        const id = 'cart-toast';
        let el = document.getElementById(id);
        if (!el) {
            const tpl = document.createElement('div');
            tpl.id = id;
            tpl.className = 'toast align-items-center text-bg-success border-0 position-fixed';
            tpl.style.right = '16px';
            tpl.style.bottom = '16px';
            tpl.setAttribute('role','alert');
            tpl.innerHTML = '<div class="d-flex"><div class="toast-body"></div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button></div>';
            document.body.appendChild(tpl);
            el = tpl;
        }
        el.querySelector('.toast-body').textContent = msg;
        const t = new bootstrap.Toast(el, { delay: 1500 });
        t.show();
    }

    // ===================================================================
    // MEGA MENU: mobile/touch toggle
    // ===================================================================
    const $megaItem = $('.has-mega');
    const $megaToggle = $('.mega-toggle');
    function isMobileNav(){ return window.matchMedia('(max-width: 991.98px)').matches; }
    $megaToggle.on('click', function(e){
        if (isMobileNav()) {
            e.preventDefault();
            const $li = $(this).closest('.has-mega');
            const willOpen = !$li.hasClass('open');
            $('.has-mega').removeClass('open');
            if (willOpen) $li.addClass('open');
        }
    });
    $(document).on('click', function(e){
        if (!$(e.target).closest('.has-mega').length) {
            $('.has-mega').removeClass('open');
        }
    });
    $(document).on('keydown', function(e){ if (e.key === 'Escape') $('.has-mega').removeClass('open'); });
});