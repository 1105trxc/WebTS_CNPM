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
    // SEARCH TYPEAHEAD
    // ===================================================================
    const $searchInput = $('#siteSearchInput');
    const $searchForm = $('#siteSearchForm');
    const $suggest = $('#siteSearchSuggest');
    let searchTimer = null;
    let lastQuery = '';

    function hideSuggest(){ $suggest.hide().empty(); }

    function renderSuggest(data){
        const products = data && Array.isArray(data.products) ? data.products : [];
        const categories = data && Array.isArray(data.categories) ? data.categories : [];
        const keywords = data && Array.isArray(data.keywords) ? data.keywords : [];
        let html = '';
        if (keywords.length) {
            html += '<div class="p-2 border-bottom"><div class="small text-muted mb-1">Gợi ý nhanh</div>';
            html += keywords.map(k => `<a class="btn btn-sm btn-outline-success me-1 mb-1 search-chip" href="${api('search?q=' + encodeURIComponent(k))}">${k}</a>`).join('');
            html += '</div>';
        }
        if (categories.length) {
            html += '<div class="p-2 border-bottom"><div class="small text-muted mb-1">Danh mục</div>';
            html += categories.map(c => `<a class="list-group-item list-group-item-action" href="${api('products?categoryId=' + encodeURIComponent(c.id))}"><i class="fa fa-folder-open me-2 text-success"></i>${c.name||''}</a>`).join('');
            html += '</div>';
        }
        if (products.length) {
            html += '<div class="list-group list-group-flush">';
            html += products.map(p => `
                <a class="list-group-item list-group-item-action d-flex align-items-center" href="${api('products/' + p.id)}">
                    <img src="${p.imageUrl || '/images/placeholder.png'}" alt="thumb" style="width:42px;height:42px;object-fit:cover;border-radius:6px;" class="me-2" />
                    <div class="flex-grow-1">
                        <div class="fw-semibold">${p.name||''}</div>
                        <div class="small text-success">${(p.price||0).toLocaleString('vi-VN')} ₫</div>
                    </div>
                </a>`).join('');
            html += '</div>';
        }
        if (!html) {
            html = '<div class="p-3 text-center text-muted">Không có gợi ý.</div>';
        }
        $suggest.html(html).show();
    }

    function fetchSuggest(q){
        $.ajax({ url: api('api/search/suggest'), data: { q: q||'' }, success: renderSuggest, error: function(){ hideSuggest(); } });
    }

    if ($searchInput.length && $suggest.length) {
        $searchInput.on('input', function(){
            const q = $(this).val() || '';
            if (q === lastQuery && $suggest.is(':visible')) return;
            lastQuery = q;
            clearTimeout(searchTimer);
            searchTimer = setTimeout(function(){ fetchSuggest(q); }, 200);
        });
        $searchInput.on('focus', function(){
            if (lastQuery === '' && !$suggest.is(':visible')) { fetchSuggest(''); } else { $suggest.show(); }
        });
        $(document).on('click', function(e){ if (!$(e.target).closest('#siteSearchForm').length) hideSuggest(); });
        // basic keyboard: enter submits form
        $searchInput.on('keydown', function(e){ if (e.key === 'Escape') { hideSuggest(); } });
        // Submit form navigates to /search?q=...
        $searchForm.on('submit', function(){ hideSuggest(); });
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

    // ===================================================================
    // NOTIFICATIONS: hover dropdown + badge count
    // ===================================================================
    const $notifArea = $('#notifArea');
    const $notifDropdown = $('#notifDropdown');
    const $notifList = $('#notifList');
    const $notifBadge = $('#notif-count-badge');
    let notifLastLoadedAt = 0;
    let notifHideTimer = null;

    function setNotifCount(n) {
        const v = Number(n || 0);
        if (!$notifBadge.length) return;
        if (v > 0) {
            $notifBadge.text(v).show();
        } else {
            $notifBadge.hide();
        }
    }

    function renderNotifications(items) {
        if (!$notifList.length) return;
        if (!items || !items.length) {
            $notifList.html('<div class="p-3 text-center text-muted">Không có thông báo.</div>');
            return;
        }
        const icon = (type) => {
            switch(type){
                case 'unpaid': return '<i class="fa fa-triangle-exclamation text-warning me-2"></i>';
                case 'review': return '<i class="fa fa-star text-warning me-2"></i>';
                case 'order': return '<i class="fa fa-receipt text-success me-2"></i>';
                default: return '<i class="fa fa-bell me-2"></i>';
            }
        };
        const html = items.map(it => `
            <a href="${api((it.url||'').replace(/^\//,''))}" class="list-group-item list-group-item-action d-flex align-items-start">
                <span class="me-2">${icon(it.type)}</span>
                <span>${it.text||''}</span>
            </a>`).join('');
        $notifList.html(html);
    }

    function loadNotifications(force) {
        const now = Date.now();
        if (!force && (now - notifLastLoadedAt) < 30000) return; // cache 30s
        $.ajax({
            url: api('api/notifications'),
            method: 'GET',
            success: function(res){
                setNotifCount(res && typeof res.count !== 'undefined' ? res.count : 0);
                renderNotifications(res && res.items ? res.items : []);
                notifLastLoadedAt = now;
            },
            statusCode: { 401: function(){ setNotifCount(0); if ($notifList.length) $notifList.html('<div class="p-3 text-center text-muted">Vui lòng đăng nhập để xem thông báo.</div>'); } },
            error: function(){ /* keep previous */ }
        });
    }

    function refreshNotifCount() {
        $.ajax({ url: api('api/notifications'), method: 'GET', success: function(res){ setNotifCount(res && typeof res.count !== 'undefined' ? res.count : 0); }, statusCode: {401: function(){ setNotifCount(0);} } });
    }

    if ($notifArea.length) {
        $notifArea.on('mouseenter', function(){
            if (notifHideTimer) { clearTimeout(notifHideTimer); notifHideTimer = null; }
            $notifDropdown.stop(true, true).fadeIn(80);
            loadNotifications(false);
        });
        $notifArea.on('mouseleave', function(){
            notifHideTimer = setTimeout(function(){ $notifDropdown.stop(true, true).fadeOut(80); }, 120);
        });
    }
    // Periodic count refresh
    refreshNotifCount();
    setInterval(refreshNotifCount, 60000);
});