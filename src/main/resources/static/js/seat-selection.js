/**
 * CineBook - Interactive Seat Selection & Real-Time Price Calculation
 */

let currentShowId = null;
let currentShow = null;
let seatLayoutData = null;
const selectedSeats = new Map(); // seatId -> seatObject

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  currentShowId = params.get('showId');

  if (!currentShowId) {
    showToast('No show selected. Please choose a showtime.', 'error');
    setTimeout(() => window.location.href = '/index.html', 1500);
    return;
  }

  loadShowMeta();
  loadSeatLayout();

  const continueBtn = document.getElementById('proceedToPaymentBtn');
  if (continueBtn) {
    continueBtn.addEventListener('click', handleProceedToPayment);
  }
});

async function loadShowMeta() {
  try {
    const res = await fetchApi(`/shows/${currentShowId}`);
    currentShow = res.data;
    renderShowMeta(currentShow);
  } catch (err) {
    showToast('Failed to load show details: ' + err.message, 'error');
  }
}

function renderShowMeta(show) {
  if (show.moviePoster) {
    const existingBg = getSelectedMovieBackground();
    if (!existingBg || sessionStorage.getItem('cinebook_selected_movie_title') !== show.movieTitle) {
      setDynamicMovieBackground(show.moviePoster, show.movieTitle, true);
    }
  }
  const titleEl = document.getElementById('summaryMovieTitle');
  const detailsEl = document.getElementById('summaryShowDetails');
  const theatreEl = document.getElementById('summaryTheatre');

  if (titleEl) titleEl.textContent = show.movieTitle;
  if (detailsEl) {
    detailsEl.textContent = `${formatDate(show.showDate)} • ${formatTime(show.startTime)} • ${show.screenName}`;
  }
  if (theatreEl) {
    theatreEl.textContent = `${show.theatreName} (${show.theatreLocation})`;
  }
}

async function loadSeatLayout() {
  const gridContainer = document.getElementById('seatGridContainer');
  if (!gridContainer) return;

  gridContainer.innerHTML = `
    <div class="empty-state">
      <i class="fas fa-spinner fa-spin icon"></i>
      <p>Loading real-time seat layout from cinema hall...</p>
    </div>
  `;

  try {
    const res = await fetchApi(`/shows/${currentShowId}/seats`);
    seatLayoutData = res.data;
    renderSeatMap(seatLayoutData);
  } catch (err) {
    gridContainer.innerHTML = `
      <div class="empty-state">
        <i class="fas fa-exclamation-triangle icon"></i>
        <p>Failed to load seat layout. ${err.message}</p>
      </div>
    `;
  }
}

function renderSeatMap(data) {
  const gridContainer = document.getElementById('seatGridContainer');
  if (!gridContainer || !data || !data.rows) return;

  const rowsMap = data.rows;
  let html = '<div class="seat-grid">';

  for (const [rowLabel, seatsInRow] of Object.entries(rowsMap)) {
    html += `
      <div class="seat-row">
        <div class="row-label">${rowLabel}</div>
        <div style="display: flex; gap: 0.5rem; align-items: center;">
    `;

    seatsInRow.forEach(seat => {
      const isBooked = seat.status === 'BOOKED' || seat.status === 'HELD';
      const isSelected = selectedSeats.has(seat.id);
      
      let categoryClass = 'category-regular';
      if (seat.seatCategory === 'PREMIUM') categoryClass = 'category-premium';
      if (seat.seatCategory === 'RECLINER') categoryClass = 'category-recliner';

      html += `
        <button type="button" 
                class="seat-btn ${categoryClass} ${isBooked ? 'booked' : ''} ${isSelected ? 'selected' : ''}"
                data-seat-id="${seat.id}"
                data-row="${seat.rowLabel}"
                data-num="${seat.seatNumber}"
                data-price="${seat.price}"
                data-category="${seat.seatCategory}"
                ${isBooked ? 'disabled title="Seat Already Booked"' : `title="Row ${seat.rowLabel}${seat.seatNumber} - ${seat.seatCategory} (₹${seat.price})"`}
                onclick="toggleSeatSelection(${seat.id})">
          ${seat.seatNumber}
        </button>
      `;
    });

    html += `
        </div>
        <div class="row-label">${rowLabel}</div>
      </div>
    `;
  }

  html += '</div>';
  gridContainer.innerHTML = html;
  updateOrderSummary();
}

