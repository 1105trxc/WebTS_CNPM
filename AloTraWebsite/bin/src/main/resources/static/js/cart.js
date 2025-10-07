(function($){
  $(function(){
    var $table = $('#cartTable');
    var $checkAll = $('#checkAll');
    var $selectedTotal = $('#selectedTotal');

    function getItemChecks() {
      return $table.find('input.item-check');
    }
    function parseLineTotal($cell) {
      if ($cell.length === 0) return 0;
      var raw = $cell.attr('data-line-total');
      if (raw != null && raw !== '' && !isNaN(raw)) {
        var n = Math.floor(parseFloat(raw));
        return isNaN(n) ? 0 : n;
      }
      var text = ($cell.text() || '').replace(/[^\d]/g, '');
      var v = parseInt(text || '0', 10);
      return isNaN(v) ? 0 : v;
    }
    function renderSelectedTotal() {
      var total = 0;
      getItemChecks().filter(':checked').each(function(){
        var $row = $(this).closest('tr');
        total += parseLineTotal($row.find('.line-total'));
      });
      $selectedTotal.text(total.toLocaleString('vi-VN') + ' ₫');
    }
    function syncCheckAll() {
      var $checks = getItemChecks();
      var all = $checks.length;
      var checked = $checks.filter(':checked').length;
      $checkAll.prop('checked', all > 0 && checked === all);
      $checkAll.prop('indeterminate', checked > 0 && checked < all);
    }

    // Row checkbox change
    $table.on('change', 'input.item-check', function(){
      renderSelectedTotal();
      syncCheckAll();
    });
    // Select-all change
    $checkAll.on('change', function(){
      var on = $(this).prop('checked');
      getItemChecks().prop('checked', on);
      renderSelectedTotal();
      syncCheckAll();
    });

    // Initial render
    renderSelectedTotal();
    syncCheckAll();

    // Expose small debug to window if needed
    window._cartDebug = {
      items: getItemChecks,
      total: renderSelectedTotal,
      syncAll: syncCheckAll
    };
  });
})(jQuery);
