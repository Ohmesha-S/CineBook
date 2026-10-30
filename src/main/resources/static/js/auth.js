/**
 * CineBook - Authentication Handler (Login & Registration)
 */

document.addEventListener('DOMContentLoaded', () => {
  const loginForm = document.getElementById('loginForm');
  const registerForm = document.getElementById('registerForm');

  if (loginForm) {
    loginForm.addEventListener('submit', handleLogin);
  }

  if (registerForm) {
    registerForm.addEventListener('submit', handleRegister);
  }
});

async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value;
  const submitBtn = document.getElementById('loginSubmitBtn');

  if (!email || !password) {
    showToast('Please enter both email and password', 'error');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Authenticating...';

  try {
    const res = await fetchApi('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    saveAuth(res.data.token, res.data.user);
    showToast('Login successful! Welcome back.', 'success');

    // Check redirect query param
    const params = new URLSearchParams(window.location.search);
    const redirect = params.get('redirect');

    setTimeout(() => {
      if (res.data.user.role === 'ADMIN' && (!redirect || redirect.includes('login'))) {
        window.location.href = '/admin.html';
      } else if (redirect) {
        window.location.href = decodeURIComponent(redirect);
      } else {
        window.location.href = '/index.html';
      }
    }, 600);
  } catch (error) {
    showToast(error.message || 'Login failed. Please check credentials.', 'error');
    submitBtn.disabled = false;
    submitBtn.innerHTML = 'Sign In <i class="fas fa-arrow-right"></i>';
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const fullName = document.getElementById('fullName').value.trim();
  const email = document.getElementById('email').value.trim();
  const phone = document.getElementById('phone').value.trim();
  const password = document.getElementById('password').value;
  const confirmPassword = document.getElementById('confirmPassword').value;
  const submitBtn = document.getElementById('registerSubmitBtn');

  if (!fullName || !email || !password) {
    showToast('Please fill in all required fields.', 'error');
    return;
  }

  if (password.length < 6) {
    showToast('Password must be at least 6 characters long.', 'error');
    return;
  }

  if (password !== confirmPassword) {
    showToast('Passwords do not match.', 'error');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Creating Account...';

  try {
    const res = await fetchApi('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, phone, password })
    });

    saveAuth(res.data.token, res.data.user);
    showToast('Registration successful! Welcome to CineBook.', 'success');

    setTimeout(() => {
      window.location.href = '/index.html';
    }, 800);
  } catch (error) {
    showToast(error.message || 'Registration failed. Email might already exist.', 'error');
    submitBtn.disabled = false;
    submitBtn.innerHTML = 'Create Account <i class="fas fa-arrow-right"></i>';
  }
}

// Quick autofill buttons for evaluation demo
function fillAdminCredentials() {
  const emailField = document.getElementById('email');
  const pwdField = document.getElementById('password');
  if (emailField) emailField.value = 'admin@cinebook.com';
  if (pwdField) pwdField.value = 'Admin@123';
  showToast('Admin credentials filled!', 'info');
}

function fillCustomerCredentials() {
  const emailField = document.getElementById('email');
  const pwdField = document.getElementById('password');
  if (emailField) emailField.value = 'customer@cinebook.com';
  if (pwdField) pwdField.value = 'Customer@123';
  showToast('Customer credentials filled!', 'info');
}

function togglePasswordVisibility(fieldId, btn) {
  const field = document.getElementById(fieldId);
  if (!field) return;
  const icon = btn.querySelector('i');
  if (field.type === 'password') {
    field.type = 'text';
    if (icon) {
      icon.classList.remove('fa-eye');
      icon.classList.add('fa-eye-slash');
    }
  } else {
    field.type = 'password';
    if (icon) {
      icon.classList.remove('fa-eye-slash');
      icon.classList.add('fa-eye');
    }
  }
}
