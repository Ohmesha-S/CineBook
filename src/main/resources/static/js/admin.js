/**
 * CineBook - Admin Dashboard Operations (CRUD & Analytics)
 */

document.addEventListener('DOMContentLoaded', () => {
  // Authorization check
  const user = getUser();
  if (!user || user.role !== 'ADMIN') {
    showToast('Access denied: Administrator privileges required.', 'error');
    setTimeout(() => {
      window.location.href = '/login.html?redirect=/admin.html';
    }, 800);
    return;
  }

  loadDashboardStats();
  setupTabNavigation();
});

// --- Tab Navigation ---
function setupTabNavigation() {
  const navItems = document.querySelectorAll('.admin-nav-item');
  navItems.forEach(item => {
    item.addEventListener('click', () => {
      const tabId = item.dataset.tab;
      switchTab(tabId);
    });
  });
}

function switchTab(tabId) {
  document.querySelectorAll('.admin-nav-item').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.admin-tab-pane').forEach(el => el.classList.remove('active'));

  const activeNav = document.querySelector(`.admin-nav-item[data-tab="${tabId}"]`);
  const activePane = document.getElementById(`${tabId}Tab`);

  if (activeNav) activeNav.classList.add('active');
  if (activePane) activePane.classList.add('active');

  // Load relevant data on tab activation
  if (tabId === 'dashboard') loadDashboardStats();
  if (tabId === 'movies') loadAdminMovies();
  if (tabId === 'theatres') loadAdminTheatres();
  if (tabId === 'screens') loadAdminScreens();
  if (tabId === 'shows') loadAdminShows();
  if (tabId === 'bookings') loadAdminBookings();
}

// --- 1. Dashboard Overview & Statistics ---
async function loadDashboardStats() {
  try {
    const res = await fetchApi('/admin/dashboard');
    const s = res.data;

    document.getElementById('statTotalMovies').textContent = s.totalMovies;
    document.getElementById('statTotalTheatres').textContent = s.totalTheatres;
    document.getElementById('statTotalScreens').textContent = s.totalScreens;
    document.getElementById('statTotalShows').textContent = s.totalShows;
    document.getElementById('statTotalCustomers').textContent = s.totalCustomers;
    document.getElementById('statTotalBookings').textContent = s.totalBookings;
    document.getElementById('statConfirmedBookings').textContent = s.confirmedBookings;
    document.getElementById('statCancelledBookings').textContent = s.cancelledBookings;
    document.getElementById('statTotalRevenue').textContent = formatCurrency(s.totalRevenue);

    renderRecentBookings(s.recentBookings || []);
  } catch (err) {
    showToast('Failed to load dashboard stats: ' + err.message, 'error');
  }
}

