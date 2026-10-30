/**
 * CineBook - Digital Ticket Generation & Printable View
 */

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  const bookingReference = params.get('ref');

  if (!bookingReference) {
    showToast('No booking reference provided.', 'error');
    setTimeout(() => window.location.href = '/my-bookings.html', 1500);
    return;
  }

  loadTicketDetails(bookingReference);
});

async function loadTicketDetails(reference) {
  try {
    const res = await fetchApi(`/tickets/${reference}`);
    const ticket = res.data;
    renderTicket(ticket);
  } catch (err) {
    showToast('Failed to load ticket: ' + err.message, 'error');
  }
}

function renderTicket(t) {
  const fallbackImg = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80";
  if (t.moviePoster) {
    setDynamicMovieBackground(t.moviePoster, t.movieTitle, true);
  }

  const container = document.getElementById('ticketDetailsContainer');
  if (!container) return;

  document.title = `Ticket - ${t.bookingReference} | CineBook`;

  container.innerHTML = `
    <div class="ticket-card" id="printableTicket">
      <div class="ticket-header">
        <div>
          <div style="font-family: var(--font-heading); font-weight: 800; font-size: 1.35rem; letter-spacing: -0.5px;">
            <i class="fas fa-ticket-alt"></i> Cine<span style="color: #fff;">Book</span> Boarding Pass
          </div>
          <div style="font-size: 0.8rem; opacity: 0.9; margin-top: 0.2rem;">
            E-TICKET REF: <strong>${t.bookingReference}</strong>
          </div>
        </div>
        <div style="text-align: right;">
          <span class="badge" style="background: rgba(255,255,255,0.25); color: #fff; font-size: 0.85rem;">
            ${t.bookingStatus}
          </span>
        </div>
      </div>

      <div class="ticket-cutout-left"></div>
      <div class="ticket-cutout-right"></div>

      <div class="ticket-body">
        <img src="${t.moviePoster || fallbackImg}" 
             alt="${t.movieTitle}" 
             class="ticket-poster" 
             onerror="this.src='${fallbackImg}'">

        <div>
          <h2 style="font-size: 1.6rem; margin-bottom: 0.35rem;">${t.movieTitle}</h2>
          <div style="color: var(--accent-secondary); font-size: 0.88rem; font-weight: 600; margin-bottom: 1.25rem;">
            ${t.movieLanguage} • ${t.movieGenre} • ${t.durationMinutes} mins
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; font-size: 0.88rem;">
            <div>
              <span style="color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; display: block;">Cinema Hall</span>
              <strong>${t.theatreName}</strong>
              <div style="font-size: 0.78rem; color: var(--text-muted);">${t.theatreLocation}</div>
            </div>

            <div>
              <span style="color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; display: block;">Screen</span>
              <strong>${t.screenName}</strong>
              <div style="font-size: 0.78rem; color: var(--text-muted);">${t.screenType || 'Digital'}</div>
            </div>

            <div>
              <span style="color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; display: block;">Show Date & Time</span>
              <strong style="color: var(--accent-primary); font-size: 1rem;">${formatDate(t.showDate)}</strong>
              <div>${formatTime(t.startTime)}</div>
            </div>

            <div>
              <span style="color: var(--text-muted); font-size: 0.75rem; text-transform: uppercase; display: block;">Reserved Seats</span>
              <div style="display: flex; gap: 0.35rem; flex-wrap: wrap; margin-top: 0.2rem;">
                ${t.seatNumbers.map(s => `<span class="seat-tag" style="font-size: 0.9rem;">${s}</span>`).join('')}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="ticket-footer">
        <div class="ticket-barcode">
          <div class="barcode-lines"></div>
          <span style="font-family: monospace; font-size: 0.8rem; letter-spacing: 2px; color: var(--text-muted);">
            ${t.verificationCode}
          </span>
        </div>

        <div style="text-align: right;">
          <span style="font-size: 0.75rem; color: var(--text-muted); text-transform: uppercase; display: block;">Paid Amount</span>
          <div style="font-size: 1.35rem; font-weight: 800; color: var(--text-primary);">${formatCurrency(t.totalAmount)}</div>
          <span class="badge badge-success" style="font-size: 0.7rem; margin-top: 0.2rem;">Payment: ${t.paymentStatus}</span>
        </div>
      </div>
    </div>

    <div style="display: flex; gap: 1rem; justify-content: center; margin-top: 2rem;">
      <button class="btn btn-primary" onclick="window.print()">
        <i class="fas fa-print"></i> Print Ticket
      </button>
      <a href="/my-bookings.html" class="btn btn-secondary">
        <i class="fas fa-arrow-left"></i> My Bookings
      </a>
      <a href="/index.html" class="btn btn-outline">
        <i class="fas fa-home"></i> Home
      </a>
    </div>
  `;
}
