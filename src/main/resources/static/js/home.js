/**
 * CineBook - Landing Page & Home Interactions
 */

let allNowShowingMovies = [];
let allUpcomingMovies = [];
let selectedGenre = 'ALL';

document.addEventListener('DOMContentLoaded', () => {
  loadHeroBanner();
  loadGenres();
  loadNowShowingMovies();
  loadUpcomingMovies();
  setupSearch();
});

async function loadHeroBanner() {
  try {
    const res = await fetchApi('/movies/now-showing');
    if (res.data && res.data.length > 0) {
      const movie = res.data[0]; // Hero feature
      if (!getSelectedMovieBackground()) {
        setDynamicMovieBackground(movie.backdropUrl || movie.posterUrl, movie.title, true);
      }
      const heroBanner = document.getElementById('heroBanner');
      if (heroBanner) {
        heroBanner.style.backgroundImage = `url('${movie.backdropUrl || movie.posterUrl}')`;
        heroBanner.innerHTML = `
          <div class="hero-overlay"></div>
          <div class="hero-content">
            <div class="hero-meta">
              <span class="badge badge-primary"><i class="fas fa-fire"></i> Featured Premiere</span>
              <span class="badge badge-gold"><i class="fas fa-star"></i> ${movie.rating || 'U/A'}</span>
              <span class="badge badge-cyan">${movie.language}</span>
            </div>
            <h1 class="hero-title">${movie.title}</h1>
            <p class="hero-desc">${movie.description}</p>
            <div class="hero-actions">
              <a href="/theatre-selection.html?movieId=${movie.id}" class="btn btn-primary btn-lg" onclick="setDynamicMovieBackground('${movie.backdropUrl || movie.posterUrl}', '${movie.title.replace(/'/g, "\\'")}', true)">
                <i class="fas fa-ticket-alt"></i> Book Tickets Now
              </a>
              <a href="/movie-details.html?id=${movie.id}" class="btn btn-secondary btn-lg" onclick="setDynamicMovieBackground('${movie.backdropUrl || movie.posterUrl}', '${movie.title.replace(/'/g, "\\'")}', true)">
                <i class="fas fa-info-circle"></i> View Details
              </a>
            </div>
          </div>
        `;
      }
    }
  } catch (err) {
    console.error('Failed to load hero banner:', err);
  }
}

async function loadGenres() {
  try {
    const res = await fetchApi('/movies/genres');
    const container = document.getElementById('genrePills');
    if (!container) return;

    let html = `<button class="pill-btn active" onclick="filterByGenre('ALL', this)">All Genres</button>`;
    if (res.data) {
      res.data.forEach(genre => {
        html += `<button class="pill-btn" onclick="filterByGenre('${genre}', this)">${genre}</button>`;
      });
    }
    container.innerHTML = html;
  } catch (err) {
    console.error('Failed to load genres:', err);
  }
}

async function loadNowShowingMovies() {
  const container = document.getElementById('nowShowingGrid');
  if (!container) return;

  container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;"><i class="fas fa-spinner fa-spin icon"></i><p>Loading premiering movies...</p></div>`;

  try {
    const res = await fetchApi('/movies/now-showing');
    allNowShowingMovies = res.data || [];
    renderMovieCards(allNowShowingMovies, container, true);
  } catch (err) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;"><p>Unable to load movies. Please ensure backend is running.</p></div>`;
  }
}

async function loadUpcomingMovies() {
  const container = document.getElementById('upcomingGrid');
  if (!container) return;

  try {
    const res = await fetchApi('/movies/upcoming');
    allUpcomingMovies = res.data || [];
    renderMovieCards(allUpcomingMovies, container, false);
  } catch (err) {
    console.error('Failed to load upcoming movies:', err);
  }
}

function renderMovieCards(movies, container, allowBooking) {
  if (!movies || movies.length === 0) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;"><i class="fas fa-film icon"></i><p>No movies found matching criteria.</p></div>`;
    return;
  }

  container.innerHTML = movies.map(movie => {
    const fallbackImg = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80";
    const bgImg = movie.backdropUrl || movie.posterUrl || fallbackImg;
    const safeTitle = movie.title.replace(/'/g, "\\'");
    return `
      <div class="movie-card" 
           data-movie-id="${movie.id}"
           onmouseenter="previewDynamicMovieBackground('${bgImg}', '${safeTitle}')"
           onmouseleave="restoreDynamicMovieBackground()"
           onclick="setDynamicMovieBackground('${bgImg}', '${safeTitle}', true)">
        <div class="movie-poster-wrap">
          <img src="${movie.posterUrl || fallbackImg}" 
               alt="${movie.title}" 
               class="movie-poster" 
               loading="lazy" 
               onerror="this.onerror=null;this.src='${fallbackImg}'">
          <div class="movie-rating-badge">
            <i class="fas fa-star"></i> ${movie.rating || '8.5'}
          </div>
          <span class="badge badge-primary movie-status-badge">${movie.language}</span>
        </div>
        <div class="movie-info">
          <h3 class="movie-title" title="${movie.title}">${movie.title}</h3>
          <div class="movie-meta-text">
            <span><i class="fas fa-clock"></i> ${movie.durationMinutes}m</span>
            <span>•</span>
            <span>${movie.genre.split('/')[0]}</span>
          </div>
          <div class="movie-actions">
            <a href="/movie-details.html?id=${movie.id}" 
               class="btn btn-secondary btn-sm" 
               style="flex: 1;"
               onclick="setDynamicMovieBackground('${bgImg}', '${safeTitle}', true)">
              Details
            </a>
            ${allowBooking ? `
              <a href="/theatre-selection.html?movieId=${movie.id}" 
                 class="btn btn-primary btn-sm" 
                 style="flex: 1.2;"
                 onclick="setDynamicMovieBackground('${bgImg}', '${safeTitle}', true)">
                Book Tickets
              </a>
            ` : `
              <button class="btn btn-outline btn-sm" style="flex: 1.2; opacity: 0.7;" disabled>
                Coming Soon
              </button>
            `}
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function filterByGenre(genre, btn) {
  selectedGenre = genre;
  document.querySelectorAll('#genrePills .pill-btn').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');

  const container = document.getElementById('nowShowingGrid');
  if (!container) return;

  if (genre === 'ALL') {
    renderMovieCards(allNowShowingMovies, container, true);
  } else {
    const filtered = allNowShowingMovies.filter(m => m.genre.toLowerCase().includes(genre.toLowerCase()));
    renderMovieCards(filtered, container, true);
  }
}

function setupSearch() {
  const searchInput = document.getElementById('movieSearchInput');
  if (!searchInput) return;

  let debounceTimer;
  searchInput.addEventListener('input', (e) => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      const query = e.target.value.trim().toLowerCase();
      const container = document.getElementById('nowShowingGrid');
      if (!container) return;

      if (!query) {
        renderMovieCards(allNowShowingMovies, container, true);
        return;
      }

      const filtered = allNowShowingMovies.filter(m => 
        m.title.toLowerCase().includes(query) ||
        m.genre.toLowerCase().includes(query) ||
        m.language.toLowerCase().includes(query)
      );
      renderMovieCards(filtered, container, true);
    }, 250);
  });
}
