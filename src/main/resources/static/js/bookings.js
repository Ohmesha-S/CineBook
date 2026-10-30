/**
 * CineBook - My Bookings & Cancellation Management
 */

let myBookingsList = [];
let currentFilter = 'ALL';
let bookingToCancelId = null;

document.addEventListener('DOMContentLoaded', () => {
  if (!isAuthenticated()) {
    showToast('Please sign in to view your bookings.', 'info');
    setTimeout(() => {
      window.location.href = `/login.html?redirect=/my-bookings.html`;
    }, 600);
    return;
  }

  loadCustomerBookings();
});

async function loadCustomerBookings() {
  const container = document.getElementById('myBookingsContainer');
  if (!container) return;

  container.innerHTML = `
    <div class="empty-state">
      <i class="fas fa-spinner fa-spin icon"></i>
      <p>Retrieving your movie bookings from database...</p>
    </div>
  `;

  try {
    const res = await fetchApi('/bookings/my');
    myBookingsList = res.data || [];
    filterBookings(currentFilter);
  } catch (err) {
    container.innerHTML = `
      <div class="empty-state">
        <i class="fas fa-exclamation-circle icon"></i>
        <p>Failed to load bookings: ${err.message}</p>
      </div>
    `;
  }
}

function filterBookings(status) {
  currentFilter = status;
  document.querySelectorAll('.filter-pills .pill-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.status === status);
  });

  let filtered = [...myBookingsList];
  const now = new Date();

  if (status === 'UPCOMING') {
    filtered = filtered.filter(b => {
      const showDateTime = new Date(`${b.showDate}T${b.startTime}`);
      return b.bookingStatus === 'CONFIRMED' && showDateTime > now;
    });
  } else if (status === 'COMPLETED') {
    filtered = filtered.filter(b => {
      const showDateTime = new Date(`${b.showDate}T${b.startTime}`);
      return b.bookingStatus === 'CONFIRMED' && showDateTime <= now;
    });
  } else if (status === 'CANCELLED') {
    filtered = filtered.filter(b => b.bookingStatus === 'CANCELLED');
  }

  renderBookingsList(filtered);
}

function renderBookingsList(bookings) {
  const container = document.getElementById('myBookingsContainer');
  if (!container) return;

  if (bookings.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <i class="fas fa-ticket-alt icon"></i>
        <h3>No Bookings Found</h3>
        <p style="margin-top: 0.5rem; margin-bottom: 1.5rem;">You don't have any ${currentFilter !== 'ALL' ? currentFilter.toLowerCase() : ''} bookings yet.</p>
        <a href="/index.html" class="btn btn-primary btn-sm">Explore Movies & Book</a>
      </div>
    `;
    return;
  }

  const fallbackImg = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80";

  container.innerHTML = bookings.map(b => {
    let statusBadge = `<span class="badge badge-success"><i class="fas fa-check-circle"></i> Confirmed</span>`;
    if (b.bookingStatus === 'CANCELLED') {
      statusBadge = `<span class="badge badge-danger"><i class="fas fa-ban"></i> Cancelled</span>`;
    } else if (b.bookingStatus === 'PENDING_PAYMENT') {
      statusBadge = `<span class="badge badge-gold"><i class="fas fa-clock"></i> Payment Pending</span>`;
    } else if (b.bookingStatus === 'PAYMENT_FAILED') {
      statusBadge = `<span class="badge badge-danger"><i class="fas fa-times-circle"></i> Failed</span>`;
    }

    return `
      <div class="theatre-card" style="display: flex; gap: 1.75rem; align-items: center; flex-wrap: wrap;">
        <img src="${b.moviePoster || fallbackImg}" 
             alt="${b.movieTitle}" 
             style="width: 100px; height: 140px; border-radius: var(--radius-md); object-fit: cover; box-shadow: var(--shadow-sm);"
             onerror="this.src='${fallbackImg}'">
        
        <div style="flex: 1; min-width: 240px;">
          <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.4rem;">
            <h3 style="font-size: 1.25rem;">${b.movieTitle}</h3>
            ${statusBadge}
          </div>
          <p style="color: var(--text-muted); font-size: 0.88rem; margin-bottom: 0.6rem;">
            Booking ID: <strong style="color: var(--text-primary); font-family: monospace;">${b.bookingReference}</strong>
          </p>
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 0.5rem; font-size: 0.85rem; color: var(--text-secondary);">
            <div><i class="fas fa-film text-gradient"></i> ${b.theatreName}</div>
            <div><i class="fas fa-calendar-alt"></i> ${formatDate(b.showDate)}</div>
            <div><i class="fas fa-clock"></i> ${formatTime(b.startTime)}</div>
            <div><i class="fas fa-chair"></i> Seats: <strong style="color: #fff;">${b.seatNumbers.join(', ')}</strong></div>
          </div>
        </div>

        <div style="text-align: right; min-width: 180px;">
          <div style="font-size: 1.4rem; font-weight: 800; color: var(--text-primary); margin-bottom: 0.75rem;">
            ${formatCurrency(b.totalAmount)}
          </div>
          <div style="display: flex; flex-direction: column; gap: 0.4rem;">
            ${b.bookingStatus === 'CONFIRMED' ? `
              <a href="/ticket.html?ref=${b.bookingReference}" class="btn btn-primary btn-sm">
                <i class="fas fa-ticket-alt"></i> View Digital Ticket
              </a>
            ` : ''}

            ${b.bookingStatus === 'PENDING_PAYMENT' ? `
              <a href="/payment.html?ref=${b.bookingReference}" class="btn btn-primary btn-sm">
                <i class="fas fa-credit-card"></i> Complete Payment
              </a>
            ` : ''}

            ${b.eligibleForCancellation ? `
              <button class="btn btn-danger btn-sm" onclick="openCancelModal(${b.id}, '${b.bookingReference}')">
                <i class="fas fa-times"></i> Cancel Booking
              </button>
            ` : ''}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function openCancelModal(bookingId, bookingRef) {
  bookingToCancelId = bookingId;
  const modal = document.getElementById('cancelModal');
  const refText = document.getElementById('cancelModalRef');
  if (refText) refText.textContent = bookingRef;
  if (modal) modal.classList.add('active');
}

function closeCancelModal() {
  bookingToCancelId = null;
  const modal = document.getElementById('cancelModal');
  if (modal) modal.classList.remove('active');
}

async function confirmCancellation() {
  if (!bookingToCancelId) return;

  const btn = document.getElementById('confirmCancelBtn');
  btn.disabled = true;
  btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cancelling...';

  try {
    await fetchApi(`/bookings/${bookingToCancelId}/cancel`, {
      method: 'POST'
    });

    showToast('Booking cancelled successfully! Seats are now released.', 'success');
    closeCancelModal();
    loadCustomerBookings();
  } catch (err) {
    showToast('Cancellation failed: ' + err.message, 'error');
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Yes, Cancel Booking';
  }
}