function toggleSeatSelection(seatId) {
  if (!seatLayoutData) return;

  const seatObj = seatLayoutData.seats.find(s => s.id === seatId);
  if (!seatObj || seatObj.status === 'BOOKED' || seatObj.status === 'HELD') return;

  if (selectedSeats.has(seatId)) {
    selectedSeats.delete(seatId);
  } else {
    // Max 10 seats per booking
    if (selectedSeats.size >= 10) {
      showToast('Maximum 10 seats can be selected per booking.', 'info');
      return;
    }
    selectedSeats.set(seatId, seatObj);
  }

  // Update button visual
  const btn = document.querySelector(`.seat-btn[data-seat-id="${seatId}"]`);
  if (btn) {
    btn.classList.toggle('selected', selectedSeats.has(seatId));
  }

  updateOrderSummary();
}

function updateOrderSummary() {
  const tagsContainer = document.getElementById('selectedSeatsTags');
  const countEl = document.getElementById('summarySeatCount');
  const subtotalEl = document.getElementById('summarySubtotal');
  const feeEl = document.getElementById('summaryConvenienceFee');
  const grandTotalEl = document.getElementById('summaryGrandTotal');
  const continueBtn = document.getElementById('proceedToPaymentBtn');

  if (selectedSeats.size === 0) {
    if (tagsContainer) tagsContainer.innerHTML = '<span style="color: var(--text-muted); font-size: 0.85rem;">No seats selected yet. Click seats on the map.</span>';
    if (countEl) countEl.textContent = '0 Seats';
    if (subtotalEl) subtotalEl.textContent = '₹0.00';
    if (feeEl) feeEl.textContent = '₹0.00';
    if (grandTotalEl) grandTotalEl.textContent = '₹0.00';
    if (continueBtn) continueBtn.disabled = true;
    return;
  }

  let subtotal = 0;
  const tagsHtml = [];

  for (const [id, seat] of selectedSeats.entries()) {
    subtotal += seat.price;
    tagsHtml.push(`<span class="seat-tag">${seat.rowLabel}${seat.seatNumber} (₹${seat.price})</span>`);
  }

  const convenienceFee = 20.00;
  const grandTotal = subtotal + convenienceFee;

  if (tagsContainer) tagsContainer.innerHTML = tagsHtml.join('');
  if (countEl) countEl.textContent = `${selectedSeats.size} Seat(s)`;
  if (subtotalEl) subtotalEl.textContent = formatCurrency(subtotal);
  if (feeEl) feeEl.textContent = formatCurrency(convenienceFee);
  if (grandTotalEl) grandTotalEl.textContent = formatCurrency(grandTotal);
  if (continueBtn) continueBtn.disabled = false;
}

async function handleProceedToPayment() {
  if (selectedSeats.size === 0) {
    showToast('Please select at least one seat before proceeding.', 'error');
    return;
  }

  if (!isAuthenticated()) {
    showToast('Please sign in to proceed with your booking.', 'info');
    const redirectUrl = encodeURIComponent(window.location.pathname + window.location.search);
    setTimeout(() => {
      window.location.href = `/login.html?redirect=${redirectUrl}`;
    }, 600);
    return;
  }

  const continueBtn = document.getElementById('proceedToPaymentBtn');
  continueBtn.disabled = true;
  continueBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Locking Seats...';

  try {
    const seatIds = Array.from(selectedSeats.keys());
    const res = await fetchApi('/bookings', {
      method: 'POST',
      body: JSON.stringify({
        showId: Number(currentShowId),
        seatIds
      })
    });

    showToast('Seats temporarily held! Redirecting to payment...', 'success');
    setTimeout(() => {
      window.location.href = `/payment.html?ref=${res.data.bookingReference}`;
    }, 600);
  } catch (err) {
    showToast(err.message || 'Seat reservation failed.', 'error');
    continueBtn.disabled = false;
    continueBtn.innerHTML = 'Proceed to Payment <i class="fas fa-arrow-right"></i>';
    // Refresh seat layout to sync with current availability
    loadSeatLayout();
  }
}