function renderRecentBookings(bookings) {
  const tbody = document.getElementById('recentBookingsTableBody');
  if (!tbody) return;

  if (bookings.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No bookings recorded yet.</td></tr>`;
    return;
  }

  tbody.innerHTML = bookings.map(b => `
    <tr>
      <td style="font-family: monospace; font-weight: 700;">${b.bookingReference}</td>
      <td><strong>${b.customerName || 'N/A'}</strong><div style="font-size: 0.75rem; color: var(--text-muted);">${b.customerEmail || ''}</div></td>
      <td>${b.movieTitle}</td>
      <td>${b.theatreName} (${b.screenName})</td>
      <td><span class="seat-tag">${b.seatNumbers.join(', ')}</span></td>
      <td><strong>${formatCurrency(b.totalAmount)}</strong></td>
      <td>
        <span class="badge ${b.bookingStatus === 'CONFIRMED' ? 'badge-success' : (b.bookingStatus === 'CANCELLED' ? 'badge-danger' : 'badge-gold')}">
          ${b.bookingStatus}
        </span>
      </td>
    </tr>
  `).join('');
}

// --- 2. Movie Management ---
let adminMoviesList = [];

async function loadAdminMovies() {
  const tbody = document.getElementById('adminMoviesTableBody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="7" class="empty-state"><i class="fas fa-spinner fa-spin"></i> Loading movies...</td></tr>`;

  try {
    const res = await fetchApi('/admin/movies');
    adminMoviesList = res.data || [];
    renderAdminMoviesTable(adminMoviesList);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">Failed to load movies.</td></tr>`;
  }
}

function renderAdminMoviesTable(movies) {
  const tbody = document.getElementById('adminMoviesTableBody');
  if (!tbody) return;

  if (movies.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No movies found. Click "Add Movie" to create one.</td></tr>`;
    return;
  }

  const fallbackImg = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop&q=80";

  tbody.innerHTML = movies.map(m => `
    <tr>
      <td>
        <img src="${m.posterUrl || fallbackImg}" alt="${m.title}" style="width: 44px; height: 62px; object-fit: cover; border-radius: 4px;" onerror="this.src='${fallbackImg}'">
      </td>
      <td>
        <strong>${m.title}</strong>
        <div style="font-size: 0.78rem; color: var(--text-muted);">${m.director || ''}</div>
      </td>
      <td>${m.genre}</td>
      <td>${m.language}</td>
      <td>${m.durationMinutes}m</td>
      <td>
        <span class="badge ${m.status === 'NOW_SHOWING' ? 'badge-primary' : (m.status === 'UPCOMING' ? 'badge-gold' : 'badge-danger')}">
          ${m.status}
        </span>
      </td>
      <td>
        <div class="table-actions">
          <button class="btn btn-secondary btn-sm" onclick="openEditMovieModal(${m.id})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn btn-danger btn-sm" onclick="deleteMovie(${m.id})" title="Deactivate">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

function openAddMovieModal() {
  document.getElementById('movieModalTitle').textContent = 'Add New Movie';
  document.getElementById('movieForm').reset();
  document.getElementById('movieIdField').value = '';
  document.getElementById('movieModal').classList.add('active');
}

function openEditMovieModal(id) {
  const movie = adminMoviesList.find(m => m.id === id);
  if (!movie) return;

  document.getElementById('movieModalTitle').textContent = 'Edit Movie';
  document.getElementById('movieIdField').value = movie.id;
  document.getElementById('movieTitleInput').value = movie.title;
  document.getElementById('movieGenreInput').value = movie.genre;
  document.getElementById('movieLanguageInput').value = movie.language;
  document.getElementById('movieDurationInput').value = movie.durationMinutes;
  document.getElementById('movieRatingInput').value = movie.rating || '';
  document.getElementById('movieReleaseDateInput').value = movie.releaseDate || '';
  document.getElementById('movieDirectorInput').value = movie.director || '';
  document.getElementById('movieCastInput').value = movie.cast || '';
  document.getElementById('moviePosterUrlInput').value = movie.posterUrl || '';
  document.getElementById('movieBackdropUrlInput').value = movie.backdropUrl || '';
  document.getElementById('movieStatusInput').value = movie.status;
  document.getElementById('movieDescInput').value = movie.description || '';

  document.getElementById('movieModal').classList.add('active');
}

function closeMovieModal() {
  document.getElementById('movieModal').classList.remove('active');
}

async function saveMovie(e) {
  e.preventDefault();
  const id = document.getElementById('movieIdField').value;
  const movieData = {
    title: document.getElementById('movieTitleInput').value.trim(),
    genre: document.getElementById('movieGenreInput').value.trim(),
    language: document.getElementById('movieLanguageInput').value.trim(),
    durationMinutes: Number(document.getElementById('movieDurationInput').value),
    rating: document.getElementById('movieRatingInput').value.trim(),
    releaseDate: document.getElementById('movieReleaseDateInput').value || null,
    director: document.getElementById('movieDirectorInput').value.trim(),
    cast: document.getElementById('movieCastInput').value.trim(),
    posterUrl: document.getElementById('moviePosterUrlInput').value.trim(),
    backdropUrl: document.getElementById('movieBackdropUrlInput').value.trim(),
    status: document.getElementById('movieStatusInput').value,
    description: document.getElementById('movieDescInput').value.trim()
  };

  try {
    if (id) {
      await fetchApi(`/admin/movies/${id}`, {
        method: 'PUT',
        body: JSON.stringify(movieData)
      });
      showToast('Movie updated successfully!', 'success');
    } else {
      await fetchApi('/admin/movies', {
        method: 'POST',
        body: JSON.stringify(movieData)
      });
      showToast('New movie added successfully!', 'success');
    }
    closeMovieModal();
    loadAdminMovies();
  } catch (err) {
    showToast('Failed to save movie: ' + err.message, 'error');
  }
}

async function deleteMovie(id) {
  if (!confirm('Are you sure you want to deactivate this movie?')) return;
  try {
    await fetchApi(`/admin/movies/${id}`, { method: 'DELETE' });
    showToast('Movie deactivated successfully!', 'success');
    loadAdminMovies();
  } catch (err) {
    showToast('Failed to deactivate movie: ' + err.message, 'error');
  }
}

// --- 3. Theatre Management ---
let adminTheatresList = [];

async function loadAdminTheatres() {
  const tbody = document.getElementById('adminTheatresTableBody');
  if (!tbody) return;

  try {
    const res = await fetchApi('/admin/theatres');
    adminTheatresList = res.data || [];
    renderAdminTheatresTable(adminTheatresList);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Failed to load theatres.</td></tr>`;
  }
}

function renderAdminTheatresTable(theatres) {
  const tbody = document.getElementById('adminTheatresTableBody');
  if (!tbody) return;

  if (theatres.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No theatres found.</td></tr>`;
    return;
  }

  tbody.innerHTML = theatres.map(t => `
    <tr>
      <td><strong>${t.name}</strong></td>
      <td><span class="badge badge-cyan">${t.location}</span></td>
      <td>${t.address || 'N/A'}</td>
      <td>${t.contactNumber || 'N/A'}</td>
      <td>
        <span class="badge ${t.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}">${t.status}</span>
      </td>
      <td>
        <div class="table-actions">
          <button class="btn btn-secondary btn-sm" onclick="openEditTheatreModal(${t.id})">
            <i class="fas fa-edit"></i>
          </button>
          <button class="btn btn-danger btn-sm" onclick="deleteTheatre(${t.id})" title="Deactivate">
            <i class="fas fa-trash"></i>
          </button>
        </div>
      </td>
    </tr>
  `).join('');
}

function openAddTheatreModal() {
  document.getElementById('theatreModalTitle').textContent = 'Add New Theatre';
  document.getElementById('theatreForm').reset();
  document.getElementById('theatreIdField').value = '';
  document.getElementById('theatreModal').classList.add('active');
}

function openEditTheatreModal(id) {
  const t = adminTheatresList.find(item => item.id === id);
  if (!t) return;

  document.getElementById('theatreModalTitle').textContent = 'Edit Theatre';
  document.getElementById('theatreIdField').value = t.id;
  document.getElementById('theatreNameInput').value = t.name;
  document.getElementById('theatreLocationInput').value = t.location;
  document.getElementById('theatreAddressInput').value = t.address || '';
  document.getElementById('theatreContactInput').value = t.contactNumber || '';
  document.getElementById('theatreStatusInput').value = t.status;
  document.getElementById('theatreModal').classList.add('active');
}

function closeTheatreModal() {
  document.getElementById('theatreModal').classList.remove('active');
}

async function saveTheatre(e) {
  e.preventDefault();
  const id = document.getElementById('theatreIdField').value;
  const data = {
    name: document.getElementById('theatreNameInput').value.trim(),
    location: document.getElementById('theatreLocationInput').value.trim(),
    address: document.getElementById('theatreAddressInput').value.trim(),
    contactNumber: document.getElementById('theatreContactInput').value.trim(),
    status: document.getElementById('theatreStatusInput').value
  };

  try {
    if (id) {
      await fetchApi(`/admin/theatres/${id}`, {
        method: 'PUT',
        body: JSON.stringify(data)
      });
      showToast('Theatre updated successfully!', 'success');
    } else {
      await fetchApi('/admin/theatres', {
        method: 'POST',
        body: JSON.stringify(data)
      });
      showToast('Theatre created successfully!', 'success');
    }
    closeTheatreModal();
    loadAdminTheatres();
  } catch (err) {
    showToast('Failed to save theatre: ' + err.message, 'error');
  }
}

async function deleteTheatre(id) {
  if (!confirm('Are you sure you want to deactivate this theatre?')) return;
  try {
    await fetchApi(`/admin/theatres/${id}`, { method: 'DELETE' });
    showToast('Theatre deactivated successfully!', 'success');
    loadAdminTheatres();
  } catch (err) {
    showToast('Failed to delete theatre: ' + err.message, 'error');
  }
}

// --- 4. Screen Management & Seat Layouts ---
async function loadAdminScreens() {
  // Populate theatre selection dropdown
  const theatreSelect = document.getElementById('screenFilterTheatre');
  const modalTheatreSelect = document.getElementById('screenTheatreSelect');
  if (!theatreSelect) return;

  try {
    const res = await fetchApi('/admin/theatres');
    const theatres = res.data || [];

    theatreSelect.innerHTML = theatres.map(t => `<option value="${t.id}">${t.name} (${t.location})</option>`).join('');
    if (modalTheatreSelect) {
      modalTheatreSelect.innerHTML = theatres.map(t => `<option value="${t.id}">${t.name} (${t.location})</option>`).join('');
    }

    if (theatres.length > 0) {
      loadScreensForTheatre(theatres[0].id);
    }
  } catch (err) {
    console.error('Failed to load theatres for screens:', err);
  }
}

async function loadScreensForTheatre(theatreId) {
  const tbody = document.getElementById('adminScreensTableBody');
  if (!tbody) return;

  tbody.innerHTML = `<tr><td colspan="6" class="empty-state"><i class="fas fa-spinner fa-spin"></i> Loading screens...</td></tr>`;

  try {
    const res = await fetchApi(`/screens/theatre/${theatreId}`);
    const screens = res.data || [];

    if (screens.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="empty-state">No screens in this theatre. Click "Add Screen" to create one.</td></tr>`;
      return;
    }

    tbody.innerHTML = screens.map(s => `
      <tr>
        <td><strong>${s.screenName}</strong></td>
        <td>${s.screenType}</td>
        <td>${s.totalRows} Rows</td>
        <td>${s.totalSeats} Seats</td>
        <td><span class="badge badge-success">Configured</span></td>
        <td>
          <button class="btn btn-danger btn-sm" onclick="deleteScreen(${s.id})">
            <i class="fas fa-trash"></i>
          </button>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state">Failed to load screens.</td></tr>`;
  }
}

function openAddScreenModal() {
  document.getElementById('screenForm').reset();
  document.getElementById('screenModal').classList.add('active');
}

function closeScreenModal() {
  document.getElementById('screenModal').classList.remove('active');
}

async function saveScreen(e) {
  e.preventDefault();
  const data = {
    theatreId: Number(document.getElementById('screenTheatreSelect').value),
    screenName: document.getElementById('screenNameInput').value.trim(),
    totalRows: Number(document.getElementById('screenRowsInput').value),
    seatsPerRow: Number(document.getElementById('screenSeatsPerRowInput').value),
    screenType: document.getElementById('screenTypeInput').value.trim()
  };

  try {
    await fetchApi('/admin/screens', {
      method: 'POST',
      body: JSON.stringify(data)
    });
    showToast('Screen created and seat map generated in MySQL!', 'success');
    closeScreenModal();
    loadScreensForTheatre(data.theatreId);
  } catch (err) {
    showToast('Failed to create screen: ' + err.message, 'error');
  }
}

async function deleteScreen(id) {
  if (!confirm('Are you sure you want to delete this screen and its seat layout?')) return;
  try {
    await fetchApi(`/admin/screens/${id}`, { method: 'DELETE' });
    showToast('Screen deleted successfully!', 'success');
    const theatreId = document.getElementById('screenFilterTheatre').value;
    loadScreensForTheatre(theatreId);
  } catch (err) {
    showToast('Failed to delete screen: ' + err.message, 'error');
  }
}

// --- 5. Show Management ---
let adminShowsList = [];

async function loadAdminShows() {
  const tbody = document.getElementById('adminShowsTableBody');
  if (!tbody) return;

  try {
    const res = await fetchApi('/admin/shows');
    adminShowsList = res.data || [];
    renderAdminShowsTable(adminShowsList);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">Failed to load shows.</td></tr>`;
  }
}

function renderAdminShowsTable(shows) {
  const tbody = document.getElementById('adminShowsTableBody');
  if (!tbody) return;

  if (shows.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No scheduled shows found. Click "Schedule Show" to create one.</td></tr>`;
    return;
  }

  tbody.innerHTML = shows.map(s => `
    <tr>
      <td><strong>${s.movieTitle}</strong></td>
      <td>${s.theatreName}</td>
      <td>${s.screenName}</td>
      <td>${formatDate(s.showDate)}</td>
      <td>${formatTime(s.startTime)} - ${formatTime(s.endTime)}</td>
      <td>
        <span class="badge ${s.status === 'SCHEDULED' ? 'badge-primary' : (s.status === 'RUNNING' ? 'badge-success' : 'badge-danger')}">
          ${s.status}
        </span>
      </td>
      <td>
        <button class="btn btn-danger btn-sm" onclick="cancelShow(${s.id})" title="Cancel Show">
          <i class="fas fa-ban"></i> Cancel
        </button>
      </td>
    </tr>
  `).join('');
}

async function openAddShowModal() {
  document.getElementById('showForm').reset();

  // Populate movies and screens dropdowns
  const movieSelect = document.getElementById('showMovieSelect');
  const screenSelect = document.getElementById('showScreenSelect');

  try {
    const [moviesRes, theatresRes] = await Promise.all([
      fetchApi('/movies'),
      fetchApi('/admin/theatres')
    ]);

    if (movieSelect) {
      movieSelect.innerHTML = (moviesRes.data || []).map(m => `<option value="${m.id}">${m.title} (${m.language})</option>`).join('');
    }

    // Populate screens from all theatres
    if (screenSelect && theatresRes.data) {
      let screensHtml = '';
      for (const t of theatresRes.data) {
        const screensRes = await fetchApi(`/screens/theatre/${t.id}`);
        (screensRes.data || []).forEach(sc => {
          screensHtml += `<option value="${sc.id}">${t.name} - ${sc.screenName} (${sc.screenType})</option>`;
        });
      }
      screenSelect.innerHTML = screensHtml;
    }

    // Set default showDate to today
    document.getElementById('showDateInput').value = new Date().toISOString().split('T')[0];

    document.getElementById('showModal').classList.add('active');
  } catch (err) {
    showToast('Failed to prepare show scheduling form: ' + err.message, 'error');
  }
}

function closeShowModal() {
  document.getElementById('showModal').classList.remove('active');
}

async function saveShow(e) {
  e.preventDefault();
  const data = {
    movieId: Number(document.getElementById('showMovieSelect').value),
    screenId: Number(document.getElementById('showScreenSelect').value),
    showDate: document.getElementById('showDateInput').value,
    startTime: document.getElementById('showStartTimeInput').value + ':00',
    status: 'SCHEDULED',
    priceMultiplier: Number(document.getElementById('showMultiplierInput').value || 1.0)
  };

  try {
    await fetchApi('/admin/shows', {
      method: 'POST',
      body: JSON.stringify(data)
    });
    showToast('Show scheduled successfully without schedule conflicts!', 'success');
    closeShowModal();
    loadAdminShows();
  } catch (err) {
    showToast('Scheduling failed: ' + err.message, 'error');
  }
}

async function cancelShow(id) {
  if (!confirm('Are you sure you want to cancel this scheduled show?')) return;
  try {
    await fetchApi(`/admin/shows/${id}`, { method: 'DELETE' });
    showToast('Show cancelled successfully!', 'success');
    loadAdminShows();
  } catch (err) {
    showToast('Failed to cancel show: ' + err.message, 'error');
  }
}

// --- 6. Booking Monitoring ---
async function loadAdminBookings() {
  const tbody = document.getElementById('adminAllBookingsTableBody');
  if (!tbody) return;

  try {
    const res = await fetchApi('/admin/bookings');
    const bookings = res.data || [];

    if (bookings.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No bookings recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = bookings.map(b => `
      <tr>
        <td style="font-family: monospace; font-weight: 700;">${b.bookingReference}</td>
        <td><strong>${b.customerName || 'N/A'}</strong><div style="font-size: 0.75rem; color: var(--text-muted);">${b.customerEmail || ''}</div></td>
        <td>${b.movieTitle}</td>
        <td>${b.theatreName} (${b.screenName})</td>
        <td>${formatDate(b.showDate)} @ ${formatTime(b.startTime)}</td>
        <td><span class="seat-tag">${b.seatNumbers.join(', ')}</span></td>
        <td><strong>${formatCurrency(b.totalAmount)}</strong></td>
        <td>
          <span class="badge ${b.bookingStatus === 'CONFIRMED' ? 'badge-success' : (b.bookingStatus === 'CANCELLED' ? 'badge-danger' : 'badge-gold')}">
            ${b.bookingStatus}
          </span>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-state">Failed to load bookings: ${err.message}</td></tr>`;
  }
}
