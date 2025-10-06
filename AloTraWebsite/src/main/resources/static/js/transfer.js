function copyText(t) {
    if (!t) return;
    navigator.clipboard?.writeText(t).then(() => {}).catch(() => {});
}

(function () {
    // Resolve context/base path
    let base = '';
    if (typeof ctx !== 'undefined' && ctx) {
        base = ctx.replace(/\/$/, '');
    } else {
        // Try to infer from current URL, e.g. /alotra-website/payment/123
        const m = window.location.pathname.match(/^(.*)\/payment\//);
        base = m ? m[1] : '';
    }

    // DOM elements
    const box = document.getElementById('statusBox');
    const cd = document.getElementById('countdown');

    // Resolve orderId and expiry from globals or data-* attributes
    let oid = (typeof orderId !== 'undefined' && Number(orderId)) ? Number(orderId) : 0;
    if ((!oid || isNaN(oid)) && cd && cd.dataset && cd.dataset.orderId) {
        oid = Number(cd.dataset.orderId);
    }
    let expiry = (typeof expiryEpochMillis !== 'undefined' && Number(expiryEpochMillis)) ? Number(expiryEpochMillis) : 0;
    if ((!expiry || isNaN(expiry)) && cd && cd.dataset && cd.dataset.expiry) {
        expiry = Number(cd.dataset.expiry);
    }

    // Build URLs
    const statusUrl = base + '/payment/' + oid + '/status';
    const successUrl = base + '/payment/' + oid + '/success';

    let stopped = false;

    function renderCountdown() {
        if (!cd || !expiry) return;
        const ms = expiry - Date.now();
        if (ms <= 0) { cd.textContent = '00:00'; return; }
        const s = Math.floor(ms / 1000);
        const mm = String(Math.floor(s / 60)).padStart(2, '0');
        const ss = String(s % 60).padStart(2, '0');
        cd.textContent = mm + ':' + ss;
    }

    function poll() {
        if (stopped || !oid) return;
        fetch(statusUrl, { cache: 'no-store' })
            .then(r => r.ok ? r.json() : Promise.reject(new Error('HTTP ' + r.status)))
            .then(j => {
                if (!box) return;
                if (j.paymentStatus === 'DaThanhToan') {
                    stopped = true;
                    box.className = 'alert alert-success py-2';
                    box.textContent = 'Đã thanh toán. Đang chuyển...';
                    setTimeout(() => location.href = successUrl, 600);
                } else if (j.orderStatus === 'DaHuy') {
                    stopped = true;
                    box.className = 'alert alert-danger py-2';
                    box.textContent = 'Đơn hàng đã bị hủy.';
                } else {
                    box.className = 'alert alert-warning py-2';
                    box.textContent = 'Đang chờ thanh toán...';
                }
            })
            .catch(() => { /* ignore */ });
    }

    // Kick off
    try { renderCountdown(); poll(); } catch(e) { /* ignore */ }
    setInterval(() => { try { renderCountdown(); poll(); } catch(e) { /* ignore */ } }, 3000);
})();