/**
 * CineBook - Unified API & Global Helper Library
 */

const API_BASE_URL = '/api';
const TOKEN_KEY = 'cinebook_auth_token';
const USER_KEY = 'cinebook_user';

// --- Authentication State Management ---
function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function getUser() {
  const userJson = localStorage.getItem(USER_KEY);
  try {
    return userJson ? JSON.parse(userJson) : null;
  } catch (e) {
    return null;
  }
}

function isAuthenticated() {
  return !!getToken();
}

function isAdmin() {
  const user = getUser();
  return user && user.role === 'ADMIN';
}

function saveAuth(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

function logout() {
  clearAuth();
  showToast('Logged out successfully', 'info');
  setTimeout(() => {
    window.location.href = '/index.html';
  }, 500);
}

// --- Fetch API Wrapper ---
async function fetchApi(endpoint, options = {}) {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;
  
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {})
  };

  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers
    });

    // Handle 401 Unauthorized
    if (response.status === 401) {
      if (!endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
        clearAuth();
        showToast('Session expired. Please log in again.', 'error');
        setTimeout(() => {
          window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`;
        }, 800);
        throw new Error('Unauthorized');
      }
    }

    const data = await response.json();

    if (!response.ok) {
      const errorMsg = data.message || (data.errors ? Object.values(data.errors).join(', ') : 'Request failed');
      throw new Error(errorMsg);
    }

    return data;
  } catch (error) {
    console.error(`API Error [${endpoint}]:`, error);
    throw error;
  }
}

// --- Toast Notifications ---
function showToast(message, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  let icon = 'info-circle';
  if (type === 'success') icon = 'check-circle';
  if (type === 'error') icon = 'exclamation-circle';

  toast.innerHTML = `
    <i class="fas fa-${icon}"></i>
    <span>${message}</span>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(50px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// --- Formatters ---
function formatCurrency(amount) {
  if (amount === undefined || amount === null) return '₹0.00';
  return '₹' + Number(amount).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(dateStr) {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  return date.toLocaleDateString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });
}

function formatTime(timeStr) {
  if (!timeStr) return '';
  const parts = timeStr.split(':');
  let hour = parseInt(parts[0], 10);
  const min = parts[1];
  const ampm = hour >= 12 ? 'PM' : 'AM';
  hour = hour % 12;
  hour = hour ? hour : 12;
  return `${hour}:${min} ${ampm}`;
}

// --- Dynamic Navbar Renderer ---
function updateNavbar() {
  const navActions = document.getElementById('navActions');
  const navLinks = document.getElementById('navLinks');
  if (!navActions) return;

  const user = getUser();

  // If user is Admin, ensure Admin Dashboard link is in navLinks
  if (user && user.role === 'ADMIN') {
    if (navLinks && !document.getElementById('navAdminLink')) {
      const adminLi = document.createElement('li');
      adminLi.id = 'navAdminLink';
      adminLi.innerHTML = `
        <a href="/admin.html" class="nav-link" style="color: var(--accent-secondary);">
          <i class="fas fa-shield-alt"></i> Admin Dashboard
        </a>
      `;
      navLinks.appendChild(adminLi);
    }
  }

  if (user) {
    navActions.innerHTML = `
      <div class="user-menu">
        <button class="user-btn" id="userMenuBtn" onclick="toggleUserDropdown(event)">
          <div class="user-avatar">${user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}</div>
          <span>${user.fullName ? user.fullName.split(' ')[0] : 'User'}</span>
          <i class="fas fa-chevron-down" style="font-size: 0.75rem;"></i>
        </button>
        <div class="dropdown-menu" id="userDropdown">
          <div style="padding: 0.5rem 0.9rem;">
            <div style="font-weight: 700; font-size: 0.95rem;">${user.fullName}</div>
            <div style="font-size: 0.75rem; color: var(--text-muted);">${user.email}</div>
          </div>
          <div class="dropdown-divider"></div>
          <a href="/my-bookings.html" class="dropdown-item">
            <i class="fas fa-ticket-alt"></i> My Bookings
          </a>
          ${user.role === 'ADMIN' ? `
            <a href="/admin.html" class="dropdown-item" style="color: var(--accent-secondary);">
              <i class="fas fa-tools"></i> Admin Panel
            </a>
          ` : ''}
          <div class="dropdown-divider"></div>
          <a href="javascript:void(0)" onclick="logout()" class="dropdown-item" style="color: var(--accent-danger);">
            <i class="fas fa-sign-out-alt"></i> Logout
          </a>
        </div>
      </div>
    `;
  } else {
    navActions.innerHTML = `
      <a href="/login.html" class="btn btn-outline btn-sm">Sign In</a>
      <a href="/register.html" class="btn btn-primary btn-sm">Register</a>
    `;
  }
}

function toggleUserDropdown(e) {
  e.stopPropagation();
  const dropdown = document.getElementById('userDropdown');
  if (dropdown) {
    dropdown.classList.toggle('show');
  }
}

document.addEventListener('click', () => {
  const dropdown = document.getElementById('userDropdown');
  if (dropdown && dropdown.classList.contains('show')) {
    dropdown.classList.remove('show');
  }
});

// --- Dynamic Selected Movie Background System ---
const BG_STORAGE_KEY = 'cinebook_selected_movie_bg';
const BG_TITLE_KEY = 'cinebook_selected_movie_title';
const BG_OPACITY_KEY = 'cinebook_bg_opacity';

function ensureDynamicBackgroundContainers() {
  if (!document.body) return;

  if (!document.getElementById('movieDynamicBgBase')) {
    const base = document.createElement('div');
    base.id = 'movieDynamicBgBase';
    document.body.prepend(base);
  }

  if (!document.getElementById('movieDynamicBg')) {
    const bg = document.createElement('div');
    bg.id = 'movieDynamicBg';
    document.body.prepend(bg);
  }

  if (!document.getElementById('movieDynamicBgOverlay')) {
    const overlay = document.createElement('div');
    overlay.id = 'movieDynamicBgOverlay';
    document.body.prepend(overlay);
  }

  if (!document.getElementById('backdropControlBadge')) {
    const badge = document.createElement('div');
    badge.id = 'backdropControlBadge';
    badge.className = 'backdrop-control-badge';
    badge.title = 'Selected movie backdrop. Click to cycle opacity (50% | 68% | 85%).';
    badge.innerHTML = `
      <span class="badge-dot"></span>
      <span class="badge-movie-title" id="backdropMovieTitle">Cinema</span>
      <span class="badge-opacity-tag" id="backdropOpacityTag">68%</span>
      <span class="backdrop-btn" id="backdropCycleBtn"><i class="fas fa-sliders-h"></i></span>
    `;
    badge.addEventListener('click', cycleBackdropOpacity);
    document.body.appendChild(badge);
  }
}

function setDynamicMovieBackground(imageUrl, movieTitle = '', persist = true) {
  if (!imageUrl) return;
  ensureDynamicBackgroundContainers();

  const bgEl = document.getElementById('movieDynamicBg');
  if (bgEl) {
    bgEl.style.backgroundImage = `url('${imageUrl}')`;
  }

  if (persist) {
    sessionStorage.setItem(BG_STORAGE_KEY, imageUrl);
    if (movieTitle) {
      sessionStorage.setItem(BG_TITLE_KEY, movieTitle);
    }
  }

  updateBackdropBadge(movieTitle || sessionStorage.getItem(BG_TITLE_KEY) || 'Movie');
}

function previewDynamicMovieBackground(imageUrl, movieTitle = '') {
  if (!imageUrl) return;
  ensureDynamicBackgroundContainers();
  const bgEl = document.getElementById('movieDynamicBg');
  if (bgEl) {
    bgEl.style.backgroundImage = `url('${imageUrl}')`;
  }
  updateBackdropBadge(movieTitle ? `${movieTitle} (Preview)` : 'Preview');
}

function restoreDynamicMovieBackground() {
  const savedBg = sessionStorage.getItem(BG_STORAGE_KEY);
  const savedTitle = sessionStorage.getItem(BG_TITLE_KEY);
  if (savedBg) {
    const bgEl = document.getElementById('movieDynamicBg');
    if (bgEl) {
      bgEl.style.backgroundImage = `url('${savedBg}')`;
    }
    updateBackdropBadge(savedTitle || 'Cinema');
  }
}

function getSelectedMovieBackground() {
  return sessionStorage.getItem(BG_STORAGE_KEY);
}

function updateBackdropBadge(title) {
  const titleEl = document.getElementById('backdropMovieTitle');
  if (titleEl && title) {
    titleEl.textContent = title;
  }
  const opacity = getComputedStyle(document.documentElement).getPropertyValue('--movie-bg-opacity').trim() || '0.68';
  const tagEl = document.getElementById('backdropOpacityTag');
  if (tagEl) {
    const pct = Math.round(parseFloat(opacity) * 100);
    tagEl.textContent = `${pct}%`;
  }
}

function cycleBackdropOpacity(e) {
  if (e) e.stopPropagation();
  const currentOpacity = parseFloat(localStorage.getItem(BG_OPACITY_KEY) || '0.68');
  let newOpacity = 0.68;
  if (Math.abs(currentOpacity - 0.68) < 0.05) {
    newOpacity = 0.85; // Maximum vividness
  } else if (Math.abs(currentOpacity - 0.85) < 0.05) {
    newOpacity = 0.50; // Medium/soft
  } else {
    newOpacity = 0.68; // Rich default
  }

  document.documentElement.style.setProperty('--movie-bg-opacity', newOpacity.toString());
  localStorage.setItem(BG_OPACITY_KEY, newOpacity.toString());
  
  const tagEl = document.getElementById('backdropOpacityTag');
  if (tagEl) {
    tagEl.textContent = `${Math.round(newOpacity * 100)}%`;
  }
  showToast(`Backdrop opacity set to ${Math.round(newOpacity * 100)}%`, 'info');
}

function initDynamicBackground() {
  ensureDynamicBackgroundContainers();

  let savedOpacity = localStorage.getItem(BG_OPACITY_KEY);
  if (!savedOpacity || savedOpacity === '0.45') {
    savedOpacity = '0.68';
    localStorage.setItem(BG_OPACITY_KEY, savedOpacity);
  }
  document.documentElement.style.setProperty('--movie-bg-opacity', savedOpacity);

  const savedBg = sessionStorage.getItem(BG_STORAGE_KEY);
  const savedTitle = sessionStorage.getItem(BG_TITLE_KEY);

  if (savedBg) {
    setDynamicMovieBackground(savedBg, savedTitle || '', false);
  } else {
    updateBackdropBadge('Cinema');
  }
}

// Run navbar update and background initialization on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  updateNavbar();
  initDynamicBackground();
});
