/**
 * CineBook - Simulated Payment Processing
 */

let bookingReference = null;
let currentBooking = null;
let selectedMethod = 'CARD';

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  bookingReference = params.get('ref');

  if (!bookingReference) {
    showToast('Missing booking reference. Redirecting...', 'error');
    setTimeout(() => window.location.href = '/index.html', 1500);
    return;
  }

  loadBookingForPayment();

  const payBtn = document.getElementById('simulatePayBtn');
  if (payBtn) {
    payBtn.addEventListener('click', handlePaymentProcess);
  }
});

async function loadBookingForPayment() {
  try {
    const res = await fetchApi(`/bookings/${bookingReference}`);
    currentBooking = res.data;
    renderPaymentSummary(currentBooking);
  } catch (err) {
    showToast('Failed to load booking: ' + err.message, 'error');
  }
}

function renderPaymentSummary(booking) {
  if (booking.moviePoster) {
    const existingBg = getSelectedMovieBackground();
    if (!existingBg || sessionStorage.getItem('cinebook_selected_movie_title') !== booking.movieTitle) {
      setDynamicMovieBackground(booking.moviePoster, booking.movieTitle, true);
    }
  }
  const refEl = document.getElementById('paymentBookingRef');
  const movieEl = document.getElementById('paymentMovieTitle');
  const detailsEl = document.getElementById('paymentShowDetails');
  const seatsEl = document.getElementById('paymentSeats');
  const amountEl = document.getElementById('paymentTotalAmount');
  const payBtnAmount = document.getElementById('payBtnAmount');

  if (refEl) refEl.textContent = booking.bookingReference;
  if (movieEl) movieEl.textContent = booking.movieTitle;
  if (detailsEl) {
    detailsEl.textContent = `${booking.theatreName} • ${formatDate(booking.showDate)} @ ${formatTime(booking.startTime)}`;
  }
  if (seatsEl) {
    seatsEl.textContent = booking.seatNumbers.join(', ');
  }
  if (amountEl) {
    amountEl.textContent = formatCurrency(booking.totalAmount);
  }
  if (payBtnAmount) {
    payBtnAmount.textContent = formatCurrency(booking.totalAmount);
  }
}

function selectPaymentMethod(method, element) {
  selectedMethod = method;
  document.querySelectorAll('.method-option').forEach(el => el.classList.remove('active'));
  if (element) element.classList.add('active');

  // Toggle visible form fields
  const cardFields = document.getElementById('cardFields');
  const upiFields = document.getElementById('upiFields');
  const netBankingFields = document.getElementById('netBankingFields');

  if (cardFields) cardFields.style.display = method === 'CARD' ? 'block' : 'none';
  if (upiFields) upiFields.style.display = method === 'UPI' ? 'block' : 'none';
  if (netBankingFields) netBankingFields.style.display = method === 'NET_BANKING' ? 'block' : 'none';
}

async function handlePaymentProcess() {
  const payBtn = document.getElementById('simulatePayBtn');
  payBtn.disabled = true;
  payBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing Payment Simulation...';

  const simulateOutcome = document.querySelector('input[name="simOutcome"]:checked')?.value || 'SUCCESS';

  try {
    const res = await fetchApi('/payments/process', {
      method: 'POST',
      body: JSON.stringify({
        bookingReference,
        paymentMethod: selectedMethod,
        simulateOutcome
      })
    });

    const payment = res.data;

    if (payment.paymentStatus === 'SUCCESS') {
      showToast('Payment completed successfully! Booking confirmed.', 'success');
      setTimeout(() => {
        window.location.href = `/confirmation.html?ref=${bookingReference}`;
      }, 1000);
    } else {
      showToast('Payment simulation failed: ' + res.message, 'error');
      payBtn.disabled = false;
      payBtn.innerHTML = `Retry Payment (${formatCurrency(currentBooking.totalAmount)}) <i class="fas fa-redo"></i>`;
      
      const statusBox = document.getElementById('paymentStatusAlert');
      if (statusBox) {
        statusBox.style.display = 'block';
        statusBox.className = 'badge badge-danger';
        statusBox.innerHTML = `<i class="fas fa-times-circle"></i> ${res.message} - Held seats have been released.`;
      }
    }
  } catch (err) {
    showToast(err.message || 'Payment processing error.', 'error');
    payBtn.disabled = false;
    payBtn.innerHTML = `Pay Now (${formatCurrency(currentBooking ? currentBooking.totalAmount : 0)}) <i class="fas fa-shield-alt"></i>`;
  }
}
