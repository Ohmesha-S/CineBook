/**
 * CineBook - Movie Catalog Browsing & Multi-Filter Logic
 */

let allCatalogMovies = [];

document.addEventListener('DOMContentLoaded', () => {
  loadFilters();
  loadAllCatalogMovies();
  setupCatalogListeners();
});

async function loadFilters() {
  try {
    const [genresRes, langsRes] = await Promise.all([
      fetchApi('/movies/genres'),
      fetchApi('/movies/languages')
    ]);

    const genreSelect = document.getElementById('catalogGenre');
    const langSelect = document.getElementById('catalogLanguage');

    if (genreSelect && genresRes.data) {
      genresRes.data.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g;
        opt.textContent = g;
        genreSelect.appendChild(opt);
      });
    }

    if (langSelect && langsRes.data) {
      langsRes.data.forEach(l => {
        const opt = document.createElement('option');
        opt.value = l;
        opt.textContent = l;
        langSelect.appendChild(opt);
      });
    }
  } catch (err) {
    console.error('Failed to load filter options:', err);
  }
}

async function loadAllCatalogMovies() {
  const container = document.getElementById('catalogGrid');
  if (!container) return;

  container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;"><i class="fas fa-spinner fa-spin icon"></i><p>Loading full movie catalog...</p></div>`;

  try {
    const res = await fetchApi('/movies');
    allCatalogMovies = res.data || [];
    applyCatalogFilters();
  } catch (err) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;"><p>Failed to load catalog.</p></div>`;
  }
}

function setupCatalogListeners() {
  const searchInput = document.getElementById('catalogSearch');
  const genreSelect = document.getElementById('catalogGenre');
  const langSelect = document.getElementById('catalogLanguage');
  const statusSelect = document.getElementById('catalogStatus');
  const sortSelect = document.getElementById('catalogSort');

  [genreSelect, langSelect, statusSelect, sortSelect].forEach(el => {
    if (el) el.addEventListener('change', applyCatalogFilters);
  });

  if (searchInput) {
    let timer;
    searchInput.addEventListener('input', () => {
      clearTimeout(timer);
      timer = setTimeout(applyCatalogFilters, 250);
    });
  }
}

function applyCatalogFilters() {
  const search = (document.getElementById('catalogSearch')?.value || '').trim().toLowerCase();
  const genre = document.getElementById('catalogGenre')?.value || 'ALL';
  const language = document.getElementById('catalogLanguage')?.value || 'ALL';
  const status = document.getElementById('catalogStatus')?.value || 'ALL';
  const sort = document.getElementById('catalogSort')?.value || 'default';

  let filtered = [...allCatalogMovies];

  if (search) {
    filtered = filtered.filter(m => 
      m.title.toLowerCase().includes(search) ||
      m.director?.toLowerCase().includes(search) ||
      m.cast?.toLowerCase().includes(search)
    );
  }

  if (genre !== 'ALL') {
    filtered = filtered.filter(m => m.genre.toLowerCase().includes(genre.toLowerCase()));
  }

  if (language !== 'ALL') {
    filtered = filtered.filter(m => m.language.toLowerCase().includes(language.toLowerCase()));
  }

  if (status !== 'ALL') {
    filtered = filtered.filter(m => m.status === status);
  }

  // Sorting
  if (sort === 'title_asc') {
    filtered.sort((a, b) => a.title.localeCompare(b.title));
  } else if (sort === 'duration_asc') {
    filtered.sort((a, b) => a.durationMinutes - b.durationMinutes);
  } else if (sort === 'duration_desc') {
    filtered.sort((a, b) => b.durationMinutes - a.durationMinutes);
  }

  const container = document.getElementById('catalogGrid');
  const countBadge = document.getElementById('catalogCountBadge');
  if (countBadge) countBadge.textContent = `${filtered.length} Movies`;

  renderCatalogGrid(filtered, container);
}

function renderCatalogGrid(movies, container) {
  if (!movies || movies.length === 0) {
    container.innerHTML = `<div class="empty-state" style="grid-column: 1/-1;"><i class="fas fa-search icon"></i><p>No movies match your filters.</p></div>`;
    return;
  }

  const fallbackImg = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80";

  container.innerHTML = movies.map(movie => {
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
               onerror="this.onerror=null;this.src='${fallbackImg}'">
          <div class="movie-rating-badge">
            <i class="fas fa-star"></i> ${movie.rating || 'U/A'}
          </div>
          <span class="badge ${movie.status === 'NOW_SHOWING' ? 'badge-primary' : 'badge-gold'} movie-status-badge">
            ${movie.status === 'NOW_SHOWING' ? 'Now Showing' : 'Upcoming'}
          </span>
        </div>
        <div class="movie-info">
          <h3 class="movie-title" title="${movie.title}">${movie.title}</h3>
          <div class="movie-meta-text">
            <span><i class="fas fa-clock"></i> ${movie.durationMinutes}m</span>
            <span>•</span>
            <span>${movie.language}</span>
          </div>
          <div class="movie-actions">
            <a href="/movie-details.html?id=${movie.id}" 
               class="btn btn-secondary btn-sm" 
               style="flex: 1;"
               onclick="setDynamicMovieBackground('${bgImg}', '${safeTitle}', true)">
              Details
            </a>
            ${movie.status === 'NOW_SHOWING' ? `
              <a href="/theatre-selection.html?movieId=${movie.id}" 
                 class="btn btn-primary btn-sm" 
                 style="flex: 1.2;"
                 onclick="setDynamicMovieBackground('${bgImg}', '${safeTitle}', true)">
                Book Tickets
              </a>
            ` : `
              <button class="btn btn-outline btn-sm" style="flex: 1.2; opacity: 0.6;" disabled>
                Notify Me
              </button>
            `}
          </div>
        </div>
      </div>
    `;
  }).join('');
}
