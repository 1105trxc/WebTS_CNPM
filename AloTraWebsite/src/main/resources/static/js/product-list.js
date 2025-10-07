(function(){
  // Product List page interactions: sidebar filters + AJAX load
  // Safe no-op if elements are not present
  const grid = document.getElementById('catalogGrid');
  const filters = document.querySelectorAll('.catalog-sidebar [data-cat], .catalog-tabs .btn');
  if (!grid || !filters || filters.length === 0) {
    return; // Not on the product list page
  }

  const ctx = (window.ctx || '').replace(/\/$/, '');

  function vnd(n){
    try { return Number(n||0).toLocaleString('vi-VN') + ' ₫'; } catch { return (n||0) + ' ₫'; }
  }

  function cardHTML(p){
    const hasSale = p.discountPercent && p.discountPercent > 0;
    const img = p.imageUrl && p.imageUrl.trim() ? p.imageUrl : '/images/placeholder.png';
    return `
<div class="card product-card position-relative">
  ${hasSale ? `<span class="badge bg-danger badge-discount">-${p.discountPercent}%</span>` : ''}
  <a href="${ctx}/products/${p.id}">
    <div class="product-thumb"><img src="${img}" alt="${p.name}"></div>
  </a>
  <div class="card-body d-flex flex-column text-center">
    <h6 class="card-title mb-1"><a class="text-decoration-none text-dark" href="${ctx}/products/${p.id}">${p.name}</a></h6>
    <div>
      ${hasSale
        ? `<div class="old">${vnd(p.originalPrice)}</div><div class="price">${vnd(p.price)}</div>`
        : `<div class="price">${vnd(p.price)}</div>`}
    </div>
    <a class="btn btn-success btn-sm btn-buy mt-1 align-self-center" href="${ctx}/products/${p.id}">Đặt mua</a>
  </div>
</div>`;
  }

  async function load(catId){
    try{
      const url = new URL(ctx + '/api/catalog/products', window.location.origin);
      if (catId) url.searchParams.set('categoryId', catId);
      const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const list = await res.json();
      grid.innerHTML = list && list.length ? list.map(cardHTML).join('') : '<div class="text-muted">Không có sản phẩm.</div>';
    }catch(e){
      // Fallback: navigate full page if API not available
      if (catId) {
        window.location.href = ctx + '/products?categoryId=' + encodeURIComponent(catId);
      } else {
        window.location.href = ctx + '/products';
      }
    }
  }

  function setActive(el){
    // Sidebar list-group active
    document.querySelectorAll('.catalog-sidebar .list-group-item').forEach(a => a.classList.remove('active'));
    if (el && el.classList.contains('list-group-item')) el.classList.add('active');
    // Legacy top tabs active
    document.querySelectorAll('.catalog-tabs .btn').forEach(b => b.classList.remove('active'));
    if (el && el.classList.contains('btn')) el.classList.add('active');
  }

  filters.forEach(btn => btn.addEventListener('click', function(ev){
    ev.preventDefault();
    const cat = this.getAttribute('data-cat')||'';
    setActive(this);
    load(cat);
  }));
})();