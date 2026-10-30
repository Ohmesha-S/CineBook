/**
 * CineBook - Movie Details & Showtime Selection
 */

let currentMovieId = null;
let selectedDate = null;
let currentMovie = null;

document.addEventListener('DOMContentLoaded', () => {
  const params = new URLSearchParams(window.location.search);
  currentMovieId = params.get('id');

  if (!currentMovieId) {
    showToast('No movie specified. Redirecting to home...', 'error');
    setTimeout(() => window.location.href = '/index.html', 1500);
    return;
  }

  loadMovieDetails();
  loadAvailableDates();
});

async function loadMovieDetails() {
  try {
    const res = await fetchApi(`/movies/${currentMovieId}`);
    currentMovie = res.data;
    renderMovieDetails(currentMovie);
  } catch (err) {
    showToast('Failed to load movie details: ' + err.message, 'error');
  }
}

function renderMovieDetails(movie) {
  const fallbackImg = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80";
  const bgImg = movie.backdropUrl || movie.posterUrl || fallbackImg;
  
  // Set the dynamic background of the whole web to the selected movie picture
  setDynamicMovieBackground(bgImg, movie.title, true);

  const hero = document.getElementById('detailsHero');
  if (hero) {
    hero.style.backgroundImage = `url('${bgImg}')`;
  }

  document.title = `${movie.title} - CineBook`;

  const metaContainer = document.getElementById('detailsMetaContainer');
  if (metaContainer) {
    metaContainer.innerHTML = `
      <img src="${movie.posterUrl || fallbackImg}" alt="${movie.title}" class="details-poster" onerror="this.src='${fallbackImg}'">
      <div class="details-meta">
        <div class="details-badges">
          <span class="badge badge-primary">${movie.status === 'NOW_SHOWING' ? 'Now Showing' : 'Upcoming'}</span>
          <span class="badge badge-gold"><i class="fas fa-star"></i> ${movie.rating || 'U/A'}</span>
          <span class="badge badge-cyan">${movie.language}</span>
          <span class="badge" style="background: rgba(255,255,255,0.1);"><i class="fas fa-clock"></i> ${movie.durationMinutes} Minutes</span>
        </div>
        <h1 class="details-title">${movie.title}</h1>
        <div style="color: var(--accent-secondary); font-weight: 600; font-size: 1.05rem; margin-bottom: 0.75rem;">
          ${movie.genre}
        </div>
        <p class="details-synopsis">${movie.description}</p>
        <div class="details-crew">
          <div class="crew-item">
            <span>Director</span>
            <strong>${movie.director || 'N/A'}</strong>
          </div>
          <div class="crew-item">
            <span>Starring Cast</span>
            <strong>${movie.cast || 'N/A'}</strong>
          </div>
          <div class="crew-item">
            <span>Release Date</span>
            <strong>${formatDate(movie.releaseDate)}</strong>
          </div>
        </div>
      </div>
    `;
  }

  const bookBtn = document.getElementById('directBookBtn');
  if (bookBtn) {
    if (movie.status === 'NOW_SHOWING') {
      bookBtn.href = `/theatre-selection.html?movieId=${movie.id}`;
      bookBtn.style.display = 'inline-flex';
    } else {
      bookBtn.style.display = 'none';
    }
  }
}

async function loadAvailableDates() {
  const container = document.getElementById('dateRibbon');
  if (!container) return;

  try {
    const res = await fetchApi(`/shows/movie/${currentMovieId}/dates`);
    const dates = res.data || [];

    if (dates.length === 0) {
      container.innerHTML = `<p style="color: var(--text-muted); padding: 1rem 0;">No shows currently scheduled for this movie.</p>`;
      return;
    }

    selectedDate = dates[0]; // Default to first date

    container.innerHTML = dates.map((dateStr, idx) => {
      const d = new Date(dateStr);
      const dayName = d.toLocaleDateString('en-US', { weekday: 'short' });
      const monthName = d.toLocaleDateString('en-US', { month: 'short' });
      const dayNum = d.getDate();

      return `
        <div class="date-card ${idx === 0 ? 'active' : ''}" onclick="selectDate('${dateStr}', this)">
          <div class="date-day">${dayName}</div>
          <div class="date-num">${dayNum}</div>
          <div style="font-size: 0.75rem; color: var(--text-muted);">${monthName}</div>
        </div>
      `;
    }).join('');

    loadTheatresForDate(selectedDate);
  } catch (err) {
    console.error('Failed to load dates:', err);
  }
}

function selectDate(dateStr, element) {
  selectedDate = dateStr;
  document.querySelectorAll('.date-card').forEach(c => c.classList.remove('active'));
  if (element) element.classList.add('active');
  loadTheatresForDate(dateStr);
}

async function loadTheatresForDate(dateStr) {
  const container = document.getElementById('theatresListContainer');
  if (!container) return;

  container.innerHTML = `<div class="empty-state"><i class="fas fa-spinner fa-spin icon"></i><p>Finding cinema halls & showtimes...</p></div>`;

  try {
    // 1. Fetch theatres showing this movie
    const theatresRes = await fetchApi(`/theatres/movie/${currentMovieId}`);
    const theatres = theatresRes.data || [];

    if (theatres.length === 0) {
      container.innerHTML = `<div class="empty-state"><i class="fas fa-calendar-times icon"></i><p>No theatres showing this movie on selected date.</p></div>`;
      return;
    }

    // 2. For each theatre, fetch shows for this date
    let html = '';
    for (const theatre of theatres) {
      const showsRes = await fetchApi(`/shows/schedule?movieId=${currentMovieId}&theatreId=${theatre.id}&date=${dateStr}`);
      const shows = showsRes.data || [];

      if (shows.length > 0) {
        html += `
          <div class="theatre-card">
            <div class="theatre-header">
              <div>
                <h3 class="theatre-name"><i class="fas fa-film text-gradient"></i> ${theatre.name}</h3>
                <p class="theatre-address"><i class="fas fa-map-marker-alt"></i> ${theatre.address || theatre.location}</p>
              </div>
              <span class="badge badge-cyan">${theatre.location}</span>
            </div>
            <div class="showtimes-grid">
              ${shows.map(show => {
                const isPast = (new Date(`${show.showDate}T${show.startTime}`)) < new Date();
                if (isPast) {
                  return `
                    <div class="showtime-chip" style="opacity: 0.4; cursor: not-allowed; border-color: rgba(255,255,255,0.08);" title="Show has passed">
                      <span class="showtime-time">${formatTime(show.startTime)} (Passed)</span>
                      <span class="showtime-type">${show.screenName}</span>
                    </div>
                  `;
                }
                return `
                  <a href="/seat-selection.html?showId=${show.id}" class="showtime-chip" title="Click to Select Seats">
                    <span class="showtime-time">${formatTime(show.startTime)}</span>
                    <span class="showtime-type">${show.screenName}</span>
                  </a>
                `;
              }).join('')}
            </div>
          </div>
        `;
      }
    }

    if (!html) {
      container.innerHTML = `<div class="empty-state"><i class="fas fa-calendar-times icon"></i><p>No active showtimes found for ${formatDate(dateStr)}.</p></div>`;
    } else {
      container.innerHTML = html;
    }
  } catch (err) {
    container.innerHTML = `<div class="empty-state"><p>Failed to load showtimes. Please try again.</p></div>`;
  }
}
