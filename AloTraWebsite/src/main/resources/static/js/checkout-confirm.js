"use strict";
// Checkout Confirm page interactions
// Safely runs on any page; no-ops when elements are absent.
(function(){
  document.addEventListener('DOMContentLoaded', function(){
    const payBank = document.getElementById('payBank');
    const bankHint = document.getElementById('bankHint');
    const recvShip = document.getElementById('recvShip');
    const shipForm = document.getElementById('shipForm');
    const pickupHint = document.getElementById('pickupHint');
    const shipInputs = shipForm ? shipForm.querySelectorAll('input, textarea, select') : null;

    function updateUI(){
      if (bankHint && payBank) bankHint.style.display = payBank.checked ? 'block' : 'none';
      const ship = recvShip && recvShip.checked;
      if (shipForm) shipForm.style.display = ship ? 'block' : 'none';
      if (pickupHint) pickupHint.style.display = ship ? 'none' : 'block';
      if (shipInputs) shipInputs.forEach(el => { el.disabled = !ship; });
    }

    // Attach listeners only when the targets exist (NodeList may be empty elsewhere)
    document.querySelectorAll('input[name="paymentMethod"], input[name="receivingMethod"]').forEach(el => {
      el.addEventListener('change', updateUI);
    });
    updateUI();
  });
})();